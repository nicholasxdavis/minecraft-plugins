package com.playpandora.welcome.managers;

import com.playpandora.welcome.Welcome;
import com.playpandora.welcome.data.PlayerData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    
    private final Welcome plugin;
    private final File dataFile;
    private final Map<UUID, PlayerData> playerDataCache;
    private BukkitTask autoSaveTask;
    
    public PlayerDataManager(Welcome plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        this.playerDataCache = new ConcurrentHashMap<>();
        
        // Ensure data folder exists
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // Load all player data
        loadAllData();
        
        // Start auto-save task (saves every 5 minutes)
        startAutoSave();
    }
    
    /**
     * Get or create player data for a player
     */
    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId(), player.getName());
    }
    
    /**
     * Get or create player data by UUID
     */
    public PlayerData getPlayerData(UUID uuid, String name) {
        return playerDataCache.computeIfAbsent(uuid, k -> {
            PlayerData data = new PlayerData(uuid, name);
            savePlayerData(data);
            return data;
        });
    }
    
    /**
     * Get player data without creating if it doesn't exist
     */
    public PlayerData getPlayerDataIfExists(UUID uuid) {
        return playerDataCache.get(uuid);
    }
    
    /**
     * Update player's last seen timestamp
     */
    public void updateLastSeen(Player player) {
        PlayerData data = getPlayerData(player);
        data.setLastSeen(System.currentTimeMillis());
        data.setName(player.getName()); // Update name in case it changed
    }
    
    /**
     * Check if player has received starter items
     */
    public boolean hasReceivedStarterItems(Player player) {
        PlayerData data = getPlayerDataIfExists(player.getUniqueId());
        return data != null && data.hasReceivedStarterItems();
    }
    
    /**
     * Mark player as having received starter items
     */
    public void setReceivedStarterItems(Player player) {
        PlayerData data = getPlayerData(player);
        data.setHasReceivedStarterItems(true);
        savePlayerData(data);
    }
    
    /**
     * Check if player has received booklet
     */
    public boolean hasReceivedBooklet(Player player) {
        PlayerData data = getPlayerDataIfExists(player.getUniqueId());
        return data != null && data.hasReceivedBooklet();
    }
    
    /**
     * Mark player as having received booklet
     */
    public void setReceivedBooklet(Player player) {
        PlayerData data = getPlayerData(player);
        data.setHasReceivedBooklet(true);
        savePlayerData(data);
    }
    
    /**
     * Add a pet to player's data
     */
    public void addPet(Player player, String pet) {
        PlayerData data = getPlayerData(player);
        data.addPet(pet);
        savePlayerData(data);
    }
    
    /**
     * Remove a pet from player's data
     */
    public void removePet(Player player, String pet) {
        PlayerData data = getPlayerData(player);
        data.removePet(pet);
        savePlayerData(data);
    }
    
    /**
     * Get all pets for a player
     */
    public List<String> getPets(Player player) {
        PlayerData data = getPlayerDataIfExists(player.getUniqueId());
        return data != null ? data.getPets() : new ArrayList<>();
    }
    
    /**
     * Set custom data for a player
     */
    public void setCustomData(Player player, String key, Object value) {
        PlayerData data = getPlayerData(player);
        data.setCustomData(key, value);
        savePlayerData(data);
    }
    
    /**
     * Get custom data for a player
     */
    public Object getCustomData(Player player, String key) {
        PlayerData data = getPlayerDataIfExists(player.getUniqueId());
        return data != null ? data.getCustomData(key) : null;
    }
    
    /**
     * Load all player data from file
     */
    private void loadAllData() {
        if (!dataFile.exists()) {
            plugin.getLogger().info("No playerdata.yml found, creating new file...");
            return;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        int loaded = 0;
        
        if (config.contains("players")) {
            Set<String> keys = config.getConfigurationSection("players").getKeys(false);
            for (String uuidString : keys) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    String path = "players." + uuidString;
                    
                    PlayerData data = new PlayerData();
                    data.setUuid(uuid);
                    data.setName(config.getString(path + ".name", "Unknown"));
                    data.setFirstJoin(config.getLong(path + ".firstJoin", System.currentTimeMillis()));
                    data.setLastSeen(config.getLong(path + ".lastSeen", System.currentTimeMillis()));
                    data.setHasReceivedStarterItems(config.getBoolean(path + ".hasReceivedStarterItems", false));
                    data.setHasReceivedBooklet(config.getBoolean(path + ".hasReceivedBooklet", false));
                    data.setPets(config.getStringList(path + ".pets"));
                    
                    // Load custom data
                    if (config.contains(path + ".customData")) {
                        Map<String, Object> customData = new HashMap<>();
                        Set<String> customKeys = config.getConfigurationSection(path + ".customData").getKeys(false);
                        for (String key : customKeys) {
                            customData.put(key, config.get(path + ".customData." + key));
                        }
                        data.setCustomData(customData);
                    }
                    
                    playerDataCache.put(uuid, data);
                    loaded++;
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in playerdata.yml: " + uuidString);
                }
            }
        }
        
        plugin.getLogger().info("Loaded " + loaded + " player data entries.");
    }
    
    /**
     * Save a specific player's data
     */
    public void savePlayerData(PlayerData data) {
        if (data == null || data.getUuid() == null) {
            return;
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        String path = "players." + data.getUuid().toString();
        
        config.set(path + ".name", data.getName());
        config.set(path + ".firstJoin", data.getFirstJoin());
        config.set(path + ".lastSeen", data.getLastSeen());
        config.set(path + ".hasReceivedStarterItems", data.hasReceivedStarterItems());
        config.set(path + ".hasReceivedBooklet", data.hasReceivedBooklet());
        config.set(path + ".pets", data.getPets());
        
        // Save custom data
        if (!data.getCustomData().isEmpty()) {
            for (Map.Entry<String, Object> entry : data.getCustomData().entrySet()) {
                config.set(path + ".customData." + entry.getKey(), entry.getValue());
            }
        }
        
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data for " + data.getUuid() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Save all player data
     */
    public void saveAllData() {
        FileConfiguration config = new YamlConfiguration();
        int saved = 0;
        
        for (PlayerData data : playerDataCache.values()) {
            if (data == null || data.getUuid() == null) {
                continue;
            }
            
            String path = "players." + data.getUuid().toString();
            
            config.set(path + ".name", data.getName());
            config.set(path + ".firstJoin", data.getFirstJoin());
            config.set(path + ".lastSeen", data.getLastSeen());
            config.set(path + ".hasReceivedStarterItems", data.hasReceivedStarterItems());
            config.set(path + ".hasReceivedBooklet", data.hasReceivedBooklet());
            config.set(path + ".pets", data.getPets());
            
            // Save custom data
            if (!data.getCustomData().isEmpty()) {
                for (Map.Entry<String, Object> entry : data.getCustomData().entrySet()) {
                    config.set(path + ".customData." + entry.getKey(), entry.getValue());
                }
            }
            
            saved++;
        }
        
        try {
            config.save(dataFile);
            plugin.getLogger().info("Saved " + saved + " player data entries.");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save all player data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Start auto-save task
     */
    private void startAutoSave() {
        // Auto-save every 5 minutes (6000 ticks) - runs synchronously to avoid file I/O issues
        autoSaveTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            plugin.getLogger().info("Auto-saving player data...");
            saveAllData();
        }, 6000L, 6000L);
    }
    
    /**
     * Stop auto-save task
     */
    public void stopAutoSave() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
    }
    
    /**
     * Shutdown and save all data
     */
    public void shutdown() {
        stopAutoSave();
        saveAllData();
    }
}

