package com.playpandora.craftaccess.listeners;

import com.playpandora.craftaccess.CraftAccess;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    
    private final CraftAccess plugin;
    
    public PlayerListener(CraftAccess plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save player data when they leave
        if (plugin.getDataManager() != null) {
            plugin.getDataManager().savePlayerData(event.getPlayer().getUniqueId());
        }
    }
}

