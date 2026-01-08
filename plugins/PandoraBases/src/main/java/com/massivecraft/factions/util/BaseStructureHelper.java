package com.massivecraft.factions.util;

import com.massivecraft.factions.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

/**
 * Helper class to identify and manage base structure blocks (walls and floors)
 */
public class BaseStructureHelper {

    private static final Material WALL_MATERIAL = Material.OBSIDIAN;
    private static final Material FLOOR_MATERIAL = Material.OBSIDIAN;
    private static final int CLAIM_RADIUS_CHUNKS = 4;

    /**
     * Check if a block is part of a base's wall structure
     */
    public static boolean isBaseWall(Block block) {
        if (block.getType() != WALL_MATERIAL) {
            return false;
        }

        Location loc = block.getLocation();
        FLocation floc = FLocation.wrap(loc);
        Faction faction = Board.getInstance().getFactionAt(floc);

        if (faction == null || !faction.isNormal() || !faction.hasBeacon()) {
            return false;
        }

        Location beaconLoc = faction.getBeaconLocation();
        if (beaconLoc == null || !beaconLoc.getWorld().equals(loc.getWorld())) {
            return false;
        }

        FLocation centerChunk = FLocation.wrap(beaconLoc);
        int minX = (centerChunk.getIntX() - CLAIM_RADIUS_CHUNKS) * 16;
        int maxX = (centerChunk.getIntX() + CLAIM_RADIUS_CHUNKS) * 16 + 15;
        int minZ = (centerChunk.getIntZ() - CLAIM_RADIUS_CHUNKS) * 16;
        int maxZ = (centerChunk.getIntZ() + CLAIM_RADIUS_CHUNKS) * 16 + 15;

        int blockX = loc.getBlockX();
        int blockZ = loc.getBlockZ();

        // Check if on perimeter (wall)
        return (blockX == minX || blockX == maxX || blockZ == minZ || blockZ == maxZ);
    }

    /**
     * Check if a block is part of a base's floor structure
     */
    public static boolean isBaseFloor(Block block) {
        if (block.getType() != FLOOR_MATERIAL) {
            return false;
        }

        Location loc = block.getLocation();
        FLocation floc = FLocation.wrap(loc);
        Faction faction = Board.getInstance().getFactionAt(floc);

        if (faction == null || !faction.isNormal() || !faction.hasBeacon()) {
            return false;
        }

        Location beaconLoc = faction.getBeaconLocation();
        if (beaconLoc == null || !beaconLoc.getWorld().equals(loc.getWorld())) {
            return false;
        }

        // Floor is 1 block below the beacon
        int floorY = beaconLoc.getBlockY() - 1;
        if (loc.getBlockY() != floorY) {
            return false;
        }

        FLocation centerChunk = FLocation.wrap(beaconLoc);
        int minX = (centerChunk.getIntX() - CLAIM_RADIUS_CHUNKS) * 16 + 1;
        int maxX = (centerChunk.getIntX() + CLAIM_RADIUS_CHUNKS) * 16 - 1;
        int minZ = (centerChunk.getIntZ() - CLAIM_RADIUS_CHUNKS) * 16 + 1;
        int maxZ = (centerChunk.getIntZ() + CLAIM_RADIUS_CHUNKS) * 16 - 1;

        int blockX = loc.getBlockX();
        int blockZ = loc.getBlockZ();

        // Check if inside interior (not walls)
        return blockX >= minX && blockX <= maxX && blockZ >= minZ && blockZ <= maxZ;
    }

    /**
     * Check if a block is part of base structure (wall or floor)
     */
    public static boolean isBaseStructure(Block block) {
        return isBaseWall(block) || isBaseFloor(block);
    }

    /**
     * Get the faction that owns this base structure block
     */
    public static Faction getBaseStructureFaction(Block block) {
        Location loc = block.getLocation();
        FLocation floc = FLocation.wrap(loc);
        Faction faction = Board.getInstance().getFactionAt(floc);

        if (faction != null && faction.isNormal() && faction.hasBeacon() && isBaseStructure(block)) {
            return faction;
        }

        return null;
    }
    
    /**
     * Check if a location is inside a base cube (the interior area)
     * @param loc The location to check
     * @param faction The faction to check against
     * @return true if the location is inside the base cube
     */
    public static boolean isInsideBaseCube(Location loc, Faction faction) {
        if (faction == null || !faction.isNormal() || !faction.hasBeacon()) {
            return false;
        }
        
        Location beaconLoc = faction.getBeaconLocation();
        if (beaconLoc == null || !beaconLoc.getWorld().equals(loc.getWorld())) {
            return false;
        }
        
        FLocation centerChunk = FLocation.wrap(beaconLoc);
        int minX = (centerChunk.getIntX() - CLAIM_RADIUS_CHUNKS) * 16 + 1;
        int maxX = (centerChunk.getIntX() + CLAIM_RADIUS_CHUNKS) * 16 - 1;
        int minZ = (centerChunk.getIntZ() - CLAIM_RADIUS_CHUNKS) * 16 + 1;
        int maxZ = (centerChunk.getIntZ() + CLAIM_RADIUS_CHUNKS) * 16 - 1;
        
        int blockX = loc.getBlockX();
        int blockZ = loc.getBlockZ();
        
        // Check if inside interior (not walls)
        return blockX >= minX && blockX <= maxX && blockZ >= minZ && blockZ <= maxZ;
    }
    
    /**
     * Get the bounds of a base cube
     * @param faction The faction
     * @return An array [minX, maxX, minZ, maxZ, floorY, maxY] or null if invalid
     */
    public static int[] getBaseCubeBounds(Faction faction) {
        if (faction == null || !faction.isNormal() || !faction.hasBeacon()) {
            return null;
        }
        
        Location beaconLoc = faction.getBeaconLocation();
        if (beaconLoc == null) {
            return null;
        }
        
        FLocation centerChunk = FLocation.wrap(beaconLoc);
        int minX = (centerChunk.getIntX() - CLAIM_RADIUS_CHUNKS) * 16 + 1;
        int maxX = (centerChunk.getIntX() + CLAIM_RADIUS_CHUNKS) * 16 - 1;
        int minZ = (centerChunk.getIntZ() - CLAIM_RADIUS_CHUNKS) * 16 + 1;
        int maxZ = (centerChunk.getIntZ() + CLAIM_RADIUS_CHUNKS) * 16 - 1;
        int floorY = beaconLoc.getBlockY() - 1;
        int maxY = floorY + 100; // Base cube is 100 blocks high
        
        return new int[]{minX, maxX, minZ, maxZ, floorY, maxY};
    }
}



