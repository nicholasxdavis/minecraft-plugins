package com.massivecraft.factions.managers;

import com.massivecraft.factions.FactionsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class LevelManager {
    
    private final FactionsPlugin plugin;
    private Object levelPlugin;
    private boolean levelPluginAvailable;
    private Method getLevelMethod;
    
    public LevelManager(FactionsPlugin plugin) {
        this.plugin = plugin;
        setupLevelPlugin();
    }
    
    private void setupLevelPlugin() {
        Plugin levelPluginInstance = Bukkit.getServer().getPluginManager().getPlugin("LevelPlugin");
        if (levelPluginInstance == null) {
            levelPluginAvailable = false;
            plugin.getLogger().warning("LevelPlugin not found! Level checks will be disabled.");
            return;
        }
        
        try {
            // Get the main plugin class
            Class<?> pluginClass = levelPluginInstance.getClass();
            
            // Try to get the API instance using getAPI() method
            Method getAPIMethod = pluginClass.getMethod("getAPI");
            Object apiInstance = getAPIMethod.invoke(levelPluginInstance);
            
            if (apiInstance != null) {
                levelPlugin = apiInstance;
                Class<?> apiClass = apiInstance.getClass();
                getLevelMethod = apiClass.getMethod("getLevel", org.bukkit.entity.Player.class);
                levelPluginAvailable = true;
                plugin.getLogger().info("Successfully connected to LevelPlugin API!");
            } else {
                levelPluginAvailable = false;
                plugin.getLogger().warning("Failed to get LevelPlugin API instance.");
            }
        } catch (Exception e) {
            levelPluginAvailable = false;
            plugin.getLogger().warning("Error setting up LevelPlugin integration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean isLevelPluginAvailable() {
        return levelPluginAvailable && levelPlugin != null && getLevelMethod != null;
    }
    
    public int getLevel(Player player) {
        if (!isLevelPluginAvailable()) {
            return 0;
        }
        
        try {
            Object result = getLevelMethod.invoke(levelPlugin, player);
            if (result instanceof Integer) {
                return (Integer) result;
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    public boolean hasLevel(Player player, int requiredLevel) {
        return getLevel(player) >= requiredLevel;
    }
}

