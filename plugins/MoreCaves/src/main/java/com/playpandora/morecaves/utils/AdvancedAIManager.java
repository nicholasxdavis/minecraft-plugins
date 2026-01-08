package com.playpandora.morecaves.utils;

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
    
    public void cleanup() {
        activeAITasks.values().forEach(AITask::cancel);
        activeAITasks.clear();
    }
    
    private abstract class AITask extends BukkitRunnable {}
}

