package com.pandora.spawners.utility;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pandora.spawners.PandoraSpawners;
import com.pandora.spawners.spawner.DropLimitManager;
import com.pandora.spawners.utility.reflect.Reflect.RF;

/**
 * Handles saving and loading drop limit tracking data to/from JSON files.
 */
public final class DropLimitStorage {
    
    private static final File DATA_FILE = new File(PandoraSpawners.instance().getDataFolder(), "drop-limits.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Saves all drop limit tracking data to JSON file.
     */
    public static void saveAll() {
        try {
            // Access the dropTrackers map
            Map<String, com.pandora.spawners.spawner.DropLimitManager.SpawnerDropTracker> dropTrackers = 
                com.pandora.spawners.spawner.DropLimitManager.dropTrackers;
            
            if (dropTrackers == null || dropTrackers.isEmpty()) {
                // No data to save, delete file if exists
                if (DATA_FILE.exists()) {
                    DATA_FILE.delete();
                }
                return;
            }
            
            JsonObject root = new JsonObject();
            
            for (Map.Entry<String, com.pandora.spawners.spawner.DropLimitManager.SpawnerDropTracker> entry : dropTrackers.entrySet()) {
                String spawnerKey = entry.getKey();
                com.pandora.spawners.spawner.DropLimitManager.SpawnerDropTracker tracker = entry.getValue();
                
                if (tracker == null) continue;
                
                JsonObject trackerData = new JsonObject();
                
                // Use reflection to get the internal maps
                try {
                    java.lang.reflect.Field shulkerField = tracker.getClass().getDeclaredField("shulkerShells");
                    shulkerField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Map<Long, Integer> shulkerShells = (Map<Long, Integer>) shulkerField.get(tracker);
                    
                    java.lang.reflect.Field netherStarField = tracker.getClass().getDeclaredField("netherStars");
                    netherStarField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Map<Long, Integer> netherStars = (Map<Long, Integer>) netherStarField.get(tracker);
                    
                    java.lang.reflect.Field totemField = tracker.getClass().getDeclaredField("totems");
                    totemField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Map<Long, Integer> totems = (Map<Long, Integer>) totemField.get(tracker);
                    
                    // Save shulker shells
                    if (shulkerShells != null && !shulkerShells.isEmpty()) {
                        JsonArray array = new JsonArray();
                        for (Map.Entry<Long, Integer> e : shulkerShells.entrySet()) {
                            JsonObject item = new JsonObject();
                            item.addProperty("timestamp", e.getKey());
                            item.addProperty("count", e.getValue());
                            array.add(item);
                        }
                        trackerData.add("shulkerShells", array);
                    }
                    
                    // Save nether stars
                    if (netherStars != null && !netherStars.isEmpty()) {
                        JsonArray array = new JsonArray();
                        for (Map.Entry<Long, Integer> e : netherStars.entrySet()) {
                            JsonObject item = new JsonObject();
                            item.addProperty("timestamp", e.getKey());
                            item.addProperty("count", e.getValue());
                            array.add(item);
                        }
                        trackerData.add("netherStars", array);
                    }
                    
                    // Save totems
                    if (totems != null && !totems.isEmpty()) {
                        JsonArray array = new JsonArray();
                        for (Map.Entry<Long, Integer> e : totems.entrySet()) {
                            JsonObject item = new JsonObject();
                            item.addProperty("timestamp", e.getKey());
                            item.addProperty("count", e.getValue());
                            array.add(item);
                        }
                        trackerData.add("totems", array);
                    }
                    
                    root.add(spawnerKey, trackerData);
                } catch (Exception e) {
                    RF.debug(e);
                }
            }
            
            try (FileWriter writer = new FileWriter(DATA_FILE)) {
                GSON.toJson(root, writer);
            }
            
            PandoraSpawners.instance().getLogger().info("Saved drop limit data for " + root.size() + " spawner(s)");
        } catch (Exception e) {
            RF.debug(e);
            PandoraSpawners.instance().getLogger().severe("Failed to save drop limit data: " + e.getMessage());
        }
    }
    
    /**
     * Loads all drop limit tracking data from JSON file.
     */
    public static void loadAll() {
        try {
            if (!DATA_FILE.exists() || !DATA_FILE.isFile()) {
                return;
            }
            
            try (FileReader reader = new FileReader(DATA_FILE)) {
                JsonElement element = JsonParser.parseReader(reader);
                if (!element.isJsonObject()) {
                    return;
                }
                
                JsonObject root = element.getAsJsonObject();
                
                // Access the dropTrackers map
                Map<String, com.pandora.spawners.spawner.DropLimitManager.SpawnerDropTracker> dropTrackers = 
                    com.pandora.spawners.spawner.DropLimitManager.dropTrackers;
                
                int loadedCount = 0;
                
                for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                    String spawnerKey = entry.getKey();
                    JsonObject trackerData = entry.getValue().getAsJsonObject();
                    
                    try {
                        // Create a new tracker instance
                        com.pandora.spawners.spawner.DropLimitManager.SpawnerDropTracker tracker = 
                            new com.pandora.spawners.spawner.DropLimitManager.SpawnerDropTracker();
                        
                        // Load shulker shells
                        if (trackerData.has("shulkerShells")) {
                            JsonArray array = trackerData.get("shulkerShells").getAsJsonArray();
                            java.lang.reflect.Field shulkerField = tracker.getClass().getDeclaredField("shulkerShells");
                            shulkerField.setAccessible(true);
                            @SuppressWarnings("unchecked")
                            Map<Long, Integer> shulkerShells = (Map<Long, Integer>) shulkerField.get(tracker);
                            
                            for (JsonElement elem : array) {
                                JsonObject item = elem.getAsJsonObject();
                                long timestamp = item.get("timestamp").getAsLong();
                                int count = item.get("count").getAsInt();
                                shulkerShells.put(timestamp, count);
                            }
                        }
                        
                        // Load nether stars
                        if (trackerData.has("netherStars")) {
                            JsonArray array = trackerData.get("netherStars").getAsJsonArray();
                            java.lang.reflect.Field netherStarField = tracker.getClass().getDeclaredField("netherStars");
                            netherStarField.setAccessible(true);
                            @SuppressWarnings("unchecked")
                            Map<Long, Integer> netherStars = (Map<Long, Integer>) netherStarField.get(tracker);
                            
                            for (JsonElement elem : array) {
                                JsonObject item = elem.getAsJsonObject();
                                long timestamp = item.get("timestamp").getAsLong();
                                int count = item.get("count").getAsInt();
                                netherStars.put(timestamp, count);
                            }
                        }
                        
                        // Load totems
                        if (trackerData.has("totems")) {
                            JsonArray array = trackerData.get("totems").getAsJsonArray();
                            java.lang.reflect.Field totemField = tracker.getClass().getDeclaredField("totems");
                            totemField.setAccessible(true);
                            @SuppressWarnings("unchecked")
                            Map<Long, Integer> totems = (Map<Long, Integer>) totemField.get(tracker);
                            
                            for (JsonElement elem : array) {
                                JsonObject item = elem.getAsJsonObject();
                                long timestamp = item.get("timestamp").getAsLong();
                                int count = item.get("count").getAsInt();
                                totems.put(timestamp, count);
                            }
                        }
                        
                        dropTrackers.put(spawnerKey, tracker);
                        loadedCount++;
                    } catch (Exception e) {
                        RF.debug(e);
                    }
                }
                
                if (loadedCount > 0) {
                    PandoraSpawners.instance().getLogger().info("Loaded drop limit data for " + loadedCount + " spawner(s)");
                }
            }
        } catch (Exception e) {
            RF.debug(e);
            PandoraSpawners.instance().getLogger().severe("Failed to load drop limit data: " + e.getMessage());
        }
    }
}

