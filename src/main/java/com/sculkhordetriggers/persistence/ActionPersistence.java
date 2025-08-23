package com.sculkhordetriggers.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class ActionPersistence {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILENAME = "sculk_horde_actions.json";
    
    private final Set<String> completedActions;
    private final Path savePath;
    
    public ActionPersistence(MinecraftServer server) {
        this.completedActions = new HashSet<>();
        this.savePath = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(FILENAME);
        load();
    }
    
    public boolean isActionCompleted(String actionId) {
        return completedActions.contains(actionId);
    }
    
    public void markActionCompleted(String actionId) {
        if (completedActions.add(actionId)) {
            save();
        }
    }
    
    private void load() {
        if (!Files.exists(savePath)) {
            return;
        }
        
        try {
            String json = Files.readString(savePath);
            Set<String> loaded = GSON.fromJson(json, new TypeToken<HashSet<String>>() {}.getType());
            if (loaded != null) {
                completedActions.clear();
                completedActions.addAll(loaded);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load completed actions", e);
        }
    }
    
    private void save() {
        try {
            Files.createDirectories(savePath.getParent());
            String json = GSON.toJson(completedActions);
            Files.writeString(savePath, json);
        } catch (IOException e) {
            LOGGER.error("Failed to save completed actions", e);
        }
    }
}
