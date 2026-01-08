package com.massivecraft.factions.util;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FactionsPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages obsidian block health system
 * Tracks HP for each obsidian block and handles damage/regeneration
 */
public class ObsidianHealthManager {

    private static ObsidianHealthManager instance;
    
    // Store HP for each obsidian block: Location -> HP
    private final Map<String, ObsidianBlockData> blockHealthMap = new ConcurrentHashMap<>();
    
    // Store last damage time for regeneration: Location -> timestamp
    private final Map<String, Long> lastDamageTime = new ConcurrentHashMap<>();
    
    private File dataFile;
    private YamlConfiguration dataConfig;

    private ObsidianHealthManager() {
        loadData();
    }

    public static ObsidianHealthManager getInstance() {
        if (instance == null) {
            instance = new ObsidianHealthManager();
        }
        return instance;
    }

    /**
     * Get or initialize HP for an obsidian block
     * OPTIMIZED: Only saves data if players are nearby to reduce I/O
     * Only gives health to generated blocks, not player-placed ones
     */
    public int getBlockHP(Block block) {
        if (block.getType() != Material.OBSIDIAN) {
            return -1; // Not obsidian
        }

        // Check if this is a generated block (has health) or player-placed (no health)
        Location loc = block.getLocation();
        if (!isGeneratedBlock(loc)) {
            return -1; // Player-placed obsidian doesn't have health
        }

        String key = getBlockKey(loc);
        ObsidianBlockData data = blockHealthMap.get(key);
        
        if (data == null) {
            // Initialize new block with max HP (based on faction's base level)
            int maxHP = getMaxHP(block);
            data = new ObsidianBlockData(maxHP, System.currentTimeMillis());
            blockHealthMap.put(key, data);
            // Only save if players nearby (reduces I/O spam)
            if (hasPlayersNearby(block.getLocation())) {
                saveData();
            }
        }
        
        return data.getCurrentHP();
    }
    
    /**
     * Check if a block was generated (has health) or player-placed (no health)
     */
    private boolean isGeneratedBlock(Location loc) {
        // Check BaseStructureGenerator (for bases)
        try {
            Class<?> baseGenClass = Class.forName("com.massivecraft.factions.util.BaseStructureGenerator");
            java.lang.reflect.Method isGeneratedMethod = baseGenClass.getMethod("isGeneratedBlock", Location.class);
            Boolean result = (Boolean) isGeneratedMethod.invoke(null, loc);
            if (result != null && result) {
                return true;
            }
        } catch (Exception e) {
            // Class not found or method not available, continue checking
        }
        
        // Check EventStructureGenerator (for events)
        try {
            Class<?> eventGenClass = Class.forName("com.pandora.events.util.EventStructureGenerator");
            java.lang.reflect.Method isGeneratedMethod = eventGenClass.getMethod("isGeneratedBlock", Location.class);
            Boolean result = (Boolean) isGeneratedMethod.invoke(null, loc);
            if (result != null && result) {
                return true;
            }
        } catch (Exception e) {
            // Class not found or method not available
        }
        
        return false; // Not a generated block, so it's player-placed (no health)
    }
    
