package de.tomalbrc.blockboy_arcade.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class ModConfig {
    private static final Path CONFIG_FILE_PATH = FabricLoader.getInstance().getConfigDir().resolve("blockboy-arcade.json");
    private static ModConfig instance;
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public boolean sound = false;
    public String dateFormat = "dd.MM yyyy, HH:mm";
    public boolean ensureNoOppositeDirection = true;

    // impl

    public static ModConfig getInstance() {
        if (instance == null) {
            load();
        }
        return instance;
    }
    public static void load() {
        if (!CONFIG_FILE_PATH.toFile().exists()) {
            instance = new ModConfig();
            try {
                if (CONFIG_FILE_PATH.toFile().createNewFile()) {
                    FileOutputStream stream = new FileOutputStream(CONFIG_FILE_PATH.toFile());
                    stream.write(gson.toJson(instance).getBytes(StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        try {
            ModConfig.instance = gson.fromJson(new FileReader(ModConfig.CONFIG_FILE_PATH.toFile()), ModConfig.class);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}