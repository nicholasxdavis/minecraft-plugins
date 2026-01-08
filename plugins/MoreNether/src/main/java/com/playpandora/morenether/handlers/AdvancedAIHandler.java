package com.playpandora.morenether.handlers;

import com.playpandora.morenether.MoreNether;
import com.playpandora.morenether.utils.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;

import java.util.List;
import java.util.stream.Collectors;

public class AdvancedAIHandler implements Listener {
    private final MoreNether plugin;
    private final ConfigManager config;
    private final AdvancedAIManager aiManager;
    private final DifficultyScaler difficultyScaler;
    
    public AdvancedAIHandler(MoreNether plugin) {
        this.plugin = plugin;
        this.config = new ConfigManager(plugin);
        this.aiManager = new AdvancedAIManager(plugin);
        this.difficultyScaler = new DifficultyScaler(plugin);
    }
    
    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player)) return;
        if (!DimensionChecker.isNether(event.getEntity().getLocation())) return;
        
        if (!config.isFeatureEnabled("advanced-ai")) return;
        
        Player player = (Player) event.getTarget();
        LivingEntity entity = (LivingEntity) event.getEntity();
        
        if (entity instanceof WitherSkeleton && 
            config.isFeatureEnabled("advanced-ai.wither-skeleton")) {
            
            if (config.getConfig().getBoolean("features.advanced-ai.wither-skeleton.predictive-positioning", true)) {
                aiManager.setPredictivePositioning(entity, player, 1.0);
            }
            
            if (config.getConfig().getBoolean("features.advanced-ai.wither-skeleton.coordinated-attacks", true)) {
                List<LivingEntity> nearby = entity.getNearbyEntities(8, 8, 8).stream()
                    .filter(e -> e instanceof WitherSkeleton)
                    .map(e -> (LivingEntity) e)
                    .collect(Collectors.toList());
                
                if (nearby.size() >= 2) {
                    nearby.add(entity);
                    aiManager.coordinateGroupAttack(nearby, player);
                }
            }
        }
        
        if ((entity instanceof Piglin || entity instanceof PiglinBrute) &&
            config.isFeatureEnabled("advanced-ai.piglin")) {
            
            if (config.getConfig().getBoolean("features.advanced-ai.piglin.coordinated-attacks", true)) {
                List<LivingEntity> nearby = entity.getNearbyEntities(10, 10, 10).stream()
                    .filter(e -> e instanceof Piglin || e instanceof PiglinBrute)
                    .map(e -> (LivingEntity) e)
                    .collect(Collectors.toList());
                
                if (nearby.size() >= 3) {
                    nearby.add(entity);
                    aiManager.coordinateGroupAttack(nearby, player);
                }
            }
        }
        
        if (entity instanceof Hoglin &&
            config.isFeatureEnabled("advanced-ai.hoglin")) {
            
            if (config.getConfig().getBoolean("features.advanced-ai.hoglin.pack-hunting", true)) {
                List<LivingEntity> pack = entity.getNearbyEntities(12, 12, 12).stream()
                    .filter(e -> e instanceof Hoglin)
                    .map(e -> (LivingEntity) e)
                    .collect(Collectors.toList());
                
                if (pack.size() >= 4) {
                    pack.add(entity);
                    pack.forEach(hoglin -> {
                        if (hoglin instanceof org.bukkit.entity.Mob) {
                            ((org.bukkit.entity.Mob) hoglin).setTarget(player);
                        }
                        org.bukkit.util.Vector charge = player.getLocation().toVector()
                            .subtract(hoglin.getLocation().toVector()).normalize();
                        charge.multiply(0.5);
                        hoglin.setVelocity(hoglin.getVelocity().add(charge));
                    });
                }
            }
        }
    }
}

