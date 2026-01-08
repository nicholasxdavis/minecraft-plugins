package com.playpandora.moreend.utils;

import org.bukkit.Location;
import org.bukkit.entity.Enderman;
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
                        if (next instanceof Enderman && last instanceof Enderman) {
                            // Teleport chain
                            Location teleportLoc = last.getLocation().clone().add(
                                (Math.random() - 0.5) * 5,
                                0,
                                (Math.random() - 0.5) * 5
                            );
                            if (teleportLoc.getBlock().getType().isAir()) {
                                next.teleport(teleportLoc);
                            }
                        }
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

