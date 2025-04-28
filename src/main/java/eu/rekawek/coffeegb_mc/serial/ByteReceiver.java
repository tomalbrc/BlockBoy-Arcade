package eu.rekawek.coffeegb_mc.serial;

public interface ByteReceiver {
    void onNewByte(int receivedByte);
}
