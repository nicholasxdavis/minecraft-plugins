package com.playpandora.pandoramaster.listeners;

import com.playpandora.pandoramaster.PandoraMaster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class GUIListener implements Listener {
    
    private final PandoraMaster plugin;
    
    public GUIListener(PandoraMaster plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player)) {
            return;
        }
        
        Inventory inv = event.getInventory();
        String title = event.getView().getTitle();
        
        // Check if this is the hub menu (check for "Hub Menu" in title)
        String strippedTitle = org.bukkit.ChatColor.stripColor(title);
        boolean isHubMenu = strippedTitle.contains("Hub Menu");
        
        if (isHubMenu) {
            // CRITICAL: Cancel ALL clicks FIRST - prevent item extraction
            event.setCancelled(true);
            
            // Block ALL interactions with player inventory while hub menu is open
            if (event.getClickedInventory() != null && 
                event.getClickedInventory().equals(event.getView().getBottomInventory())) {
                // Clear cursor to prevent item extraction
                event.setCursor(null);
                return;
            }
            
            // Block shift-click to prevent moving items
            if (event.isShiftClick()) {
                return;
            }
            
            // Block number key clicks (1-9) to prevent hotbar swapping
            if (event.getHotbarButton() >= 0) {
                return;
            }
            
            // Clear cursor on any click to prevent item pickup
            if (event.getCursor() != null && event.getCursor().getType() != org.bukkit.Material.AIR) {
                event.setCursor(null);
            }
            
            // Block item movement actions
            if (event.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY ||
                event.getAction() == org.bukkit.event.inventory.InventoryAction.HOTBAR_SWAP) {
                event.setCurrentItem(null);
                return;
            }
            
            // Only process clicks in the hub inventory (not player inventory)
            if (event.getClickedInventory() == inv) {
                plugin.getHubGUI().handleClick(
                    (org.bukkit.entity.Player) event.getWhoClicked(),
                    event.getSlot(),
                    event.getCurrentItem()
                );
            }
        }
    }
    
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player)) {
            return;
        }
        
        String title = event.getView().getTitle();
        String strippedTitle = org.bukkit.ChatColor.stripColor(title);
        boolean isHubMenu = strippedTitle.contains("Hub Menu");
        
        if (isHubMenu) {
            // CRITICAL: Block ALL drag events to prevent item extraction
            event.setCancelled(true);
        }
    }
}

