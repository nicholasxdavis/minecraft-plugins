package com.playpandora.treasurehunt.managers;

import com.playpandora.treasurehunt.TreasureHunt;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class IntegrationManager {
    
    private final TreasureHunt plugin;
    private Object hookAPI;
    private Object levelAPI;
    private Object hookIntegration;
    
    public IntegrationManager(TreasureHunt plugin) {
        this.plugin = plugin;
    }
    
    public void initializeIntegrations() {
        // Hook into Hook plugin
        Plugin hookPlugin = Bukkit.getPluginManager().getPlugin("Hook");
        if (hookPlugin != null && hookPlugin.isEnabled()) {
            try {
                Object hookInstance = hookPlugin.getClass().getMethod("getInstance").invoke(null);
                hookAPI = hookInstance.getClass().getMethod("getAPI").invoke(hookInstance);
                
                // Also get the integration manager to call integration methods directly
                Object integrations = hookInstance.getClass().getMethod("getIntegrations").invoke(hookInstance);
                if (integrations != null) {
                    // Try to get TreasureHuntIntegration
                    try {
                        hookIntegration = integrations.getClass().getField("treasureHunt").get(integrations);
                    } catch (Exception e) {
                        // Field might not be accessible, that's okay
                    }
                }
                
                plugin.getLogger().info("Hook integration enabled!");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to get Hook API: " + e.getMessage());
            }
        }
        
        // Hook into LevelPlugin
        Plugin levelPlugin = Bukkit.getPluginManager().getPlugin("LevelPlugin");
        if (levelPlugin != null && levelPlugin.isEnabled()) {
            try {
                Object levelInstance = levelPlugin.getClass().getMethod("getInstance").invoke(null);
                levelAPI = levelInstance.getClass().getMethod("getAPI").invoke(levelInstance);
                plugin.getLogger().info("LevelPlugin integration enabled!");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to get LevelPlugin API: " + e.getMessage());
            }
        }
    }
    
    public void sendTreasureFound(Player player, String treasureName) {
        if (hookAPI == null) {
            return;
        }
        
        try {
            // Call Hook API's sendTreasureFound method
            hookAPI.getClass().getMethod("sendTreasureFound", Player.class, String.class)
                .invoke(hookAPI, player, treasureName);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send treasure found notification: " + e.getMessage());
        }
    }
    
    public void sendHintClaimed(Player player, int hintsRemaining) {
        if (hookAPI == null) {
            return;
        }
        
        try {
            // Call Hook API's sendHintClaimed method
            hookAPI.getClass().getMethod("sendHintClaimed", Player.class, int.class)
                .invoke(hookAPI, player, hintsRemaining);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send hint claimed notification: " + e.getMessage());
        }
    }
    
    public void announceHintDistance(Player player, int hintsAway) {
        if (hookAPI == null) {
            return;
        }
        
        try {
            // Call Hook API's sendTreasureDistance method (broadcasts to public chat)
            hookAPI.getClass().getMethod("sendTreasureDistance", Player.class, int.class)
                .invoke(hookAPI, player, hintsAway);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to announce hint distance: " + e.getMessage());
        }
    }
    
    public void giveLevels(Player player, int levels) {
        if (levelAPI == null) {
            return;
        }
        
        try {
            // Add XP equivalent to the levels
            // LevelPlugin uses XP, so we'll add a large amount of XP to simulate levels
            // Calculate XP needed for the number of levels requested
            int currentLevel = (Integer) levelAPI.getClass().getMethod("getLevel", Player.class).invoke(levelAPI, player);
            double xpPerLevel = 100.0; // Base XP per level
            // Exponential growth for higher levels
            double totalXP = 0;
            for (int i = 0; i < levels; i++) {
                totalXP += xpPerLevel * Math.pow(1.1, currentLevel + i);
            }
            levelAPI.getClass().getMethod("addXP", Player.class, double.class)
                .invoke(levelAPI, player, totalXP);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to give levels: " + e.getMessage());
        }
    }
    
    public void giveXP(Player player, double xp) {
        if (levelAPI == null) {
            return;
        }
        
        try {
            levelAPI.getClass().getMethod("addXP", Player.class, double.class)
                .invoke(levelAPI, player, xp);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to give XP: " + e.getMessage());
        }
    }
}

