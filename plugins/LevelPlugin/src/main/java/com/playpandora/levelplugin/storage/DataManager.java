package com.playpandora.levelplugin.storage;

import com.playpandora.levelplugin.LevelPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DataManager {
    
    private final LevelPlugin plugin;
    private File dataFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, PlayerData> playerData = new HashMap<>();
    private final Set<String> rewardedLevels = new HashSet<>(); // Track rewarded levels: "uuid_level"
    private BukkitTask autoSaveTask;
    private boolean isSaving = false;
    
    public DataManager(LevelPlugin plugin) {
        this.plugin = plugin;
        loadData();
        startAutoSave();
    }
    
    public void loadData() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create data.yml: " + e.getMessage());
                return;
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        // Load player data
        if (dataConfig.contains("players")) {
            for (String uuidString : dataConfig.getConfigurationSection("players").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidString);
                String path = "players." + uuidString;
                double xp = dataConfig.getDouble(path + ".xp", 0.0);
                long playtime = dataConfig.getLong(path + ".playtime", 0L);
                
                playerData.put(uuid, new PlayerData(uuid, xp, playtime));
            }
        }
        
        // Load rewarded levels
        if (dataConfig.contains("rewarded-levels")) {
            rewardedLevels.addAll(dataConfig.getStringList("rewarded-levels"));
        }
        
        plugin.getLogger().info("Loaded data for " + playerData.size() + " players and " + rewardedLevels.size() + " rewarded levels");
    }
    
    public void saveData() {
        if (dataConfig == null || dataFile == null || isSaving) {
            return;
        }
        
        isSaving = true;
        
        try {
            // Clear existing data
            dataConfig.set("players", null);
            dataConfig.set("rewarded-levels", null);
            
            // Save player data
            for (Map.Entry<UUID, PlayerData> entry : playerData.entrySet()) {
                UUID uuid = entry.getKey();
                PlayerData data = entry.getValue();
                String path = "players." + uuid.toString();
                dataConfig.set(path + ".xp", data.getXP());
                dataConfig.set(path + ".playtime", data.getPlaytime());
            }
            
            // Save rewarded levels
            dataConfig.set("rewarded-levels", new java.util.ArrayList<>(rewardedLevels));
            
            // Save to file
            dataConfig.save(dataFile);
            
            plugin.getLogger().fine("Auto-saved data: " + playerData.size() + " players, " + rewardedLevels.size() + " rewarded levels");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save data.yml: " + e.getMessage());
            e.printStackTrace();
        } finally {
            isSaving = false;
        }
    }
    
    public void saveAllData() {
        saveData();
    }
    
    private void startAutoSave() {
        // Auto-save every 5 minutes (6000 ticks = 5 minutes)
        int autoSaveInterval = plugin.getConfig().getInt("auto-save-interval-minutes", 5);
        long ticks = autoSaveInterval * 60 * 20L; // Convert minutes to ticks
        
        autoSaveTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            // Run save on main thread to avoid async issues
            plugin.getServer().getScheduler().runTask(plugin, this::saveData);
        }, ticks, ticks);
        
        plugin.getLogger().info("Auto-save enabled: saving every " + autoSaveInterval + " minutes");
    }
    
    public void stopAutoSave() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
    }
    
    public PlayerData getPlayerData(UUID uuid) {
        return playerData.computeIfAbsent(uuid, k -> new PlayerData(uuid, 0.0, 0L));
    }
    
    public void setXP(UUID uuid, double xp) {
        getPlayerData(uuid).setXP(xp);
    }
    
    public void addXP(UUID uuid, double amount) {
        getPlayerData(uuid).addXP(amount);
    }
    
    public double getXP(UUID uuid) {
        return getPlayerData(uuid).getXP();
    }
    
    public void addPlaytime(UUID uuid, long minutes) {
        getPlayerData(uuid).addPlaytime(minutes);
    }
    
    public long getPlaytime(UUID uuid) {
        return getPlayerData(uuid).getPlaytime();
    }
    
    // Rewarded levels management
    public boolean isLevelRewarded(UUID uuid, int level) {
        String key = uuid.toString() + "_" + level;
        return rewardedLevels.contains(key);
    }
    
    public void markLevelRewarded(UUID uuid, int level) {
        String key = uuid.toString() + "_" + level;
        rewardedLevels.add(key);
    }
    
    public Set<String> getRewardedLevels() {
        return new HashSet<>(rewardedLevels);
    }
    
    public static class PlayerData {
        private final UUID uuid;
        private double xp;
        private long playtime; // in minutes
        
        public PlayerData(UUID uuid, double xp, long playtime) {
            this.uuid = uuid;
            this.xp = xp;
            this.playtime = playtime;
        }
        
        public UUID getUUID() {
            return uuid;
        }
        
        public double getXP() {
            return xp;
        }
        
        public void setXP(double xp) {
            this.xp = xp;
        }
        
        public void addXP(double amount) {
            this.xp += amount;
        }
        
        public long getPlaytime() {
            return playtime;
        }
        
        public void addPlaytime(long minutes) {
            this.playtime += minutes;
        }
    }
}


