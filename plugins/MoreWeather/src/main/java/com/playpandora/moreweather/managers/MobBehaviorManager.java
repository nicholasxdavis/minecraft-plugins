package com.playpandora.moreweather.managers;

import com.playpandora.moreweather.MoreWeather;
import com.playpandora.moreweather.models.WeatherType;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;

import java.util.HashMap;
import java.util.Map;

public class MobBehaviorManager {
    
    private final MoreWeather plugin;
    private boolean fogActive = false;
    private final Map<Skeleton, Integer> skeletonBowModifiers = new HashMap<>();
    
    public MobBehaviorManager(MoreWeather plugin) {
        this.plugin = plugin;
    }
    
    public void applyRainSkeletonModifier(Skeleton skeleton) {
        // Store modifier for when skeleton shoots
        skeletonBowModifiers.put(skeleton, 3); // Reduced arrow velocity
    }
    
    public void onSkeletonShoot(EntityShootBowEvent event, Skeleton skeleton) {
        if (skeletonBowModifiers.containsKey(skeleton)) {
            // Reduce arrow velocity by 30%
            if (event.getProjectile() instanceof Arrow) {
                Arrow arrow = (Arrow) event.getProjectile();
                arrow.setVelocity(arrow.getVelocity().multiply(0.7));
                skeletonBowModifiers.remove(skeleton);
            }
        }
    }
    
    public void modifyMobBehavior(Entity entity) {
        if (!(entity instanceof LivingEntity)) return;
        
        LivingEntity living = (LivingEntity) entity;
        WeatherType weather = plugin.getWeatherManager().getCurrentWeather(living.getWorld());
        
        switch (weather) {
            case HEAVY_RAIN:
            case STORM:
            case THUNDER:
                if (entity instanceof Skeleton) {
                    // Skeletons perform worse in rain
                    applySkeletonRainEffect((Skeleton) entity);
                }
                break;
                
            case HEATWAVE:
                if (entity instanceof Creeper) {
                    // Creepers ignite faster
                    ((Creeper) entity).setMaxFuseTicks(
                        (int)(((Creeper) entity).getMaxFuseTicks() * 0.8)
                    );
                }
                break;
                
            case FOG:
                if (fogActive) {
                    // Reduce detection range
                    if (living instanceof Mob) {
                        Mob mob = (Mob) living;
                        // This would require NMS or special handling
                        // Simplified for now
                    }
                }
                break;
        }
    }
    
    private void applySkeletonRainEffect(Skeleton skeleton) {
        // Effect is applied when shooting
    }
    
    public void handleVillagerStormBehavior(Villager villager) {
        WeatherType weather = plugin.getWeatherManager().getCurrentWeather(villager.getWorld());
        
        if (weather == WeatherType.STORM || weather == WeatherType.THUNDER) {
            // Villagers try to hide indoors during storms
            // Simplified - would need pathfinding
        }
    }
    
    public void setFogActive(boolean active) {
        this.fogActive = active;
    }
    
    public boolean isFogActive() {
        return fogActive;
    }
}








