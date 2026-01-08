package com.playpandora.pandoraminer.integration;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Integration with PandoraBases for claim checking
 */
public class PandoraBasesIntegration {
    
    private static Boolean enabled = null;
    
    /**
     * Check if PandoraBases is available
     */
    public static boolean isEnabled() {
        if (enabled == null) {
            try {
                Class.forName("com.massivecraft.factions.Board");
                enabled = true;
            } catch (ClassNotFoundException e) {
                enabled = false;
            }
        }
        return enabled;
    }
    
    /**
     * Check if a location is in claimed land
     */
    public static boolean isInClaimedLand(Location location) {
        if (!isEnabled()) {
            return false;
        }
        
        try {
            // Use reflection to avoid compile-time dependency
            Class<?> fLocationClass = Class.forName("com.massivecraft.factions.FLocation");
            Object fLocation = fLocationClass.getMethod("wrap", Location.class).invoke(null, location);
            
            Class<?> boardClass = Class.forName("com.massivecraft.factions.Board");
            Object boardInstance = boardClass.getMethod("getInstance").invoke(null);
            Object faction = boardClass.getMethod("getFactionAt", fLocationClass).invoke(boardInstance, fLocation);
            
            if (faction == null) {
                return false;
            }
            
            // Check if it's wilderness
            boolean isWilderness = (Boolean) faction.getClass().getMethod("isWilderness").invoke(faction);
            if (isWilderness) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if a player owns the land at a location
     */
    public static boolean ownsLand(Player player, Location location) {
        if (!isEnabled()) {
            return false;
        }
        
        try {
            // Use reflection to avoid compile-time dependency
            Class<?> fLocationClass = Class.forName("com.massivecraft.factions.FLocation");
            Object fLocation = fLocationClass.getMethod("wrap", Location.class).invoke(null, location);
            
            Class<?> boardClass = Class.forName("com.massivecraft.factions.Board");
            Object boardInstance = boardClass.getMethod("getInstance").invoke(null);
            Object faction = boardClass.getMethod("getFactionAt", fLocationClass).invoke(boardInstance, fLocation);
            
            if (faction == null) {
                return false;
            }
            
            // Get player's faction
            Class<?> fPlayerClass = Class.forName("com.massivecraft.factions.FPlayer");
            Object fPlayer = fPlayerClass.getMethod("get", Player.class).invoke(null, player);
            Object playerFaction = fPlayerClass.getMethod("getFaction").invoke(fPlayer);
            
            if (playerFaction == null) {
                return false;
            }
            
            // Check if factions match
            String factionId = (String) faction.getClass().getMethod("getId").invoke(faction);
            String playerFactionId = (String) playerFaction.getClass().getMethod("getId").invoke(playerFaction);
            
            return factionId != null && factionId.equals(playerFactionId);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if a player can use miner at this location
     * Returns true if in wilderness or owns the land
     */
    public static boolean canUseMiner(Player player, Location location) {
        if (!isInClaimedLand(location)) {
            return true; // Wilderness is allowed
        }
        
        return ownsLand(player, location);
    }
}


