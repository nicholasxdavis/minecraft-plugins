package com.playpandora.farmshop.managers;

import com.playpandora.farmshop.FarmShop;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Protection Manager
 * Checks if a location is protected and prevents pasting in:
 * - Safe zones (WorldGuard regions or PandoraBases SafeZone)
 * - War zones (WorldGuard regions or PandoraBases WarZone)
 * - Claimed territories (PandoraBases)
 */
public class ProtectionManager {
    
    private final FarmShop plugin;
    private boolean worldGuardAvailable = false;
    private boolean factionsAvailable = false;
    
    public ProtectionManager(FarmShop plugin) {
        this.plugin = plugin;
        checkDependencies();
    }
    
    private void checkDependencies() {
        worldGuardAvailable = plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null;
        factionsAvailable = plugin.getServer().getPluginManager().getPlugin("Factions") != null;
        
        if (worldGuardAvailable) {
            plugin.getLogger().info("WorldGuard integration enabled");
        }
        if (factionsAvailable) {
            plugin.getLogger().info("PandoraBases/Factions integration enabled");
        }
    }
    
    /**
     * Check if a location is protected (safe zone, war zone, or claimed territory)
     * @param location The location to check
     * @param player The player attempting to paste (can be null)
     * @return ProtectionResult with details about why it's protected
     */
    public ProtectionResult isLocationProtected(Location location, Player player) {
        // Check WorldGuard regions first
        if (worldGuardAvailable) {
            ProtectionResult wgResult = checkWorldGuard(location, player);
            if (wgResult.isProtected()) {
                return wgResult;
            }
        }
        
        // Check PandoraBases/Factions
        if (factionsAvailable) {
            ProtectionResult factionsResult = checkFactions(location, player);
            if (factionsResult.isProtected()) {
                return factionsResult;
            }
        }
        
        return new ProtectionResult(false, null, null);
    }
    
