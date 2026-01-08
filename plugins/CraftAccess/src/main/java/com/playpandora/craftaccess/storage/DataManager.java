package com.playpandora.craftaccess.storage;

import com.playpandora.craftaccess.CraftAccess;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataManager {
    
    private final CraftAccess plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, Set<String>> unlockedBlocksCache;
    private BukkitTask autoSaveTask;
    private boolean needsSave = false;
    
    public DataManager(CraftAccess plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        this.unlockedBlocksCache = new HashMap<>();
        loadData();
        startAutoSave();
    }
    
    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create data.yml: " + e.getMessage());
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        // Load all player data into cache
        if (dataConfig.contains("players")) {
            Set<String> playerKeys = dataConfig.getConfigurationSection("players").getKeys(false);
            for (String uuidString : playerKeys) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    List<String> unlocked = dataConfig.getStringList("players." + uuidString + ".unlocked");
                    unlockedBlocksCache.put(uuid, new HashSet<>(unlocked));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in data.yml: " + uuidString);
                }
            }
        }
        
        plugin.getLogger().info("Loaded data for " + unlockedBlocksCache.size() + " players");
    }
    
    private void startAutoSave() {
        // Auto-save every 5 minutes (6000 ticks = 5 minutes)
        int autoSaveInterval = plugin.getConfig().getInt("auto-save-interval-ticks", 6000);
        autoSaveTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (needsSave) {
                saveData();
            }
        }, autoSaveInterval, autoSaveInterval);
        
        plugin.getLogger().info("Auto-save enabled (interval: " + (autoSaveInterval / 20) + " seconds)");
    }
    
    public void unlockBlock(UUID uuid, String blockType) {
        Set<String> unlocked = unlockedBlocksCache.computeIfAbsent(uuid, k -> new HashSet<>());
        if (unlocked.add(blockType)) {
            needsSave = true;
            // Also save immediately on unlock for critical data
            saveData();
        }
    }
    
    public boolean hasUnlocked(UUID uuid, String blockType) {
        Set<String> unlocked = unlockedBlocksCache.get(uuid);
        return unlocked != null && unlocked.contains(blockType);
    }
    
    public void saveData() {
        try {
            // Save all cached data to file
            dataConfig.set("players", null); // Clear existing data
            
            for (Map.Entry<UUID, Set<String>> entry : unlockedBlocksCache.entrySet()) {
                UUID uuid = entry.getKey();
                Set<String> unlocked = entry.getValue();
                if (unlocked != null && !unlocked.isEmpty()) {
                    List<String> unlockedList = new ArrayList<>(unlocked);
                    dataConfig.set("players." + uuid.toString() + ".unlocked", unlockedList);
                }
            }
            
            // Add metadata
            dataConfig.set("last-saved", System.currentTimeMillis());
            dataConfig.set("total-players", unlockedBlocksCache.size());
            
            dataConfig.save(dataFile);
            needsSave = false;
            plugin.getLogger().fine("Data saved successfully (" + unlockedBlocksCache.size() + " players)");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save data.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void savePlayerData(UUID uuid) {
        Set<String> unlocked = unlockedBlocksCache.get(uuid);
        if (unlocked != null) {
            needsSave = true;
            List<String> unlockedList = new ArrayList<>(unlocked);
            dataConfig.set("players." + uuid.toString() + ".unlocked", unlockedList);
        }
    }
    
    public void close() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        saveData();
    }
}

