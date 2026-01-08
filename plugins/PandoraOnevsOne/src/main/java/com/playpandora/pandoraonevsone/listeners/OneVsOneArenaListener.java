package com.playpandora.pandoraonevsone.listeners;

import com.playpandora.pandoraonevsone.PandoraOnevsOne;
import com.playpandora.pandoraonevsone.util.ColorUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Listener for arena protection - teleport out players not in active duel
 */
public class OneVsOneArenaListener implements Listener {
    
    private final PandoraOnevsOne plugin;
    private static final double ARENA_CHECK_DISTANCE = 50.0; // Check within 50 blocks of arena
    
    public OneVsOneArenaListener(PandoraOnevsOne plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Check if player is near arena and teleport out if not in active duel
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Allow ops with bypass permission
        if (player.hasPermission("pandoraonevsone.bypass")) {
            return;
        }
        
        Location arenaLocation = plugin.getOneVsOneManager().getArenaLocation();
        if (arenaLocation == null) {
            return; // No arena set
        }
        
        // Check if player is in duel - if so, allow them in arena
        if (plugin.getOneVsOneManager().isInDuel(player)) {
            return; // Player is in active duel - allow
        }
        
        // Check if player is near arena
        if (player.getWorld().equals(arenaLocation.getWorld())) {
            double distance = player.getLocation().distance(arenaLocation);
            
            if (distance <= ARENA_CHECK_DISTANCE) {
                // Player is near arena but not in duel - teleport to spawn
                Location spawn = player.getWorld().getSpawnLocation();
                if (spawn != null) {
                    player.teleport(spawn);
                    player.sendMessage(ColorUtil.error("You cannot be in the 1v1 arena unless you're in an active duel!"));
                }
            }
        }
    }
}

