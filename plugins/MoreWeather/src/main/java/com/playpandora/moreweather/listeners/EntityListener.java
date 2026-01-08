package com.playpandora.moreweather.listeners;

import com.playpandora.moreweather.MoreWeather;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

public class EntityListener implements Listener {
    
    private final MoreWeather plugin;
    
    public EntityListener(MoreWeather plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Skeleton) {
            plugin.getMobBehaviorManager().onSkeletonShoot(event, (Skeleton) event.getEntity());
        }
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Modify damage based on weather
        plugin.getMobBehaviorManager().modifyMobBehavior(event.getDamager());
    }
}








