package com.playpandora.hook.integrations;

import com.playpandora.hook.Hook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class BaseSystemIntegration implements Listener {
    
    private final Hook plugin;
    private Object pandoraBasesPlugin;
    
    public BaseSystemIntegration(Hook plugin) {
        this.plugin = plugin;
    }
    
    public void hook() {
        // Hook into PandoraBases (not BaseSystem)
        org.bukkit.plugin.Plugin pandoraBases = Bukkit.getPluginManager().getPlugin("PandoraBases");
        
        if (pandoraBases == null) {
            plugin.getLogger().warning("PandoraBases plugin not found - base event notifications disabled");
            return;
        }
        
        pandoraBasesPlugin = pandoraBases;
        
        // Register this as a listener to catch PandoraBases events
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        plugin.getLogger().info("PandoraBases integration enabled!");
    }
    
    /**
     * Called when a base event occurs
     */
    public void onBaseEvent(Player player, String eventType) {
        if (!plugin.getConfig().getBoolean("integrations.basesystem.event-notification", true)) {
            return;
        }
        
        if (pandoraBasesPlugin == null) {
            return;
        }
        
        plugin.getAPI().sendBaseEvent(player, eventType);
    }
}

