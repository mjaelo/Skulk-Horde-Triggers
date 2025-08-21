package com.sculkhordetriggers.discovery;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.sculkhordetriggers.data.ItemGroup;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class DiscoveryConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("sculk-horde-triggers/config.json");
    public static final String DEFAULT_CONFIG_PATH = "/data/sculkhordetriggers/config/default_config.json";

    private static DiscoveryConfig INSTANCE;
    private final Set<String> discoveredGroupNames = new HashSet<>();
    private final List<ItemGroup> itemGroups = new ArrayList<>();

    private DiscoveryConfig() {
        loadConfig();
    }

    public static DiscoveryConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DiscoveryConfig();
        }
        return INSTANCE;
    }

    public void loadConfig() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                createConfig();
            }

            String json = Files.readString(CONFIG_PATH);
            var jsonObject = JsonParser.parseString(json).getAsJsonObject();
            
            // Load discovered item groups
            if (jsonObject.has("discovered_item_groups")) {
                discoveredGroupNames.clear();
                jsonObject.getAsJsonArray("discovered_item_groups")
                    .forEach(element -> discoveredGroupNames.add(element.getAsString()));
            }
            
            // Load item groups
            if (jsonObject.has("item_groups")) {
                itemGroups.clear();
                for (JsonElement element : jsonObject.getAsJsonArray("item_groups")) {
                    try {
                        var groupObj = element.getAsJsonObject();
                        var group = new ItemGroup(
                            groupObj.get("name").getAsString(),
                            ItemGroup.TriggerType.valueOf(groupObj.get("triggerType").getAsString().toUpperCase()),
                            groupObj.get("triggerValue").getAsString(),
                            toStringList(groupObj.getAsJsonArray("keywords")),
                            toStringList(groupObj.getAsJsonArray("namespaces"))
                        );
                        itemGroups.add(group);
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse item group: {}", element, e);
                    }
                }
            }
            
            LOGGER.info("Loaded {} item groups and {} discovered groups", itemGroups.size(), discoveredGroupNames.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load config", e);
            createConfig();
        }
    }

    private List<String> toStringList(com.google.gson.JsonArray array) {
        List<String> list = new ArrayList<>();
        array.forEach(element -> list.add(element.getAsString()));
        return list;
    }

    private void createConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (var inputStream = getClass().getResourceAsStream(DEFAULT_CONFIG_PATH)) {
                if (inputStream == null) {
                    throw new IllegalStateException("Could not find default config in resources");
                }
                String defaultConfig = new String(inputStream.readAllBytes());
                var jsonObject = JsonParser.parseString(defaultConfig).getAsJsonObject();
                Files.writeString(CONFIG_PATH, GSON.toJson(jsonObject));
                LOGGER.info("Created config file at {}", CONFIG_PATH);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create default config", e);
        }
    }
    
    public void saveConfig() {
        try {
            var config = Map.of(
                "discovered_item_groups", new ArrayList<>(discoveredGroupNames),
                "item_groups", itemGroups.stream()
                    .map(group -> Map.of(
                        "name", group.groupName(),
                        "triggerType", group.triggerType().name(),
                        "triggerValue", group.triggerValue(),
                        "keywords", group.keywords(),
                        "namespaces", group.namespaces()
                    ))
                    .collect(Collectors.toList())
            );
            
            Files.writeString(CONFIG_PATH, GSON.toJson(config));
        } catch (Exception e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    public List<ItemGroup> getItemGroups() {
        return itemGroups;
    }

    public Set<String> getDiscoveredGroupNames() {
        return discoveredGroupNames;
    }
}
