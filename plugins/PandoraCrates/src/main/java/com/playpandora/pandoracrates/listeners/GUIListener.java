package com.playpandora.pandoracrates.listeners;

import com.playpandora.pandoracrates.PandoraCrates;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class GUIListener implements Listener {
    
    private final PandoraCrates plugin;
    
    public GUIListener(PandoraCrates plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        
        // Protect Preview GUI
        if (title.contains("Preview")) {
            event.setCancelled(true);
            
            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                int slot = event.getSlot();
                
                // Check slot numbers directly for navigation buttons
                if (slot == 45 || slot == 53) {
                    // Page navigation buttons
                    plugin.getPreviewGUI().handlePageNavigation(player, slot);
                    return;
                }
                
                // Check for close button
                if (slot == 49 && event.getCurrentItem() != null && 
                    event.getCurrentItem().getType() == org.bukkit.Material.BARRIER) {
                    // Close button
                    event.getWhoClicked().closeInventory();
                    plugin.getPreviewGUI().cleanup(player);
                    return;
                }
            }
            return;
        }
        
        // Protect Spinner GUI (crate opening animation)
        if (title.contains("Opening...") || title.contains("Opening")) {
            event.setCancelled(true);
            return;
        }
        
        // Prevent opening other inventories while crate animation is active
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            if (plugin.getAnimationManager().isPlayerAnimating(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        
        // Protect Preview GUI
        if (title.contains("Preview")) {
            event.setCancelled(true);
            return;
        }
        
        // Protect Spinner GUI (crate opening animation)
        if (title.contains("Opening...") || title.contains("Opening")) {
            event.setCancelled(true);
            return;
        }
        
        // Prevent dragging items while crate animation is active
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            if (plugin.getAnimationManager().isPlayerAnimating(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        // Prevent opening other inventories while crate animation is active
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            if (plugin.getAnimationManager().isPlayerAnimating(player.getUniqueId())) {
                String title = event.getView().getTitle();
                // Only cancel if it's not the spinner GUI
                if (!title.contains("Opening...") && !title.contains("Opening")) {
                    event.setCancelled(true);
                }
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Clean up if needed
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            String title = event.getView().getTitle();
            
            // Clean up preview GUI data
            if (title.contains("Preview")) {
                plugin.getPreviewGUI().cleanup(player);
            }
            
            // If player closes spinner GUI while animation is active, cancel it
            if ((title.contains("Opening...") || title.contains("Opening")) && 
                plugin.getAnimationManager().isPlayerAnimating(player.getUniqueId())) {
                // Don't cancel animation on close - let it finish naturally
                // The animation will handle cleanup when it completes
            }
        }
    }
}

