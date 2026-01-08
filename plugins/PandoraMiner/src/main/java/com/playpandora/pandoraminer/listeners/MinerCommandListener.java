package com.playpandora.pandoraminer.listeners;

import com.playpandora.pandoraminer.PandoraMiner;
import com.playpandora.pandoraminer.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Listener to block auction house commands during miner state
 */
public class MinerCommandListener implements Listener {
    
    private final PandoraMiner plugin;
    
    public MinerCommandListener(PandoraMiner plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Block auction house commands during miner state
     * Blocks: ./ah, /auctionhouse, /auction, /sell, /sellall
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        
        // Allow ops with bypass permission (if we add one)
        // For now, just check miner state
        
        // Check if player is in miner mode
        if (!plugin.getMinerManager().isMiner(player)) {
            return;
        }
        
        String command = event.getMessage().toLowerCase().trim();
        
        // Block risky commands: trade, sellall, ah, auction
        // Allow: ./sell, ./bp, ./backpack (but still prevent placing gold set/netherite pickaxe via MinerListener)
        if (command.startsWith("./ah") || 
            command.startsWith("/ah") ||
            command.startsWith("./auctionhouse") ||
            command.startsWith("/auctionhouse") ||
            command.startsWith("./auction") ||
            command.startsWith("/auction") ||
            command.startsWith("./sellall") ||
            command.startsWith("/sellall") ||
            command.startsWith("./trade") ||
            command.startsWith("/trade")) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.error("You cannot use that command in miner mode! Use /miner to disable miner mode."));
            return;
        }
    }
}



