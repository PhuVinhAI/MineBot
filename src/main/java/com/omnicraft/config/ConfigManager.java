package com.omnicraft.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("omnicraft_secrets.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static SecretConfig currentConfig = new SecretConfig();

    public static void init() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                currentConfig = GSON.fromJson(reader, SecretConfig.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            save();
        }
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(currentConfig, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SecretConfig getConfig() {
        return currentConfig;
    }

    public static class SecretConfig {
        public String apiKey = "";
        public String baseUrl = "https://api.openai.com/v1/";
        public String model = "gpt-4o";
    }
}