    /**
     * Check if any players are nearby a location
     */
    private boolean hasPlayersNearby(Location loc) {
        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (player.getWorld() == loc.getWorld() && 
                player.getLocation().distance(loc) <= 100.0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get max HP for a block (based on faction's base level)
     */
    public int getMaxHP(Block block) {
        if (block.getType() != Material.OBSIDIAN) {
            return -1;
        }
        
        // Get faction at this location
        com.massivecraft.factions.FLocation floc = com.massivecraft.factions.FLocation.wrap(block.getLocation());
        com.massivecraft.factions.Faction faction = com.massivecraft.factions.Board.getInstance().getFactionAt(floc);
        
        // If in a faction's territory, use base level multiplier
        if (faction != null && faction.isNormal()) {
            return com.massivecraft.factions.managers.BaseLevelManager.getEffectiveObsidianHealth(faction);
        }
        
        // Default to base health
        return Conf.obsidianMaxHealth;
    }

    /**
     * Apply damage to an obsidian block
     * @return true if block was destroyed (HP <= 0)
     * Only damages generated blocks, not player-placed ones
     */
    public boolean damageBlock(Block block, double damage, double distance) {
        if (block.getType() != Material.OBSIDIAN) {
            return false;
        }

        // Check if this is a generated block (has health) or player-placed (no health)
        Location loc = block.getLocation();
        if (!isGeneratedBlock(loc)) {
            return false; // Player-placed obsidian doesn't have health, can't be damaged
        }

        String key = getBlockKey(loc);
        ObsidianBlockData data = blockHealthMap.get(key);
        
        if (data == null) {
            int maxHP = getMaxHP(block);
            data = new ObsidianBlockData(maxHP, System.currentTimeMillis());
            blockHealthMap.put(key, data);
        }

        // Apply distance-based damage reduction if enabled
        double finalDamage = damage;
        if (Conf.obsidianDistanceBasedDamage) {
            finalDamage = calculateDistanceDamage(damage, distance);
        }

        // Apply damage
        int newHP = (int) Math.max(0, data.getCurrentHP() - finalDamage);
        data.setCurrentHP(newHP);
        data.setLastDamageTime(System.currentTimeMillis());
        lastDamageTime.put(key, System.currentTimeMillis());

        // Save data (only if players nearby to reduce I/O)
        if (hasPlayersNearby(block.getLocation())) {
            saveData();
        }

        // Check if block should break
        if (newHP <= 0) {
            blockHealthMap.remove(key);
            lastDamageTime.remove(key);
            saveData();
            return true; // Block destroyed
        }

        return false; // Block still alive
    }

    /**
     * Calculate damage based on distance
     */
    private double calculateDistanceDamage(double baseDamage, double distance) {
        if (distance <= 1.0) {
            return baseDamage; // 100% damage
        } else if (distance <= 2.0) {
            return baseDamage * 0.7; // 70% damage
        } else if (distance <= 3.0) {
            return baseDamage * 0.4; // 40% damage
        } else {
            return baseDamage * 0.2; // 20% damage (far away)
        }
    }

    /**
     * Regenerate HP for a block
     */
    public void regenerateBlock(Block block) {
        if (block.getType() != Material.OBSIDIAN) {
            return;
        }

        String key = getBlockKey(block.getLocation());
        ObsidianBlockData data = blockHealthMap.get(key);
        
        if (data == null) {
            return; // Block not tracked
        }

        int currentHP = data.getCurrentHP();
        int maxHP = getMaxHP(block);
        if (currentHP >= maxHP) {
            return; // Already at max
        }

        // Regenerate
        int newHP = Math.min(maxHP, currentHP + Conf.obsidianRegenAmount);
        data.setCurrentHP(newHP);
        
        // Only save if players nearby (reduces I/O)
        if (hasPlayersNearby(block.getLocation())) {
            saveData();
        }
    }

    /**
     * Check if block should regenerate (1 hour since last damage)
     */
    public boolean shouldRegenerate(Block block) {
        if (block.getType() != Material.OBSIDIAN) {
            return false;
        }

        String key = getBlockKey(block.getLocation());
        Long lastDamage = lastDamageTime.get(key);
        
        if (lastDamage == null) {
            return false; // Never damaged
        }

        long timeSinceDamage = System.currentTimeMillis() - lastDamage;
        long regenDelayMs = Conf.obsidianRegenDelayMinutes * 60 * 1000;
        
        return timeSinceDamage >= regenDelayMs;
    }

    /**
     * Remove block from tracking (when broken)
     */
    public void removeBlock(Block block) {
        String key = getBlockKey(block.getLocation());
        blockHealthMap.remove(key);
        lastDamageTime.remove(key);
        saveData();
    }

    /**
     * Get block key for storage
     */
    private String getBlockKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    /**
     * Load data from file
     */
    private void loadData() {
        dataFile = new File(FactionsPlugin.getInstance().getDataFolder(), "obsidian-health.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                Logger.print("Failed to create obsidian-health.yml: " + e.getMessage(), Logger.PrefixType.FAILED);
                return;
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        // Load block health data
        if (dataConfig.contains("blocks")) {
            for (String key : dataConfig.getConfigurationSection("blocks").getKeys(false)) {
                // Use base health as default (will be adjusted by faction level when loaded)
                int currentHP = dataConfig.getInt("blocks." + key + ".hp", 24);
                long lastDamage = dataConfig.getLong("blocks." + key + ".lastDamage", System.currentTimeMillis());
                blockHealthMap.put(key, new ObsidianBlockData(currentHP, lastDamage));
                
                if (lastDamage > 0) {
                    lastDamageTime.put(key, lastDamage);
                }
            }
        }

        Logger.print("Loaded " + blockHealthMap.size() + " obsidian blocks with health data", Logger.PrefixType.DEFAULT);
    }

    // Track last save time to batch saves (reduce I/O)
    private long lastSaveTime = 0;
    private static final long SAVE_COOLDOWN_MS = 5000; // Only save every 5 seconds max
    
    /**
     * Save data to file
     * OPTIMIZED: Batches saves to reduce I/O operations
     */
    public void saveData() {
        if (dataConfig == null || dataFile == null) {
            return;
        }
        
        // Throttle saves - only save every 5 seconds to reduce I/O spam
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSaveTime < SAVE_COOLDOWN_MS) {
            return; // Too soon, skip save
        }
        lastSaveTime = currentTime;

        // Clear old data
        dataConfig.set("blocks", null);

        // Save current data (only blocks that still exist and are obsidian)
        int saved = 0;
        for (Map.Entry<String, ObsidianBlockData> entry : blockHealthMap.entrySet()) {
            String key = entry.getKey();
            String[] parts = key.split(":");
            if (parts.length == 4) {
                try {
                    org.bukkit.World world = FactionsPlugin.getInstance().getServer().getWorld(parts[0]);
                    if (world != null) {
                        Location loc = new Location(world, 
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]),
                            Integer.parseInt(parts[3]));
                        
                        Block block = loc.getBlock();
                        // Only save if block is still obsidian
                        if (block.getType() == Material.OBSIDIAN) {
                            ObsidianBlockData data = entry.getValue();
                            dataConfig.set("blocks." + key + ".hp", data.getCurrentHP());
                            dataConfig.set("blocks." + key + ".lastDamage", data.getLastDamageTime());
                            saved++;
                        }
                    }
                } catch (Exception e) {
                    // Invalid location, skip
                }
            }
        }

        try {
            dataConfig.save(dataFile);
            // Only log if significant number of blocks saved (reduce log spam)
            if (saved > 10 && Conf.logFactionCreate) {
                Logger.print("Saved " + saved + " obsidian blocks with health data", Logger.PrefixType.DEFAULT);
            }
        } catch (IOException e) {
            Logger.print("Failed to save obsidian-health.yml: " + e.getMessage(), Logger.PrefixType.FAILED);
        }
    }

    /**
     * Get all blocks that need regeneration
     */
    public Map<Location, Block> getBlocksForRegeneration() {
        Map<Location, Block> toRegen = new HashMap<>();
        
        for (Map.Entry<String, ObsidianBlockData> entry : blockHealthMap.entrySet()) {
            String key = entry.getKey();
            String[] parts = key.split(":");
            if (parts.length == 4) {
                try {
                    org.bukkit.World world = FactionsPlugin.getInstance().getServer().getWorld(parts[0]);
                    if (world != null) {
                        Location loc = new Location(world, 
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]),
                            Integer.parseInt(parts[3]));
                        
                        Block block = loc.getBlock();
                        if (block.getType() == Material.OBSIDIAN && shouldRegenerate(block)) {
                            toRegen.put(loc, block);
                        }
                    }
                } catch (Exception e) {
                    // Invalid location, skip
                }
            }
        }
        
        return toRegen;
    }

    /**
     * Data class for obsidian block health
     */
    public static class ObsidianBlockData {
        private int currentHP;
        private long lastDamageTime;

        public ObsidianBlockData(int currentHP, long lastDamageTime) {
            this.currentHP = currentHP;
            this.lastDamageTime = lastDamageTime;
        }

        public int getCurrentHP() {
            return currentHP;
        }

        public void setCurrentHP(int currentHP) {
            this.currentHP = currentHP;
        }

        public long getLastDamageTime() {
            return lastDamageTime;
        }

        public void setLastDamageTime(long lastDamageTime) {
            this.lastDamageTime = lastDamageTime;
        }
    }
}

