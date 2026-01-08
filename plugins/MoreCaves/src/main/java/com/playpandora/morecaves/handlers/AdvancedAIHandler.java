package com.playpandora.morecaves.handlers;

import com.playpandora.morecaves.MoreCaves;
import com.playpandora.morecaves.utils.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;

import java.util.List;
import java.util.stream.Collectors;

public class AdvancedAIHandler implements Listener {
    private final MoreCaves plugin;
    private final ConfigManager config;
    private final AdvancedAIManager aiManager;
    
    public AdvancedAIHandler(MoreCaves plugin) {
        this.plugin = plugin;
        this.config = new ConfigManager(plugin);
        this.aiManager = new AdvancedAIManager(plugin);
    }
    
    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player)) return;
        if (!CaveChecker.isInCave(event.getEntity().getLocation())) return;
        
        if (!config.isFeatureEnabled("advanced-ai")) return;
        
        Player player = (Player) event.getTarget();
        LivingEntity entity = (LivingEntity) event.getEntity();
        
        if (entity instanceof Zombie &&
            config.isFeatureEnabled("advanced-ai.zombie")) {
            
            if (config.getConfig().getBoolean("features.advanced-ai.zombie.formation-attacks", true)) {
                List<LivingEntity> group = entity.getNearbyEntities(8, 8, 8).stream()
                    .filter(e -> e instanceof Zombie)
                    .map(e -> (LivingEntity) e)
                    .collect(Collectors.toList());
                
                if (group.size() >= 3) {
                    group.add(entity);
                    aiManager.coordinateGroupAttack(group, player);
                }
            }
        }
        
        if (entity instanceof Skeleton &&
            config.isFeatureEnabled("advanced-ai.skeleton")) {
            
            if (config.getConfig().getBoolean("features.advanced-ai.skeleton.sniper-coordination", true)) {
                List<LivingEntity> squad = entity.getNearbyEntities(12, 12, 12).stream()
                    .filter(e -> e instanceof Skeleton)
                    .map(e -> (LivingEntity) e)
                    .collect(Collectors.toList());
                
                if (squad.size() >= 2) {
                    squad.forEach(skeleton -> {
                        if (skeleton instanceof org.bukkit.entity.Mob) {
                            ((org.bukkit.entity.Mob) skeleton).setTarget(player);
                        }
                    });
                }
            }
        }
        
        if (entity instanceof Creeper &&
            config.isFeatureEnabled("advanced-ai.creeper")) {
            
            if (config.getConfig().getBoolean("features.advanced-ai.creeper.coordinated-detonation", true)) {
                List<Creeper> creepers = entity.getNearbyEntities(5, 5, 5).stream()
                    .filter(e -> e instanceof Creeper)
                    .map(e -> (Creeper) e)
                    .collect(Collectors.toList());
                
                if (creepers.size() >= 2) {
                    // Sync detonation timing would require more complex logic
                    creepers.forEach(creeper -> {
                        if (creeper.getTarget() == player && 
                            creeper.getLocation().distance(player.getLocation()) <= 3) {
                            // Trigger explosion together - simplified
                        }
                    });
                }
            }
        }
    }
}

