package com.playpandora.hook.integrations;

import com.playpandora.hook.Hook;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class LevelPluginIntegration implements Listener {
    
    private final Hook plugin;
    
    public LevelPluginIntegration(Hook plugin) {
        this.plugin = plugin;
    }
    
    public void hook() {
        // Register as listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Hook into LevelPlugin events
        // This will need to be updated when LevelPlugin fires level up events
        // For now, we'll check periodically or use a custom event system
    }
    
    // Note: This will need to be updated when LevelPlugin exposes level up events
    // For now, we can monitor player levels or hook into LevelPlugin's level system directly
    
    /**
     * Called when a player levels up
     * This should be called from LevelPlugin when a level up occurs
     */
    public void onLevelUp(org.bukkit.entity.Player player, int newLevel) {
        if (!plugin.getConfig().getBoolean("integrations.levelplugin.level-up-notification", true)) {
            return;
        }
        
        plugin.getAPI().sendLevelUp(player, newLevel);
    }
}

