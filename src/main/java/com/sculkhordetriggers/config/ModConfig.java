package com.sculkhordetriggers.config;

import com.google.gson.*;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    public enum TriggerType {
        BIOME, ITEM, MOB, EFFECT, ADVANCEMENT, DIMENSION
    }

    public record TriggerData(
            TriggerType type,
            float probability,
            List<String> keywords
    ) {
    }

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("sculkhorde-triggers.json");

    private static ModConfig INSTANCE;
    private String command;
    private final List<TriggerData> triggers;

    private ModConfig() {
        this.command = "/sculkhorde config trigger_automatically trigger_ancient_node_automatically true @p";
        this.triggers = new ArrayList<>();
        loadConfig();
    }

    public static ModConfig get() {
        if (INSTANCE == null) {
            INSTANCE = new ModConfig();
        }
        return INSTANCE;
    }

    public String getCommand() {
        return command;
    }

    public List<TriggerData> getTriggers(TriggerType type) {
        return triggers.stream()
                .filter(trigger -> trigger.type() == type)
                .toList();
    }

    private void loadConfig() {
        try {
            String json = Files.readString(CONFIG_PATH);
            JsonObject config = JsonParser.parseString(json).getAsJsonObject();

            // Load command
            if (config.has("command")) {
                this.command = config.get("command").getAsString();
            }

            // Load triggers
            triggers.clear();
            if (config.has("triggers")) {
                for (JsonElement element : config.getAsJsonArray("triggers")) {
                    try {
                        JsonObject triggerObj = element.getAsJsonObject();
                        TriggerType type = TriggerType.valueOf(triggerObj.get("type").getAsString());
                        float probability = triggerObj.has("probability") ?
                                triggerObj.get("probability").getAsFloat() : 1.0f;
                        List<String> keywords = new ArrayList<>();

                        if (triggerObj.has("keywords")) {
                            triggerObj.getAsJsonArray("keywords")
                                    .forEach(k -> keywords.add(k.getAsString()));
                        }

                        triggers.add(new TriggerData(type, probability, keywords));
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse trigger: {}", element, e);
                    }
                }
            }

            LOGGER.info("Loaded {} triggers from config", triggers.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load config, loading defaults", e);
            loadDefaultConfig();
            saveConfig(); // Save the loaded defaults
        }
    }

    private void saveConfig() {
        try {
            JsonObject config = new JsonObject();
            config.addProperty("command", command);

            JsonArray triggersArray = new JsonArray();
            for (TriggerData trigger : triggers) {
                JsonObject triggerObj = new JsonObject();
                triggerObj.addProperty("type", trigger.type().name());
                triggerObj.addProperty("probability", trigger.probability());

                JsonArray keywordsArray = new JsonArray();
                for (String keyword : trigger.keywords()) {
                    keywordsArray.add(keyword);
                }
                triggerObj.add("keywords", keywordsArray);

                triggersArray.add(triggerObj);
            }
            config.add("triggers", triggersArray);

            // Create parent directories if they don't exist
            Files.createDirectories(CONFIG_PATH.getParent());

            // Write to file
            Files.writeString(CONFIG_PATH, GSON.toJson(config));
        } catch (Exception e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    private void loadDefaultConfig() {
        try {
            // Load default config from resources
            String defaultConfig = new String(getClass().getClassLoader()
                    .getResourceAsStream("data/sculkhordetriggers/config/default_config.json")
                    .readAllBytes());

            JsonObject config = JsonParser.parseString(defaultConfig).getAsJsonObject();

            // Load command
            if (config.has("command")) {
                this.command = config.get("command").getAsString();
            }

            // Load triggers
            triggers.clear();
            if (config.has("item_groups")) {
                for (JsonElement element : config.getAsJsonArray("item_groups")) {
                    try {
                        JsonObject groupObj = element.getAsJsonObject();
                        JsonObject triggerObj = groupObj.getAsJsonObject("triggerConfig");

                        TriggerType type = TriggerType.valueOf(triggerObj.get("type").getAsString());
                        float probability = triggerObj.has("probability") ?
                                triggerObj.get("probability").getAsFloat() : 1.0f;
                        List<String> keywords = new ArrayList<>();

                        if (triggerObj.has("keywords")) {
                            triggerObj.getAsJsonArray("keywords")
                                    .forEach(k -> keywords.add(k.getAsString()));
                        }

                        triggers.add(new TriggerData(type, probability, keywords));
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse default trigger group: {}", element, e);
                    }
                }
            }

            LOGGER.info("Loaded default configuration with {} triggers", triggers.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load default config, using fallback", e);
            // Fallback to hardcoded defaults if loading from file fails
            triggers.clear();
            triggers.add(new TriggerData(TriggerType.BIOME, 1.0f,
                    List.of("deep_dark", "ancient_city")));
            triggers.add(new TriggerData(TriggerType.ITEM, 1.0f,
                    List.of("echo_shard", "recovery_compass")));
            triggers.add(new TriggerData(TriggerType.MOB, 1.0f,
                    List.of("warden")));
        }
    }
}
