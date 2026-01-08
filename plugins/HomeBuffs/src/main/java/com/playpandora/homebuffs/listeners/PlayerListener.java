package com.playpandora.homebuffs.listeners;

import com.playpandora.homebuffs.HomeBuffs;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    
    private final HomeBuffs plugin;
    
    public PlayerListener(HomeBuffs plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Enable buffs by default for new players
        plugin.getBuffManager().setBuffsEnabled(event.getPlayer().getUniqueId(), true);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up
        plugin.getBuffManager().setBuffsEnabled(event.getPlayer().getUniqueId(), false);
    }
}


