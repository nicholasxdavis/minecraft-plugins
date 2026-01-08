package com.playpandora.welcome.listeners;

import com.playpandora.welcome.Welcome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {
    
    private final Welcome plugin;
    
    public PlayerJoinListener(Welcome plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Update last seen timestamp
        plugin.getPlayerDataManager().updateLastSeen(player);
        
        // Check if player has already received starter items
        boolean hasReceivedStarterItems = plugin.getPlayerDataManager().hasReceivedStarterItems(player);
        boolean hasReceivedBooklet = plugin.getPlayerDataManager().hasReceivedBooklet(player);
        
        // Give monthly crate key ONLY if they haven't received starter items yet
        if (!hasReceivedStarterItems) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    String command = "/crate key " + player.getName() + " monthly";
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
                    // Mark as received
                    plugin.getPlayerDataManager().setReceivedStarterItems(player);
                }
            }, 20L); // 1 second delay to ensure player is fully loaded
        }
        
        // Check if welcome is enabled
        if (!plugin.getConfig().getBoolean("welcome.enabled", true)) {
            return;
        }
        
        // Check if first join
        boolean isFirstJoin = !player.hasPlayedBefore();
        
        // Send welcome messages via Hook if available
        if (plugin.getHookIntegration().isHookAvailable() && 
            plugin.getConfig().getBoolean("hook.use-welcome-title", true)) {
            plugin.getHookIntegration().sendWelcome(player, isFirstJoin);
        }
        
        // Send chat messages after a short delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            
            if (isFirstJoin) {
                // First join messages
                for (String line : plugin.getConfig().getStringList("welcome.first-join-message")) {
                    player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
                }
                
                // Give welcome booklet ONLY if they haven't received it yet
                if (plugin.getConfig().getBoolean("welcome.give-booklet", true) && !hasReceivedBooklet) {
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            plugin.getBookletManager().giveBooklet(player);
                            // Mark as received
                            plugin.getPlayerDataManager().setReceivedBooklet(player);
                        }
                    }, 40L); // 2 seconds delay
                }
            } else {
                // Return join messages
                for (String line : plugin.getConfig().getStringList("welcome.return-join-message")) {
                    player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
                }
            }
        }, 20L); // 1 second delay
        
        // Start tips task if enabled
        if (plugin.getConfig().getBoolean("tips.enabled", true)) {
            plugin.getTipsManager().startTipsTask(player);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Update last seen timestamp
        plugin.getPlayerDataManager().updateLastSeen(player);
        
        // Save player data immediately on quit
        plugin.getPlayerDataManager().savePlayerData(plugin.getPlayerDataManager().getPlayerData(player));
        
        // Clean up tips task when player leaves
        plugin.getTipsManager().cancelPlayerTask(player.getUniqueId());
    }
}

