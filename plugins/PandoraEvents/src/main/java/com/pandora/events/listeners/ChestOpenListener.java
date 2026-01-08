package com.pandora.events.listeners;

import com.pandora.events.PandoraEventsPlugin;
import com.pandora.events.managers.EventManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.lang.reflect.Method;

/**
 * Listener for chest opening - announces first person to open a chest in the event base
 */
public class ChestOpenListener implements Listener {
    
    private static boolean firstChestOpened = false;
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChestOpen(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CHEST) {
            return;
        }
        
        EventManager eventManager = PandoraEventsPlugin.getInstance().getEventManager();
        if (!eventManager.isEventActive()) {
            firstChestOpened = false; // Reset when event ends
            return;
        }
        
        // Check if this chest is in the event base
        if (!isInEventBase(block.getLocation())) {
            return;
        }
        
        // Check if this is the first chest opened
        if (!firstChestOpened) {
            firstChestOpened = true;
            Player player = event.getPlayer();
            
            String message = com.pandora.events.util.PandoraMessage.format(
                com.pandora.events.util.PandoraMessage.highlight(player.getName()) +
                com.pandora.events.util.PandoraMessage.text(" was the first to break into the event base!")
            );
            Bukkit.broadcastMessage(message);
        }
    }
    
    /**
     * Check if a location is inside the event base
     */
    private boolean isInEventBase(org.bukkit.Location loc) {
        try {
            EventManager eventManager = PandoraEventsPlugin.getInstance().getEventManager();
            if (!eventManager.isEventActive()) {
                return false;
            }
            
            org.bukkit.Location beaconLoc = eventManager.getEventBeaconLocation();
            if (beaconLoc == null || !beaconLoc.getWorld().equals(loc.getWorld())) {
                return false;
            }
            
            // Check if location is within 50 blocks of beacon (base cube is 50x50, claim is 100x100)
            double distance = loc.distance(beaconLoc);
            return distance <= 50;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Reset the first chest opened flag (called when event starts)
     */
    public static void reset() {
        firstChestOpened = false;
    }
}

