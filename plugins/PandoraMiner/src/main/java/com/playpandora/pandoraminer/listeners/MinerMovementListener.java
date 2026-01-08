package com.playpandora.pandoraminer.listeners;

import com.playpandora.pandoraminer.PandoraMiner;
import com.playpandora.pandoraminer.integration.PandoraBasesIntegration;
import com.playpandora.pandoraminer.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener to check if miner enters claimed land
 */
public class MinerMovementListener implements Listener {
    
    private final PandoraMiner plugin;
    private final Map<UUID, org.bukkit.Location> lastCheckedLocation;
    
    public MinerMovementListener(PandoraMiner plugin) {
        this.plugin = plugin;
        this.lastCheckedLocation = new HashMap<>();
    }
    
    /**
     * Check if miner enters claimed land
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getMinerManager().isMiner(player)) {
            return;
        }
        
        // Only check when moving to a different block
        org.bukkit.Location from = event.getFrom();
        org.bukkit.Location to = event.getTo();
        
        if (to == null) {
            return;
        }
        
        // Check if moved to a different chunk
        if (from.getChunk().getX() != to.getChunk().getX() || 
            from.getChunk().getZ() != to.getChunk().getZ()) {
            
            // Check location to prevent spam
            UUID uuid = player.getUniqueId();
            org.bukkit.Location lastLocation = lastCheckedLocation.get(uuid);
            
            if (lastLocation != null && 
                lastLocation.getBlockX() == to.getBlockX() &&
                lastLocation.getBlockY() == to.getBlockY() &&
                lastLocation.getBlockZ() == to.getBlockZ()) {
                return; // Already checked this location
            }
            
            lastCheckedLocation.put(uuid, to.clone());
            
            // Check if player entered claimed land (and doesn't own it)
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        return;
                    }
                    
                    if (!plugin.getMinerManager().isMiner(player)) {
                        return;
                    }
                    
                    if (!PandoraBasesIntegration.canUseMiner(player, player.getLocation())) {
                        // Disable miner mode
                        plugin.getMinerManager().disableMiner(player);
                        player.sendMessage(ColorUtil.error("Miner mode disabled! You entered claimed land."));
                    }
                }
            }.runTask(plugin);
        }
    }
}


