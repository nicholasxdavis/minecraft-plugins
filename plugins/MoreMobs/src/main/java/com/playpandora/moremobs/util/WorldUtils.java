package com.playpandora.moremobs.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WorldUtils {
    
    private static final Random random = new Random();
    
    /**
     * Find nearby structures by checking for structure-specific blocks
     */
    public static boolean isNearStructure(Location loc, StructureType structureType, int radius) {
        int checkRadius = Math.min(radius, 128); // Limit for performance
        int centerX = loc.getBlockX();
        int centerY = loc.getBlockY();
        int centerZ = loc.getBlockZ();
        World world = loc.getWorld();
        
        if (world == null) return false;
        
        // Sample blocks in a sphere
        int samples = Math.max(1, checkRadius * checkRadius / 64);
        for (int i = 0; i < samples; i++) {
            int x = centerX + random.nextInt(checkRadius * 2) - checkRadius;
            int y = Math.max(world.getMinHeight(), Math.min(world.getMaxHeight() - 1, 
                    centerY + random.nextInt(checkRadius * 2) - checkRadius));
            int z = centerZ + random.nextInt(checkRadius * 2) - checkRadius;
            
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();
            
            switch (structureType) {
                case SHIPWRECK:
                    if (type == Material.CHEST || type == Material.BARREL || 
                        type == Material.OAK_SLAB || type == Material.SPRUCE_SLAB) {
                        // Check for water around it (indicating shipwreck)
                        if (isInWater(block)) {
                            return true;
                        }
                    }
                    break;
                case OCEAN_RUINS:
                    if (type == Material.MOSSY_COBBLESTONE || type == Material.STONE_BRICKS) {
                        if (isInWater(block)) {
                            return true;
                        }
                    }
                    break;
                case TRIAL_CHAMBERS:
                    if (type == Material.COBBLED_DEEPSLATE || type == Material.DEEPSLATE_BRICKS ||
                        type == Material.TRIAL_SPAWNER) {
                        return true;
                    }
                    break;
                case VILLAGE:
                    // Check for village blocks
                    if (type == Material.COMPOSTER || type == Material.BELL || 
                        type == Material.LECTERN || type == Material.SMITHING_TABLE) {
                        return true;
                    }
                    break;
                default:
                    break;
            }
        }
        
        return false;
    }
    
    private static boolean isInWater(Block block) {
        Material type = block.getType();
        return type == Material.WATER || type == Material.KELP || 
               type == Material.SEAGRASS || block.getRelative(0, 1, 0).getType() == Material.WATER;
    }
    
    /**
     * Count blocks of a specific type in an area
     */
    public static int countBlocksInArea(Location center, Material material, int radius, int height) {
        int count = 0;
        World world = center.getWorld();
        if (world == null) return 0;
        
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();
        
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                for (int y = centerY - height; y <= centerY + height; y++) {
                    if (y < world.getMinHeight() || y >= world.getMaxHeight()) continue;
                    
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == material) {
                        count++;
                    }
                }
            }
        }
        
        return count;
    }
    
    /**
     * Find a safe spawn location near the given location
     */
    public static Location findSafeSpawnLocation(Location center, int radius, EntityType entityType) {
        World world = center.getWorld();
        if (world == null) return null;
        
        for (int attempt = 0; attempt < 20; attempt++) {
            int x = center.getBlockX() + random.nextInt(radius * 2) - radius;
            int z = center.getBlockZ() + random.nextInt(radius * 2) - radius;
            
            // Find suitable Y level
            int y = world.getHighestBlockYAt(x, z);
            Location testLoc = new Location(world, x + 0.5, y + 1, z + 0.5);
            
            if (isSafeSpawnLocation(testLoc, entityType)) {
                return testLoc;
            }
        }
        
        return null;
    }
    
    private static boolean isSafeSpawnLocation(Location loc, EntityType entityType) {
        Block block = loc.getBlock();
        Block below = loc.getBlock().getRelative(0, -1, 0);
        
        // Check if spawn location is safe
        if (block.getType().isSolid() || below.getType().isAir()) {
            return false;
        }
        
        // Water creatures need water
        if (entityType == EntityType.DOLPHIN || entityType == EntityType.AXOLOTL) {
            return block.getType() == Material.WATER || 
                   block.getRelative(0, -1, 0).getType() == Material.WATER;
        }
        
        // Lava creatures need lava
        if (entityType == EntityType.STRIDER) {
            return block.getType() == Material.LAVA || 
                   below.getType() == Material.LAVA;
        }
        
        return true;
    }
    
    /**
     * Find nearby entities of a specific type
     */
    public static List<LivingEntity> findNearbyEntities(Location center, double radius, EntityType entityType) {
        List<LivingEntity> entities = new ArrayList<>();
        if (center.getWorld() == null) return entities;
        
        for (LivingEntity entity : center.getWorld().getLivingEntities()) {
            if (entity.getType() == entityType && entity.getLocation().distance(center) <= radius) {
                entities.add(entity);
            }
        }
        
        return entities;
    }
    
    /**
     * Check if a location is in a warm biome
     */
    public static boolean isWarmBiome(Location loc) {
        if (loc.getWorld() == null) return false;
        String biomeName = loc.getBlock().getBiome().getKey().toString().toLowerCase();
        
        return biomeName.contains("desert") || biomeName.contains("savanna") ||
               biomeName.contains("badlands") || biomeName.contains("jungle") ||
               biomeName.contains("warm_ocean") || biomeName.contains("beach");
    }
    
    /**
     * Check if location is in a swamp biome
     */
    public static boolean isSwampBiome(Location loc) {
        if (loc.getWorld() == null) return false;
        String biomeName = loc.getBlock().getBiome().getKey().toString().toLowerCase();
        return biomeName.contains("swamp");
    }
    
    public enum StructureType {
        SHIPWRECK,
        OCEAN_RUINS,
        TRIAL_CHAMBERS,
        VILLAGE
    }
}

