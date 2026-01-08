package com.playpandora.moreweather.listeners;

import com.playpandora.moreweather.MoreWeather;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {
    
    private final MoreWeather plugin;
    
    public PlayerListener(MoreWeather plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Player effects are applied automatically by scheduled tasks
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up player data
        plugin.getTemperatureManager().removePlayer(event.getPlayer());
        plugin.getPlayerEffectManager().removePlayer(event.getPlayer());
    }
}