    /**
     * Check WorldGuard regions for safe zones and war zones
     */
    private ProtectionResult checkWorldGuard(Location location, Player player) {
        try {
            org.bukkit.plugin.Plugin wgPlugin = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
            if (wgPlugin == null) {
                return new ProtectionResult(false, null, null);
            }
            
            // Try WorldGuard 7.x API (newer)
            try {
                Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
                java.lang.reflect.Method getInstanceMethod = worldGuardClass.getMethod("getInstance");
                Object worldGuard = getInstanceMethod.invoke(null);
                
                java.lang.reflect.Method getPlatformMethod = worldGuard.getClass().getMethod("getPlatform");
                Object platform = getPlatformMethod.invoke(worldGuard);
                
                java.lang.reflect.Method getRegionContainerMethod = platform.getClass().getMethod("getRegionContainer");
                Object container = getRegionContainerMethod.invoke(platform);
                
                java.lang.reflect.Method createQueryMethod = container.getClass().getMethod("createQuery");
                Object query = createQueryMethod.invoke(container);
                
                Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
                java.lang.reflect.Method asLocationMethod = bukkitAdapterClass.getMethod("asLocation", Location.class);
                Object wgLocation = asLocationMethod.invoke(null, location);
                
                java.lang.reflect.Method getApplicableRegionsMethod = query.getClass().getMethod("getApplicableRegions", wgLocation.getClass());
                Object regions = getApplicableRegionsMethod.invoke(query, wgLocation);
                
                java.lang.reflect.Method iteratorMethod = regions.getClass().getMethod("iterator");
                java.util.Iterator<?> iterator = (java.util.Iterator<?>) iteratorMethod.invoke(regions);
                
                while (iterator.hasNext()) {
                    Object region = iterator.next();
                    java.lang.reflect.Method getIdMethod = region.getClass().getMethod("getId");
                    Object idObj = getIdMethod.invoke(region);
                    String regionId = idObj != null ? idObj.toString().toLowerCase() : "";
                    
                    if (regionId.contains("safezone") || regionId.contains("safe-zone") || 
                        regionId.contains("safe_zone") || regionId.equals("safezone")) {
                        return new ProtectionResult(true, "Safe Zone", "You cannot paste in safe zones!");
                    }
                    if (regionId.contains("warzone") || regionId.contains("war-zone") || 
                        regionId.contains("war_zone") || regionId.equals("warzone")) {
                        return new ProtectionResult(true, "War Zone", "You cannot paste in war zones!");
                    }
                }
            } catch (ClassNotFoundException e) {
                // Try WorldGuard 6.x API (older)
                java.lang.reflect.Method getRegionManagerMethod = wgPlugin.getClass().getMethod("getRegionManager", org.bukkit.World.class);
                Object regionManager = getRegionManagerMethod.invoke(wgPlugin, location.getWorld());
                
                if (regionManager != null) {
                    java.lang.reflect.Method getApplicableRegionsMethod = regionManager.getClass().getMethod("getApplicableRegions", org.bukkit.Location.class);
                    Object regions = getApplicableRegionsMethod.invoke(regionManager, location);
                    
                    java.lang.reflect.Method iteratorMethod = regions.getClass().getMethod("iterator");
                    java.util.Iterator<?> iterator = (java.util.Iterator<?>) iteratorMethod.invoke(regions);
                    
                    while (iterator.hasNext()) {
                        Object region = iterator.next();
                        java.lang.reflect.Method getIdMethod = region.getClass().getMethod("getId");
                        String regionId = ((String) getIdMethod.invoke(region)).toLowerCase();
                        
                        if (regionId.contains("safezone") || regionId.contains("safe-zone") || 
                            regionId.contains("safe_zone") || regionId.equals("safezone")) {
                            return new ProtectionResult(true, "Safe Zone", "You cannot paste in safe zones!");
                        }
                        if (regionId.contains("warzone") || regionId.contains("war-zone") || 
                            regionId.contains("war_zone") || regionId.equals("warzone")) {
                            return new ProtectionResult(true, "War Zone", "You cannot paste in war zones!");
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking WorldGuard protection: " + e.getMessage());
        }
        
        return new ProtectionResult(false, null, null);
    }
    
    /**
     * Check PandoraBases/Factions for safe zones, war zones, and claimed territories
     */
    private ProtectionResult checkFactions(Location location, Player player) {
        try {
            Object factionsPlugin = plugin.getServer().getPluginManager().getPlugin("Factions");
            if (factionsPlugin == null) {
                return new ProtectionResult(false, null, null);
            }
            
            // Get Board instance
            java.lang.reflect.Method getInstanceMethod = factionsPlugin.getClass().getClassLoader()
                .loadClass("com.massivecraft.factions.Board").getMethod("getInstance");
            Object board = getInstanceMethod.invoke(null);
            
            // Get FLocation wrapper method
            java.lang.reflect.Method wrapMethod = factionsPlugin.getClass().getClassLoader()
                .loadClass("com.massivecraft.factions.FLocation").getMethod("wrap", Location.class);
            Object floc = wrapMethod.invoke(null, location);
            
            // Get faction at location
            java.lang.reflect.Method getFactionAtMethod = board.getClass().getMethod("getFactionAt", floc.getClass());
            Object faction = getFactionAtMethod.invoke(board, floc);
            
            if (faction == null) {
                return new ProtectionResult(false, null, null);
            }
            
            // Check if it's a safe zone
            java.lang.reflect.Method isSafeZoneMethod = faction.getClass().getMethod("isSafeZone");
            boolean isSafeZone = (Boolean) isSafeZoneMethod.invoke(faction);
            if (isSafeZone) {
                return new ProtectionResult(true, "Safe Zone", "You cannot paste in safe zones!");
            }
            
            // Check if it's a war zone
            java.lang.reflect.Method isWarZoneMethod = faction.getClass().getMethod("isWarZone");
            boolean isWarZone = (Boolean) isWarZoneMethod.invoke(faction);
            if (isWarZone) {
                return new ProtectionResult(true, "War Zone", "You cannot paste in war zones!");
            }
            
            // Check if it's claimed territory (not wilderness)
            java.lang.reflect.Method isWildernessMethod = faction.getClass().getMethod("isWilderness");
            boolean isWilderness = (Boolean) isWildernessMethod.invoke(faction);
            
            if (!isWilderness) {
                // Check if player is in their own faction (allow own faction)
                if (player != null) {
                    try {
                        java.lang.reflect.Method getFPlayersMethod = factionsPlugin.getClass().getClassLoader()
                            .loadClass("com.massivecraft.factions.FPlayers").getMethod("getInstance");
                        Object fPlayers = getFPlayersMethod.invoke(null);
                        
                        java.lang.reflect.Method getByPlayerMethod = fPlayers.getClass().getMethod("getByPlayer", Player.class);
                        Object fPlayer = getByPlayerMethod.invoke(fPlayers, player);
                        
                        if (fPlayer != null) {
                            java.lang.reflect.Method hasFactionMethod = fPlayer.getClass().getMethod("hasFaction");
                            boolean hasFaction = (Boolean) hasFactionMethod.invoke(fPlayer);
                            
                            if (hasFaction) {
                                java.lang.reflect.Method getFactionMethod = fPlayer.getClass().getMethod("getFaction");
                                Object playerFaction = getFactionMethod.invoke(fPlayer);
                                
                                if (playerFaction != null && playerFaction.equals(faction)) {
                                    // Player is in their own faction - allow paste
                                    return new ProtectionResult(false, null, null);
                                }
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error checking player faction: " + e.getMessage());
                    }
                }
                
                // Territory is claimed by another faction - prevent paste
                return new ProtectionResult(true, "Claimed Territory", "You cannot paste in claimed territories!");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking Factions protection: " + e.getMessage());
        }
        
        return new ProtectionResult(false, null, null);
    }
    
    /**
     * Result of protection check
     */
    public static class ProtectionResult {
        private final boolean protected_;
        private final String protectionType;
        private final String message;
        
        public ProtectionResult(boolean protected_, String protectionType, String message) {
            this.protected_ = protected_;
            this.protectionType = protectionType;
            this.message = message;
        }
        
        public boolean isProtected() {
            return protected_;
        }
        
        public String getProtectionType() {
            return protectionType;
        }
        
        public String getMessage() {
            return message;
        }
    }
}

