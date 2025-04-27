package de.tomalbrc.blockboy_arcade.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.tomalbrc.blockboy_arcade.config.ModConfig;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Consumer;

public class BatterySave implements TooltipProvider {
    static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(ModConfig.getInstance().dateFormat)
            .withZone(ZoneId.systemDefault());

    private ByteBuffer buffer;
    private Optional<Long> timestamp;

    public static final Codec<BatterySave> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            Codec.BYTE_BUFFER.fieldOf("data").forGetter(BatterySave::buffer),
            Codec.LONG.optionalFieldOf("timestamp").forGetter(BatterySave::timestamp)
    ).apply(instance, BatterySave::new));

    public BatterySave(ByteBuffer buffer, Optional<Long> timestamp) {
        this.buffer = buffer;
        this.timestamp = timestamp;
    }

    public BatterySave(ByteBuffer buffer) {
        this(buffer, Optional.of(System.currentTimeMillis() / 1000L));
    }

    public boolean hasData() {
        return this.buffer.hasArray() && this.buffer.array().length > 0;
    }

    public ByteBuffer buffer() {
        return this.buffer;
    }

    public Optional<Long> timestamp() {
        return this.timestamp;
    }

    @Override
    public void addToTooltip(Item.TooltipContext tooltipContext, Consumer<Component> consumer, TooltipFlag tooltipFlag, DataComponentGetter dataComponentGetter) {
        if (buffer.hasArray() && buffer.array().length > 0) {
            consumer.accept(Component.literal("Contains " + buffer.array().length + " bytes save data"));
            consumer.accept(Component.literal("Last save: " + FORMATTER.format(Instant.ofEpochSecond(timestamp.orElse(0L)))));
        }
    }
}
