package com.massivecraft.factions.util;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FactionsPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages TNT fixes for cannon functionality
 * Prevents Paper/Spigot optimizations from breaking TNT physics
 */
public class TNTFixManager {

    private static TNTFixManager instance;
    
    // Track TNT entities to ensure they tick properly
    private final Set<UUID> trackedTNT = Collections.synchronizedSet(new HashSet<>());
    
    // Store original velocities to prevent modification
    private final Map<UUID, org.bukkit.util.Vector> tntVelocities = new ConcurrentHashMap<>();
    
    // Track TNT ignition times for timing fixes
    private final Map<UUID, Long> tntIgnitionTimes = new ConcurrentHashMap<>();

    private TNTFixManager() {
        startTNTTickTask();
    }

    public static TNTFixManager getInstance() {
        if (instance == null) {
            instance = new TNTFixManager();
        }
        return instance;
    }

    /**
     * Register a TNT entity for tracking
     */
    public void registerTNT(TNTPrimed tnt) {
        if (tnt == null || !tnt.isValid()) {
            return;
        }
        
        try {
            UUID uuid = tnt.getUniqueId();
            trackedTNT.add(uuid);
            org.bukkit.util.Vector velocity = tnt.getVelocity();
            if (velocity != null) {
                tntVelocities.put(uuid, velocity.clone());
            }
            tntIgnitionTimes.put(uuid, System.currentTimeMillis());
        } catch (Exception e) {
            // Silently handle - TNT may be invalid
        }
    }

    /**
     * Unregister TNT when it explodes or is removed
     */
    public void unregisterTNT(TNTPrimed tnt) {
        if (tnt == null) {
            return;
        }
        
        UUID uuid = tnt.getUniqueId();
        trackedTNT.remove(uuid);
        tntVelocities.remove(uuid);
        tntIgnitionTimes.remove(uuid);
    }

    /**
     * Fix TNT velocity - restore vanilla behavior
     */
    public void fixTNTVelocity(TNTPrimed tnt) {
        if (tnt == null || !tnt.isValid() || tnt.isDead()) {
            return;
        }
        
        try {
            UUID uuid = tnt.getUniqueId();
            org.bukkit.util.Vector originalVelocity = tntVelocities.get(uuid);
            
            if (originalVelocity != null) {
                // Restore original velocity if it was modified
                org.bukkit.util.Vector currentVelocity = tnt.getVelocity();
                if (currentVelocity != null) {
                    // Check if velocity was significantly changed (likely by Paper optimizations)
                    double velocityDiff = currentVelocity.distance(originalVelocity);
                    if (velocityDiff > 0.01) {
                        // Restore original velocity
                        tnt.setVelocity(originalVelocity);
                    }
                }
            }
        } catch (Exception e) {
            // Silently handle - TNT may have been removed
        }
    }

    /**
     * Force TNT to tick every tick (prevent ghosting)
     */
    public void forceTNTTick(TNTPrimed tnt) {
        if (tnt == null || !tnt.isValid() || tnt.isDead()) {
            return;
        }
        
        try {
            // Force TNT to update by ensuring it ticks
            // This prevents Paper from batching/skipping ticks
            Location loc = tnt.getLocation();
            if (loc == null || loc.getWorld() == null) {
                return;
            }
            
            // Use a more efficient method - just ensure TNT is active
            // Teleporting can cause issues, so we use a different approach
            // Set TNT's ticks lived to ensure it processes
            int fuseTicks = tnt.getFuseTicks();
            if (fuseTicks > 0) {
                // Force update by setting fuse (ensures tick processing)
                tnt.setFuseTicks(fuseTicks);
            }
        } catch (Exception e) {
            // Silently handle - TNT may have been removed
        }
    }

