package com.playpandora.pandoraonevsone.listeners;

import com.playpandora.pandoraonevsone.PandoraOnevsOne;
import com.playpandora.pandoraonevsone.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Listener to block ALL commands during 1v1 state
 */
public class OneVsOneCommandListener implements Listener {
    
    private final PandoraOnevsOne plugin;
    
    public OneVsOneCommandListener(PandoraOnevsOne plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Block ALL commands during 1v1 state (except ops with bypass permission)
     * Specifically blocks auction house commands: ./ah, /auctionhouse, /auction, /sell, /sellall
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        
        // Allow ops with bypass permission
        if (player.hasPermission("pandoraonevsone.bypass")) {
            return;
        }
        
        // Check if player is in duel
        if (!plugin.getOneVsOneManager().isInDuel(player)) {
            return;
        }
        
        String command = event.getMessage().toLowerCase().trim();
        
        // Block ALL risky commands: trade, sellall, ah, auction, bp, backpack, etc.
        if (command.startsWith("./ah") || 
            command.startsWith("/ah") ||
            command.startsWith("./auctionhouse") ||
            command.startsWith("/auctionhouse") ||
            command.startsWith("./auction") ||
            command.startsWith("/auction") ||
            command.startsWith("./sell") ||
            command.startsWith("/sell") ||
            command.startsWith("./sellall") ||
            command.startsWith("/sellall") ||
            command.startsWith("./trade") ||
            command.startsWith("/trade") ||
            command.startsWith("./bp") ||
            command.startsWith("/bp") ||
            command.startsWith("./backpack") ||
            command.startsWith("/backpack")) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.error("You cannot use that command during a 1v1 duel!"));
            return;
        }
        
        // Block ALL other commands (no exceptions)
        event.setCancelled(true);
        player.sendMessage(ColorUtil.error("You cannot use commands during a 1v1 duel!"));
    }
}


