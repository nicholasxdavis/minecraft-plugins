package com.playpandora.morecaves.utils;

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
            case CAVE_HORROR:
                tasks.add(createCaveHorrorAmbience(player));
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
    
    private BukkitRunnable createCaveHorrorAmbience(Player player) {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                
                Location loc = player.getLocation();
                
                // Warden heartbeat in Deep Dark
                if (loc.getBlock().getBiome().toString().contains("DEEP_DARK")) {
                    if (random.nextDouble() < 0.1) { // 10% chance
                        try {
                            SoundManager.playSoundAtLocation(loc, Sound.ENTITY_WARDEN_HEARTBEAT,
                                SoundCategory.HOSTILE, 0.5f, 1.0f);
                        } catch (Exception e) {
                            // Sound may not exist in this version
                        }
                    }
                }
                
                // Distant zombie echoes
                if (random.nextDouble() < 0.03) { // 3% chance
                    SoundManager.playDistantSound(loc, Sound.ENTITY_ZOMBIE_AMBIENT,
                        SoundCategory.HOSTILE, 0.2f, 0.7f + random.nextFloat() * 0.6f, 30);
                }
                
                // Amplified bat squeaks
                if (random.nextDouble() < 0.05) { // 5% chance
                    SoundManager.playDistantSound(loc, Sound.ENTITY_BAT_AMBIENT,
                        SoundCategory.NEUTRAL, 0.4f, 1.5f + random.nextFloat() * 0.5f, 15);
                }
                
                // Dripstone impacts
                if (random.nextDouble() < 0.02) { // 2% chance
                    // Use stone break sound as fallback
                    SoundManager.playDistantSound(loc, Sound.BLOCK_STONE_BREAK,
                        SoundCategory.BLOCKS, 0.3f, 0.8f + random.nextFloat() * 0.4f, 20);
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
        CAVE_HORROR
    }
}