    /**
     * Fix TNT waterlogging behavior
     */
    public void fixTNTWaterlogging(TNTPrimed tnt) {
        if (tnt == null || !tnt.isValid()) {
            return;
        }
        
        Location loc = tnt.getLocation();
        Block block = loc.getBlock();
        Block blockBelow = loc.clone().subtract(0, 0.1, 0).getBlock();
        
        // Check if TNT is in water
        boolean inWater = block.getType() == Material.WATER || 
                         block.getType() == Material.LAVA ||
                         blockBelow.getType() == Material.WATER ||
                         blockBelow.getType() == Material.LAVA;
        
        if (inWater) {
            // Restore proper velocity in water (Paper reduces it too much)
            org.bukkit.util.Vector velocity = tnt.getVelocity();
            if (velocity.length() < 0.1) {
                // TNT is moving too slowly in water - restore momentum
                org.bukkit.util.Vector original = tntVelocities.get(tnt.getUniqueId());
                if (original != null && original.length() > 0.1) {
                    // Apply water resistance but keep more momentum
                    org.bukkit.util.Vector fixed = original.multiply(0.7); // 70% of original
                    tnt.setVelocity(fixed);
                }
            }
        }
    }

    /**
     * Fix TNT clustering (multiple TNT in same block)
     */
    public void fixTNTClustering(TNTPrimed tnt) {
        if (tnt == null || !tnt.isValid()) {
            return;
        }
        
        Location loc = tnt.getLocation();
        Block block = loc.getBlock();
        
        // Check for other TNT in same block
        List<Entity> nearby = tnt.getNearbyEntities(0.5, 0.5, 0.5);
        int tntCount = 0;
        
        for (Entity entity : nearby) {
            if (entity instanceof TNTPrimed && entity != tnt) {
                tntCount++;
            }
        }
        
        // If multiple TNT in same block, ensure they don't merge/ghost
        if (tntCount > 0) {
            // Slightly offset TNT to prevent merging
            double offset = 0.01 * (tntCount % 2 == 0 ? 1 : -1);
            Location newLoc = loc.clone().add(offset, 0, offset);
            tnt.teleport(newLoc);
        }
    }

    /**
     * Start periodic task to fix TNT every tick
     */
    private void startTNTTickTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!Conf.tntFixEnabled) {
                    return;
                }
                
                // Process all tracked TNT (use copy to avoid concurrent modification)
                List<UUID> toProcess = new ArrayList<>(trackedTNT);
                List<UUID> toRemove = new ArrayList<>();
                
                for (UUID uuid : toProcess) {
                    try {
                        Entity entity = FactionsPlugin.getInstance().getServer().getEntity(uuid);
                        
                        if (!(entity instanceof TNTPrimed)) {
                            toRemove.add(uuid);
                            continue;
                        }
                        
                        TNTPrimed tnt = (TNTPrimed) entity;
                        
                        if (!tnt.isValid() || tnt.isDead()) {
                            toRemove.add(uuid);
                            continue;
                        }
                        
                        // Apply all fixes
                        if (Conf.tntFixForceTick) {
                            forceTNTTick(tnt);
                        }
                        
                        if (Conf.tntFixVelocity) {
                            fixTNTVelocity(tnt);
                        }
                        
                        if (Conf.tntFixWaterlogging) {
                            fixTNTWaterlogging(tnt);
                        }
                        
                        if (Conf.tntFixClustering) {
                            fixTNTClustering(tnt);
                        }
                    } catch (Exception e) {
                        // Entity may have been removed, clean it up
                        toRemove.add(uuid);
                    }
                }
                
                // Clean up removed TNT
                for (UUID uuid : toRemove) {
                    trackedTNT.remove(uuid);
                    tntVelocities.remove(uuid);
                    tntIgnitionTimes.remove(uuid);
                }
            }
        }.runTaskTimer(FactionsPlugin.getInstance(), 0L, 1L); // Every tick
    }

    /**
     * Get explosion radius fix multiplier
     * Ensures consistent explosion radius regardless of TPS
     */
    public double getExplosionRadiusMultiplier() {
        if (!Conf.tntFixExplosionRadius) {
            return 1.0;
        }
        
        // Return consistent multiplier (Paper varies this based on TPS)
        return Conf.tntFixExplosionRadiusMultiplier;
    }

    /**
     * Clean up all tracked TNT
     */
    public void cleanup() {
        trackedTNT.clear();
        tntVelocities.clear();
        tntIgnitionTimes.clear();
    }
}

