package de.tomalbrc.blockboy_arcade.behaviour;

import de.tomalbrc.filament.api.behaviour.BehaviourType;
import de.tomalbrc.filament.api.registry.BehaviourRegistry;
import net.minecraft.resources.ResourceLocation;

public class BlockBoyBehaviours {
    public static final BehaviourType<ArcadeBehaviour, ArcadeBehaviour.Config> ARCADE = BehaviourRegistry.registerBehaviour(ResourceLocation.fromNamespaceAndPath("blockboy", "arcade"), ArcadeBehaviour.class);
    public static void init() {
    }
}
