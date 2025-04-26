package de.tomalbrc.blockboy_arcade.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import eu.rekawek.coffeegb.emulator.BlockBoyDisplay;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class Assets {
    private static final double PIXEL_SIZE = 16.0/ BlockBoyDisplay.DISPLAY_WIDTH;

    public static void addToResourcePack(ResourcePackBuilder resourcePackBuilder) {
        if (true) return;

        Gson gson = new GsonBuilder().create();

        JsonObject itemsAssetModel = generateItemsAssetModel();
        byte[] assetModelBytes = gson.toJson(itemsAssetModel).getBytes(StandardCharsets.UTF_8);

        JsonObject itemModel = generateItemModel();
        byte[] itemModelBytes = gson.toJson(itemModel).getBytes(StandardCharsets.UTF_8);

        resourcePackBuilder.addData("assets/blockboy/items/screen.json", assetModelBytes);
        resourcePackBuilder.addData("assets/blockboy/models/item/screen.json", itemModelBytes);
        resourcePackBuilder.addData("assets/blockboy/textures/item/pixel.png", generateTextureData());
    }

    private static JsonObject generateItemModel() {
        JsonObject model = new JsonObject();

        JsonObject textures = new JsonObject();
        textures.addProperty("p", "blockboy:item/pixel");
        textures.addProperty("particle", "#p");
        model.add("textures", textures);

        JsonArray elements = new JsonArray();

        for (int y = 0; y < BlockBoyDisplay.DISPLAY_HEIGHT; y++) {
            for (int x = 0; x < BlockBoyDisplay.DISPLAY_WIDTH; x++) {
                float x0 = (float) (x * PIXEL_SIZE);
                float y0 = (float) (16 - (y + 1) * PIXEL_SIZE);
                float x1 = (float) (x0 + PIXEL_SIZE);
                float y1 = (float) (y0 + PIXEL_SIZE);
                float z = 8;

                JsonObject element = new JsonObject();
                element.add("from", arrayOf(x0, y0, z));
                element.add("to", arrayOf(x1, y1, z));
                element.addProperty("light_emission", 10);

                JsonObject faces = new JsonObject();
                JsonObject upFace = new JsonObject();
                upFace.addProperty("texture", "#p");
                var idx = y * BlockBoyDisplay.DISPLAY_WIDTH + x;
                upFace.addProperty("tintindex", idx);
                upFace.add("uv", createUVArray());
                faces.add("north", upFace);

                element.add("faces", faces);
                elements.add(element);
            }
        }

        model.add("elements", elements);
        return model;
    }

    private static JsonArray arrayOf(float x, float y, float z) {
        JsonArray arr = new JsonArray();
        arr.add(x);
        arr.add(y);
        arr.add(z);
        return arr;
    }

    private static JsonArray createUVArray() {
        JsonArray uv = new JsonArray();
        uv.add(0);
        uv.add(0);
        uv.add(16);
        uv.add(16);
        return uv;
    }

    private static JsonObject generateItemsAssetModel() {
        JsonObject model = new JsonObject();

        JsonObject modelObj = new JsonObject();
        modelObj.addProperty("type", "model");
        modelObj.addProperty("model", "blockboy:item/screen");

        JsonArray tints = new JsonArray();
        for (int i = 0; i < BlockBoyDisplay.DISPLAY_WIDTH * BlockBoyDisplay.DISPLAY_HEIGHT; i++) {
            JsonObject tint = new JsonObject();
            tint.addProperty("type", "custom_model_data");
            tint.addProperty("index", i);
            tint.addProperty("default", 0xFF_FF_FF);
            tints.add(tint);
        }
        modelObj.add("tints", tints);

        model.add("model", modelObj);
        return model;
    }

    public static byte[] generateTextureData() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                img.setRGB(x,y, 0xFFFFFFFF);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }
}
