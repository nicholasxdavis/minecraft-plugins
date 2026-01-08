package com.playpandora.morecaves.handlers;

import com.playpandora.morecaves.MoreCaves;
import com.playpandora.morecaves.utils.*;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class SpawnHandler implements Listener {
    private final MoreCaves plugin;
    private final ConfigManager config;
    
    public SpawnHandler(MoreCaves plugin) {
        this.plugin = plugin;
        this.config = new ConfigManager(plugin);
    }
    
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!CaveChecker.isInCave(event.getLocation())) {
            return;
        }
        
        if (!config.isFeatureEnabled("spawn-rates")) {
            return;
        }
        
        EntityType type = event.getEntityType();
        
        // Low light multiplier
        if (LightLevelChecker.isLowLight(event.getLocation())) {
            double multiplier = config.getMultiplier("spawn-rates", "low-light-multiplier");
            if (multiplier > 1.0 && Math.random() < (multiplier - 1.0) * 0.3) {
                // Allow additional spawn
            }
        }
        
        // Specific mob spawn adjustments
        switch (type) {
            case ZOMBIE:
                // Zombie groups instead of solos
                if (config.getConfig().getBoolean("features.spawn-rates.zombie-group-spawn", true)) {
                    if (Math.random() < 0.3) {
                        spawnAdditionalMob(event.getLocation(), type);
                    }
                }
                break;
            case SKELETON:
                // Skeleton spacing - spawn additional if far enough
                if (config.getConfig().getBoolean("features.spawn-rates.skeleton-spacing", true)) {
                    if (Math.random() < 0.25) {
                        spawnAdditionalMob(event.getLocation(), type);
                    }
                }
                break;
            case CREEPER:
                double creeperMultiplier = config.getMultiplier("spawn-rates", "creeper-increase");
                if (creeperMultiplier > 1.0 && Math.random() < (creeperMultiplier - 1.0) * 0.2) {
                    spawnAdditionalMob(event.getLocation(), type);
                }
                break;
            case SPIDER:
                String biome = event.getLocation().getBlock().getBiome().toString();
                if (biome.contains("LUSH_CAVES")) {
                    double spiderMultiplier = config.getMultiplier("spawn-rates", "spider-lush-cave-multiplier");
                    if (spiderMultiplier > 1.0 && Math.random() < (spiderMultiplier - 1.0) * 0.2) {
                        spawnAdditionalMob(event.getLocation(), type);
                    }
                }
                break;
        }
    }
    
    private void spawnAdditionalMob(org.bukkit.Location location, EntityType type) {
        // First, try to add to an existing stack instead of spawning a new mob
        if (plugin.getIntegrationManager().tryAddToStack(location, type)) {
            return; // Successfully added to existing stack
        }
        
        // If no suitable stack found, spawn a new mob
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 5; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 4;
            double offsetZ = (random.nextDouble() - 0.5) * 4;
            org.bukkit.Location spawnLoc = location.clone().add(offsetX, 0, offsetZ);
            spawnLoc.setY(location.getY());
            org.bukkit.block.Block block = spawnLoc.getBlock();
            if (block.getType().isAir() && block.getRelative(0, 1, 0).getType().isAir()) {
                org.bukkit.entity.LivingEntity entity = (org.bukkit.entity.LivingEntity) 
                    location.getWorld().spawnEntity(spawnLoc, type, 
                        org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM);
                plugin.getIntegrationManager().applyLevelledMobs(entity);
                break;
            }
        }
    }
}

