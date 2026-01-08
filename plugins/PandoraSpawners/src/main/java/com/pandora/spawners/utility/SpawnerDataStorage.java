package com.pandora.spawners.utility;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pandora.spawners.PandoraSpawners;
import com.pandora.spawners.api.spawner.IGenerator;
import com.pandora.spawners.api.spawner.ISpawner;
import com.pandora.spawners.spawner.generator.GeneratorRegistry;
import com.pandora.spawners.spawner.type.SpawnerType;
import com.pandora.spawners.utility.reflect.Reflect.RF;

/**
 * Handles saving and loading all spawner data to/from JSON files.
 * Saves spawner type, upgrade levels, charges, spawnable amount, owner, enabled state, etc.
 */
public final class SpawnerDataStorage {
    
    private static final File DATA_DIR = new File(PandoraSpawners.instance().getDataFolder(), "spawner-data");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    static {
        DATA_DIR.mkdirs();
    }
    
    /**
     * Saves all spawner data to JSON files organized by world.
     */
    public static void saveAll() {
        try {
            // Clear old data
            if (DATA_DIR.exists()) {
                File[] files = DATA_DIR.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && file.getName().endsWith(".json")) {
                            file.delete();
                        }
                    }
                }
            }
            
            // Save spawners by world
            Map<World, JsonArray> worldData = new HashMap<>();
            
            for (World world : Bukkit.getWorlds()) {
                if (com.pandora.spawners.configuration.Settings.inactive(world)) continue;
                
                JsonArray spawnersArray = new JsonArray();
                
                java.util.List<IGenerator> generators = GeneratorRegistry.list(world);
                for (IGenerator generator : generators) {
                    if (generator == null || !generator.active()) continue;
                    
                    Block block = generator.block();
                    if (block == null || block.getType() != Material.SPAWNER) continue;
                    
                    ISpawner spawner = generator.spawner();
                    if (spawner == null) continue;
                    
                    JsonObject spawnerData = new JsonObject();
                    spawnerData.addProperty("x", block.getX());
                    spawnerData.addProperty("y", block.getY());
                    spawnerData.addProperty("z", block.getZ());
                    
                    SpawnerType type = spawner.getType();
                    spawnerData.addProperty("type", type != null ? type.name() : "PIG");
                    
                    int[] levels = spawner.getUpgradeLevels();
                    JsonArray levelsArray = new JsonArray();
                    if (levels != null && levels.length >= 3) {
                        levelsArray.add(levels[0]);
                        levelsArray.add(levels[1]);
                        levelsArray.add(levels[2]);
                    } else {
                        levelsArray.add(1);
                        levelsArray.add(1);
                        levelsArray.add(1);
                    }
                    spawnerData.add("upgradeLevels", levelsArray);
                    
                    spawnerData.addProperty("charges", spawner.getCharges());
                    spawnerData.addProperty("spawnable", spawner.getSpawnable());
                    spawnerData.addProperty("stack", spawner.getStack());
                    spawnerData.addProperty("empty", spawner.isEmpty());
                    spawnerData.addProperty("enabled", spawner.isEnabled());
                    
                    UUID owner = spawner.getOwnerID();
                    if (owner != null) {
                        spawnerData.addProperty("owner", owner.toString());
                    }
                    
                    spawnersArray.add(spawnerData);
                }
                
                if (spawnersArray.size() > 0) {
                    worldData.put(world, spawnersArray);
                }
            }
            
            // Write to files
            for (Map.Entry<World, JsonArray> entry : worldData.entrySet()) {
                File worldFile = new File(DATA_DIR, entry.getKey().getName() + ".json");
                try (FileWriter writer = new FileWriter(worldFile)) {
                    GSON.toJson(entry.getValue(), writer);
                } catch (IOException e) {
                    RF.debug(e);
                }
            }
            
            PandoraSpawners.instance().getLogger().info("Saved spawner data for " + worldData.size() + " world(s)");
        } catch (Exception e) {
            RF.debug(e);
            PandoraSpawners.instance().getLogger().severe("Failed to save spawner data: " + e.getMessage());
        }
    }
    
    /**
     * Loads all spawner data from JSON files and applies it to spawners.
     */
    public static void loadAll() {
        try {
            if (!DATA_DIR.exists() || !DATA_DIR.isDirectory()) {
                return;
            }
            
            File[] files = DATA_DIR.listFiles((dir, name) -> name.endsWith(".json"));
            if (files == null || files.length == 0) {
                return;
            }
            
            int loadedCount = 0;
            
            for (File file : files) {
                String worldName = file.getName().replace(".json", "");
                World world = Bukkit.getWorld(worldName);
                if (world == null || com.pandora.spawners.configuration.Settings.inactive(world)) {
                    continue;
                }
                
                try (FileReader reader = new FileReader(file)) {
                    JsonElement element = JsonParser.parseReader(reader);
                    if (!element.isJsonArray()) continue;
                    
                    JsonArray spawnersArray = element.getAsJsonArray();
                    
                    for (JsonElement spawnerElement : spawnersArray) {
                        if (!spawnerElement.isJsonObject()) continue;
                        
                        JsonObject spawnerData = spawnerElement.getAsJsonObject();
                        
                        int x = spawnerData.get("x").getAsInt();
                        int y = spawnerData.get("y").getAsInt();
                        int z = spawnerData.get("z").getAsInt();
                        
                        Location loc = new Location(world, x, y, z);
                        Block block = loc.getBlock();
                        
                        if (block.getType() != Material.SPAWNER) {
                            continue;
                        }
                        
                        // Wait for chunk to load if needed
                        if (!block.getChunk().isLoaded()) {
                            block.getChunk().load();
                        }
                        
                        // Apply spawner data
                        try {
                            String typeName = spawnerData.has("type") ? spawnerData.get("type").getAsString() : "PIG";
                            SpawnerType type = SpawnerType.of(typeName);
                            if (type == null) type = SpawnerType.PIG;
                            
                            int[] levels = new int[]{1, 1, 1};
                            if (spawnerData.has("upgradeLevels") && spawnerData.get("upgradeLevels").isJsonArray()) {
                                JsonArray levelsArray = spawnerData.get("upgradeLevels").getAsJsonArray();
                                if (levelsArray.size() >= 3) {
                                    levels[0] = levelsArray.get(0).getAsInt();
                                    levels[1] = levelsArray.get(1).getAsInt();
                                    levels[2] = levelsArray.get(2).getAsInt();
                                }
                            }
                            
                            int charges = spawnerData.has("charges") ? spawnerData.get("charges").getAsInt() : 0;
                            int spawnable = spawnerData.has("spawnable") ? spawnerData.get("spawnable").getAsInt() : -1;
                            int stack = spawnerData.has("stack") ? spawnerData.get("stack").getAsInt() : 1;
                            boolean empty = spawnerData.has("empty") && spawnerData.get("empty").getAsBoolean();
                            boolean enabled = !spawnerData.has("enabled") || spawnerData.get("enabled").getAsBoolean();
                            
                            // Apply data using DataManager
                            DataManager.setType(block, type);
                            DataManager.setUpgradeLevels(block, levels);
                            DataManager.setCharges(block, charges);
                            if (spawnable > 0) {
                                DataManager.setSpawnable(block, spawnable);
                            }
                            DataManager.setStack(block, stack);
                            if (empty) {
                                DataManager.setEmpty(block);
                            }
                            DataManager.setEnabled(block, enabled);
                            
                            if (spawnerData.has("owner")) {
                                try {
                                    UUID owner = UUID.fromString(spawnerData.get("owner").getAsString());
                                    DataManager.setOwner(block, owner);
                                } catch (Exception e) {
                                    // Invalid UUID, skip
                                }
                            }
                            
                            DataManager.updateValues(block);
                            
                            // Register with GeneratorRegistry
                            GeneratorRegistry.put(block);
                            
                            loadedCount++;
                        } catch (Exception e) {
                            RF.debug(e);
                        }
                    }
                } catch (Exception e) {
                    RF.debug(e);
                }
            }
            
            if (loadedCount > 0) {
                PandoraSpawners.instance().getLogger().info("Loaded " + loadedCount + " spawner(s) from saved data");
            }
        } catch (Exception e) {
            RF.debug(e);
            PandoraSpawners.instance().getLogger().severe("Failed to load spawner data: " + e.getMessage());
        }
    }
}

