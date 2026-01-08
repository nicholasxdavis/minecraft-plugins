package com.playpandora.hook.integrations;

import com.playpandora.hook.Hook;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PandoraMinerIntegration implements Listener {
    
    private final Hook plugin;
    
    public PandoraMinerIntegration(Hook plugin) {
        this.plugin = plugin;
    }
    
    public void hook() {
        // Register as listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        plugin.getLogger().info("PandoraMiner integration ready!");
    }
    
    /**
     * Called when a player enables miner mode
     */
    public void onMinerEnabled(org.bukkit.entity.Player player) {
        if (!plugin.getConfig().getBoolean("integrations.pandoraminer.enabled-notification", true)) {
            return;
        }
        
        plugin.getAPI().sendCustom(player, "&e&lMiner Mode", "&7Enabled", 300, 2000, 800);
    }
    
    /**
     * Called when a player disables miner mode
     */
    public void onMinerDisabled(org.bukkit.entity.Player player) {
        if (!plugin.getConfig().getBoolean("integrations.pandoraminer.disabled-notification", true)) {
            return;
        }
        
        plugin.getAPI().sendCustom(player, "&7Miner Mode", "&cDisabled", 300, 2000, 800);
    }
}



