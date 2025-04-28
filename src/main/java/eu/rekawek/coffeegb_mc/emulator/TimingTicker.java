package eu.rekawek.coffeegb_mc.emulator;

import eu.rekawek.coffeegb_mc.Gameboy;

public class TimingTicker implements Runnable {

    private long lastSleep = System.nanoTime();
    private long ticks = 0;
    private volatile boolean delayEnabled = true;

    public static final long PERIODS_PER_SECOND = 65536L;
    public static final long TICKS_PER_PERIOD = Gameboy.TICKS_PER_SEC / PERIODS_PER_SECOND;
    public static final long PERIOD_IN_NANOS = 1000000000L / PERIODS_PER_SECOND;

    @Override
    public void run() {
        if (++ticks < TICKS_PER_PERIOD) {
            return;
        }
        ticks = 0;
        if (delayEnabled) {
            while (System.nanoTime() - lastSleep < PERIOD_IN_NANOS) {
                // Busy wait loop (not ideal, consider alternatives)
            }
        }
        lastSleep = System.nanoTime();
    }

    public void setDelayEnabled(boolean delayEnabled) {
        this.delayEnabled = delayEnabled;
    }
}
