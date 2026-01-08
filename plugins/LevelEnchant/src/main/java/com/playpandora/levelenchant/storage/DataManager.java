package com.playpandora.levelenchant.storage;

import com.playpandora.levelenchant.LevelEnchant;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    
    private final LevelEnchant plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, PlayerData> playerDataCache;
    private BukkitTask autoSaveTask;
    private boolean needsSave = false;
    
    public class PlayerData {
        private int lastKnownLevel;
        private double lastKnownXP;
        private long lastUpdated;
        
        public PlayerData() {
            this.lastKnownLevel = 0;
            this.lastKnownXP = 0.0;
            this.lastUpdated = System.currentTimeMillis();
        }
        
        public PlayerData(int level, double xp, long updated) {
            this.lastKnownLevel = level;
            this.lastKnownXP = xp;
            this.lastUpdated = updated;
        }
        
        public int getLastKnownLevel() {
            return lastKnownLevel;
        }
        
        public void setLastKnownLevel(int lastKnownLevel) {
            this.lastKnownLevel = lastKnownLevel;
            this.lastUpdated = System.currentTimeMillis();
            DataManager.this.needsSave = true;
        }
        
        public double getLastKnownXP() {
            return lastKnownXP;
        }
        
        public void setLastKnownXP(double lastKnownXP) {
            this.lastKnownXP = lastKnownXP;
            this.lastUpdated = System.currentTimeMillis();
            DataManager.this.needsSave = true;
        }
        
        public long getLastUpdated() {
            return lastUpdated;
        }
        
        public void update(int level, double xp) {
            this.lastKnownLevel = level;
            this.lastKnownXP = xp;
            this.lastUpdated = System.currentTimeMillis();
            DataManager.this.needsSave = true;
        }
    }
    
    public DataManager(LevelEnchant plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        this.playerDataCache = new HashMap<>();
        loadData();
        startAutoSave();
    }
    
    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create playerdata.yml: " + e.getMessage());
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        // Load all player data into cache
        if (dataConfig.contains("players")) {
            for (String uuidString : dataConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    String path = "players." + uuidString;
                    int level = dataConfig.getInt(path + ".last-level", 0);
                    double xp = dataConfig.getDouble(path + ".last-xp", 0.0);
                    long updated = dataConfig.getLong(path + ".last-updated", System.currentTimeMillis());
                    
                    playerDataCache.put(uuid, new PlayerData(level, xp, updated));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in playerdata.yml: " + uuidString);
                }
            }
        }
        
        plugin.getLogger().info("Loaded data for " + playerDataCache.size() + " players");
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
    
    public PlayerData getPlayerData(UUID uuid) {
        return playerDataCache.computeIfAbsent(uuid, k -> new PlayerData());
    }
    
    public void updatePlayerData(UUID uuid, int level, double xp) {
        PlayerData data = getPlayerData(uuid);
        data.update(level, xp);
        needsSave = true;
    }
    
    public void savePlayerData(UUID uuid) {
        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            needsSave = true;
            String path = "players." + uuid.toString();
            dataConfig.set(path + ".last-level", data.getLastKnownLevel());
            dataConfig.set(path + ".last-xp", data.getLastKnownXP());
            dataConfig.set(path + ".last-updated", data.getLastUpdated());
        }
    }
    
    public void saveData() {
        try {
            // Save all cached data to file
            dataConfig.set("players", null); // Clear existing data
            
            for (Map.Entry<UUID, PlayerData> entry : playerDataCache.entrySet()) {
                UUID uuid = entry.getKey();
                PlayerData data = entry.getValue();
                if (data != null) {
                    String path = "players." + uuid.toString();
                    dataConfig.set(path + ".last-level", data.getLastKnownLevel());
                    dataConfig.set(path + ".last-xp", data.getLastKnownXP());
                    dataConfig.set(path + ".last-updated", data.getLastUpdated());
                }
            }
            
            // Add metadata
            dataConfig.set("last-saved", System.currentTimeMillis());
            dataConfig.set("total-players", playerDataCache.size());
            
            dataConfig.save(dataFile);
            needsSave = false;
            plugin.getLogger().fine("Data saved successfully (" + playerDataCache.size() + " players)");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save playerdata.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void close() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        saveData();
    }
}

