package com.massivecraft.factions.util;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FactionsPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages sand/gravel fixes for cannon functionality
 * Prevents Paper/Spigot optimizations from breaking falling block physics
 */
public class SandFixManager {

    private static SandFixManager instance;
    
    // Track falling blocks (sand, gravel, etc.)
    private final Set<UUID> trackedFallingBlocks = Collections.synchronizedSet(new HashSet<>());
    
    // Store original velocities to prevent modification
    private final Map<UUID, Vector> blockVelocities = new ConcurrentHashMap<>();
    
    // Track block spawn times for timing fixes
    private final Map<UUID, Long> blockSpawnTimes = new ConcurrentHashMap<>();
    
    // Track last positions to detect teleport bugs
    private final Map<UUID, Location> lastPositions = new ConcurrentHashMap<>();

    private SandFixManager() {
        startSandTickTask();
    }

    public static SandFixManager getInstance() {
        if (instance == null) {
            instance = new SandFixManager();
        }
        return instance;
    }

    /**
     * Register a falling block for tracking
     */
    public void registerFallingBlock(FallingBlock block) {
        if (block == null || !block.isValid()) {
            return;
        }
        
        try {
            UUID uuid = block.getUniqueId();
            trackedFallingBlocks.add(uuid);
            Vector velocity = block.getVelocity();
            if (velocity != null) {
                blockVelocities.put(uuid, velocity.clone());
            }
            blockSpawnTimes.put(uuid, System.currentTimeMillis());
            Location loc = block.getLocation();
            if (loc != null) {
                lastPositions.put(uuid, loc.clone());
            }
        } catch (Exception e) {
            // Silently handle - block may be invalid
        }
    }

    /**
     * Unregister falling block when it lands or is removed
     */
    public void unregisterFallingBlock(FallingBlock block) {
        if (block == null) {
            return;
        }
        
        UUID uuid = block.getUniqueId();
        trackedFallingBlocks.remove(uuid);
        blockVelocities.remove(uuid);
        blockSpawnTimes.remove(uuid);
        lastPositions.remove(uuid);
    }

    /**
     * Fix sand teleport bug - detect and correct teleportation
     */
    public void fixSandTeleport(FallingBlock block) {
        if (block == null || !block.isValid()) {
            return;
        }
        
        UUID uuid = block.getUniqueId();
        Location currentLoc = block.getLocation();
        Location lastLoc = lastPositions.get(uuid);
        
        if (lastLoc == null) {
            lastPositions.put(uuid, currentLoc.clone());
            return;
        }
        
        // Check for teleportation (sudden large movement)
        double distance = currentLoc.distance(lastLoc);
        double maxExpectedDistance = 0.5; // Max expected movement per tick
        
        if (distance > maxExpectedDistance && distance < 10) {
            // Likely teleport bug - correct it
            Vector velocity = block.getVelocity();
            Location correctedLoc = lastLoc.clone().add(velocity);
            block.teleport(correctedLoc);
            
            if (Conf.sandFixLogTeleports && Conf.logFactionCreate) {
                com.massivecraft.factions.util.Logger.print("Fixed sand teleport bug for block at " + 
                    currentLoc.getBlockX() + ", " + currentLoc.getBlockY() + ", " + currentLoc.getBlockZ(),
                    com.massivecraft.factions.util.Logger.PrefixType.DEFAULT);
            }
        }
        
        lastPositions.put(uuid, block.getLocation().clone());
    }

    /**
     * Fix sand velocity/momentum - restore vanilla behavior
     */
    public void fixSandVelocity(FallingBlock block) {
        if (block == null || !block.isValid() || block.isDead()) {
            return;
        }
        
        try {
            UUID uuid = block.getUniqueId();
            Vector originalVelocity = blockVelocities.get(uuid);
            
            if (originalVelocity != null) {
                Vector currentVelocity = block.getVelocity();
                if (currentVelocity != null) {
                    // Check if velocity was significantly changed
                    double velocityDiff = currentVelocity.distance(originalVelocity);
                    if (velocityDiff > 0.01) {
                        // Restore original velocity (Paper may have modified it)
                        block.setVelocity(originalVelocity);
                    }
                }
            }
        } catch (Exception e) {
            // Silently handle - block may have been removed
        }
    }

    /**
     * Fix sand de-sync with TNT
     * Ensures sand falls at correct rate to sync with TNT timing
     */
    public void fixSandDesync(FallingBlock block) {
        if (block == null || !block.isValid() || block.isDead()) {
            return;
        }
        
        try {
            // Ensure proper falling speed (vanilla gravity)
            Vector velocity = block.getVelocity();
            if (velocity != null && velocity.getY() > -0.98) {
                // Sand should fall at ~0.98 blocks/tick (vanilla gravity)
                velocity.setY(Math.max(-0.98, velocity.getY() - 0.04));
                block.setVelocity(velocity);
            }
        } catch (Exception e) {
            // Silently handle - block may have been removed
        }
    }

