package com.playpandora.pandoracavern.managers;

import com.playpandora.pandoracavern.PandoraCavern;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RespawnManager {
    
    private final PandoraCavern plugin;
    private BukkitTask respawnTask;
    private Map<Location, Long> minedBlocks; // Location -> time when mined
    
    public RespawnManager(PandoraCavern plugin) {
        this.plugin = plugin;
        this.minedBlocks = new HashMap<>();
    }
    
    public void startRespawnTask() {
        respawnTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            long respawnDelayMs = plugin.getConfig().getInt("respawn-delay", 7) * 1000L;
            
            Set<Location> blocksToRespawn = new java.util.HashSet<>();
            
            for (Map.Entry<Location, Long> entry : minedBlocks.entrySet()) {
                Location location = entry.getKey();
                long minedTime = entry.getValue();
                
                if (currentTime - minedTime >= respawnDelayMs) {
                    blocksToRespawn.add(location);
                }
            }
            
            for (Location location : blocksToRespawn) {
                respawnBlock(location);
                minedBlocks.remove(location);
            }
        }, 20L, 20L); // Start after 1 second, check every second
    }
    
    public void stopRespawnTask() {
        if (respawnTask != null) {
            respawnTask.cancel();
            respawnTask = null;
        }
    }
    
    public void markBlockAsMined(Location location) {
        minedBlocks.put(location, System.currentTimeMillis());
    }
    
    private void respawnBlock(Location location) {
        // Check if this location is still a valid cavern block
        if (!plugin.getDataManager().isCavernBlock(location)) {
            minedBlocks.remove(location);
            return;
        }
        
        Block block = location.getBlock();
        if (block.getType() != Material.ANCIENT_DEBRIS) {
            plugin.getBlockManager().respawnCavernBlock(location);
        }
    }
    
    public boolean isBlockMined(Location location) {
        return minedBlocks.containsKey(location);
    }
}

