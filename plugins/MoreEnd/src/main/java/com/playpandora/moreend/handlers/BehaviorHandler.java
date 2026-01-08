package com.playpandora.moreend.handlers;

import com.playpandora.moreend.MoreEnd;
import com.playpandora.moreend.utils.ConfigManager;
import com.playpandora.moreend.utils.DimensionChecker;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class BehaviorHandler implements Listener {
    private final MoreEnd plugin;
    private final ConfigManager config;
    
    public BehaviorHandler(MoreEnd plugin) {
        this.plugin = plugin;
        this.config = new ConfigManager(plugin);
    }
    
    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!DimensionChecker.isEnd(event.getEntity().getLocation())) {
            return;
        }
        
        // Enderman behavior
        if (event.getEntity() instanceof Enderman && 
            config.isFeatureEnabled("enderman-behavior")) {
            Enderman enderman = (Enderman) event.getEntity();
            if (event.getTarget() instanceof Player) {
                Player player = (Player) event.getTarget();
                
                // Faster follow speed
                double speedMultiplier = config.getMultiplier("enderman-behavior", "faster-follow-multiplier");
                if (speedMultiplier > 1.0) {
                    // Note: Speed modification may require NMS or Paper API
                }
                
                // Teleport behind player
                double teleportChance = config.getConfig().getDouble("features.enderman-behavior.teleport-behind-chance", 0.3);
                if (Math.random() < teleportChance) {
                    Location behind = player.getLocation().clone();
                    behind.setDirection(player.getLocation().getDirection().multiply(-1));
                    behind.add(behind.getDirection().multiply(2));
                    behind.setY(player.getLocation().getY());
                    
                    if (behind.getBlock().getType().isAir() && behind.getBlock().getRelative(0, 1, 0).getType().isAir()) {
                        enderman.teleport(behind);
                    }
                }
                
                // Freeze and stare
                int stareTicks = config.getConfig().getInt("features.enderman-behavior.hover-stare-ticks", 60);
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (!enderman.isValid() || !player.isOnline() || ticks >= stareTicks) {
                            cancel();
                            return;
                        }
                        Location enderLoc = enderman.getLocation();
                        enderLoc.setDirection(player.getLocation().subtract(enderLoc).toVector().normalize());
                        enderman.teleport(enderLoc);
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0, 1);
            }
        }
    }
    
    @EventHandler
    public void onEntityTeleport(EntityTeleportEvent event) {
        if (!(event.getEntity() instanceof Endermite)) return;
        if (!DimensionChecker.isEnd(event.getEntity().getLocation())) return;
        
        // Endermite spawns from pearl teleport
        if (config.isFeatureEnabled("endermite-behavior")) {
            int minSpawn = config.getConfig().getInt("features.endermite-behavior.pearl-teleport-spawn-min", 3);
            int maxSpawn = config.getConfig().getInt("features.endermite-behavior.pearl-teleport-spawn-max", 6);
            int spawnCount = minSpawn + (int)(Math.random() * (maxSpawn - minSpawn + 1));
            
            // Note: This would need to detect player pearl teleport, which is complex
            // For now, this is a placeholder
        }
    }
}

