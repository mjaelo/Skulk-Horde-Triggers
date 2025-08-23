package com.sculkhordetriggers.config;

import com.google.gson.*;
import com.sculkhordetriggers.data.*;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class ModConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(TriggerType.class, (JsonDeserializer<TriggerType>) (json, type, context) -> 
                    TriggerType.valueOf(json.getAsString().toUpperCase()))
            .registerTypeAdapter(EffectType.class, (JsonDeserializer<EffectType>) (json, type, context) -> 
                    EffectType.valueOf(json.getAsString().toUpperCase()))
            .create();
    
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("sculkhorde-triggers.json");
    private static final String DEFAULT_CONFIG_RESOURCE = "data/sculkhordetriggers/config/default_config.json";
    
    private static ModConfig INSTANCE;
    private final Map<String, ActionData> actions = new HashMap<>();
    
    private ModConfig() {
        loadConfig();
    }

    public static ModConfig get() {
        if (INSTANCE == null) {
            INSTANCE = new ModConfig();
        }
        return INSTANCE;
    }

    /**
     * Get all actions
     * @return An unmodifiable map of all actions
     */
    public Map<String, ActionData> getAllActions() {
        return Collections.unmodifiableMap(actions);
    }

    private void loadConfig() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                Files.createDirectories(CONFIG_PATH.getParent());
                try (InputStream is = getClass().getClassLoader().getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
                    if (is == null) {
                        throw new IOException("Could not find default config in resources: " + DEFAULT_CONFIG_RESOURCE);
                    }
                    Files.copy(is, CONFIG_PATH);
                }
            }

            // Read and parse the config file
            String json = Files.readString(CONFIG_PATH);
            JsonObject config = JsonParser.parseString(json).getAsJsonObject();
            
            // Clear existing actions
            actions.clear();
            
            // Load each action
            for (Map.Entry<String, JsonElement> entry : config.entrySet()) {
                String actionId = entry.getKey();
                JsonObject actionObj = entry.getValue().getAsJsonObject();
                
                // Parse triggers
                List<TriggerData> triggers = parseTriggers(actionObj);
                
                // Parse effects
                List<EffectData> effects = parseEffects(actionObj);
                
                // Add the action to our map
                actions.put(actionId, new ActionData(triggers, effects));
            }
            
            LOGGER.info("Loaded {} actions from config", actions.size());
            
        } catch (Exception e) {
            LOGGER.error("Failed to load config", e);
        }
    }
    
    private List<TriggerData> parseTriggers(JsonObject actionObj) {
        List<TriggerData> triggers = new ArrayList<>();
        JsonArray triggersArray = actionObj.getAsJsonArray("triggers");
        
        for (JsonElement triggerElement : triggersArray) {
            try {
                JsonObject triggerObj = triggerElement.getAsJsonObject();
                TriggerType type = TriggerType.valueOf(triggerObj.get("type").getAsString().toUpperCase());
                float probability = triggerObj.get("probability").getAsFloat();
                List<String> keywords = new ArrayList<>();
                
                JsonArray keywordsArray = triggerObj.getAsJsonArray("keywords");
                for (JsonElement keyword : keywordsArray) {
                    keywords.add(keyword.getAsString().toLowerCase());
                }
                
                triggers.add(new TriggerData(type, probability, keywords));
            } catch (Exception e) {
                LOGGER.error("Failed to parse trigger: {}", triggerElement, e);
            }
        }
        
        return triggers;
    }
    
    private List<EffectData> parseEffects(JsonObject actionObj) {
        List<EffectData> effects = new ArrayList<>();
        JsonArray effectsArray = actionObj.getAsJsonArray("effects");
        
        for (JsonElement effectElement : effectsArray) {
            try {
                JsonObject effectObj = effectElement.getAsJsonObject();
                EffectType type = EffectType.valueOf(effectObj.get("type").getAsString().toUpperCase());
                String value = effectObj.get("value").getAsString();
                effects.add(new EffectData(type, value));
            } catch (Exception e) {
                LOGGER.error("Failed to parse effect: {}", effectElement, e);
            }
        }
        
        return effects;
    }
}
