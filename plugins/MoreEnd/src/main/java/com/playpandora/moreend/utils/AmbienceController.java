package com.playpandora.moreend.utils;

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
            case END_DREAD:
                tasks.add(createEndDreadAmbience(player));
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
    
    private BukkitRunnable createEndDreadAmbience(Player player) {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                
                Location loc = player.getLocation();
                
                // Random Enderman screams (no entity)
                if (random.nextDouble() < 0.04) { // 4% chance
                    SoundManager.playDistantSound(loc, Sound.ENTITY_ENDERMAN_SCREAM,
                        SoundCategory.HOSTILE, 0.3f, 0.9f + random.nextFloat() * 0.2f, 25);
                }
                
                // Chorus flower breaking at distance
                if (random.nextDouble() < 0.02) { // 2% chance
                    SoundManager.playDistantSound(loc, Sound.BLOCK_CHORUS_FLOWER_DEATH,
                        SoundCategory.BLOCKS, 0.2f, 1.0f, 30);
                }
                
                // Portal hum (layered)
                if (random.nextDouble() < 0.01) { // 1% chance
                    SoundManager.playDistantSound(loc, Sound.BLOCK_PORTAL_AMBIENT,
                        SoundCategory.AMBIENT, 0.1f, 0.5f + random.nextFloat() * 0.5f, 40);
                }
            }
        };
        task.runTaskTimer(plugin, 0, 50); // Run every 2.5 seconds
        return task;
    }
    
    public void cleanup() {
        activeAmbience.values().forEach(tasks -> tasks.forEach(BukkitRunnable::cancel));
        activeAmbience.clear();
    }
    
    public enum AmbienceType {
        END_DREAD
    }
}

