package eu.rekawek.coffeegb_mc.emulator;

import com.mojang.logging.LogUtils;
import eu.rekawek.coffeegb_mc.gpu.Display;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;

public class BlockBoyDisplay implements Display, Runnable {
    private final int[] pixels = new int[DISPLAY_WIDTH * DISPLAY_HEIGHT];

    public static final int[] COLORS = new int[]{0xe6f8da, 0x99c886, 0x437969, 0x051f2a};

    public static final int[] COLORS_GRAYSCALE = new int[]{0xFFFFFF, 0xAAAAAA, 0x555555, 0x000000};

    private final int[] rgb;

    private final int[] waitingFrame;

    private boolean enabled;

    private volatile boolean doStop;

    private volatile boolean isStopped;

    private boolean frameIsWaiting;

    private volatile boolean grayscale;

    private int pos;

    public BlockBoyDisplay(boolean grayscale) {
        super();

        rgb = new int[DISPLAY_WIDTH * DISPLAY_HEIGHT];
        waitingFrame = new int[rgb.length];
        this.grayscale = grayscale;

        for (int i = 0; i < DISPLAY_WIDTH * DISPLAY_HEIGHT; i++) {
            pixels[i] = 0;
        }
    }

    @Override
    public void putDmgPixel(int color) {
        rgb[pos++] = grayscale ? COLORS_GRAYSCALE[color] : COLORS[color];
        pos = pos % rgb.length;
    }

    @Override
    public void putColorPixel(int gbcRgb) {
        rgb[pos++] = Display.translateGbcRgb(gbcRgb);
    }

    @Override
    public synchronized void frameIsReady() {
        pos = 0;
        if (frameIsWaiting) {
            return;
        }
        frameIsWaiting = true;
        System.arraycopy(rgb, 0, waitingFrame, 0, rgb.length);
        notify();
    }

    @Override
    public void enableLcd() {
        enabled = true;
    }

    @Override
    public void disableLcd() {
        enabled = false;
    }

    @Override
    public void run() {
        doStop = false;
        isStopped = false;
        frameIsWaiting = false;
        enabled = true;
        pos = 0;

        while (!doStop) {
            synchronized (this) {
                if (frameIsWaiting) {
                    for (int y = 0, rowBase = 0; y < DISPLAY_HEIGHT; y++, rowBase += DISPLAY_WIDTH) {
                        int rowEnd = rowBase + DISPLAY_WIDTH - 1;
                        for (int x = 0; x < DISPLAY_WIDTH; x++) {
                            pixels[rowBase + x] = waitingFrame[rowEnd - x];
                        }
                    }
                    frameIsWaiting = false;
                } else {
                    try {
                        wait(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        isStopped = true;
        synchronized (this) {
            notifyAll();
        }
    }

    public void stop() {
        doStop = true;
        synchronized (this) {
            while (!isStopped) {
                try {
                    wait(10);
                } catch (InterruptedException e) {
                    LogUtils.getLogger().warn("Received Interruption trying to end emulation");
                }
            }
        }
    }

    public CustomModelData render(int width, int height) {
        return new CustomModelData(List.of(), List.of(), List.of(), IntArrayList.wrap(this.pixels));
    }
}