    /**
     * Fix floating sand glitch - ensure all sand ticks
     */
    public void fixFloatingSand(FallingBlock block) {
        if (block == null || !block.isValid() || block.isDead()) {
            return;
        }
        
        try {
            // Check if sand is frozen (not moving but should be)
            Vector velocity = block.getVelocity();
            if (velocity == null) {
                return;
            }
            
            Location loc = block.getLocation();
            if (loc == null || loc.getWorld() == null) {
                return;
            }
            
            Block blockBelow = loc.clone().subtract(0, 0.1, 0).getBlock();
            if (blockBelow == null) {
                return;
            }
            
            // If sand has no velocity but there's air below, it should be falling
            if (velocity.length() < 0.01 && 
                (blockBelow.getType() == Material.AIR || 
                 blockBelow.getType() == Material.CAVE_AIR ||
                 !blockBelow.getType().isSolid())) {
                
                // Force sand to fall
                velocity.setY(-0.98);
                block.setVelocity(velocity);
                
                if (Conf.sandFixLogFloating && Conf.logFactionCreate) {
                    com.massivecraft.factions.util.Logger.print("Fixed floating sand at " + 
                        loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(),
                        com.massivecraft.factions.util.Logger.PrefixType.DEFAULT);
                }
            }
        } catch (Exception e) {
            // Silently handle - block may have been removed
        }
    }

    /**
     * Fix sand hitbox/fence gate issues
     */
    public void fixSandHitbox(FallingBlock block) {
        if (block == null || !block.isValid() || block.isDead()) {
            return;
        }
        
        try {
            Location loc = block.getLocation();
            if (loc == null || loc.getWorld() == null) {
                return;
            }
            
            Block blockBelow = loc.clone().subtract(0, 0.1, 0).getBlock();
            if (blockBelow == null) {
                return;
            }
            
            // Check for fence gates or other collision issues
            Material belowType = blockBelow.getType();
            
            // Fence gates can cause sand to get stuck
            if (belowType.name().contains("FENCE_GATE") || 
                belowType.name().contains("TRAPDOOR")) {
                
                // Check if sand is stuck
                Vector velocity = block.getVelocity();
                if (velocity != null && velocity.length() < 0.01) {
                    // Sand is stuck - give it a small push
                    velocity.setY(-0.1);
                    block.setVelocity(velocity);
                }
            }
        } catch (Exception e) {
            // Silently handle - block may have been removed
        }
    }

    /**
     * Start periodic task to fix sand every tick
     */
    private void startSandTickTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!Conf.sandFixEnabled) {
                    return;
                }
                
                // Process all tracked falling blocks (use copy to avoid concurrent modification)
                List<UUID> toProcess = new ArrayList<>(trackedFallingBlocks);
                List<UUID> toRemove = new ArrayList<>();
                
                for (UUID uuid : toProcess) {
                    try {
                        org.bukkit.entity.Entity entity = FactionsPlugin.getInstance().getServer().getEntity(uuid);
                        
                        if (!(entity instanceof FallingBlock)) {
                            toRemove.add(uuid);
                            continue;
                        }
                        
                        FallingBlock block = (FallingBlock) entity;
                        
                        if (!block.isValid() || block.isDead()) {
                            toRemove.add(uuid);
                            continue;
                        }
                        
                        // Apply all fixes
                        if (Conf.sandFixTeleport) {
                            fixSandTeleport(block);
                        }
                        
                        if (Conf.sandFixVelocity) {
                            fixSandVelocity(block);
                        }
                        
                        if (Conf.sandFixDesync) {
                            fixSandDesync(block);
                        }
                        
                        if (Conf.sandFixFloating) {
                            fixFloatingSand(block);
                        }
                        
                        if (Conf.sandFixHitbox) {
                            fixSandHitbox(block);
                        }
                    } catch (Exception e) {
                        // Entity may have been removed, clean it up
                        toRemove.add(uuid);
                    }
                }
                
                // Clean up removed blocks
                for (UUID uuid : toRemove) {
                    trackedFallingBlocks.remove(uuid);
                    blockVelocities.remove(uuid);
                    blockSpawnTimes.remove(uuid);
                    lastPositions.remove(uuid);
                }
            }
        }.runTaskTimer(FactionsPlugin.getInstance(), 0L, 1L); // Every tick
    }

    /**
     * Clean up all tracked blocks
     */
    public void cleanup() {
        trackedFallingBlocks.clear();
        blockVelocities.clear();
        blockSpawnTimes.clear();
        lastPositions.clear();
    }
}

