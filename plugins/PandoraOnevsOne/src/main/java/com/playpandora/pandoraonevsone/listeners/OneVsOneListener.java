package com.playpandora.pandoraonevsone.listeners;

import com.playpandora.pandoraonevsone.PandoraOnevsOne;
import com.playpandora.pandoraonevsone.models.Duel;
import com.playpandora.pandoraonevsone.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

/**
 * Listener for 1v1 duel events (death, quit, respawn)
 */
public class OneVsOneListener implements Listener {
    
    private final PandoraOnevsOne plugin;
    
    public OneVsOneListener(PandoraOnevsOne plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle player death in duel
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        if (!plugin.getOneVsOneManager().isInDuel(player)) {
            return;
        }
        
        Duel duel = plugin.getOneVsOneManager().getActiveDuel();
        if (duel == null || !duel.containsPlayer(player.getUniqueId())) {
            return;
        }
        
        // Get opponent (winner)
        UUID opponentUUID = duel.getOpponent(player.getUniqueId());
        
        // Prevent drops
        event.getDrops().clear();
        event.setKeepInventory(true);
        
        // End duel (opponent wins)
        plugin.getOneVsOneManager().endDuel(duel, opponentUUID);
    }
    
    /**
     * Handle player quit during duel - CLEAR STATE to prevent persistence
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getOneVsOneManager().isInDuel(player)) {
            Duel duel = plugin.getOneVsOneManager().getActiveDuel();
            if (duel != null && duel.containsPlayer(player.getUniqueId())) {
                // Get opponent (winner)
                UUID opponentUUID = duel.getOpponent(player.getUniqueId());
                
                // End duel (opponent wins)
                plugin.getOneVsOneManager().endDuel(duel, opponentUUID);
            } else {
                // Player in duel but no active duel - clean up state
                plugin.getOneVsOneManager().clearPlayerState(player);
            }
        }
        
        // ALWAYS clear saved data on quit to prevent persistence on rejoin
        plugin.getOneVsOneManager().clearSavedData(player.getUniqueId());
    }
    
    /**
     * Handle player join - CLEAR STATE to prevent persistence
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Clear any lingering state from previous session
        plugin.getOneVsOneManager().clearPlayerState(player);
        plugin.getOneVsOneManager().clearSavedData(player.getUniqueId());
    }
    
    /**
     * Handle player respawn - restore immediately
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // If player was in a duel, they've already been restored
        // This is just to ensure they don't respawn at arena
        if (plugin.getOneVsOneManager().isInDuel(player)) {
            // Player should already be restored, but ensure they're removed
            plugin.getOneVsOneManager().leaveDuel(player);
        }
    }
}

