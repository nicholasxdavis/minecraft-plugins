package com.playpandora.hook.integrations;

import com.playpandora.hook.Hook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class BountyIntegration implements Listener {
    
    private final Hook plugin;
    
    public BountyIntegration(Hook plugin) {
        this.plugin = plugin;
    }
    
    public void hook() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler
    public void onBountyClaimed(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("integrations.bounty.claim-notification", true)) {
            return;
        }
        
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        if (killer == null || !(killer instanceof Player)) {
            return;
        }
        
        // Check if victim had a bounty (qab.sk system)
        // This would need to check Skript variables
        try {
            // Try to get bounty amount from Skript
            // Note: This is a simplified check - actual implementation would need Skript API
            plugin.getAPI().sendCustom(killer, "Bounty Claimed!", 
                "You claimed the bounty on " + victim.getName() + "!", 
                500, 3000, 1000);
        } catch (Exception e) {
            // Silently fail
        }
    }
}




