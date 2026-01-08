package com.playpandora.pandoraonevsone.listeners;

import com.playpandora.pandoraonevsone.PandoraOnevsOne;
import com.playpandora.pandoraonevsone.integration.PandoraBasesIntegration;
import com.playpandora.pandoraonevsone.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;

/**
 * Listener to prevent opening base chests during 1v1 duels
 */
public class OneVsOneBaseChestListener implements Listener {
    
    private final PandoraOnevsOne plugin;
    
    public OneVsOneBaseChestListener(PandoraOnevsOne plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Prevent opening base chests during 1v1
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        
        if (!plugin.getOneVsOneManager().isInDuel(player)) {
            return;
        }
        
        // Check if this is a base chest
        if (PandoraBasesIntegration.isBaseChest(event.getInventory())) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.error("You cannot access base chests during a 1v1 duel!"));
        }
    }
}


