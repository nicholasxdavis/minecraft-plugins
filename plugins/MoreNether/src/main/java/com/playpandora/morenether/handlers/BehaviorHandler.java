package com.playpandora.morenether.handlers;

import com.playpandora.morenether.MoreNether;
import com.playpandora.morenether.utils.ConfigManager;
import com.playpandora.morenether.utils.DimensionChecker;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class BehaviorHandler implements Listener {
    private final MoreNether plugin;
    private final ConfigManager config;
    
    public BehaviorHandler(MoreNether plugin) {
        this.plugin = plugin;
        this.config = new ConfigManager(plugin);
    }
    
    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!DimensionChecker.isNether(event.getEntity().getLocation())) {
            return;
        }
        
        // Wither Skeleton silent until close and stare behavior
        if (event.getEntity() instanceof WitherSkeleton && 
            config.isFeatureEnabled("wither-skeleton-behavior")) {
            WitherSkeleton skeleton = (WitherSkeleton) event.getEntity();
            if (event.getTarget() instanceof Player) {
                Player player = (Player) event.getTarget();
                double distance = skeleton.getLocation().distance(player.getLocation());
                int silentBlocks = config.getConfig().getInt("features.wither-skeleton-behavior.silent-until-blocks", 5);
                
                if (distance <= silentBlocks) {
                    // Stare behavior
                    int stareTicks = config.getConfig().getInt("features.wither-skeleton-behavior.stare-duration-ticks", 40);
                    new BukkitRunnable() {
                        int ticks = 0;
                        @Override
                        public void run() {
                            if (!skeleton.isValid() || !player.isOnline() || ticks >= stareTicks) {
                                cancel();
                                return;
                            }
                            // Make skeleton look at player
                            Location skeletonLoc = skeleton.getLocation();
                            skeletonLoc.setDirection(player.getLocation().subtract(skeletonLoc).toVector().normalize());
                            skeleton.teleport(skeletonLoc);
                            ticks++;
                        }
                    }.runTaskTimer(plugin, 0, 1);
                }
            }
        }
        
        // Enderman aggressive teleport
        if (event.getEntity() instanceof Enderman && 
            config.isFeatureEnabled("enderman-behavior")) {
            Enderman enderman = (Enderman) event.getEntity();
            if (event.getTarget() instanceof Player) {
                Player player = (Player) event.getTarget();
                double teleportChance = config.getConfig().getDouble("features.enderman-behavior.teleport-behind-chance", 0.3);
                
                if (Math.random() < teleportChance) {
                    // Teleport behind player
                    Location behind = player.getLocation().clone();
                    behind.setDirection(player.getLocation().getDirection().multiply(-1));
                    behind.add(behind.getDirection().multiply(2));
                    behind.setY(player.getLocation().getY());
                    
                    if (behind.getBlock().getType().isAir() && behind.getBlock().getRelative(0, 1, 0).getType().isAir()) {
                        enderman.teleport(behind);
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!DimensionChecker.isNether(event.getPlayer().getLocation())) {
            return;
        }
        
        // Piglin gold detection
        if (config.isFeatureEnabled("piglin-behavior")) {
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItemInMainHand();
            
            if (item.getType() == Material.GOLD_INGOT || 
                item.getType() == Material.GOLD_NUGGET ||
                item.getType() == Material.GOLDEN_APPLE ||
                item.getType() == Material.GOLDEN_CARROT) {
                
                // Check if in bastion (rough check - within certain Y levels and structure)
                Location loc = player.getLocation();
                if (loc.getY() > 30 && loc.getY() < 120) { // Bastions typically in this range
                    // Make nearby Piglins hostile
                    player.getNearbyEntities(16, 16, 16).forEach(entity -> {
                        if (entity instanceof Piglin) {
                            Piglin piglin = (Piglin) entity;
                            if (piglin instanceof org.bukkit.entity.Mob) {
                                ((org.bukkit.entity.Mob) piglin).setTarget(player);
                            }
                        } else if (entity instanceof PiglinBrute) {
                            PiglinBrute piglin = (PiglinBrute) entity;
                            if (piglin instanceof org.bukkit.entity.Mob) {
                                ((org.bukkit.entity.Mob) piglin).setTarget(player);
                            }
                        }
                    });
                }
            }
        }
    }
}

