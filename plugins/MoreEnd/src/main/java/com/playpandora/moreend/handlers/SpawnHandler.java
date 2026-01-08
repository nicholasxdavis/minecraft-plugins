package com.playpandora.moreend.handlers;

import com.playpandora.moreend.MoreEnd;
import com.playpandora.moreend.utils.ConfigManager;
import com.playpandora.moreend.utils.DimensionChecker;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class SpawnHandler implements Listener {
    private final MoreEnd plugin;
    private final ConfigManager config;
    
    public SpawnHandler(MoreEnd plugin) {
        this.plugin = plugin;
        this.config = new ConfigManager(plugin);
    }
    
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!DimensionChecker.isEnd(event.getLocation())) {
            return;
        }
        
        if (!config.isFeatureEnabled("spawn-rates")) {
            return;
        }
        
        EntityType type = event.getEntityType();
        
        // Enderman spawns near void edges
        if (type == EntityType.ENDERMAN) {
            double voidY = 0;
            double distanceFromVoid = Math.abs(event.getLocation().getY() - voidY);
            if (distanceFromVoid <= 10) {
                double multiplier = config.getMultiplier("spawn-rates", "enderman-void-edge-multiplier");
                if (multiplier > 1.0 && Math.random() < (multiplier - 1.0) * 0.2) {
                    spawnAdditionalMob(event.getLocation(), type);
                }
            }
        }
        
        // Shulker spawns in End Cities
        if (type == EntityType.SHULKER) {
            double shulkerMultiplier = config.getMultiplier("spawn-rates", "shulker-city-multiplier");
            if (shulkerMultiplier > 1.0 && Math.random() < (shulkerMultiplier - 1.0) * 0.2) {
                spawnAdditionalMob(event.getLocation(), type);
            }
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

