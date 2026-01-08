package com.playpandora.levelenchant.listeners;

import com.playpandora.levelenchant.LevelEnchant;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    
    private final LevelEnchant plugin;
    
    public PlayerListener(LevelEnchant plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Update XP bar when player joins and load/save player data
        if (plugin.getDataManager() != null && plugin.getLevelIntegration().isAvailable()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                org.bukkit.entity.Player player = event.getPlayer();
                int level = plugin.getLevelIntegration().getLevel(player);
                double xp = plugin.getLevelIntegration().getXP(player);
                plugin.getDataManager().updatePlayerData(player.getUniqueId(), level, xp);
            }, 10L); // Small delay to ensure LevelPlugin has loaded player data
        }
        
        if (plugin.getXPBarManager() != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getXPBarManager().updatePlayer(event.getPlayer());
            }, 10L); // Small delay to ensure LevelPlugin has loaded player data
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save player data when they leave
        if (plugin.getDataManager() != null) {
            plugin.getDataManager().savePlayerData(event.getPlayer().getUniqueId());
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        // Update XP bar when vanilla XP changes (to override it with LevelPlugin level)
        if (plugin.getXPBarManager() != null && 
            plugin.getConfig().getBoolean("features.display-level-on-xp-bar", true)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getXPBarManager().updatePlayer(event.getPlayer());
            });
        }
    }
}

