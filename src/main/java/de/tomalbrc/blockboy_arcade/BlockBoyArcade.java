package de.tomalbrc.blockboy_arcade;

import de.tomalbrc.blockboy_arcade.behaviour.ArcadeBehaviour;
import de.tomalbrc.blockboy_arcade.behaviour.BlockBoyBehaviours;
import de.tomalbrc.blockboy_arcade.command.BlockBoyCommand;
import de.tomalbrc.blockboy_arcade.component.BlockBoyComponents;
import de.tomalbrc.blockboy_arcade.util.Assets;
import de.tomalbrc.filament.util.FilamentReloadUtil;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockBoyArcade implements ModInitializer {
    public static Map<ServerPlayer, ArcadeBehaviour> ACTIVE_SESSIONS = new ConcurrentHashMap<>();
    public static Map<Identifier, RomWrapper> ROMS = new Object2ObjectArrayMap<>();
    public static final Logger LOGGER = LogManager.getLogger("blockboy-arcade");

    @Override
    public void onInitialize() {
        PolymerResourcePackUtils.addModAssets("blockboy-arcade");
        BlockBoyComponents.init();
        BlockBoyBehaviours.init();
        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(Assets::addToResourcePack);
        ServerTickEvents.START_SERVER_TICK.register(minecraftServer -> {
            ACTIVE_SESSIONS.entrySet().removeIf(entry -> {
                var shift = entry.getKey().isShiftKeyDown();
                if (!shift)
                    entry.getValue().tick();
                else
                    entry.getValue().pauseSession(entry.getKey(), true);

                return shift;
            });
        });

        FilamentReloadUtil.registerEarlyReloadListener(new RomDataReloadListener());
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> BlockBoyCommand.register(dispatcher));
    }
}
