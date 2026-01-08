package com.playpandora.morecaves.handlers;

import com.playpandora.morecaves.MoreCaves;
import com.playpandora.morecaves.utils.*;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class BehaviorHandler implements Listener {
    private final MoreCaves plugin;
    private final ConfigManager config;
    
    public BehaviorHandler(MoreCaves plugin) {
        this.plugin = plugin;
        this.config = new ConfigManager(plugin);
    }
    
    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!CaveChecker.isInCave(event.getEntity().getLocation())) {
            return;
        }
        
        // Creeper behavior - silent until close
        if (event.getEntity() instanceof Creeper && 
            config.isFeatureEnabled("creeper-behavior")) {
            Creeper creeper = (Creeper) event.getEntity();
            if (event.getTarget() instanceof Player) {
                Player player = (Player) event.getTarget();
                double distance = creeper.getLocation().distance(player.getLocation());
                int silentBlocks = config.getConfig().getInt("features.creeper-behavior.silent-until-blocks", 4);
                
                if (distance > silentBlocks) {
                    // Make silent (no hiss)
                    // Note: May require NMS or Paper API
                } else if (distance <= config.getConfig().getInt("features.creeper-behavior.sprint-distance", 4)) {
                    // Sprint toward player
                    org.bukkit.util.Vector direction = player.getLocation().toVector()
                        .subtract(creeper.getLocation().toVector()).normalize();
                    creeper.setVelocity(creeper.getVelocity().add(direction.multiply(0.2)));
                }
            }
        }
        
        // Zombie behavior - faster in tunnels
        if (event.getEntity() instanceof Zombie &&
            config.isFeatureEnabled("zombie-behavior")) {
            Zombie zombie = (Zombie) event.getEntity();
            if (event.getTarget() instanceof Player) {
                // Check if in small tunnel (surrounded by blocks)
                Location loc = zombie.getLocation();
                int airBlocks = 0;
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (loc.clone().add(x, 0, z).getBlock().getType().isAir()) {
                            airBlocks++;
                        }
                    }
                }
                
                // If in small tunnel (few air blocks), increase speed
                if (airBlocks <= 3) {
                    double speedMultiplier = config.getMultiplier("zombie-behavior", "tunnel-speed-multiplier");
                    // Note: Speed modification may require NMS or Paper API
                }
                
                // Random sprint bursts
                double sprintChance = config.getConfig().getDouble("features.zombie-behavior.sprint-burst-chance", 0.1);
                if (Math.random() < sprintChance) {
                    org.bukkit.util.Vector direction = ((Player) event.getTarget()).getLocation().toVector()
                        .subtract(zombie.getLocation().toVector()).normalize();
                    zombie.setVelocity(zombie.getVelocity().add(direction.multiply(0.3)));
                }
            }
        }
        
        // Skeleton behavior - hold long range
        if (event.getEntity() instanceof Skeleton &&
            config.isFeatureEnabled("skeleton-behavior")) {
            Skeleton skeleton = (Skeleton) event.getEntity();
            if (event.getTarget() instanceof Player) {
                Player player = (Player) event.getTarget();
                double distance = skeleton.getLocation().distance(player.getLocation());
                
                // Don't approach, hold position
                if (config.getConfig().getBoolean("features.skeleton-behavior.hold-long-range", true)) {
                    if (distance < 8) {
                        // Move away slightly
                        org.bukkit.util.Vector away = skeleton.getLocation().toVector()
                            .subtract(player.getLocation().toVector()).normalize();
                        skeleton.setVelocity(skeleton.getVelocity().add(away.multiply(0.1)));
                    }
                }
            }
        }
        
        // Spider behavior - ceiling drops
        if (event.getEntity() instanceof Spider &&
            config.isFeatureEnabled("spider-behavior")) {
            Spider spider = (Spider) event.getEntity();
            if (event.getTarget() instanceof Player) {
                Player player = (Player) event.getTarget();
                Location spiderLoc = spider.getLocation();
                Location playerLoc = player.getLocation();
                
                // Check if spider is above player
                if (spiderLoc.getY() > playerLoc.getY() + 2 &&
                    config.getConfig().getBoolean("features.spider-behavior.ceiling-drop", true)) {
                    // Drop down on player
                    Location dropLoc = playerLoc.clone();
                    dropLoc.setY(spiderLoc.getY());
                    
                    if (dropLoc.getBlock().getType().isAir()) {
                        spider.teleport(dropLoc);
                    }
                }
            }
        }
    }
}

