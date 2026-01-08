package com.massivecraft.factions.util;

import com.massivecraft.factions.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class BaseRoofValidator {

    /**
     * Check if a base has any roof violations (non-air blocks above interior)
     * Optimized with early exit and chunk-based checking
     * @param faction The faction to check
     * @param centerChunk Center chunk location
     * @param radiusChunks Radius in chunks
     * @param floorY Floor Y level
     * @return List of violating block locations, empty if valid
     */
    public static List<Location> validateRoof(Faction faction, FLocation centerChunk, int radiusChunks, int floorY) {
        List<Location> violations = new ArrayList<>();
        
        if (!faction.hasBeacon()) {
            return violations; // No beacon = can't validate
        }

        World world = centerChunk.getWorld();
        if (world == null) {
            return violations;
        }

        int maxHeight = world.getMaxHeight();
        int minX = (centerChunk.getIntX() - radiusChunks) * 16 + 1;
        int maxX = (centerChunk.getIntX() + radiusChunks) * 16 - 1;
        int minZ = (centerChunk.getIntZ() - radiusChunks) * 16 + 1;
        int maxZ = (centerChunk.getIntZ() + radiusChunks) * 16 - 1;

        // Optimize: Check chunks instead of individual blocks
        // Only check loaded chunks to avoid performance issues
        int maxViolations = 1000; // Limit violations found per check to avoid lag
        
        // Check interior area (not walls) - optimized with chunk loading check
        for (int x = minX; x <= maxX && violations.size() < maxViolations; x++) {
            for (int z = minZ; z <= maxZ && violations.size() < maxViolations; z++) {
                // Check if chunk is loaded
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue; // Skip unloaded chunks
                }
                
                // Check from floor+1 to maxHeight
                // Optimize: Start from top and work down (most violations likely near top)
                for (int y = maxHeight - 1; y > floorY && violations.size() < maxViolations; y--) {
                    Location loc = new Location(world, x, y, z);
                    Block block = loc.getBlock();
                    
                    // Fast check: is it air?
                    if (block.getType() == Material.AIR) {
                        continue; // Air is fine
                    }
                    
                    // Non-air block found
                    // Check if it's a transparent block (glass, etc) - configurable
                    if (isTransparentBlock(block.getType()) && Conf.baseAllowTransparentRoof) {
                        continue; // Allow transparent blocks if configured
                    }
                    
                    violations.add(loc);
                    
                    // Early exit optimization: if we found many violations, stop checking this column
                    // (likely a full roof, no need to check every block)
                    if (violations.size() >= maxViolations) {
                        break;
                    }
                }
            }
        }

        return violations;
    }

    /**
     * Check if beacon has clear sky access
     * Optimized with chunk loading check
     * @param beaconLoc Beacon location
     * @return true if clear to sky
     */
    public static boolean validateBeaconSkyAccess(Location beaconLoc) {
        if (beaconLoc == null || beaconLoc.getWorld() == null) {
            return false;
        }

        World world = beaconLoc.getWorld();
        int maxHeight = world.getMaxHeight();
        int beaconY = beaconLoc.getBlockY();
        int beaconX = beaconLoc.getBlockX();
        int beaconZ = beaconLoc.getBlockZ();

        // Check if chunk is loaded
        if (!world.isChunkLoaded(beaconX >> 4, beaconZ >> 4)) {
            return true; // Assume valid if chunk not loaded (can't check)
        }

        // Optimize: Check from top down (most likely violations near top)
        // Also check in larger steps first, then refine
        int stepSize = Math.max(1, (maxHeight - beaconY) / 100); // Adaptive step size
        
        // First pass: large steps
        for (int y = maxHeight - 1; y > beaconY; y -= stepSize) {
            Location checkLoc = new Location(world, beaconX, y, beaconZ);
            Block block = checkLoc.getBlock();
            
            if (block.getType() != Material.AIR) {
                // Found a block - do detailed check around it
                int startY = Math.max(beaconY + 1, y - stepSize);
                int endY = Math.min(maxHeight, y + stepSize);
                
                for (int checkY = startY; checkY < endY; checkY++) {
                    Location detailedLoc = new Location(world, beaconX, checkY, beaconZ);
                    Block detailedBlock = detailedLoc.getBlock();
                    
                    if (detailedBlock.getType() != Material.AIR) {
                        // Check if transparent blocks are allowed
                        if (isTransparentBlock(detailedBlock.getType()) && Conf.baseAllowTransparentBeaconColumn) {
                            continue;
                        }
                        return false; // Blocked
                    }
                }
            }
        }

        return true; // Clear to sky
    }

    /**
     * Check if a material is transparent (glass, etc)
     * Uses Material.isTransparent() if available, otherwise checks common transparent materials
     */
    private static boolean isTransparentBlock(Material material) {
        // Use Bukkit's built-in check if available (1.13+)
        try {
            if (material.isTransparent()) {
                return true;
            }
        } catch (Exception e) {
            // Fallback for older versions - continue with manual check
        }
        
        // Manual check for common transparent blocks
        return material == Material.GLASS ||
               material == Material.GLASS_PANE ||
               material == Material.WHITE_STAINED_GLASS ||
               material == Material.ORANGE_STAINED_GLASS ||
               material == Material.MAGENTA_STAINED_GLASS ||
               material == Material.LIGHT_BLUE_STAINED_GLASS ||
               material == Material.YELLOW_STAINED_GLASS ||
               material == Material.LIME_STAINED_GLASS ||
               material == Material.PINK_STAINED_GLASS ||
               material == Material.GRAY_STAINED_GLASS ||
               material == Material.LIGHT_GRAY_STAINED_GLASS ||
               material == Material.CYAN_STAINED_GLASS ||
               material == Material.PURPLE_STAINED_GLASS ||
               material == Material.BLUE_STAINED_GLASS ||
               material == Material.BROWN_STAINED_GLASS ||
               material == Material.GREEN_STAINED_GLASS ||
               material == Material.RED_STAINED_GLASS ||
               material == Material.BLACK_STAINED_GLASS ||
               material == Material.ICE ||
               material == Material.PACKED_ICE ||
               material == Material.BLUE_ICE ||
               material == Material.FROSTED_ICE;
    }

    /**
     * Remove roof violations
     * @param violations List of violating block locations
     * @return Number of blocks removed
     */
    public static int removeRoofViolations(List<Location> violations) {
        int removed = 0;
        for (Location loc : violations) {
            if (loc.getBlock().getType() != Material.AIR) {
                loc.getBlock().setType(Material.AIR, false);
                removed++;
            }
        }
        return removed;
    }
}

