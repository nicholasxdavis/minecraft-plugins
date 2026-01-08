package com.pandora.spawners.integration;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

// PandoraBases integration - using reflection to avoid compile-time dependency
// import com.massivecraft.factions.Board;
// import com.massivecraft.factions.FLocation;
// import com.massivecraft.factions.Faction;
// import com.massivecraft.factions.FPlayer;
// import com.massivecraft.factions.FPlayers;
import com.pandora.spawners.PandoraSpawners;

/**
 * Integration with PandoraBases (Factions) plugin
 * Handles territory checks and permissions for spawner operations
 */
public class PandoraBasesIntegration {
    
    private static boolean enabled = false;
    private static Plugin pandoraBases = null;
    
    /**
     * Initialize PandoraBases integration
     */
    public static void initialize() {
        pandoraBases = PandoraSpawners.instance().getServer().getPluginManager().getPlugin("PandoraBases");
        enabled = (pandoraBases != null && pandoraBases.isEnabled());
        
        if (enabled) {
            PandoraSpawners.instance().getLogger().info("PandoraBases integration enabled!");
        } else {
            PandoraSpawners.instance().getLogger().warning("PandoraBases not found! Some features may not work.");
        }
    }
    
    /**
     * Check if PandoraBases is enabled
     */
    public static boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Check if a player can build/place at a location (faction territory check)
     */
    public static boolean canBuild(Player player, Location location) {
        if (!enabled) return true;
        
        try {
            // Use reflection to avoid compile-time dependency
            Class<?> fPlayersClass = Class.forName("com.massivecraft.factions.FPlayers");
            Object fPlayersInstance = fPlayersClass.getMethod("getInstance").invoke(null);
            Object fPlayer = fPlayersClass.getMethod("getByPlayer", Player.class).invoke(fPlayersInstance, player);
            if (fPlayer == null) return true;
            
            Class<?> fLocationClass = Class.forName("com.massivecraft.factions.FLocation");
            Object fLocation = fLocationClass.getMethod("wrap", Location.class).invoke(null, location);
            
            Class<?> boardClass = Class.forName("com.massivecraft.factions.Board");
            Object boardInstance = boardClass.getMethod("getInstance").invoke(null);
            Object faction = boardClass.getMethod("getFactionAt", fLocationClass).invoke(boardInstance, fLocation);
            
            if (faction == null || (Boolean) faction.getClass().getMethod("isWilderness").invoke(faction)) {
                return true; // Wilderness - allow
            }
            
            // Check if player can build in this territory
            Object playerFaction = fPlayer.getClass().getMethod("getFaction").invoke(fPlayer);
            Object relation = playerFaction.getClass().getMethod("getRelationTo", faction.getClass()).invoke(playerFaction, faction);
            boolean isMember = (Boolean) relation.getClass().getMethod("isMember").invoke(relation);
            boolean isAlly = (Boolean) relation.getClass().getMethod("isAlly").invoke(relation);
            boolean sameFaction = faction.equals(playerFaction);
            
            return isMember || isAlly || sameFaction;
        } catch (Exception e) {
            // If there's an error, allow by default
            return true;
        }
    }
    
    /**
     * Check if a player can break blocks at a location (faction territory check)
     */
    public static boolean canBreak(Player player, Location location) {
        return canBuild(player, location); // Same logic
    }
    
    /**
     * Check if spawners can spawn in this territory
     */
    public static boolean canSpawnInTerritory(Location location) {
        if (!enabled) return true;
        
        try {
            // Use reflection to avoid compile-time dependency
            Class<?> fLocationClass = Class.forName("com.massivecraft.factions.FLocation");
            Object fLocation = fLocationClass.getMethod("wrap", Location.class).invoke(null, location);
            
            Class<?> boardClass = Class.forName("com.massivecraft.factions.Board");
            Object boardInstance = boardClass.getMethod("getInstance").invoke(null);
            Object faction = boardClass.getMethod("getFactionAt", fLocationClass).invoke(boardInstance, fLocation);
            
            if (faction == null || (Boolean) faction.getClass().getMethod("isWilderness").invoke(faction)) {
                return true; // Wilderness - allow spawning
            }
            
            // Allow spawning in faction territory (spawners are allowed)
            return true;
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Get faction name at location
     */
    public static String getFactionName(Location location) {
        if (!enabled) return null;
        
        try {
            // Use reflection to avoid compile-time dependency
            Class<?> fLocationClass = Class.forName("com.massivecraft.factions.FLocation");
            Object fLocation = fLocationClass.getMethod("wrap", Location.class).invoke(null, location);
            
            Class<?> boardClass = Class.forName("com.massivecraft.factions.Board");
            Object boardInstance = boardClass.getMethod("getInstance").invoke(null);
            Object faction = boardClass.getMethod("getFactionAt", fLocationClass).invoke(boardInstance, fLocation);
            
            if (faction == null || (Boolean) faction.getClass().getMethod("isWilderness").invoke(faction)) {
                return null;
            }
            
            return (String) faction.getClass().getMethod("getTag").invoke(faction);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Check if location is in faction territory
     */
    public static boolean isInFactionTerritory(Location location) {
        if (!enabled) return false;
        
        try {
            // Use reflection to avoid compile-time dependency
            Class<?> fLocationClass = Class.forName("com.massivecraft.factions.FLocation");
            Object fLocation = fLocationClass.getMethod("wrap", Location.class).invoke(null, location);
            
            Class<?> boardClass = Class.forName("com.massivecraft.factions.Board");
            Object boardInstance = boardClass.getMethod("getInstance").invoke(null);
            Object faction = boardClass.getMethod("getFactionAt", fLocationClass).invoke(boardInstance, fLocation);
            
            return faction != null && !(Boolean) faction.getClass().getMethod("isWilderness").invoke(faction);
        } catch (Exception e) {
            return false;
        }
    }
}

