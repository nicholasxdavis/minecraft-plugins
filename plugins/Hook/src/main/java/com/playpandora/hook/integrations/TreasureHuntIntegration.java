package com.playpandora.hook.integrations;

import com.playpandora.hook.Hook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TreasureHuntIntegration implements Listener {
    
    private final Hook plugin;
    
    public TreasureHuntIntegration(Hook plugin) {
        this.plugin = plugin;
    }
    
    public void hook() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Called when a player finds treasure
     * This should be called from TreasureHunt plugin
     */
    public void onTreasureFound(Player player, String treasureName) {
        if (!plugin.getConfig().getBoolean("integrations.treasurehunt.treasure-notification", true)) {
            return;
        }
        
        plugin.getAPI().sendTreasureFound(player, treasureName);
    }
    
    /**
     * Called when a player claims a hint
     * This should be called from TreasureHunt plugin
     */
    public void onHintClaimed(Player player, int hintsRemaining) {
        if (!plugin.getConfig().getBoolean("integrations.treasurehunt.hint-notification", true)) {
            return;
        }
        
        plugin.getAPI().sendHintClaimed(player, hintsRemaining);
    }
    
    /**
     * Called when a player's distance from treasure should be announced
     * This should be called from TreasureHunt plugin
     */
    public void onTreasureDistanceUpdate(Player player, int hintsAway) {
        if (!plugin.getConfig().getBoolean("integrations.treasurehunt.distance-announcement", true)) {
            return;
        }
        
        plugin.getAPI().sendTreasureDistance(player, hintsAway);
    }
}





