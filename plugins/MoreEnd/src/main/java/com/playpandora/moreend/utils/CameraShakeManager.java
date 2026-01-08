package com.playpandora.moreend.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class CameraShakeManager {
    private final JavaPlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, BukkitRunnable> activeShakes = new HashMap<>();
    
    public CameraShakeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Apply camera shake to player (simulated via small teleports)
     * Note: This is a workaround - Paper API may have better methods
     */
    public void shakeCamera(Player player, float intensity, int durationTicks) {
        if (intensity <= 0 || durationTicks <= 0) return;
        
        Location originalLoc = player.getLocation().clone();
        final int finalDurationTicks = durationTicks;
        final int[] ticks = {0};
        
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || ticks[0] >= finalDurationTicks) {
                    cancel();
                    return;
                }
                
                // Small random offset
                double offsetX = (random.nextDouble() - 0.5) * intensity * 0.1;
                double offsetY = (random.nextDouble() - 0.5) * intensity * 0.1;
                double offsetZ = (random.nextDouble() - 0.5) * intensity * 0.1;
                
                Location shakeLoc = originalLoc.clone().add(offsetX, offsetY, offsetZ);
                
                // Use Paper API teleport with relative flag if available
                try {
                    // Try to use Paper's relative teleport
                    player.teleport(shakeLoc);
                } catch (Exception e) {
                    // Fallback to regular teleport
                    player.teleport(shakeLoc);
                }
                
                ticks[0]++;
            }
        };
        
        task.runTaskTimer(plugin, 0, 1);
    }
    
    /**
     * Continuous shake for void pressure
     */
    public void startVoidShake(Player player, float intensity) {
        if (activeShakes.containsKey(player.getUniqueId())) {
            return; // Already shaking
        }
        
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    activeShakes.remove(player.getUniqueId());
                    return;
                }
                
                // Apply small shake every few ticks
                if (random.nextDouble() < 0.3) { // 30% chance per tick
                    double offsetX = (random.nextDouble() - 0.5) * intensity * 0.05;
                    double offsetY = (random.nextDouble() - 0.5) * intensity * 0.05;
                    double offsetZ = (random.nextDouble() - 0.5) * intensity * 0.05;
                    
                    Location current = player.getLocation();
                    Location shakeLoc = current.clone().add(offsetX, offsetY, offsetZ);
                    
                    try {
                        player.teleport(shakeLoc);
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        };
        
        task.runTaskTimer(plugin, 0, 1);
        activeShakes.put(player.getUniqueId(), task);
    }
    
    public void stopVoidShake(Player player) {
        BukkitRunnable task = activeShakes.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
    
    public void cleanup() {
        activeShakes.values().forEach(BukkitRunnable::cancel);
        activeShakes.clear();
    }
}

