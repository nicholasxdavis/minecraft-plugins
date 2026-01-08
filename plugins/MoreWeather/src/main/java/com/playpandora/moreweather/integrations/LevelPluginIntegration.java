package com.playpandora.moreweather.integrations;

import com.playpandora.moreweather.MoreWeather;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class LevelPluginIntegration {
    
    private final MoreWeather plugin;
    private Object levelAPI;
    private Method addXPMethod;
    private boolean available;
    
    public LevelPluginIntegration(MoreWeather plugin) {
        this.plugin = plugin;
        checkLevelPlugin();
    }
    
    private void checkLevelPlugin() {
        if (plugin.getServer().getPluginManager().getPlugin("LevelPlugin") == null) {
            available = false;
            return;
        }
        
        try {
            Class<?> levelPluginClass = Class.forName("com.playpandora.levelplugin.LevelPlugin");
            Method getInstanceMethod = levelPluginClass.getMethod("getInstance");
            Object levelPlugin = getInstanceMethod.invoke(null);
            
            Method getAPIMethod = levelPluginClass.getMethod("getAPI");
            levelAPI = getAPIMethod.invoke(levelPlugin);
            
            addXPMethod = levelAPI.getClass().getMethod("addXP", org.bukkit.entity.Player.class, double.class);
            
            available = true;
            plugin.getLogger().info("LevelPlugin integration enabled!");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to integrate with LevelPlugin: " + e.getMessage());
            available = false;
        }
    }
    
    public void awardWeatherXP(Player player, String reason) {
        if (!plugin.getConfig().getBoolean("integrations.levelplugin.enabled", true)) {
            return;
        }
        
        if (!available || levelAPI == null || addXPMethod == null) {
            return;
        }
        
        double xpAmount = plugin.getConfig().getDouble("integrations.levelplugin.xp-amounts." + reason, 0.0);
        if (xpAmount <= 0) {
            return;
        }
        
        try {
            addXPMethod.invoke(levelAPI, player, xpAmount);
        } catch (Exception e) {
            plugin.getLogger().warning("Error giving weather XP to player: " + e.getMessage());
        }
    }
    
    public boolean isAvailable() {
        return available;
    }
}







