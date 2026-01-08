package com.playpandora.morenether.utils;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class AdvancedAIManager {
    private final JavaPlugin plugin;
    private final Map<UUID, AITask> activeAITasks = new HashMap<>();
    
    public AdvancedAIManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void setPredictivePositioning(LivingEntity entity, Player target, double predictionTime) {
        UUID id = entity.getUniqueId();
        
        AITask existing = activeAITasks.remove(id);
        if (existing != null) {
            existing.cancel();
        }
        
        AITask task = new AITask() {
            @Override
            public void run() {
                if (!entity.isValid() || !target.isOnline()) {
                    cancel();
                    return;
                }
                
                Location entityLoc = entity.getLocation();
                Location targetLoc = target.getLocation();
                Vector targetVelocity = target.getVelocity();
                
                Location predictedLoc = targetLoc.clone().add(
                    targetVelocity.multiply(predictionTime)
                );
                
                Vector direction = predictedLoc.toVector().subtract(entityLoc.toVector()).normalize();
                entity.setVelocity(direction.multiply(0.3));
            }
        };
        
        task.runTaskTimer(plugin, 0, 5);
        activeAITasks.put(id, task);
    }
    
    public void coordinateGroupAttack(List<LivingEntity> group, Player target) {
        if (group.isEmpty() || target == null) return;
        
        int attackers = group.size() / 2;
        
        for (int i = 0; i < group.size(); i++) {
            LivingEntity entity = group.get(i);
            
            if (i < attackers) {
                if (entity instanceof org.bukkit.entity.Mob) {
                    ((org.bukkit.entity.Mob) entity).setTarget(target);
                }
            } else {
                Location flankPos = calculateFlankPosition(target.getLocation(), entity.getLocation());
                if (flankPos != null && flankPos.getBlock().getType().isAir()) {
                    entity.teleport(flankPos);
                    if (entity instanceof org.bukkit.entity.Mob) {
                        ((org.bukkit.entity.Mob) entity).setTarget(target);
                    }
                }
            }
        }
    }
    
    private Location calculateFlankPosition(Location target, Location entity) {
        Vector toTarget = target.toVector().subtract(entity.toVector()).normalize();
        Vector perpendicular = new Vector(-toTarget.getZ(), 0, toTarget.getX()).normalize();
        
        Location flank = target.clone().add(perpendicular.multiply(3));
        return flank;
    }
    
    public void triggerChainReaction(LivingEntity trigger, Class<? extends LivingEntity> entityType, 
                                    double radius, int maxChain) {
        List<LivingEntity> chain = new ArrayList<>();
        chain.add(trigger);
        
        new BukkitRunnable() {
            int currentChain = 0;
            @Override
            public void run() {
                if (currentChain >= maxChain || chain.isEmpty()) {
                    cancel();
                    return;
                }
                
                LivingEntity last = chain.get(chain.size() - 1);
                if (!last.isValid()) {
                    cancel();
                    return;
                }
                
                last.getNearbyEntities(radius, radius, radius).forEach(entity -> {
                    if (entityType.isInstance(entity) && !chain.contains(entity) && currentChain < maxChain) {
                        LivingEntity next = (LivingEntity) entity;
                        chain.add(next);
                        currentChain++;
                    }
                });
            }
        }.runTaskTimer(plugin, 0, 10);
    }
    
    public void cleanup() {
        activeAITasks.values().forEach(AITask::cancel);
        activeAITasks.clear();
    }
    
    private abstract class AITask extends BukkitRunnable {}
}

