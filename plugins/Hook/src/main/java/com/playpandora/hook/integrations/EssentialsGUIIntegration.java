package com.playpandora.hook.integrations;

import com.playpandora.hook.Hook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class EssentialsGUIIntegration implements Listener {
    
    private final Hook plugin;
    
    public EssentialsGUIIntegration(Hook plugin) {
        this.plugin = plugin;
    }
    
    public void hook() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!plugin.getConfig().getBoolean("integrations.essentialsgui.teleport-notification", true)) {
            return;
        }
        
        Player player = event.getPlayer();
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        
        // Only notify for command-based teleports (warp/home)
        if (cause == PlayerTeleportEvent.TeleportCause.COMMAND || 
            cause == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            
            // Check if it's a warp or home teleport by checking recent command
            // This is a simple check - could be enhanced
            String destination = getTeleportDestination(event.getTo());
            if (destination != null) {
                plugin.getAPI().sendCustom(player, "Teleporting...", 
                    "Arrived at " + destination, 500, 2500, 1000);
            }
        }
    }
    
    private String getTeleportDestination(org.bukkit.Location location) {
        // Try to determine if this is a warp or home location
        // This is a simplified check - could be enhanced with actual warp/home tracking
        return "destination";
    }
}




