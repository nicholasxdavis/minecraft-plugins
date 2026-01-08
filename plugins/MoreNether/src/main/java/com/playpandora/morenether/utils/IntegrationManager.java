package com.playpandora.morenether.utils;

import com.playpandora.morenether.MoreNether;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class IntegrationManager {
    
    private final MoreNether plugin;
    private Object levelAPI;
    private Object levelledMobsAPI;
    private boolean skriptAvailable;
    
    public IntegrationManager(MoreNether plugin) {
        this.plugin = plugin;
        initializeIntegrations();
    }
    
    private void initializeIntegrations() {
        // LevelPlugin integration
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
        
        // LevelledMobs integration
        Plugin levelledMobsPlugin = Bukkit.getPluginManager().getPlugin("LevelledMobs");
        if (levelledMobsPlugin != null && levelledMobsPlugin.isEnabled()) {
            try {
                // Try to get LevelledMobs API
                Class<?> lmClass = Class.forName("com.ticxo.levelledmobs.api.LevelledMobsAPI");
                levelledMobsAPI = lmClass.getMethod("getAPI").invoke(null);
                plugin.getLogger().info("LevelledMobs integration enabled!");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to get LevelledMobs API: " + e.getMessage());
            }
        }
        
        // Skript integration (for qab.sk quest system)
        skriptAvailable = Bukkit.getPluginManager().getPlugin("Skript") != null;
        if (skriptAvailable) {
            plugin.getLogger().info("Skript integration available for quest system!");
        }
    }
    
    /**
     * Apply LevelledMobs leveling to a mob
     */
    public void applyLevelledMobs(LivingEntity entity) {
        if (levelledMobsAPI == null) {
            return;
        }
        
        try {
            // Use LevelledMobs API to apply leveling
            levelledMobsAPI.getClass().getMethod("applyLevelToMob", LivingEntity.class)
                .invoke(levelledMobsAPI, entity);
        } catch (Exception e) {
            // Silently fail - LevelledMobs will handle it naturally
        }
    }
    
    /**
     * Try to add to an existing stack instead of spawning a new mob
     * Returns true if successfully added to stack, false if should spawn new mob
     */
    public boolean tryAddToStack(org.bukkit.Location location, org.bukkit.entity.EntityType type) {
        if (levelledMobsAPI == null) {
            return false;
        }
        
        try {
            // Try to get StackManager instance
            Class<?> stackManagerClass = Class.forName("io.github.arcaneplugins.levelledmobs.managers.StackManager");
            Object stackManager = stackManagerClass.getMethod("getInstance").invoke(null);
            if (stackManager == null) {
                return false;
            }
            
            // Check for nearby mobs of the same type within stacking radius (25 blocks)
            double stackRadius = 25.0;
            java.util.Collection<org.bukkit.entity.Entity> nearbyEntities = location.getWorld()
                .getNearbyEntities(location, stackRadius, stackRadius, stackRadius);
            
            for (org.bukkit.entity.Entity nearby : nearbyEntities) {
                if (!(nearby instanceof LivingEntity)) continue;
                if (nearby.getType() != type) continue;
                if (nearby.isDead() || !nearby.isValid()) continue;
                
                LivingEntity nearbyMob = (LivingEntity) nearby;
                
                // Check if this mob is part of a stack
                int currentStackSize = (Integer) stackManagerClass
                    .getMethod("getStackSize", LivingEntity.class)
                    .invoke(stackManager, nearbyMob);
                
                // Get max stack size
                boolean isEnabled = (Boolean) stackManagerClass
                    .getMethod("isStackingEnabled")
                    .invoke(stackManager);
                
                if (!isEnabled) {
                    return false;
                }
                
                // Try to get max stack size from config or use default 64
                int maxStackSize = 64;
                try {
                    maxStackSize = (Integer) stackManagerClass
                        .getMethod("getMaxStackSize")
                        .invoke(stackManager);
                } catch (Exception e) {
                    // Use default
                }
                
                // If stack is not at max, increment it instead of spawning new mob
                if (currentStackSize < maxStackSize) {
                    stackManagerClass
                        .getMethod("incrementStackSize", LivingEntity.class, int.class)
                        .invoke(stackManager, nearbyMob, 1);
                    return true; // Successfully added to stack
                }
            }
            
            return false; // No suitable stack found, should spawn new mob
        } catch (Exception e) {
            // If reflection fails, fall back to normal spawning
            plugin.getLogger().fine("Could not access LevelledMobs StackManager: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Give XP to player for killing mob in Nether
     */
    public void giveKillXP(Player player, LivingEntity killed) {
        if (levelAPI == null) {
            return;
        }
        
        // Calculate XP based on mob type and dimension
        double baseXP = getBaseXPForMob(killed.getType());
        double netherMultiplier = plugin.getConfig().getDouble("integrations.levelplugin.nether-xp-multiplier", 1.5);
        double xp = baseXP * netherMultiplier;
        
        try {
            levelAPI.getClass().getMethod("addXP", Player.class, double.class)
                .invoke(levelAPI, player, xp);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to give XP: " + e.getMessage());
        }
    }
    
    /**
     * Update quest progress for qab.sk
     */
    public void updateQuestProgress(Player player, String mobType) {
        if (!skriptAvailable) {
            return;
        }
        
        // Quest tracking is handled by Skript events in qab.sk
        // This method is here for future enhancements
    }
    
    /**
     * Send Hook notification for special events
     */
    public void sendEventNotification(Player player, String eventName) {
        try {
            org.bukkit.plugin.Plugin hookPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("Hook");
            if (hookPlugin != null && hookPlugin.isEnabled()) {
                Object hookInstance = hookPlugin.getClass().getMethod("getInstance").invoke(null);
                Object hookAPI = hookInstance.getClass().getMethod("getAPI").invoke(hookInstance);
                hookAPI.getClass().getMethod("sendCustom", org.bukkit.entity.Player.class, 
                    String.class, String.class, int.class, int.class, int.class)
                    .invoke(hookAPI, player, "Nether Event!", eventName, 500, 3000, 1000);
            }
        } catch (Exception e) {
            // Silently fail
        }
    }
    
    private double getBaseXPForMob(org.bukkit.entity.EntityType type) {
        switch (type) {
            case BLAZE: return 10.0;
            case GHAST: return 15.0;
            case WITHER_SKELETON: return 20.0;
            case PIGLIN: return 5.0;
            case PIGLIN_BRUTE: return 8.0;
            case HOGLIN: return 12.0;
            case ZOMBIFIED_PIGLIN: return 3.0;
            case MAGMA_CUBE: return 6.0;
            case ENDERMAN: return 8.0;
            default: return 5.0;
        }
    }
}

