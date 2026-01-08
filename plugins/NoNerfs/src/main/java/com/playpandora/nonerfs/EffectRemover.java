package com.playpandora.nonerfs;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class EffectRemover {
    
    private final NoNerfs plugin;
    private BukkitTask task;
    
    public EffectRemover(NoNerfs plugin) {
        this.plugin = plugin;
    }
    
    public void start() {
        // Run every tick (20 times per second) to immediately remove effects
        task = new BukkitRunnable() {
            @Override
            public void run() {
                // Check all online players
                plugin.getServer().getOnlinePlayers().forEach(player -> {
                    removeBadEffects(player);
                });
                
                // Also check all entities in loaded chunks (for non-player entities)
                plugin.getServer().getWorlds().forEach(world -> {
                    world.getEntitiesByClass(LivingEntity.class).forEach(entity -> {
                        if (entity instanceof org.bukkit.entity.Player) {
                            return; // Already handled above
                        }
                        removeBadEffects(entity);
                    });
                });
            }
        }.runTaskTimer(plugin, 0L, 1L); // Run every tick
    }
    
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
    
    private void removeBadEffects(LivingEntity entity) {
        // Remove weakness
        if (entity.hasPotionEffect(PotionEffectType.WEAKNESS)) {
            entity.removePotionEffect(PotionEffectType.WEAKNESS);
        }
        
        // Remove slowness
        if (entity.hasPotionEffect(PotionEffectType.SLOWNESS)) {
            entity.removePotionEffect(PotionEffectType.SLOWNESS);
        }
        
        // Remove blindness
        if (entity.hasPotionEffect(PotionEffectType.BLINDNESS)) {
            entity.removePotionEffect(PotionEffectType.BLINDNESS);
        }
    }
}

