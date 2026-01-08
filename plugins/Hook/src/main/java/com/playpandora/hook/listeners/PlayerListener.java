package com.playpandora.hook.listeners;

import com.playpandora.hook.Hook;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerListener implements Listener {
    
    private final Hook plugin;
    
    public PlayerListener(Hook plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("notifications.welcome-on-join", true)) {
            return;
        }
        
        // Check if first join
        boolean isFirstJoin = !event.getPlayer().hasPlayedBefore();
        
        // Send welcome notification
        plugin.getNotificationManager().sendWelcome(event.getPlayer(), isFirstJoin);
    }
    
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!plugin.getConfig().getBoolean("notifications.respawn-notification", true)) {
            return;
        }
        
        // Send respawn notification
        plugin.getNotificationManager().sendRespawn(event.getPlayer());
    }
}








