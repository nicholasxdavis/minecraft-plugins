package com.playpandora.pandoraonevsone.integration;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Integration with PandoraBases for claim checking and base chest protection
 */
public class PandoraBasesIntegration {
    
    private static Boolean enabled = null;
    
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
    
    public static boolean isInClaimedLand(Location location) {
        if (!isEnabled()) {
            return false;
        }
        
        try {
            Class<?> fLocationClass = Class.forName("com.massivecraft.factions.FLocation");
            Object fLocation = fLocationClass.getMethod("wrap", Location.class).invoke(null, location);
            
            Class<?> boardClass = Class.forName("com.massivecraft.factions.Board");
            Object boardInstance = boardClass.getMethod("getInstance").invoke(null);
            Object faction = boardClass.getMethod("getFactionAt", fLocationClass).invoke(boardInstance, fLocation);
            
            if (faction == null) {
                return false;
            }
            
            boolean isWilderness = (Boolean) faction.getClass().getMethod("isWilderness").invoke(faction);
            if (isWilderness) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static boolean isBaseChest(org.bukkit.inventory.Inventory inventory) {
        if (!isEnabled() || inventory == null) {
            return false;
        }
        
        try {
            Class<?> baseChestManagerClass = Class.forName("com.massivecraft.factions.util.BaseChestManager");
            java.lang.reflect.Method isBaseChestMethod = baseChestManagerClass.getMethod("isBaseChest", org.bukkit.inventory.Inventory.class);
            Boolean result = (Boolean) isBaseChestMethod.invoke(null, inventory);
            
            return result != null && result;
        } catch (Exception e) {
            // Fallback: check by title
            try {
                if (!inventory.getViewers().isEmpty()) {
                    org.bukkit.entity.HumanEntity viewer = inventory.getViewers().get(0);
                    if (viewer != null && viewer.getOpenInventory() != null) {
                        String title = viewer.getOpenInventory().getTitle();
                        if (title != null && title.startsWith("Base Chest - ")) {
                            return true;
                        }
                    }
                }
            } catch (Exception ex) {
                // Ignore
            }
            return false;
        }
    }
}


