package com.playpandora.morenether.utils;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class AmbienceController {
    private final JavaPlugin plugin;
    private final Map<UUID, List<BukkitRunnable>> activeAmbience = new HashMap<>();
    private final Random random = new Random();
    
    public AmbienceController(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Start ambient sound loop for a player
     */
    public void startAmbience(Player player, AmbienceType type) {
        stopAmbience(player);
        
        List<BukkitRunnable> tasks = new ArrayList<>();
        
        switch (type) {
            case NETHER_HORROR:
                tasks.add(createNetherHorrorAmbience(player));
                break;
        }
        
        activeAmbience.put(player.getUniqueId(), tasks);
    }
    
    public void stopAmbience(Player player) {
        List<BukkitRunnable> tasks = activeAmbience.remove(player.getUniqueId());
        if (tasks != null) {
            tasks.forEach(BukkitRunnable::cancel);
        }
    }
    
    private BukkitRunnable createNetherHorrorAmbience(Player player) {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                
                Location loc = player.getLocation();
                
                // Random ghast sounds (no ghast nearby)
                if (random.nextDouble() < 0.05) { // 5% chance per check
                    SoundManager.playDistantSound(loc, Sound.ENTITY_GHAST_SCREAM, 
                        SoundCategory.HOSTILE, 0.4f, 0.8f + random.nextFloat() * 0.4f, 20);
                }
                
                // Random footsteps
                if (random.nextDouble() < 0.03) { // 3% chance
                    SoundManager.playRandomFootsteps(loc);
                }
            }
        };
        task.runTaskTimer(plugin, 0, 40); // Run every 2 seconds
        return task;
    }
    
    public void cleanup() {
        activeAmbience.values().forEach(tasks -> tasks.forEach(BukkitRunnable::cancel));
        activeAmbience.clear();
    }
    
    public enum AmbienceType {
        NETHER_HORROR
    }
}

