package com.playpandora.morenether.handlers;

import com.playpandora.morenether.MoreNether;
import com.playpandora.morenether.utils.ConfigManager;
import com.playpandora.morenether.utils.DimensionChecker;
import com.playpandora.morenether.utils.LightLevelChecker;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class SpawnHandler implements Listener {
    private final MoreNether plugin;
    private final ConfigManager config;
    
    public SpawnHandler(MoreNether plugin) {
        this.plugin = plugin;
        this.config = new ConfigManager(plugin);
    }
    
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!DimensionChecker.isNether(event.getLocation())) {
            return;
        }
        
        if (!config.isFeatureEnabled("spawn-rates")) {
            return;
        }
        
        EntityType type = event.getEntityType();
        
        // Low light multiplier - spawn additional mobs
        if (LightLevelChecker.isLowLight(event.getLocation())) {
            double multiplier = config.getMultiplier("spawn-rates", "low-light-multiplier");
            if (multiplier > 1.0 && Math.random() < (multiplier - 1.0) * 0.3) {
                // Spawn additional mob of same type nearby
                spawnAdditionalMob(event.getLocation(), type);
            }
        }
        
        // Specific mob multipliers
        switch (type) {
            case BLAZE:
                double blazeMultiplier = config.getMultiplier("spawn-rates", "blaze-multiplier");
                if (blazeMultiplier > 1.0 && Math.random() < (blazeMultiplier - 1.0) * 0.2) {
                    spawnAdditionalMob(event.getLocation(), type);
                }
                break;
            case SKELETON:
                String biome = event.getLocation().getBlock().getBiome().toString();
                if (biome.contains("SOUL_SAND_VALLEY")) {
                    double skeletonMultiplier = config.getMultiplier("spawn-rates", "skeleton-valley-multiplier");
                    if (skeletonMultiplier > 1.0 && Math.random() < (skeletonMultiplier - 1.0) * 0.2) {
                        spawnAdditionalMob(event.getLocation(), type);
                    }
                }
                break;
            case HOGLIN:
                // Hoglin pack size increase
                double hoglinPackSize = config.getMultiplier("spawn-rates", "hoglin-pack-size");
                if (hoglinPackSize > 1.0) {
                    int additionalSpawns = (int) ((hoglinPackSize - 1.0) * 2);
                    for (int i = 0; i < additionalSpawns && Math.random() < 0.5; i++) {
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
        // Find a safe spawn location nearby
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 5; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 4;
            double offsetZ = (random.nextDouble() - 0.5) * 4;
            org.bukkit.Location spawnLoc = location.clone().add(offsetX, 0, offsetZ);
            
            // Find safe Y position
            spawnLoc.setY(location.getY());
            org.bukkit.block.Block block = spawnLoc.getBlock();
            if (block.getType().isAir() && block.getRelative(0, 1, 0).getType().isAir()) {
                // Spawn the mob
                org.bukkit.entity.LivingEntity entity = (org.bukkit.entity.LivingEntity) 
                    location.getWorld().spawnEntity(spawnLoc, type, 
                        org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM);
                
                // Integrate with LevelledMobs if available
                plugin.getIntegrationManager().applyLevelledMobs(entity);
                break;
            }
        }
    }
}

