package com.pandora.events.util;

import com.pandora.events.PandoraEventsPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates the event base structure with 4 outer walls and 3 interior walls
 * Now generates 50x50 cube (instead of 100x100) loaded in chunks over 1 minute
 */
public class EventStructureGenerator {
    
    private static final Material WALL_MATERIAL = Material.OBSIDIAN;
    private static final Material FLOOR_MATERIAL = Material.OBSIDIAN;
    
    // Track generated obsidian blocks (these get health, player-placed ones don't)
    private static final Set<String> generatedBlocks = ConcurrentHashMap.newKeySet();
    
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
        
        // Interior walls: 3 walls, approximate
        int interiorWallBlocks = 3 * height * (width / 2 + depth / 2);
        
        return wallBlocks + floorBlocks + roofBlocks + interiorWallBlocks;
    }
    
    /**
     * Generate event base structure
     * @param faction The event faction object
     * @param center The center location
     * @param radiusChunks Radius in chunks (4 = 100x100 claim area, but cube is 50x50)
     * @param floorY The Y level for the floor
     * @param beaconLoc The beacon location
     */
    public static void generateEventStructure(Object faction, Location center, int radiusChunks, int floorY, Location beaconLoc) {
        World world = center.getWorld();
        if (world == null) {
            PandoraEventsPlugin.getInstance().getLogger().warning("Cannot generate event structure: World is null");
            return;
        }
        
        int maxHeight = floorY + 100;
        // Calculate block coordinates for 50x50 cube (centered in the 100x100 claim)
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();
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
        
        PandoraEventsPlugin.getInstance().getLogger().info("Generating 50x50 event structure over 1 minute (" + totalBlocks + " blocks, ~" + blocksPerTick + " per tick)");
        
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
            private int roofX = minX;
            private int roofZ = minZ;
            
            // Interior walls state (3 interior walls)
            private boolean interiorWallsDone = false;
            private int interiorWallIndex = 0;
            private int interiorWallZ = minZ + 1;
            private int interiorWallY = floorY;
            
            @Override
            public void run() {
                // Check if we've exceeded 1 minute
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > DURATION_MS) {
                    // Force completion if time exceeded
                    if (!wallsDone) wallsDone = true;
                    if (!floorDone) floorDone = true;
                    if (!roofDone) roofDone = true;
                    if (!interiorWallsDone) interiorWallsDone = true;
                }
                
                int processed = 0;
                int targetPerTick = blocksPerTick;
                
                // Generate outer walls first
                if (!wallsDone) {
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
                if (wallsDone && !floorDone) {
                    while (processed < targetPerTick && floorX <= maxX) {
                        if (floorZ < maxZ) {
                            Location loc = new Location(world, floorX, floorY, floorZ);
                            
                            // Skip beacon location
                            if (beaconLoc != null && 
                                loc.getBlockX() == beaconLoc.getBlockX() && 
                                loc.getBlockZ() == beaconLoc.getBlockZ() &&
                                loc.getBlockY() == beaconLoc.getBlockY() - 1) {
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
                
                // Generate roof
                if (wallsDone && floorDone && !roofDone) {
                    int roofY = maxHeight - 1;
                    while (processed < targetPerTick && roofX <= maxX) {
                        if (roofZ <= maxZ) {
                            Location loc = new Location(world, roofX, roofY, roofZ);
                            Block block = loc.getBlock();
                            
                            if (block.getType() != WALL_MATERIAL) {
                                block.setType(WALL_MATERIAL, false);
                                generatedBlocks.add(getBlockKey(loc));
                                blocksPlaced++;
                                processed++;
                            }
                            
                            roofZ++;
                        } else {
                            roofZ = minZ;
                            roofX++;
                            if (roofX > maxX) {
                                roofDone = true;
                                break;
                            }
                        }
                    }
                } else if (wallsDone && floorDone) {
                    roofDone = true;
                }
                
                // Generate 3 interior walls
                if (wallsDone && floorDone && roofDone && !interiorWallsDone) {
                    int wallHeight = maxHeight - floorY - 1;
                    
                    // Wall 1: Vertical wall at 1/4 from west
                    int wall1X = minX + (maxX - minX) / 4;
                    // Wall 2: Vertical wall at 3/4 from west
                    int wall2X = minX + 3 * (maxX - minX) / 4;
                    // Wall 3: Horizontal wall at center
                    int wall3Z = (minZ + maxZ) / 2;
                    
                    while (processed < targetPerTick && !interiorWallsDone && interiorWallY < maxHeight) {
                        if (interiorWallIndex == 0) {
                            // Wall 1: Vertical
                            if (interiorWallZ < maxZ) {
                                Location loc = new Location(world, wall1X, interiorWallY, interiorWallZ);
                                Block block = loc.getBlock();
                                if (block.getType() != WALL_MATERIAL) {
                                    block.setType(WALL_MATERIAL, false);
                                    generatedBlocks.add(getBlockKey(loc));
                                    blocksPlaced++;
                                    processed++;
                                }
                                interiorWallZ++;
                            } else {
                                interiorWallZ = minZ + 1;
                                interiorWallY++;
                                if (interiorWallY >= maxHeight) {
                                    interiorWallIndex = 1;
                                    interiorWallY = floorY;
                                    interiorWallZ = minZ + 1;
                                }
                            }
                        } else if (interiorWallIndex == 1) {
                            // Wall 2: Vertical
                            if (interiorWallZ < maxZ) {
                                Location loc = new Location(world, wall2X, interiorWallY, interiorWallZ);
                                Block block = loc.getBlock();
                                if (block.getType() != WALL_MATERIAL) {
                                    block.setType(WALL_MATERIAL, false);
                                    generatedBlocks.add(getBlockKey(loc));
                                    blocksPlaced++;
                                    processed++;
                                }
                                interiorWallZ++;
                            } else {
                                interiorWallZ = minZ + 1;
                                interiorWallY++;
                                if (interiorWallY >= maxHeight) {
                                    interiorWallIndex = 2;
                                    interiorWallY = floorY;
                                    interiorWallZ = minX + 1;
                                }
                            }
                        } else {
                            // Wall 3: Horizontal
                            if (interiorWallZ < maxX) {
                                Location loc = new Location(world, interiorWallZ, interiorWallY, wall3Z);
                                Block block = loc.getBlock();
                                if (block.getType() != WALL_MATERIAL) {
                                    block.setType(WALL_MATERIAL, false);
                                    generatedBlocks.add(getBlockKey(loc));
                                    blocksPlaced++;
                                    processed++;
                                }
                                interiorWallZ++;
                            } else {
                                interiorWallZ = minX + 1;
                                interiorWallY++;
                                if (interiorWallY >= maxHeight) {
                                    interiorWallsDone = true;
                                    break;
                                }
                            }
                        }
                    }
                } else if (wallsDone && floorDone && roofDone) {
                    interiorWallsDone = true;
                }
                
                // Check if done
                if (wallsDone && floorDone && roofDone && interiorWallsDone) {
                    PandoraEventsPlugin.getInstance().getLogger().info(
                        "Generated event base structure: " + blocksPlaced + " blocks placed over " + 
                        ((System.currentTimeMillis() - startTime) / 1000) + " seconds"
                    );
                    cancel();
                }
            }
        }.runTaskTimer(PandoraEventsPlugin.getInstance(), 0L, 1L);
    }
}
