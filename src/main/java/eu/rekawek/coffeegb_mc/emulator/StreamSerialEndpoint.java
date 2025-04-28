package eu.rekawek.coffeegb_mc.emulator;

import eu.rekawek.coffeegb_mc.cpu.BitUtils;
import eu.rekawek.coffeegb_mc.serial.SerialEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class StreamSerialEndpoint implements SerialEndpoint, Runnable {
    private final PipedInputStream inputStream;
    private final PipedOutputStream outputStream;
    private int localSb = 0xFF;
    private volatile int remoteSb = 0xFF;
    private AtomicInteger bitsReceived = new AtomicInteger();
    private int getBitIndex = 7;
    private volatile boolean doStop = true;

    public StreamSerialEndpoint() {
        this.inputStream = new PipedInputStream();
        this.outputStream = new PipedOutputStream();
    }

    public PipedInputStream getInputStream() {
        return inputStream;
    }

    public PipedOutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public void setSb(int sb) {
        if (localSb != sb) {
            sendCommand(Command.SET_SB, sb);
            localSb = sb;
        }
    }

    @Override
    public int recvBit() {
        if (bitsReceived.get() == 0) {
            return -1;
        }
        bitsReceived.decrementAndGet();
        return shift();
    }

    @Override
    public int recvByte() {
        if (bitsReceived.get() < 8) {
            return -1;
        }
        bitsReceived.addAndGet(-8);
        return remoteSb;
    }

    @Override
    public void startSending() {
        getBitIndex = 7;
        bitsReceived.set(0);
    }

    @Override
    public int sendBit() {
        sendCommand(Command.SEND_BIT, 1);
        return shift();
    }

    @Override
    public int sendByte() {
        sendCommand(Command.SEND_BIT, 8);
        return remoteSb;
    }

    private int shift() {
        int bit = (BitUtils.getBit(remoteSb, getBitIndex)) ? 1 : 0;
        if (--getBitIndex == -1) {
            getBitIndex = 7;
        }
        return bit;
    }

    @Override
    public void run() {
        doStop = false;

        byte[] buffer = new byte[5];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        while (!doStop) {
            try {
                if (inputStream.readNBytes(buffer, 0, 5) < 5) {
                    break;
                }
                byteBuffer.rewind();
                handlePacket(byteBuffer);
            } catch (IOException e) {
                LOG.error("Can't read the input stream", e);
                try {
                    inputStream.close();
                } catch (IOException ex) {
                    LOG.error("Could not close input stream!", e);
                    throw new RuntimeException(ex);
                }
                break;
            }
        }
    }

    public void stop() {
        doStop = true;
        try {
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            LOG.error("Error while closing input stream", e);
        }
        Thread.currentThread().interrupt();
    }

    private void sendCommand(Command command, int argument) {
        if (doStop) return;

        byte[] buffer = new byte[5];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        createPacket(byteBuffer, command, argument);
        try {
            outputStream.write(buffer);
            outputStream.flush();
        } catch (IOException e) {}
    }

    private void createPacket(ByteBuffer buffer, Command command, int argument) {
        buffer.put((byte) command.ordinal());
        buffer.putInt(argument);
    }

    private void handlePacket(ByteBuffer buffer) {
        Command command = Command.values()[buffer.get()];
        int argument = buffer.getInt();
        switch (command) {
            case SET_SB:
                remoteSb = argument;
                break;
            case SEND_BIT:
                bitsReceived.addAndGet(argument);
                break;
        }
    }

    private enum Command {
        SET_SB, SEND_BIT
    }

    private static final Logger LOG = LoggerFactory.getLogger(StreamSerialEndpoint.class);
}