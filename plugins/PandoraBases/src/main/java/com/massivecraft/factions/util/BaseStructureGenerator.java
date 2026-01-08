package com.massivecraft.factions.util;

import com.massivecraft.factions.*;
import com.massivecraft.factions.util.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;


public class BaseStructureGenerator {

    private static final Material WALL_MATERIAL = Material.OBSIDIAN;
    private static final Material FLOOR_MATERIAL = Material.OBSIDIAN;
    
    // Track which factions have completed interior clearing to prevent re-clearing
    private static final java.util.Set<String> clearedFactions = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<String, Boolean>());
    
    // Track when blocks were placed by players to protect them from clearing
    private static final java.util.Map<org.bukkit.Location, Long> playerPlacedBlocks = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Track generated obsidian blocks (these get health, player-placed ones don't)
    private static final java.util.Set<String> generatedBlocks = java.util.concurrent.ConcurrentHashMap.newKeySet();
    
    /**
     * Check if a block was generated (has health) or player-placed (no health)
     */
    public static boolean isGeneratedBlock(Location loc) {
        String key = getBlockKey(loc);
        return generatedBlocks.contains(key);
    }
    
    /**
     * Get block key for tracking
     */
    private static String getBlockKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    /**
     * Calculate total blocks needed for structure
     */
    private static int calculateTotalBlocks(int minX, int maxX, int minZ, int maxZ, int floorY, int maxHeight) {
        int width = maxX - minX + 1;
        int depth = maxZ - minZ + 1;
        int height = maxHeight - floorY;
        
        // Walls: 4 walls * height * (width + depth - 2) (subtract corners counted twice)
        int wallBlocks = 4 * height * (width + depth) - 4 * height; // Simplified
        
        // Floor: width * depth
        int floorBlocks = width * depth;
        
        // Roof: width * depth
        int roofBlocks = width * depth;
        
        return wallBlocks + floorBlocks + roofBlocks;
    }
    
    /**
     * Generate walls and floor for a base claim
     * @param faction The faction
     * @param centerChunk The center chunk location (where beacon is placed)
     * @param radiusChunks Radius in chunks (4 = 100x100 claim area, but cube is 50x50)
     * @param floorY The Y level for the floor
     * @param beaconLoc The beacon location (to skip when generating floor)
     */
    public static void generateBaseStructure(Faction faction, FLocation centerChunk, int radiusChunks, int floorY, Location beaconLoc) {
        World world = centerChunk.getWorld();
        if (world == null) {
            Logger.print("Cannot generate base structure: World is null", Logger.PrefixType.FAILED);
            return;
        }

        // Base walls are 100 blocks high instead of world max height
        int maxHeight = floorY + 100;
        
        // Calculate block coordinates for 50x50 cube (centered in the 100x100 claim)
        int centerX = beaconLoc != null ? beaconLoc.getBlockX() : (centerChunk.getIntX() * 16 + 8);
        int centerZ = beaconLoc != null ? beaconLoc.getBlockZ() : (centerChunk.getIntZ() * 16 + 8);
        int cubeSize = 50; // 50x50 cube instead of 100x100
        int halfSize = cubeSize / 2;
        int minX = centerX - halfSize;
        int maxX = centerX + halfSize - 1;
        int minZ = centerZ - halfSize;
        int maxZ = centerZ + halfSize - 1;
        
        // Calculate total blocks to place for progress tracking
        int totalBlocks = calculateTotalBlocks(minX, maxX, minZ, maxZ, floorY, maxHeight);
        
        // Load over 1 minute (60 seconds = 1200 ticks)
        // Calculate blocks per tick to finish in 1 minute
        long totalTicks = 1200L; // 1 minute
        int blocksPerTick = Math.max(1, totalBlocks / (int)totalTicks);
        
        if (Conf.logFactionCreate) {
            Logger.print("Generating 50x50 base structure for " + faction.getTag() + " over 1 minute (" + totalBlocks + " blocks, ~" + blocksPerTick + " per tick)", Logger.PrefixType.DEFAULT);
        }

        // Generate in batches over 1 minute
        new BukkitRunnable() {
            private int blocksPlaced = 0;
            private long startTime = System.currentTimeMillis();
            private final long DURATION_MS = 60000L; // 1 minute
            
            // Wall generation state
            private int wallX = minX;
            private int wallZ = minZ;
            private int wallY = floorY;
            private boolean wallsDone = false;
            
            // Floor generation state
            private int floorX = minX + 1;
            private int floorZ = minZ + 1;
            private boolean floorDone = false;
            
            // Roof generation state
            private boolean roofDone = false;
            // Start from minX/minZ to cover entire area including walls
            private int roofX = minX;
            private int roofZ = minZ;

            @Override
            public void run() {
                // Check if we've exceeded 1 minute
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > DURATION_MS) {
                    // Force completion if time exceeded
                    if (!wallsDone) wallsDone = true;
                    if (!floorDone) floorDone = true;
                    if (!roofDone) roofDone = true;
                }
                
                int processed = 0;
                int targetPerTick = blocksPerTick;
                
                // Generate walls first
                if (!wallsDone && (Conf.baseAutoGenerateWalls)) {
                    // Generate walls: perimeter at minX, maxX, minZ, maxZ
                    while (processed < targetPerTick && !wallsDone && wallY < maxHeight) {
                        // North wall (minZ)
                        if (wallX >= minX && wallX <= maxX) {
                            Location loc = new Location(world, wallX, wallY, minZ);
                            Block block = loc.getBlock();
                            if (block.getType() != WALL_MATERIAL) {
                                block.setType(WALL_MATERIAL, false);
                                generatedBlocks.add(getBlockKey(loc));
                                blocksPlaced++;
                                processed++;
                            }
                        }
                        
                        // South wall (maxZ)
                        if (wallX >= minX && wallX <= maxX && processed < targetPerTick) {
                            Location loc = new Location(world, wallX, wallY, maxZ);
                            Block block = loc.getBlock();
                            if (block.getType() != WALL_MATERIAL) {
                                block.setType(WALL_MATERIAL, false);
                                generatedBlocks.add(getBlockKey(loc));
                                blocksPlaced++;
                                processed++;
                            }
                        }
                        
                        // West wall (minX)
                        if (wallZ >= minZ && wallZ <= maxZ && processed < targetPerTick) {
                            Location loc = new Location(world, minX, wallY, wallZ);
                            Block block = loc.getBlock();
                            if (block.getType() != WALL_MATERIAL) {
                                block.setType(WALL_MATERIAL, false);
                                generatedBlocks.add(getBlockKey(loc));
                                blocksPlaced++;
                                processed++;
                            }
                        }
                        
                        // East wall (maxX)
                        if (wallZ >= minZ && wallZ <= maxZ && processed < targetPerTick) {
                            Location loc = new Location(world, maxX, wallY, wallZ);
                            Block block = loc.getBlock();
                            if (block.getType() != WALL_MATERIAL) {
                                block.setType(WALL_MATERIAL, false);
                                generatedBlocks.add(getBlockKey(loc));
                                blocksPlaced++;
                                processed++;
                            }
                        }
                        
                        wallY++;
                        if (wallY >= maxHeight) {
                            wallY = floorY;
                            wallX++;
                            if (wallX > maxX) {
                                wallX = minX;
                                wallZ++;
                                if (wallZ > maxZ) {
                                    wallsDone = true;
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    wallsDone = true;
                }
                
                // Generate floor
                if (wallsDone && !floorDone && Conf.baseAutoGenerateFloor) {
                    while (processed < targetPerTick && floorX <= maxX) {
                        if (floorZ < maxZ) {
                            Location loc = new Location(world, floorX, floorY, floorZ);
                            
                            // Skip beacon location to avoid removing it
                            if (beaconLoc != null && 
                                loc.getBlockX() == beaconLoc.getBlockX() && 
                                loc.getBlockZ() == beaconLoc.getBlockZ() &&
                                loc.getBlockY() == beaconLoc.getBlockY() - 1) {
                                // This is directly under the beacon - skip it
                                floorZ++;
                                continue;
                            }
                            
                            Block block = loc.getBlock();
                            if (block.getType() != FLOOR_MATERIAL) {
                                block.setType(FLOOR_MATERIAL, false);
                                generatedBlocks.add(getBlockKey(loc));
                                blocksPlaced++;
                                processed++;
                            }
                            
                            floorZ++;
                        } else {
                            floorZ = minZ + 1;
                            floorX++;
                            if (floorX >= maxX) {
                                floorDone = true;
                                break;
                            }
                        }
                    }
                } else if (wallsDone) {
                    floorDone = true;
                }

                // Generate roof (at maxHeight - 1, covering the entire base area INCLUDING WALLS)
                if (wallsDone && floorDone && !roofDone && Conf.baseAutoGenerateRoof) {
                    int roofY = maxHeight - 1; // Roof at top of walls
                    // Generate roof covering entire area from minX to maxX, minZ to maxZ (including walls)
                    while (processed < targetPerTick && roofX <= maxX) {
                        if (roofZ <= maxZ) {
                            Location loc = new Location(world, roofX, roofY, roofZ);
                            Block block = loc.getBlock();
                            
                            // Place roof block if it's not already obsidian
                            // This ensures full coverage including wall perimeters
                            if (block.getType() != WALL_MATERIAL) {
                                block.setType(WALL_MATERIAL, false);
                                generatedBlocks.add(getBlockKey(loc));
                                blocksPlaced++;
                                processed++;
                            }
                            
                            roofZ++;
                        } else {
                            roofZ = minZ; // Start from minZ to cover walls
                            roofX++;
                            if (roofX > maxX) {
                                roofDone = true;
                                break;
                            }
                        }
                    }
                } else if (wallsDone && floorDone && !roofDone) {
                    roofDone = true;
                }

                // Check if done
                if (wallsDone && floorDone && roofDone) {
                    if (Conf.logFactionCreate) {
                        Logger.print("Generated base structure for " + faction.getTag() + 
                            ": " + blocksPlaced + " blocks placed over " + 
                            ((System.currentTimeMillis() - startTime) / 1000) + " seconds", Logger.PrefixType.DEFAULT);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(FactionsPlugin.getInstance(), 0L, 1L);
    }

    // Track when clearing started for each faction to prevent clearing player-placed blocks
    private static final java.util.Map<String, Long> clearingStartTimes = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long CLEARING_DURATION_MS = 30000; // 30 seconds max for clearing to complete
    
    /**
     * Mark a block location as player-placed to protect it from clearing
     */
    public static void markBlockAsPlayerPlaced(org.bukkit.Location loc) {
        if (loc != null) {
            playerPlacedBlocks.put(loc, System.currentTimeMillis());
        }
    }
    
    /**
     * Check if a block was placed by a player (should be protected from clearing)
     */
    public static boolean isPlayerPlacedBlock(org.bukkit.Location loc) {
        return playerPlacedBlocks.containsKey(loc);
    }
    
    /**
     * Clean up old player-placed block tracking (blocks placed more than 1 hour ago)
     */
    public static void cleanupOldPlayerPlacedBlocks() {
        long oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000);
        playerPlacedBlocks.entrySet().removeIf(entry -> entry.getValue() < oneHourAgo);
    }
    
    /**
     * Clear blocks in the way (non-air blocks above floor, inside walls)
     * IMPORTANT: This should only run ONCE during base creation, not continuously!
     * @param beaconLoc The beacon location to skip (don't clear the beacon)
     */
    public static void clearInteriorBlocks(Faction faction, FLocation centerChunk, int radiusChunks, int floorY, Location beaconLoc) {
        World world = centerChunk.getWorld();
        if (world == null) return;

        // IMPORTANT: Only clear ONCE per faction - if already cleared, skip!
        // This prevents clearing from running multiple times and removing player-placed blocks
        if (clearedFactions.contains(faction.getId())) {
            if (Conf.logFactionCreate) {
                Logger.print("Skipping clearInteriorBlocks for " + faction.getTag() + " - already cleared", Logger.PrefixType.DEFAULT);
            }
            return; // Already cleared, don't clear again!
        }

        // Track when clearing started - only clear blocks that existed at this time
        long clearingStartTime = System.currentTimeMillis();
        clearingStartTimes.put(faction.getId(), clearingStartTime);
        
        int minX = (centerChunk.getIntX() - radiusChunks) * 16 + 1;
        int maxX = (centerChunk.getIntX() + radiusChunks) * 16 - 1;
        int minZ = (centerChunk.getIntZ() - radiusChunks) * 16 + 1;
        int maxZ = (centerChunk.getIntZ() + radiusChunks) * 16 - 1;

        new BukkitRunnable() {
            private int currentX = minX;
            private int currentZ = minZ;
            private int blocksCleared = 0;
            private final int BATCH_SIZE = 500;
            private final long startTime = clearingStartTime;

            @Override
            public void run() {
                // Safety check: Stop clearing if it's been running too long (shouldn't happen)
                if (System.currentTimeMillis() - startTime > CLEARING_DURATION_MS) {
                    if (Conf.logFactionCreate) {
                        Logger.print("Clearing took too long, stopping for " + faction.getTag(), Logger.PrefixType.WARNING);
                    }
                    clearingStartTimes.remove(faction.getId());
                    cancel();
                    return;
                }
                
                int processed = 0;
                
                // Calculate roof Y level - roof is at maxHeight - 1, where maxHeight = floorY + 100
                // So roof is at floorY + 99
                int roofY = floorY + 99; // Roof Y level (floorY + 100 - 1)
                
                for (int x = currentX; x <= maxX && processed < BATCH_SIZE; x++) {
                    for (int z = currentZ; z <= maxZ && processed < BATCH_SIZE; z++) {
                        // Clear from floorY+1 to roofY-1 (don't clear the roof at roofY!)
                        // Only clear interior blocks below the roof, leave the roof intact
                        for (int y = floorY + 1; y < roofY && processed < BATCH_SIZE; y++) {
                            Location loc = new Location(world, x, y, z);
                            
                            // Skip beacon location - don't clear the beacon!
                            if (beaconLoc != null && 
                                loc.getBlockX() == beaconLoc.getBlockX() && 
                                loc.getBlockZ() == beaconLoc.getBlockZ() &&
                                loc.getBlockY() == beaconLoc.getBlockY()) {
                                continue; // Skip beacon
                            }
                            
                            Block block = loc.getBlock();
                            
                            // IMPORTANT: Only clear blocks that existed BEFORE base creation
                            // Skip obsidian (structure blocks - walls/floor/roof)
                            // PROTECT player-placed blocks from being cleared!
                            Material blockType = block.getType();
                            if (blockType != Material.AIR && blockType != Material.OBSIDIAN) {
                                // IMPORTANT: Don't clear player-placed blocks or blocks placed after clearing started
                                // Check if this block was placed by a player - if so, PROTECT IT!
                                if (isPlayerPlacedBlock(loc)) {
                                    continue; // Skip player-placed blocks - protect them!
                                }
                                
                                // Check if clearing has been running too long - if so, stop to avoid clearing player blocks
                                if (System.currentTimeMillis() - startTime > 10000) {
                                    // Clearing has been running for more than 10 seconds - stop to protect player blocks
                                    if (Conf.logFactionCreate) {
                                        Logger.print("Stopping clearing early for " + faction.getTag() + " to protect player blocks", Logger.PrefixType.WARNING);
                                    }
                                    clearingStartTimes.remove(faction.getId());
                                    clearedFactions.add(faction.getId()); // Mark as done to prevent re-clearing
                                    cancel();
                                    return;
                                }
                                
                                // Only clear natural blocks that existed before base creation
                                block.setType(Material.AIR, false);
                                blocksCleared++;
                                processed++;
                            }
                        }
                    }
                }

                // Move to next batch
                currentZ++;
                if (currentZ > maxZ) {
                    currentZ = minZ;
                    currentX++;
                }

                // Check if done
                if (currentX > maxX) {
                    // Mark this faction as cleared - never clear again!
                    clearedFactions.add(faction.getId());
                    clearingStartTimes.remove(faction.getId()); // Mark clearing as complete
                    if (Conf.logFactionCreate) {
                        Logger.print("Cleared " + blocksCleared + " blocks from base interior for " + 
                            faction.getTag() + " (clearing complete, will not run again)", Logger.PrefixType.DEFAULT);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(FactionsPlugin.getInstance(), 0L, 1L);
    }
    
    /**
     * Check if clearing is currently in progress for a faction
     */
    public static boolean isClearingInProgress(Faction faction) {
        return clearingStartTimes.containsKey(faction.getId());
    }
    
    /**
     * Check if a block should be protected from clearing (placed after clearing started)
     */
    public static boolean isBlockProtectedFromClearing(Faction faction, long blockPlaceTime) {
        Long clearingStart = clearingStartTimes.get(faction.getId());
        if (clearingStart == null) {
            return true; // No clearing in progress, block is safe
        }
        // Block was placed after clearing started - protect it
        return blockPlaceTime > clearingStart;
    }

    /**
     * Remove all base structure blocks (walls, floor, roof) when beacon is destroyed
     * @param faction The faction
     * @param centerChunk The center chunk location (where beacon was)
     * @param radiusChunks Radius in chunks (4 = 100x100 area)
     * @param floorY The Y level for the floor
     */
    public static void removeBaseStructure(Faction faction, FLocation centerChunk, int radiusChunks, int floorY) {
        World world = centerChunk.getWorld();
        if (world == null) {
            Logger.print("Cannot remove base structure: World is null", Logger.PrefixType.WARNING);
            return;
        }

        int maxHeight = floorY + 100; // Same as wall height
        int minX = (centerChunk.getIntX() - radiusChunks) * 16;
        int maxX = (centerChunk.getIntX() + radiusChunks) * 16 + 15;
        int minZ = (centerChunk.getIntZ() - radiusChunks) * 16;
        int maxZ = (centerChunk.getIntZ() + radiusChunks) * 16 + 15;

        // Remove in batches to avoid lag
        new BukkitRunnable() {
            private int blocksRemoved = 0;
            private final int BATCH_SIZE = 500;
            
            // Removal state
            private int currentX = minX;
            private int currentZ = minZ;
            private int currentY = 0;
            private boolean wallsRemoved = false;
            private boolean floorRemoved = false;
            private boolean roofRemoved = false;

            @Override
            public void run() {
                int processed = 0;

                // Remove walls
                if (!wallsRemoved) {
                    while (processed < BATCH_SIZE && currentX <= maxX) {
                        if (currentZ <= maxZ) {
                            // Check if on perimeter (wall)
                            boolean isWall = (currentX == minX || currentX == maxX || currentZ == minZ || currentZ == maxZ);
                            
                            if (isWall) {
                                for (int y = currentY; y < maxHeight && processed < BATCH_SIZE; y++) {
                                    Location loc = new Location(world, currentX, y, currentZ);
                                    Block block = loc.getBlock();
                                    
                                    // Only remove obsidian (our structure blocks)
                                    if (block.getType() == WALL_MATERIAL) {
                                        block.setType(Material.AIR, false);
                                        blocksRemoved++;
                                        processed++;
                                    }
                                }
                            }
                            
                            currentZ++;
                            if (currentZ > maxZ) {
                                currentZ = minZ;
                                currentX++;
                                if (currentX > maxX) {
                                    wallsRemoved = true;
                                    currentX = minX + 1;
                                    currentZ = minZ + 1;
                                    currentY = 0;
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    wallsRemoved = true;
                }

                // Remove floor
                if (wallsRemoved && !floorRemoved) {
                    while (processed < BATCH_SIZE && currentX < maxX) {
                        if (currentZ < maxZ) {
                            Location loc = new Location(world, currentX, floorY, currentZ);
                            Block block = loc.getBlock();
                            
                            // Only remove obsidian (our floor blocks)
                            if (block.getType() == FLOOR_MATERIAL) {
                                block.setType(Material.AIR, false);
                                blocksRemoved++;
                                processed++;
                            }
                            
                            currentZ++;
                        } else {
                            currentZ = minZ + 1;
                            currentX++;
                            if (currentX >= maxX) {
                                floorRemoved = true;
                                currentX = minX + 1;
                                currentZ = minZ + 1;
                                break;
                            }
                        }
                    }
                } else if (wallsRemoved) {
                    floorRemoved = true;
                }

                // Remove roof
                if (wallsRemoved && floorRemoved && !roofRemoved) {
                    int roofY = maxHeight - 1;
                    while (processed < BATCH_SIZE && currentX < maxX) {
                        if (currentZ < maxZ) {
                            Location loc = new Location(world, currentX, roofY, currentZ);
                            Block block = loc.getBlock();
                            
                            // Only remove obsidian (our roof blocks)
                            if (block.getType() == WALL_MATERIAL) {
                                block.setType(Material.AIR, false);
                                blocksRemoved++;
                                processed++;
                            }
                            
                            currentZ++;
                        } else {
                            currentZ = minZ + 1;
                            currentX++;
                            if (currentX >= maxX) {
                                roofRemoved = true;
                                break;
                            }
                        }
                    }
                } else if (wallsRemoved && floorRemoved) {
                    roofRemoved = true;
                }

                // Check if done
                if (wallsRemoved && floorRemoved && roofRemoved) {
                    if (Conf.logFactionCreate) {
                        Logger.print("Removed base structure for " + faction.getTag() + 
                            ": " + blocksRemoved + " blocks removed", Logger.PrefixType.DEFAULT);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(FactionsPlugin.getInstance(), 0L, 1L);
    }
}

