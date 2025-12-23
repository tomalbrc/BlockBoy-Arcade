package de.tomalbrc.blockboy_arcade.component;

import eu.pb4.polymer.core.api.other.PolymerComponent;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class BlockBoyComponents {
    public static final DataComponentType<Identifier> ROM = register(
            Identifier.fromNamespaceAndPath("blockboy", "rom"),
            builder -> builder.persistent(Identifier.CODEC)
    );

    public static final DataComponentType<BatterySave> BATTERY_SAVE = register(
            Identifier.fromNamespaceAndPath("blockboy", "battery_save"),
            builder -> builder.persistent(BatterySave.CODEC)
    );

    public static void init() {

    }

    private static <T> DataComponentType<T> register(Identifier name, java.util.function.Function<DataComponentType.Builder<T>, DataComponentType.Builder<T>> builder) {
        var component = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                name,
                builder.apply(DataComponentType.builder()).build()
        );
        PolymerComponent.registerDataComponent(component);
        return component;
    }
}