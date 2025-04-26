package de.tomalbrc.blockboy_arcade.component;

import com.mojang.serialization.Codec;
import eu.pb4.polymer.core.api.other.PolymerComponent;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

import java.nio.ByteBuffer;
import java.util.stream.IntStream;

public class BlockBoyComponents {
    public static final DataComponentType<ResourceLocation> ROM = register(
            ResourceLocation.fromNamespaceAndPath("blockboy", "rom"),
            builder -> builder.persistent(ResourceLocation.CODEC)
    );

    public static final DataComponentType<IntStream> SAVE_STATE = register(
            ResourceLocation.fromNamespaceAndPath("blockboy", "save_state"),
            builder -> builder.persistent(Codec.INT_STREAM)
    );

    public static final DataComponentType<ByteBuffer> BATTERY_SAVE = register(
            ResourceLocation.fromNamespaceAndPath("blockboy", "battery_save"),
            builder -> builder.persistent(Codec.BYTE_BUFFER)
    );

    public static void init() {

    }

    private static <T> DataComponentType<T> register(ResourceLocation name, java.util.function.Function<DataComponentType.Builder<T>, DataComponentType.Builder<T>> builder) {
        var component = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                name,
                builder.apply(DataComponentType.builder()).build()
        );
        PolymerComponent.registerDataComponent(component);
        return component;
    }
}