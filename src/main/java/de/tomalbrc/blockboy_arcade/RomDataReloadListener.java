package de.tomalbrc.blockboy_arcade;

import de.tomalbrc.filament.registry.ModelRegistry;
import de.tomalbrc.filament.util.resource.FilamentSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.BiConsumer;

public class RomDataReloadListener implements FilamentSynchronousResourceReloadListener {
    @Override
    public @NonNull Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath("blockboy", "roms");
    }

    @Override
    public void onResourceManagerReload(@NonNull ResourceManager resourceManager) {
        loadRom("blockboy", "", resourceManager, (id, inputStream) -> {
            try {
                var newId = sanitize(id);
                var data = inputStream.readAllBytes();
                BlockBoyArcade.ROMS.put(newId, new RomWrapper(newId.getPath(), data));
                BlockBoyArcade.ROMS.put(ModelRegistry.sanitize(id), new RomWrapper(newId.getPath(), data));
                BlockBoyArcade.LOGGER.info("Loaded {}", newId);
            } catch (IOException e) {
                BlockBoyArcade.LOGGER.error("Failed to load block resource \"{}\".", id);
            }
        });
    }

    public static Identifier sanitize(Identifier resourceLocation) {
        String path = resourceLocation.getPath();
        String customPath = path.substring(path.contains("/") ? path.lastIndexOf('/')+1 : 0);
        return Identifier.fromNamespaceAndPath(resourceLocation.getNamespace(), customPath);
    }

    public void loadRom(@NotNull String root, @Nullable String endsWith, @NotNull ResourceManager resourceManager, @NotNull BiConsumer<Identifier, InputStream> onRead) {
        Map<Identifier, Resource> resources = resourceManager.listResources(root, (path) -> path.getPath().contains(".gb"));
        for(Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            try (InputStream inputStream = entry.getValue().open()) {
                onRead.accept(entry.getKey(), inputStream);
            } catch (IllegalStateException | IOException e) {
                FilamentSynchronousResourceReloadListener.error(entry.getKey(), e);
            }
        }
    }
}