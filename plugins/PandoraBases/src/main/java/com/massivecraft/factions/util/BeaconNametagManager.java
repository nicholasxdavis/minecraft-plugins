package com.massivecraft.factions.util;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages nametags above base beacons showing faction names
 */
public class BeaconNametagManager {

    private static final Map<String, UUID> beaconNametags = new HashMap<>(); // factionId -> armorStand UUID
    private static org.bukkit.scheduler.BukkitTask persistenceTask = null; // Task for persistence checking

    /**
     * Create or update nametag above a beacon
     */
    public static void createNametag(Faction faction, Location beaconLoc) {
        if (faction == null || beaconLoc == null || !faction.hasBeacon()) {
            return;
        }

        // Remove existing nametag if any
        removeNametag(faction);

        // Spawn armor stand 1 block above beacon (moved down from 2.0)
        Location nametagLoc = beaconLoc.clone().add(0.5, 1.0, 0.5);
        
        // Use a delayed task to ensure beacon is placed
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    ArmorStand stand = (ArmorStand) nametagLoc.getWorld().spawnEntity(nametagLoc, EntityType.ARMOR_STAND);
                    
                    // Configure armor stand
                    stand.setCustomName(ChatColor.YELLOW + "" + ChatColor.BOLD + faction.getTag());
                    stand.setCustomNameVisible(true);
                    stand.setVisible(false); // Invisible
                    stand.setGravity(false); // No gravity
                    stand.setInvulnerable(true); // Can't be damaged
                    stand.setSmall(true); // Small size
                    stand.setMarker(true); // Marker (no hitbox)
                    stand.setCollidable(false); // No collision
                    
                    // Store the UUID
                    beaconNametags.put(faction.getId(), stand.getUniqueId());
                } catch (Exception e) {
                    Logger.print("Error creating beacon nametag for " + faction.getTag() + ": " + e.getMessage(), Logger.PrefixType.WARNING);
                }
            }
        }.runTaskLater(FactionsPlugin.getInstance(), 5L); // 5 ticks delay to ensure beacon is placed
        
        // Global persistence checker will be started separately on plugin load
    }
    
    /**
     * Start a global recurring task that checks all beacon nametags and recreates missing ones
     */
    /**
     * Start a global recurring task that checks all beacon nametags and recreates missing ones
     */
    public static void startGlobalPersistenceChecker() {
        // Only start if not already running
        if (persistenceTask != null && !persistenceTask.isCancelled()) {
            return;
        }
        
        persistenceTask = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                // Check all factions with beacons
                for (com.massivecraft.factions.Faction faction : com.massivecraft.factions.Factions.getInstance().getAllFactions()) {
                    if (faction == null || !faction.isNormal() || !faction.hasBeacon()) {
                        continue;
                    }
                    
                    Location beaconLoc = faction.getBeaconLocation();
                    if (beaconLoc == null || beaconLoc.getWorld() == null) {
                        continue;
                    }
                    
                    // Check if beacon block still exists
                    if (beaconLoc.getBlock().getType() != org.bukkit.Material.BEACON) {
                        continue;
                    }
                    
                    UUID storedUUID = beaconNametags.get(faction.getId());
                    
                    // Check if nametag exists
                    boolean nametagExists = false;
                    if (storedUUID != null) {
                        for (org.bukkit.entity.Entity entity : beaconLoc.getWorld().getNearbyEntities(
                                beaconLoc.clone().add(0.5, 1.0, 0.5), 2.0, 2.0, 2.0)) {
                            if (entity instanceof ArmorStand && entity.getUniqueId().equals(storedUUID)) {
                                nametagExists = true;
                                break;
                            }
                        }
                    }
                    
                    // If nametag is missing but beacon exists, recreate it (but don't start another checker)
                    if (!nametagExists) {
                        // Recreate without starting another persistence checker
                        recreateNametagWithoutChecker(faction, beaconLoc);
                    }
                }
            }
        }.runTaskTimer(FactionsPlugin.getInstance(), 60L, 60L); // Check every 3 seconds, start after 3 seconds
    }
    
    /**
     * Recreate nametag without starting persistence checker (to avoid recursion)
     */
    private static void recreateNametagWithoutChecker(Faction faction, Location beaconLoc) {
        if (faction == null || beaconLoc == null || !faction.hasBeacon()) {
            return;
        }
        
        // Remove existing from map
        UUID oldUUID = beaconNametags.remove(faction.getId());
        if (oldUUID != null) {
            // Try to remove old armor stand
            for (org.bukkit.entity.Entity entity : beaconLoc.getWorld().getNearbyEntities(
                    beaconLoc.clone().add(0.5, 1.0, 0.5), 2.0, 2.0, 2.0)) {
                if (entity instanceof ArmorStand && entity.getUniqueId().equals(oldUUID)) {
                    entity.remove();
                    break;
                }
            }
        }
        
        // Create new nametag
        Location nametagLoc = beaconLoc.clone().add(0.5, 1.0, 0.5);
        try {
            ArmorStand stand = (ArmorStand) nametagLoc.getWorld().spawnEntity(nametagLoc, EntityType.ARMOR_STAND);
            
            stand.setCustomName(ChatColor.YELLOW + "" + ChatColor.BOLD + faction.getTag());
            stand.setCustomNameVisible(true);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setSmall(true);
            stand.setMarker(true);
            stand.setCollidable(false);
            
            beaconNametags.put(faction.getId(), stand.getUniqueId());
        } catch (Exception e) {
            Logger.print("Error recreating beacon nametag for " + faction.getTag() + ": " + e.getMessage(), Logger.PrefixType.WARNING);
        }
    }

    /**
     * Remove nametag for a faction
     */
    public static void removeNametag(Faction faction) {
        if (faction == null) return;
        
        UUID standUUID = beaconNametags.remove(faction.getId());
        if (standUUID != null) {
            // Find and remove the armor stand
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        Location beaconLoc = faction.getBeaconLocation();
                        if (beaconLoc != null && beaconLoc.getWorld() != null) {
                            // Search in a small radius around beacon
                            for (org.bukkit.entity.Entity entity : beaconLoc.getWorld().getNearbyEntities(
                                    beaconLoc.clone().add(0.5, 1.0, 0.5), 1.0, 1.0, 1.0)) {
                                if (entity instanceof ArmorStand && entity.getUniqueId().equals(standUUID)) {
                                    entity.remove();
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Logger.print("Error removing beacon nametag: " + e.getMessage(), Logger.PrefixType.WARNING);
                    }
                }
            }.runTask(FactionsPlugin.getInstance());
        }
    }

    /**
     * Update nametag text (when faction name changes)
     */
    public static void updateNametag(Faction faction) {
        if (faction == null || !faction.hasBeacon()) return;
        
        UUID standUUID = beaconNametags.get(faction.getId());
        if (standUUID != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        Location beaconLoc = faction.getBeaconLocation();
                        if (beaconLoc != null && beaconLoc.getWorld() != null) {
                            for (org.bukkit.entity.Entity entity : beaconLoc.getWorld().getNearbyEntities(
                                    beaconLoc.clone().add(0.5, 1.0, 0.5), 1.0, 1.0, 1.0)) {
                                if (entity instanceof ArmorStand && entity.getUniqueId().equals(standUUID)) {
                                    ((ArmorStand) entity).setCustomName(ChatColor.YELLOW + "" + ChatColor.BOLD + faction.getTag());
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Logger.print("Error updating beacon nametag: " + e.getMessage(), Logger.PrefixType.WARNING);
                    }
                }
            }.runTask(FactionsPlugin.getInstance());
        } else {
            // Nametag doesn't exist, create it
            createNametag(faction, faction.getBeaconLocation());
        }
    }

    /**
     * Cleanup all nametags (on server shutdown)
     */
    public static void cleanupAll() {
        // Stop persistence checker
        if (persistenceTask != null && !persistenceTask.isCancelled()) {
            persistenceTask.cancel();
            persistenceTask = null;
        }
        
        for (String factionId : new java.util.HashSet<>(beaconNametags.keySet())) {
            Faction faction = com.massivecraft.factions.Factions.getInstance().getFactionById(factionId);
            if (faction != null) {
                removeNametag(faction);
            }
        }
        beaconNametags.clear();
    }
}

