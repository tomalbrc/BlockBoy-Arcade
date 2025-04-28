package eu.rekawek.coffeegb_mc.emulator;

import de.tomalbrc.blockboy_arcade.BlockBoyArcade;
import de.tomalbrc.blockboy_arcade.RomWrapper;
import de.tomalbrc.blockboy_arcade.config.ModConfig;
import de.tomalbrc.blockboy_arcade.voicechat.BlockBoySoundOutput;
import eu.rekawek.coffeegb_mc.CartridgeOptions;
import eu.rekawek.coffeegb_mc.Gameboy;
import eu.rekawek.coffeegb_mc.controller.ButtonListener;
import eu.rekawek.coffeegb_mc.memory.cart.Cartridge;
import eu.rekawek.coffeegb_mc.sound.SoundOutput;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;

public class EmulationController {
    private final BlockBoyDisplay display;
    private final SoundOutput sound;

    private final CartridgeOptions options;

    private final RomWrapper currentRom;

    private Cartridge cart;

    private Gameboy gameboy;

    private boolean isRunning;

    private final Cartridge.GameboyType type;

    private ServerPlayer player;
    private final ItemStack cartridgeItem;

    private final StreamSerialEndpoint streamSerial = new StreamSerialEndpoint();

    private Thread serialThread;

    private ServerPlayer linkedPlayer = null;

    public EmulationController(CartridgeOptions options, RomWrapper initialRom, ServerPlayer player, ItemStack cartridgeItem) {
        this.options = options;
        this.currentRom = initialRom;

        this.cartridgeItem = cartridgeItem;
        this.type = Cartridge.GameboyType.AUTOMATIC;
        this.display = new BlockBoyDisplay(false);
        this.sound = FabricLoader.getInstance().isModLoaded("voicechat") && ModConfig.getInstance().sound ? new BlockBoySoundOutput(player) : SoundOutput.NULL_OUTPUT;
        this.player = player;
    }

    public BlockBoyDisplay getDisplay() {
        return this.display;
    }

    public void unlink() throws IOException {
        boolean wasAlive = serialThread.isAlive();
        if (wasAlive) {
            serialThread.interrupt();
            streamSerial.stop();

            if (linkedPlayer != null && BlockBoyArcade.ACTIVE_SESSIONS.containsKey(linkedPlayer)) {
                var controller = BlockBoyArcade.ACTIVE_SESSIONS.get(linkedPlayer).getController();
                if (controller != null) controller.unlink();
            }
        }
    }

    public void link(EmulationController friend) throws IOException {
        this.streamSerial.getInputStream().connect(friend.streamSerial.getOutputStream());
        this.streamSerial.getOutputStream().connect(friend.streamSerial.getInputStream());

        this.linkedPlayer = friend.player;

        friend.serialThread = new Thread(this.streamSerial);
        this.serialThread = new Thread(friend.streamSerial);

        friend.serialThread.start();
        this.serialThread.start();
    }


    public void startEmulation() {
        Cartridge newCart;
        try {
            newCart = loadRom(currentRom, cartridgeItem);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        stopEmulation();
        cart = newCart;
        gameboy = new Gameboy(cart);
        gameboy.init(display, sound, streamSerial);
        gameboy.registerTickListener(new TimingTicker());

        new Thread(display).start();
        if (sound != SoundOutput.NULL_OUTPUT) new Thread((BlockBoySoundOutput)sound).start(); // TODO: add sounds? using noteblocks? doesn't seem feasible
        new Thread(gameboy).start();
        isRunning = true;
    }

    public void stopEmulation() {
        if (!isRunning) {
            return;
        }
        isRunning = false;
        if (gameboy != null) {
            gameboy.stop();
            gameboy = null;
        }
        if (cart != null) {
            cart.flushBattery();
            cart = null;
        }

        streamSerial.stop();
        display.stop();
        sound.stop();
    }

    public void pause() {
        this.gameboy.pause();
    }

    public void resume() {
        this.gameboy.resume();
    }

    private Cartridge loadRom(RomWrapper rom, ItemStack cartridgeItem) throws IOException {
        return new Cartridge(rom, cartridgeItem, options.isSupportBatterySaves(), type, options.isUsingBootstrap());
    }

    public void pressed(ButtonListener.Button button) {
        this.gameboy.pressedButton(button);
    }

    public void released(ButtonListener.Button button) {
        this.gameboy.releasedButton(button);
    }

    public void setPlayer(ServerPlayer player) {
        this.player = player;
    }
}