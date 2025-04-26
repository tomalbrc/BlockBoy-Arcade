package de.tomalbrc.blockboy_arcade.voicechat;

import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import eu.rekawek.coffeegb.Gameboy;
import eu.rekawek.coffeegb.sound.SoundOutput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.UUID;

public class BlockBoySoundOutput implements SoundOutput, Runnable {
    private static final int SAMPLE_RATE = 24000;

    private static final int BUFFER_SIZE = 960;

    private static final int DIVIDER = (Gameboy.TICKS_PER_SEC / SAMPLE_RATE);

    private static final long NANOS_IN_SEC = 1_000_000_000L;

    private static final long BUFFER_LENGTH_NANOS = (BUFFER_SIZE / 2) * NANOS_IN_SEC / SAMPLE_RATE;

    private short[] buffer = new short[BUFFER_SIZE];

    private short[] lockedBuffer = new short[BUFFER_SIZE];

    private final short[] finalBuffer = new short[BUFFER_SIZE];

    private volatile boolean enabled = true;

    private volatile int pos;

    private int tick;

    private volatile boolean doStop;

    private volatile boolean isPlaying;

    private volatile long writeStart;

    private StaticAudioChannel channel;

    private final ServerLevel level;
    private final ServerPlayer player;
    private AudioPlayer audioPlayer;

    public BlockBoySoundOutput(ServerPlayer player) {
        this.level = player.serverLevel();
        this.player = player;
    }

    @Override
    public void start() {
        if (SVCPlugin.API.getConnectionOf(this.player.getUUID()) == null || this.isPlaying) {
            return;
        }

        if (this.channel == null) {
            this.channel = SVCPlugin.API.createStaticAudioChannel(UUID.randomUUID(), SVCPlugin.API.fromServerLevel(level), SVCPlugin.API.getConnectionOf(this.player.getUUID()));
        }

        if (this.audioPlayer == null) {
            this.audioPlayer = SVCPlugin.API.createAudioPlayer(this.channel, SVCPlugin.API.createEncoder(), ()-> doStop ? null : finalBuffer);
        }

        this.audioPlayer.startPlaying();
        this.isPlaying = true;
    }

    public static short convert8BitTo16Bit(byte input) {
        return (short) ((input & 0xFF - 128) << 8);
    }

    @Override
    public void stop() {
        if (this.audioPlayer != null) this.audioPlayer.stopPlaying();
        this.isPlaying = false;
        this.doStop = true;
    }

    @Override
    public void play(int left, int right) {
        if (tick++ != 0) {
            tick %= DIVIDER;
            return;
        }

        while (pos >= BUFFER_SIZE) {
        }

        buffer[pos] = convert8BitTo16Bit((byte) left);
        buffer[pos + 1] = convert8BitTo16Bit((byte) right);
        pos += 2;
    }

    @Override
    public void run() {
        pos = 0;
        tick = 0;
        doStop = false;
        while (pos < BUFFER_SIZE && !doStop) {
        }

        while (!doStop) {
            while (pos < BUFFER_SIZE && !doStop) {
                if (System.nanoTime() - writeStart > BUFFER_LENGTH_NANOS * 1.1f) {
                    break;
                }
            }
            if (doStop) {
                break;
            }
            int localPos = pos;
            short[] tmp = lockedBuffer;
            lockedBuffer = buffer;
            buffer = tmp;
            pos = 0;
            if (!isPlaying || !enabled) {
                Arrays.fill(finalBuffer, (short) 0);
            } else {
                fill(lockedBuffer, localPos, finalBuffer);
            }

            writeStart = System.nanoTime();
        }
    }

    private static void fill(short[] src, int srcLen, short[] dst) {
        for (int i = 0; i < dst.length; i++) {
            if (srcLen == 0) {
                dst[i] = 0;
            } else if (i >= srcLen) {
                dst[i] = src[srcLen - 1];
            } else {
                dst[i] = src[i];
            }
        }
    }
}