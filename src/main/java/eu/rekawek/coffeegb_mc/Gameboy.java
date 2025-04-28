package eu.rekawek.coffeegb_mc;

import eu.rekawek.coffeegb_mc.controller.ButtonListener;
import eu.rekawek.coffeegb_mc.controller.Joypad;
import eu.rekawek.coffeegb_mc.cpu.Cpu;
import eu.rekawek.coffeegb_mc.cpu.InterruptManager;
import eu.rekawek.coffeegb_mc.cpu.Registers;
import eu.rekawek.coffeegb_mc.cpu.SpeedMode;
import eu.rekawek.coffeegb_mc.gpu.Display;
import eu.rekawek.coffeegb_mc.gpu.Gpu;
import eu.rekawek.coffeegb_mc.memory.*;
import eu.rekawek.coffeegb_mc.memory.cart.Cartridge;
import eu.rekawek.coffeegb_mc.serial.SerialEndpoint;
import eu.rekawek.coffeegb_mc.serial.SerialPort;
import eu.rekawek.coffeegb_mc.sound.Sound;
import eu.rekawek.coffeegb_mc.sound.SoundOutput;
import eu.rekawek.coffeegb_mc.timer.Timer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Gameboy implements Runnable, Serializable {

    public static final int TICKS_PER_SEC = 4_194_304;

    private final Gpu gpu;

    private final Mmu mmu;

    private final Cpu cpu;

    private final Timer timer;

    private final Dma dma;

    private final Hdma hdma;

    private transient Display display;

    private final Sound sound;

    private final Joypad joypad;

    private final SerialPort serialPort;

    private final boolean gbc;

    private final SpeedMode speedMode;

    private transient volatile boolean doStop;

    private transient List<Runnable> tickListeners;

    private boolean requestedScreenRefresh;

    private boolean lcdDisabled;

    private transient volatile boolean doPause;

    private transient volatile boolean paused;

    public Gameboy(Cartridge rom) {
        gbc = rom.isGbc();
        speedMode = new SpeedMode();
        InterruptManager interruptManager = new InterruptManager(gbc);
        timer = new Timer(interruptManager, speedMode);
        mmu = new Mmu();

        Ram oamRam = new Ram(0xfe00, 0x00a0);
        dma = new Dma(mmu, oamRam, speedMode);
        gpu = new Gpu(interruptManager, dma, oamRam, gbc);
        hdma = new Hdma(mmu);
        sound = new Sound(gbc);
        joypad = new Joypad(interruptManager);
        serialPort = new SerialPort(interruptManager, gbc, speedMode);
        mmu.addAddressSpace(rom);
        mmu.addAddressSpace(gpu);
        mmu.addAddressSpace(joypad);
        mmu.addAddressSpace(interruptManager);
        mmu.addAddressSpace(serialPort);
        mmu.addAddressSpace(timer);
        mmu.addAddressSpace(dma);
        mmu.addAddressSpace(sound);

        mmu.addAddressSpace(new Ram(0xc000, 0x1000));
        if (gbc) {
            mmu.addAddressSpace(speedMode);
            mmu.addAddressSpace(hdma);
            mmu.addAddressSpace(new GbcRam());
            mmu.addAddressSpace(new UndocumentedGbcRegisters());
        } else {
            mmu.addAddressSpace(new Ram(0xd000, 0x1000));
        }
        mmu.addAddressSpace(new Ram(0xff80, 0x7f));
        mmu.addAddressSpace(new ShadowAddressSpace(mmu, 0xe000, 0xc000, 0x1e00));
        mmu.indexSpaces();

        cpu = new Cpu(mmu, interruptManager, gpu, speedMode);

        interruptManager.disableInterrupts(false);
        if (!rom.isUseBootstrap()) {
            initRegs();
        }
    }

    public void init(Display display, SoundOutput soundOutput, SerialEndpoint serialEndpoint) {
        this.display = display;
        this.tickListeners = new ArrayList<>();

        gpu.init(display);
        cpu.init(display);
        sound.init(soundOutput);
        serialPort.init(serialEndpoint);
    }

    private void initRegs() {
        Registers r = cpu.getRegisters();

        r.setAF(0x01b0);
        if (gbc) {
            r.setA(0x11);
        }
        r.setBC(0x0013);
        r.setDE(0x00d8);
        r.setHL(0x014d);
        r.setSP(0xfffe);
        r.setPC(0x0100);
    }

    public void run() {
        doStop = false;
        while (!doStop) {
            if (doPause) {
                haltIfNeeded();
            }
            tick();
        }
    }

    private synchronized void haltIfNeeded() {
        paused = true;
        notifyAll();
        while (doPause && !doStop) {
            try {
                wait(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        paused = false;
    }

    public synchronized void stop() {
        doStop = true;
        notifyAll();
    }

    public void tick() {
        Gpu.Mode newMode = tickSubsystems();
        if (newMode != null) {
            hdma.onGpuUpdate(newMode);
        }

        if (!lcdDisabled && !gpu.isLcdEnabled()) {
            lcdDisabled = true;
            display.frameIsReady();
            hdma.onLcdSwitch(false);
        } else if (newMode == Gpu.Mode.VBlank) {
            requestedScreenRefresh = true;
            display.frameIsReady();
        }

        if (lcdDisabled && gpu.isLcdEnabled()) {
            lcdDisabled = false;
            hdma.onLcdSwitch(true);
        } else if (requestedScreenRefresh && newMode == Gpu.Mode.OamSearch) {
            requestedScreenRefresh = false;
        }

        tickListeners.forEach(Runnable::run);
    }

    private Gpu.Mode tickSubsystems() {
        timer.tick();
        if (hdma.isTransferInProgress()) {
            hdma.tick();
        } else {
            cpu.tick();
        }
        dma.tick();
        sound.tick();
        serialPort.tick();
        return gpu.tick();
    }

    public AddressSpace getAddressSpace() {
        return mmu;
    }

    public Cpu getCpu() {
        return cpu;
    }

    public SpeedMode getSpeedMode() {
        return speedMode;
    }

    public Gpu getGpu() {
        return gpu;
    }

    public void registerTickListener(Runnable tickListener) {
        tickListeners.add(tickListener);
    }

    public void unregisterTickListener(Runnable tickListener) {
        tickListeners.remove(tickListener);
    }

    public Sound getSound() {
        return sound;
    }

    public synchronized void pause() {
        doPause = true;
        while (!paused && !doStop) {
            try {
                wait(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public synchronized void resume() {
        doPause = false;
        notifyAll();
        while (paused && !doStop) {
            try {
                wait(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public void pressedButton(ButtonListener.Button button) {
        joypad.pressedButton(button);
    }

    public void releasedButton(ButtonListener.Button button) {
        joypad.releaseButton(button);
    }
}
