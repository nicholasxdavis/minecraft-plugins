package com.playpandora.treasurehunt.listeners;

import com.playpandora.treasurehunt.TreasureHunt;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class TreasureListener implements Listener {
    
    private final TreasureHunt plugin;
    
    public TreasureListener(TreasureHunt plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CHEST) {
            return;
        }
        
        Player player = event.getPlayer();
        org.bukkit.Location treasureLoc = plugin.getTreasureManager().getCurrentTreasureLocation();
        
        // Check if this is the treasure chest
        if (treasureLoc != null && block.getLocation().equals(treasureLoc)) {
            event.setCancelled(true);
            
            double distance = player.getLocation().distance(treasureLoc);
            
            // Check if player is close enough for hints (within hint-distance)
            double hintDistance = plugin.getConfig().getDouble("treasure.hint-distance", 100.0);
            
            // First check if player can still claim hints
            int hintsRemaining = plugin.getTreasureManager().getHintsRemaining(player);
            if (hintsRemaining > 0 && distance <= hintDistance) {
                // Player can claim hints and is within hint distance, try to claim hint
                if (plugin.getTreasureManager().claimHint(player)) {
                    // Success - message sent in claimHint
                } else {
                    // Already claimed all hints or other issue
                }
            } else if (distance <= 5.0 && hintsRemaining <= 0) {
                // Player is close enough and has claimed all hints, can claim treasure
                plugin.getTreasureManager().claimTreasure(player);
            } else if (distance > hintDistance) {
                // Too far for hints
                player.sendMessage(plugin.formatMessage(plugin.getConfig().getString("messages.too-far", "{prefix} &7You are too far from the treasure location!")));
            } else if (hintsRemaining <= 0 && distance > 5.0) {
                // Has all hints but not close enough for treasure
                String prefix = "&e&lPandora &8» &r";
                player.sendMessage(plugin.formatMessage(prefix + " &7You have claimed all hints! Get closer to &6" + String.format("%.1f", distance) + " &7blocks to claim the treasure."));
            }
        }
    }
}





