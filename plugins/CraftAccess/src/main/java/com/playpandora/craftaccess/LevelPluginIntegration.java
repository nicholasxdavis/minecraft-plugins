package com.playpandora.craftaccess;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class LevelPluginIntegration {
    
    private final CraftAccess plugin;
    private Object levelAPI;
    private Method getLevelMethod;
    private boolean available;
    
    public LevelPluginIntegration(CraftAccess plugin) {
        this.plugin = plugin;
        checkLevelPlugin();
    }
    
    private void checkLevelPlugin() {
        if (Bukkit.getPluginManager().getPlugin("LevelPlugin") == null) {
            plugin.getLogger().warning("LevelPlugin not found! Level requirements will be disabled.");
            available = false;
            return;
        }
        
        try {
            Class<?> levelPluginClass = Class.forName("com.playpandora.levelplugin.LevelPlugin");
            Method getInstanceMethod = levelPluginClass.getMethod("getInstance");
            Object levelPlugin = getInstanceMethod.invoke(null);
            
            Method getAPIMethod = levelPluginClass.getMethod("getAPI");
            levelAPI = getAPIMethod.invoke(levelPlugin);
            
            getLevelMethod = levelAPI.getClass().getMethod("getLevel", org.bukkit.entity.Player.class);
            
            available = true;
            plugin.getLogger().info("LevelPlugin integration enabled!");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to integrate with LevelPlugin: " + e.getMessage());
            available = false;
        }
    }
    
    public int getPlayerLevel(Player player) {
        if (!available || levelAPI == null || getLevelMethod == null) {
            return 0;
        }
        
        try {
            return (Integer) getLevelMethod.invoke(levelAPI, player);
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting player level: " + e.getMessage());
            return 0;
        }
    }
    
    public boolean hasLevel(Player player, int requiredLevel) {
        return getPlayerLevel(player) >= requiredLevel;
    }
    
    public boolean isAvailable() {
        return available;
    }
}


