package com.playpandora.pandoraminer.listeners;

import com.playpandora.pandoraminer.PandoraMiner;
import com.playpandora.pandoraminer.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener to clear miner state on quit/join to prevent persistence
 */
public class MinerQuitListener implements Listener {
    
    private final PandoraMiner plugin;
    
    public MinerQuitListener(PandoraMiner plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Clear miner state on quit to prevent persistence
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getMinerManager().isMiner(player)) {
            // Disable miner and clear state
            plugin.getMinerManager().disableMiner(player);
            plugin.getMinerManager().clearSavedInventory(player.getUniqueId());
        }
    }
    
    /**
     * Clear miner state on join to prevent persistence
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Clear any lingering state
        plugin.getMinerManager().clearSavedInventory(player.getUniqueId());
        
        // Clear miner state if somehow still active
        if (plugin.getMinerManager().isMiner(player)) {
            plugin.getMinerManager().disableMiner(player);
        }
    }
}

