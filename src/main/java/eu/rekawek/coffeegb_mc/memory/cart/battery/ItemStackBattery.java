package eu.rekawek.coffeegb_mc.memory.cart.battery;

import de.tomalbrc.blockboy_arcade.component.BatterySave;
import de.tomalbrc.blockboy_arcade.component.BlockBoyComponents;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.PrimitiveIterator;
import java.util.stream.IntStream;

public class ItemStackBattery implements Battery {

    private final ItemStack saveDataStack;

    private final byte[] clockBuffer;

    private final byte[] ramBuffer;

    private boolean isClockPresent;

    private boolean isDirty;

    public ItemStackBattery(ItemStack stack, int ramSize) {
        this.saveDataStack = stack;
        this.clockBuffer = new byte[11 * 4];
        this.ramBuffer = new byte[ramSize];
    }

    @Override
    public void loadRam(int[] ram) {
        loadRamWithClock(ram, null);
    }

    @Override
    public void saveRam(int[] ram) {
        saveRamWithClock(ram, null);
    }

    @Override
    public void loadRamWithClock(int[] ram, long[] clockData) {
        if (saveDataStack.isEmpty() || !saveDataStack.has(BlockBoyComponents.BATTERY_SAVE) || saveDataStack.get(BlockBoyComponents.BATTERY_SAVE).buffer() == null) {
            return;
        }

        byte[] save = saveDataStack.get(BlockBoyComponents.BATTERY_SAVE).buffer().array();

        int ramSize = ram.length;
        int available = save.length - (save.length % 0x2000);
        int len = Math.min(ramSize, available);

        for (int i = 0; i < len; i++) {
            ram[i] = save[i] & 0xff;
        }

        if (clockData != null && save.length > len) {
            int clockBytes = clockData.length * Long.BYTES;
            int availableClock = Math.min(clockBytes, save.length - len);

            byte[] byteBuff = new byte[4 * clockData.length];
            if (availableClock >= 0) System.arraycopy(save, len, byteBuff, 0, availableClock);
            ByteBuffer clockBuf = ByteBuffer.wrap(byteBuff).order(ByteOrder.LITTLE_ENDIAN);
            int i = 0;
            while (clockBuf.hasRemaining()) {
                clockData[i++] = clockBuf.getInt();
            }
        }
    }

    @Override
    public void saveRamWithClock(int[] ram, long[] clockData) {
        doSaveRam(ram);
        if (clockData != null) {
            doSaveClock(clockData);
            isClockPresent = true;
        }
        isDirty = true;
    }

    public void flush() {
        if (!isDirty) {
            return;
        }

        int totalSize = ramBuffer.length + (isClockPresent ? clockBuffer.length : 0);
        byte[] fullSave = new byte[totalSize];

        System.arraycopy(ramBuffer, 0, fullSave, 0, ramBuffer.length);
        if (isClockPresent) {
            System.arraycopy(clockBuffer, 0, fullSave, ramBuffer.length, clockBuffer.length);
            isClockPresent = false;
        }

        this.saveDataStack.set(BlockBoyComponents.BATTERY_SAVE, new BatterySave(ByteBuffer.wrap(fullSave)));

        isDirty = false;
    }

    private void loadClock(long[] clockData, IntStream stream) {
        if (stream == null)
            return;

        PrimitiveIterator.OfInt iterator = stream.iterator();
        for (int i = 0; i < clockData.length; i++) {
            int b0 = iterator.hasNext() ? iterator.nextInt() & 0xFF : 0;
            int b1 = iterator.hasNext() ? iterator.nextInt() & 0xFF : 0;
            int b2 = iterator.hasNext() ? iterator.nextInt() & 0xFF : 0;
            int b3 = iterator.hasNext() ? iterator.nextInt() & 0xFF : 0;

            clockData[i] = ((long) b3 << 24) | ((long) b2 << 16) | ((long) b1 << 8) | b0;
        }
    }

    private void loadRam(int[] ram, IntStream is, long length) throws IOException {
        PrimitiveIterator.OfInt iterator = is.limit(length).iterator();
        for (int i = 0; i < ram.length && iterator.hasNext(); i++) {
            ram[i] = iterator.nextInt() & 0xFF;
        }
    }

    private void doSaveClock(long[] clockData) {
        ByteBuffer buff = ByteBuffer.wrap(clockBuffer);
        buff.order(ByteOrder.LITTLE_ENDIAN);
        for (long d : clockData) {
            buff.putInt((int) d);
        }
    }

    private void doSaveRam(int[] ram) {
        for (int i = 0; i < ram.length; i++) {
            ramBuffer[i] = (byte) (ram[i]);
        }
    }
}
