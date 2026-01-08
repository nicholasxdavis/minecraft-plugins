package com.playpandora.sellgui.managers;

import com.playpandora.sellgui.SellGUI;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class LevelIntegration {
    
    private final SellGUI plugin;
    private Object levelAPI;
    private Method addXPMethod;
    private boolean available;
    
    public LevelIntegration(SellGUI plugin) {
        this.plugin = plugin;
        checkLevelPlugin();
    }
    
    private void checkLevelPlugin() {
        if (plugin.getServer().getPluginManager().getPlugin("LevelPlugin") == null) {
            plugin.getLogger().info("LevelPlugin not found. XP rewards will be disabled.");
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
    
    public void giveXP(Player player, double amount) {
        if (!available || levelAPI == null || addXPMethod == null) {
            return;
        }
        
        try {
            addXPMethod.invoke(levelAPI, player, amount);
        } catch (Exception e) {
            plugin.getLogger().warning("Error giving XP to player: " + e.getMessage());
        }
    }
    
    public boolean isAvailable() {
        return available;
    }
}


