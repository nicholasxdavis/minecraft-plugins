package com.playpandora.moreend.handlers;

import com.playpandora.moreend.MoreEnd;
import com.playpandora.moreend.utils.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

public class AdvancedAIHandler implements Listener {
    private final MoreEnd plugin;
    private final ConfigManager config;
    private final AdvancedAIManager aiManager;
    
    public AdvancedAIHandler(MoreEnd plugin) {
        this.plugin = plugin;
        this.config = new ConfigManager(plugin);
        this.aiManager = new AdvancedAIManager(plugin);
    }
    
    @EventHandler
    public void onEndermanTeleport(EntityTeleportEvent event) {
        if (!(event.getEntity() instanceof Enderman)) return;
        if (!DimensionChecker.isEnd(event.getEntity().getLocation())) return;
        
        if (!config.isFeatureEnabled("advanced-ai.enderman")) return;
        
        Enderman enderman = (Enderman) event.getEntity();
        
        if (config.getConfig().getBoolean("features.advanced-ai.enderman.teleport-chain-reactions", true)) {
            aiManager.triggerChainReaction(enderman, Enderman.class, 10.0, 5);
        }
    }
    
    @EventHandler
    public void onShulkerBulletLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.ShulkerBullet)) return;
        if (!DimensionChecker.isEnd(event.getEntity().getLocation())) return;
        
        if (!config.isFeatureEnabled("advanced-ai.shulker")) return;
        if (!config.getConfig().getBoolean("features.advanced-ai.shulker.homing-bullets", true)) {
            return;
        }
        
        org.bukkit.entity.ShulkerBullet bullet = (org.bukkit.entity.ShulkerBullet) event.getEntity();
        ProjectileSource source = bullet.getShooter();
        
        if (source instanceof Shulker) {
            double homingStrength = config.getConfig().getDouble(
                "features.advanced-ai.shulker.homing-strength", 0.3);
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!bullet.isValid()) {
                        cancel();
                        return;
                    }
                    
                    if (bullet.getTarget() != null) {
                        org.bukkit.util.Vector toTarget = bullet.getTarget().getLocation().toVector()
                            .subtract(bullet.getLocation().toVector()).normalize();
                        
                        org.bukkit.util.Vector current = bullet.getVelocity().normalize();
                        org.bukkit.util.Vector adjusted = current.multiply(1 - homingStrength)
                            .add(toTarget.multiply(homingStrength)).normalize();
                        
                        bullet.setVelocity(bullet.getVelocity().normalize().multiply(adjusted));
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
        }
    }
}

