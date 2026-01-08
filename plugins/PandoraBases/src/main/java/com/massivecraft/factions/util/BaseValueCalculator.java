package com.massivecraft.factions.util;

import com.massivecraft.factions.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calculates the value of everything inside a base cube
 */
public class BaseValueCalculator {
    
    private static final Map<String, Double> blockValues = new ConcurrentHashMap<>();
    private static final Map<String, Double> itemValues = new ConcurrentHashMap<>();
    private static final Map<String, Double> spawnerValues = new ConcurrentHashMap<>();
    private static double defaultBlockValue = 0.0;
    private static double defaultItemValue = 0.0;
    private static double defaultSpawnerValue = 100.0;
    
    /**
     * Load values from config
     */
    public static void loadConfig(FileConfiguration config) {
        blockValues.clear();
        itemValues.clear();
        spawnerValues.clear();
        
        // Load block values
        if (config.contains("base-values.blocks")) {
            for (String key : config.getConfigurationSection("base-values.blocks").getKeys(false)) {
                blockValues.put(key.toUpperCase(), config.getDouble("base-values.blocks." + key, 0.0));
            }
        }
        
        // Load item values
        if (config.contains("base-values.items")) {
            for (String key : config.getConfigurationSection("base-values.items").getKeys(false)) {
                itemValues.put(key.toUpperCase(), config.getDouble("base-values.items." + key, 0.0));
            }
        }
        
        // Load spawner values
        if (config.contains("base-values.spawners")) {
            for (String key : config.getConfigurationSection("base-values.spawners").getKeys(false)) {
                spawnerValues.put(key.toUpperCase(), config.getDouble("base-values.spawners." + key, 100.0));
            }
        }
        
        defaultBlockValue = config.getDouble("base-values.default-block-value", 0.0);
        defaultItemValue = config.getDouble("base-values.default-item-value", 0.0);
        defaultSpawnerValue = config.getDouble("base-values.default-spawner-value", 100.0);
    }
    
    /**
     * Calculate the total value of a base
     */
    public static double calculateBaseValue(Faction faction) {
        if (faction == null || !faction.isNormal() || !faction.hasBeacon()) {
            return 0.0;
        }
        
        Location beaconLoc = faction.getBeaconLocation();
        if (beaconLoc == null) {
            return 0.0;
        }
        
        int[] bounds = BaseStructureHelper.getBaseCubeBounds(faction);
        if (bounds == null) {
            return 0.0;
        }
        
        int minX = bounds[0];
        int maxX = bounds[1];
        int minZ = bounds[2];
        int maxZ = bounds[3];
        int floorY = bounds[4];
        int maxY = bounds[5];
        
        World world = beaconLoc.getWorld();
        if (world == null) {
            return 0.0;
        }
        
        double totalValue = 0.0;
        
        // First, scan all blocks in the base cube
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = floorY; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block == null || block.getType() == Material.AIR) {
                        continue;
                    }
                    
                    // Check for spawners
                    if (block.getType() == Material.SPAWNER) {
                        CreatureSpawner spawner = (CreatureSpawner) block.getState();
                        String entityType = spawner.getSpawnedType() != null ? 
                            spawner.getSpawnedType().name() : "UNKNOWN";
                        totalValue += getSpawnerValue(entityType);
                        continue;
                    }
                    
                    // Check for containers and their contents (chests, barrels, shulker boxes, etc.)
                    BlockState state = block.getState();
                    if (state instanceof InventoryHolder) {
                        InventoryHolder container = (InventoryHolder) state;
                        if (container.getInventory() != null) {
                            for (ItemStack item : container.getInventory().getContents()) {
                                if (item != null && item.getType() != Material.AIR) {
                                    totalValue += getItemValue(item);
                                }
                            }
                        }
                    }
                    
                    // Add block value
                    totalValue += getBlockValue(block.getType());
                }
            }
        }
        
        // Now scan for item frames in the base cube (entities, not blocks)
        // Item frames can contain items that have value
        try {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof ItemFrame)) {
                    continue;
                }
                
                Location entityLoc = entity.getLocation();
                if (entityLoc.getWorld() != world) {
                    continue;
                }
                
                // Check if entity is within base cube bounds
                if (entityLoc.getBlockX() < minX || entityLoc.getBlockX() > maxX ||
                    entityLoc.getBlockY() < floorY || entityLoc.getBlockY() > maxY ||
                    entityLoc.getBlockZ() < minZ || entityLoc.getBlockZ() > maxZ) {
                    continue;
                }
                
                // Check if item frame is in this faction's territory
                FLocation entityFloc = FLocation.wrap(entityLoc);
                Faction entityFaction = Board.getInstance().getFactionAt(entityFloc);
                if (entityFaction != faction) {
                    continue; // Item frame is not in this faction's territory
                }
                
                // Get item from item frame
                ItemFrame itemFrame = (ItemFrame) entity;
                ItemStack frameItem = itemFrame.getItem();
                if (frameItem != null && frameItem.getType() != Material.AIR) {
                    totalValue += getItemValue(frameItem);
                }
            }
        } catch (Exception e) {
            // If there's an error scanning entities, log it but continue
            FactionsPlugin.getInstance().getLogger().warning("Error scanning item frames for base value: " + e.getMessage());
        }
        
        return totalValue;
    }
    
    /**
     * Get the value of a block type
     */
    private static double getBlockValue(Material material) {
        if (material == null) {
            return defaultBlockValue;
        }
        
        String key = material.name().toUpperCase();
        return blockValues.getOrDefault(key, defaultBlockValue);
    }
    
    /**
     * Get the value of an item
     */
    private static double getItemValue(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return 0.0;
        }
        
        String key = item.getType().name().toUpperCase();
        double value = itemValues.getOrDefault(key, defaultItemValue);
        
        // Multiply by amount
        return value * item.getAmount();
    }
    
    /**
     * Get the value of a spawner by entity type
     */
    private static double getSpawnerValue(String entityType) {
        if (entityType == null || entityType.isEmpty()) {
            return defaultSpawnerValue;
        }
        
        return spawnerValues.getOrDefault(entityType.toUpperCase(), defaultSpawnerValue);
    }
    
    /**
     * Get cached base value (for performance)
     */
    private static final Map<String, Double> cachedValues = new ConcurrentHashMap<>();
    private static final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 60000; // 1 minute cache
    
    public static double getCachedBaseValue(Faction faction) {
        if (faction == null) {
            return 0.0;
        }
        
        String factionId = faction.getId();
        Long timestamp = cacheTimestamps.get(factionId);
        
        if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_DURATION_MS) {
            return cachedValues.getOrDefault(factionId, 0.0);
        }
        
        // Recalculate
        double value = calculateBaseValue(faction);
        cachedValues.put(factionId, value);
        cacheTimestamps.put(factionId, System.currentTimeMillis());
        
        return value;
    }
    
    /**
     * Invalidate cache for a faction
     */
    public static void invalidateCache(Faction faction) {
        if (faction != null) {
            cachedValues.remove(faction.getId());
            cacheTimestamps.remove(faction.getId());
        }
    }
    
    /**
     * Invalidate all caches
     */
    public static void invalidateAllCaches() {
        cachedValues.clear();
        cacheTimestamps.clear();
    }
}





