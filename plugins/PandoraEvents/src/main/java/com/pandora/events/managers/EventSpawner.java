package com.pandora.events.managers;

import com.pandora.events.PandoraEventsPlugin;
import com.pandora.events.util.EventStructureGenerator;
import com.pandora.events.util.EventLootManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Handles spawning event bases at specified locations
 */
public class EventSpawner {
    
    private static final int BASE_SIZE = 50; // 50x50 cube (claim is still 100x100)
    private static final int RADIUS_CHUNKS = 4; // 4 chunks radius = 100 blocks (for claim)
    
    /**
     * Spawn an event base at the specified location
     */
    public boolean spawnEventBase(Location spawnLocation) {
        if (spawnLocation == null) {
            PandoraEventsPlugin.getInstance().getLogger().warning("Spawn location is null!");
            return false;
        }
        
        World world = spawnLocation.getWorld();
        if (world == null) {
            PandoraEventsPlugin.getInstance().getLogger().warning("World is null for event spawn!");
            return false;
        }
        
        // Get event faction manager
        EventFactionManager factionManager = PandoraEventsPlugin.getInstance().getEventFactionManager();
        EventManager eventManager = PandoraEventsPlugin.getInstance().getEventManager();
        
        // Set beacon location (center of base) - use player's Y or find safe Y
        Location beaconLoc = spawnLocation.clone();
        int safeY = findSafeY(world, spawnLocation.getBlockX(), spawnLocation.getBlockZ());
        if (safeY > 0) {
            beaconLoc.setY(safeY + 1); // Place beacon 1 block above floor
        } else {
            // Use player's Y if we can't find a safe Y
            beaconLoc.setY(spawnLocation.getY());
        }
        
        // Actually place the beacon block
        beaconLoc.getBlock().setType(org.bukkit.Material.BEACON, false);
        
        // Mark event as active
        eventManager.setEventActive(true);
        eventManager.setEventBeaconLocation(beaconLoc);
        
        // Create/claim faction
        Object eventFaction = factionManager.getOrCreateEventFaction();
        eventManager.setEventFaction(eventFaction);
        factionManager.setBeaconLocation(beaconLoc);
        
        // Save faction ID
        if (eventFaction != null) {
            try {
                String factionId = (String) eventFaction.getClass().getMethod("getId").invoke(eventFaction);
                eventManager.setEventFactionId(factionId);
            } catch (Exception e) {
                // Ignore
            }
        }
        
        // Save state immediately
        PandoraEventsPlugin.getInstance().saveEventState();
        
        // Claim area using chunk coordinates
        int floorY = safeY;
        boolean claimed = factionManager.claimArea(spawnLocation, RADIUS_CHUNKS);
        if (!claimed) {
            PandoraEventsPlugin.getInstance().getLogger().warning("Failed to claim area for event faction!");
            eventManager.setEventActive(false);
            return false;
        }
        
        // Generate structure
        EventStructureGenerator.generateEventStructure(
            factionManager.getEventFaction(),
            spawnLocation,
            RADIUS_CHUNKS,
            floorY,
            beaconLoc
        );
        
        // Populate loot (run after structure is generated)
        Bukkit.getScheduler().runTaskLater(PandoraEventsPlugin.getInstance(), () -> {
            EventLootManager.populateLoot(world, spawnLocation, floorY, BASE_SIZE);
        }, 100L); // Wait 5 seconds for structure to generate
        
        // Announce to all players
        String worldName = getWorldDisplayName(world);
        String coords = String.format("X: %d, Y: %d, Z: %d", 
            spawnLocation.getBlockX(), 
            spawnLocation.getBlockY(), 
            spawnLocation.getBlockZ());
        
        // Use proper color scheme
        String message = com.pandora.events.util.PandoraMessage.format(
            com.pandora.events.util.PandoraMessage.success("An event base has spawned in the ") +
            com.pandora.events.util.PandoraMessage.highlight(worldName) +
            com.pandora.events.util.PandoraMessage.text(" at ") +
            com.pandora.events.util.PandoraMessage.highlight(coords)
        );
        
        Bukkit.broadcastMessage(message);
        
        // Send Hook notifications to all online players
        com.pandora.events.integration.HookIntegration hook = PandoraEventsPlugin.getInstance().getHookIntegration();
        if (hook != null && hook.isHookAvailable()) {
            String title = com.pandora.events.util.PandoraMessage.highlight("Event Spawned!");
            String subtitle = com.pandora.events.util.PandoraMessage.text(worldName + " at " + coords);
            hook.broadcastEvent(title, subtitle);
        }
        
        PandoraEventsPlugin.getInstance().getLogger().info("Event base spawned at " + coords + " in " + worldName);
        
        // Reset first chest opened flag
        com.pandora.events.listeners.ChestOpenListener.reset();
        
        // Schedule 3-minute reminder
        scheduleReminder(worldName, coords, beaconLoc);
        
        return true;
    }
    
    /**
     * Schedule a 3-minute reminder if event is still active
     */
    private void scheduleReminder(String worldName, String coords, Location beaconLoc) {
        org.bukkit.scheduler.BukkitRunnable reminderTask = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                EventManager eventManager = PandoraEventsPlugin.getInstance().getEventManager();
                if (!eventManager.isEventActive()) {
                    return; // Event already ended
                }
                
                // Send reminder with coords
                String reminderMessage = com.pandora.events.util.PandoraMessage.format(
                    com.pandora.events.util.PandoraMessage.highlight("Event Reminder: ") +
                    com.pandora.events.util.PandoraMessage.text("Event is still active in ") +
                    com.pandora.events.util.PandoraMessage.highlight(worldName) +
                    com.pandora.events.util.PandoraMessage.text(" at ") +
                    com.pandora.events.util.PandoraMessage.highlight(coords)
                );
                Bukkit.broadcastMessage(reminderMessage);
            }
        };
        
        // Run after 3 minutes (3600 ticks)
        reminderTask.runTaskLater(PandoraEventsPlugin.getInstance(), 3600L);
    }
    
    
    /**
     * Find a safe Y level for spawning (uses getHighestBlockYAt to avoid chunk loading)
     */
    private int findSafeY(World world, int x, int z) {
        try {
            // Use getHighestBlockYAt which doesn't require chunk loading
            int highestY = world.getHighestBlockYAt(x, z);
            
            // Ensure it's within valid bounds
            int maxY = world.getMaxHeight() - 10;
            int minY = world.getMinHeight() + 10;
            
            if (highestY >= minY && highestY <= maxY) {
                return highestY + 1; // Return one block above the highest block
            }
            
            // Fallback: use a safe default Y based on world type
            if (world.getEnvironment() == Environment.NETHER) {
                return 64; // Nether default
            } else if (world.getEnvironment() == Environment.THE_END) {
                return 64; // End default
            } else {
                return 64; // Overworld default
            }
        } catch (Exception e) {
            PandoraEventsPlugin.getInstance().getLogger().warning("Error finding safe Y: " + e.getMessage());
            // Return a safe default Y
            return world.getEnvironment() == Environment.NETHER ? 64 : 64;
        }
    }
    
    
    /**
     * Get display name for world
     */
    private String getWorldDisplayName(World world) {
        Environment env = world.getEnvironment();
        switch (env) {
            case NORMAL:
                return "Overworld";
            case NETHER:
                return "Nether";
            case THE_END:
                return "End";
            default:
                return world.getName();
        }
    }
}

