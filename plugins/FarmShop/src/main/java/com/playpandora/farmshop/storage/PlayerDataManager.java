package com.playpandora.farmshop.storage;

import com.playpandora.farmshop.FarmShop;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Comprehensive Player Data Manager for FarmShop
 * Handles saving and loading of all player data including:
 * - Farms
 * - Pets
 * - Custom player data
 */
public class PlayerDataManager {
    
    private final FarmShop plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, PlayerData> playerDataCache;
    private BukkitTask autoSaveTask;
    private boolean needsSave = false;
    
    public PlayerDataManager(FarmShop plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        this.playerDataCache = new HashMap<>();
        loadData();
        startAutoSave();
    }
    
    /**
     * Load all player data from file
     */
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
            Set<String> playerKeys = dataConfig.getConfigurationSection("players").getKeys(false);
            for (String uuidString : playerKeys) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    PlayerData data = loadPlayerData(uuid);
                    if (data != null) {
                        playerDataCache.put(uuid, data);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in playerdata.yml: " + uuidString);
                }
            }
        }
        
        plugin.getLogger().info("Loaded data for " + playerDataCache.size() + " players");
    }
    
    /**
     * Load player data from config
     */
    private PlayerData loadPlayerData(UUID uuid) {
        String path = "players." + uuid.toString();
        
        List<String> pets = new ArrayList<>(dataConfig.getStringList(path + ".pets"));
        List<String> farms = new ArrayList<>(dataConfig.getStringList(path + ".farms"));
        
        Map<String, Object> customData = new HashMap<>();
        if (dataConfig.contains(path + ".custom")) {
            Set<String> customKeys = dataConfig.getConfigurationSection(path + ".custom").getKeys(false);
            for (String key : customKeys) {
                customData.put(key, dataConfig.get(path + ".custom." + key));
            }
        }
        
        return new PlayerData(uuid, pets, farms, customData);
    }
    
    /**
     * Start auto-save task
     */
    private void startAutoSave() {
        // Auto-save every 30 seconds (600 ticks)
        autoSaveTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (needsSave) {
                saveDataSync();
                needsSave = false;
            }
        }, 600L, 600L); // Every 30 seconds
        
        plugin.getLogger().info("Auto-save enabled (every 30 seconds)");
    }
    
    /**
     * Get or create player data
     */
    private PlayerData getOrCreatePlayerData(UUID uuid) {
        return playerDataCache.computeIfAbsent(uuid, k -> new PlayerData(uuid));
    }
    
    // ========== FARM METHODS ==========
    
    public void addFarm(UUID uuid, String farmId) {
        PlayerData data = getOrCreatePlayerData(uuid);
        if (!data.getFarms().contains(farmId)) {
            data.getFarms().add(farmId);
            needsSave = true;
            saveDataSync();
        }
    }
    
    public void removeFarm(UUID uuid, String farmId) {
        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            data.getFarms().remove(farmId);
            needsSave = true;
            saveDataSync();
        }
    }
    
    public List<String> getPlayerFarms(UUID uuid) {
        PlayerData data = playerDataCache.get(uuid);
        return data != null ? new ArrayList<>(data.getFarms()) : new ArrayList<>();
    }
    
    public boolean hasFarm(UUID uuid, String farmId) {
        PlayerData data = playerDataCache.get(uuid);
        return data != null && data.getFarms().contains(farmId);
    }
    
    public int getFarmCount(UUID uuid) {
        PlayerData data = playerDataCache.get(uuid);
        return data != null ? data.getFarms().size() : 0;
    }
    
    // ========== PET METHODS ==========
    
    public void addPet(UUID uuid, String petId) {
        PlayerData data = getOrCreatePlayerData(uuid);
        if (!data.getPets().contains(petId)) {
            data.getPets().add(petId);
            needsSave = true;
            saveDataSync();
        }
    }
    
    public void removePet(UUID uuid, String petId) {
        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            data.getPets().remove(petId);
            needsSave = true;
            saveDataSync();
        }
    }
    
    public List<String> getPlayerPets(UUID uuid) {
        PlayerData data = playerDataCache.get(uuid);
        return data != null ? new ArrayList<>(data.getPets()) : new ArrayList<>();
    }
    
    public boolean hasPet(UUID uuid, String petId) {
        PlayerData data = playerDataCache.get(uuid);
        return data != null && data.getPets().contains(petId);
    }
    
    public int getPetCount(UUID uuid) {
        PlayerData data = playerDataCache.get(uuid);
        return data != null ? data.getPets().size() : 0;
    }
    
    // ========== CUSTOM DATA METHODS ==========
    
    public void setCustomData(UUID uuid, String key, Object value) {
        PlayerData data = getOrCreatePlayerData(uuid);
        data.getCustomData().put(key, value);
        needsSave = true;
    }
    
    public Object getCustomData(UUID uuid, String key) {
        PlayerData data = playerDataCache.get(uuid);
        return data != null ? data.getCustomData().get(key) : null;
    }
    
    public <T> T getCustomData(UUID uuid, String key, Class<T> type) {
        Object value = getCustomData(uuid, key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }
    
    // ========== SAVE METHODS ==========
    
    /**
     * Save all data synchronously
     */
    private void saveDataSync() {
        // Ensure we're on the main thread for file operations
        if (!plugin.getServer().isPrimaryThread()) {
            plugin.getServer().getScheduler().runTask(plugin, this::saveDataSync);
            return;
        }
        
        try {
            // Save all cached data to file
            for (PlayerData data : playerDataCache.values()) {
                String path = "players." + data.getUuid().toString();
                
                // Save pets
                dataConfig.set(path + ".pets", new ArrayList<>(data.getPets()));
                
                // Save farms
                dataConfig.set(path + ".farms", new ArrayList<>(data.getFarms()));
                
                // Save custom data
                for (Map.Entry<String, Object> entry : data.getCustomData().entrySet()) {
                    dataConfig.set(path + ".custom." + entry.getKey(), entry.getValue());
                }
            }
            
            // Force save with retry logic
            int retries = 3;
            IOException lastException = null;
            while (retries > 0) {
                try {
                    dataConfig.save(dataFile);
                    plugin.getLogger().fine("Player data saved successfully");
                    needsSave = false;
                    return;
                } catch (IOException e) {
                    lastException = e;
                    retries--;
                    if (retries > 0) {
                        try {
                            Thread.sleep(100); // Wait 100ms before retry
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
            
            // If we get here, all retries failed
            plugin.getLogger().severe("Failed to save playerdata.yml after 3 attempts: " + 
                (lastException != null ? lastException.getMessage() : "Unknown error"));
            if (lastException != null) {
                lastException.printStackTrace();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save playerdata.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Force save all data immediately
     */
    public void save() {
        needsSave = true;
        saveDataSync();
    }
    
    /**
     * Close and save all data
     */
    public void close() {
        // Cancel auto-save task
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        
        // Final save of all data
        saveDataSync();
        plugin.getLogger().info("All player data saved on plugin disable");
    }
    
    /**
     * Player Data container class
     */
    public static class PlayerData {
        private final UUID uuid;
        private final List<String> pets;
        private final List<String> farms;
        private final Map<String, Object> customData;
        
        public PlayerData(UUID uuid) {
            this(uuid, new ArrayList<>(), new ArrayList<>(), new HashMap<>());
        }
        
        public PlayerData(UUID uuid, List<String> pets, List<String> farms,
                         Map<String, Object> customData) {
            this.uuid = uuid;
            this.pets = pets != null ? pets : new ArrayList<>();
            this.farms = farms != null ? farms : new ArrayList<>();
            this.customData = customData != null ? customData : new HashMap<>();
        }
        
        public UUID getUuid() { return uuid; }
        public List<String> getPets() { return pets; }
        public List<String> getFarms() { return farms; }
        public Map<String, Object> getCustomData() { return customData; }
    }
}

