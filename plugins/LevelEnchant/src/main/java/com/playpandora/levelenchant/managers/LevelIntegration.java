package com.playpandora.levelenchant.managers;

import com.playpandora.levelenchant.LevelEnchant;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class LevelIntegration {
    
    private final LevelEnchant plugin;
    private Object levelAPI;
    private Object levelManager;
    private Method getLevelMethod;
    private Method getXPMethod;
    private Method getXPForCurrentLevelMethod;
    private Method getXPRequiredForNextLevelMethod;
    private boolean available;
    
    public LevelIntegration(LevelEnchant plugin) {
        this.plugin = plugin;
        checkLevelPlugin();
    }
    
    private void checkLevelPlugin() {
        if (plugin.getServer().getPluginManager().getPlugin("LevelPlugin") == null) {
            plugin.getLogger().warning("LevelPlugin not found! Some features may not work.");
            available = false;
            return;
        }
        
        try {
            Class<?> levelPluginClass = Class.forName("com.playpandora.levelplugin.LevelPlugin");
            Method getInstanceMethod = levelPluginClass.getMethod("getInstance");
            Object levelPlugin = getInstanceMethod.invoke(null);
            
            Method getAPIMethod = levelPluginClass.getMethod("getAPI");
            levelAPI = getAPIMethod.invoke(levelPlugin);
            
            Method getLevelManagerMethod = levelPluginClass.getMethod("getLevelManager");
            levelManager = getLevelManagerMethod.invoke(levelPlugin);
            
            getLevelMethod = levelAPI.getClass().getMethod("getLevel", org.bukkit.entity.Player.class);
            getXPMethod = levelAPI.getClass().getMethod("getXP", org.bukkit.entity.Player.class);
            getXPForCurrentLevelMethod = levelManager.getClass().getMethod("getXPForCurrentLevel", java.util.UUID.class);
            getXPRequiredForNextLevelMethod = levelManager.getClass().getMethod("getXPRequiredForNextLevel", java.util.UUID.class);
            
            available = true;
            plugin.getLogger().info("LevelPlugin integration enabled!");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to integrate with LevelPlugin: " + e.getMessage());
            e.printStackTrace();
            available = false;
        }
    }
    
    public int getLevel(Player player) {
        if (!available || levelAPI == null || getLevelMethod == null) {
            return 0;
        }
        
        try {
            return (Integer) getLevelMethod.invoke(levelAPI, player);
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting level for player: " + e.getMessage());
            return 0;
        }
    }
    
    public double getXP(Player player) {
        if (!available || levelAPI == null || getXPMethod == null) {
            return 0.0;
        }
        
        try {
            return (Double) getXPMethod.invoke(levelAPI, player);
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting XP for player: " + e.getMessage());
            return 0.0;
        }
    }
    
    public double getXPForCurrentLevel(Player player) {
        if (!available || levelManager == null || getXPForCurrentLevelMethod == null) {
            return 0.0;
        }
        
        try {
            return (Double) getXPForCurrentLevelMethod.invoke(levelManager, player.getUniqueId());
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting XP for current level: " + e.getMessage());
            return 0.0;
        }
    }
    
    public double getXPRequiredForNextLevel(Player player) {
        if (!available || levelManager == null || getXPRequiredForNextLevelMethod == null) {
            return 1.0;
        }
        
        try {
            return (Double) getXPRequiredForNextLevelMethod.invoke(levelManager, player.getUniqueId());
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting XP required for next level: " + e.getMessage());
            return 1.0;
        }
    }
    
    public boolean isAvailable() {
        return available;
    }
}

