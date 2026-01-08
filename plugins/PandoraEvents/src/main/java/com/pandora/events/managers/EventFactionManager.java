package com.pandora.events.managers;

import com.pandora.events.PandoraEventsPlugin;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Method;

/**
 * Manages the event faction creation and management
 */
public class EventFactionManager {
    
    private static final String EVENT_FACTION_TAG = "event";
    private Object eventFaction = null;
    
    /**
     * Create or get the event faction
     */
    public Object getOrCreateEventFaction() {
        if (eventFaction != null) {
            return eventFaction;
        }
        
        try {
            // Access PandoraBases Factions class
            Class<?> factionsClass = Class.forName("com.massivecraft.factions.Factions");
            Method getInstanceMethod = factionsClass.getMethod("getInstance");
            Object factionsInstance = getInstanceMethod.invoke(null);
            
            // Check if event faction already exists
            Method getByTagMethod = factionsClass.getMethod("getByTag", String.class);
            Object existingFaction = getByTagMethod.invoke(factionsInstance, EVENT_FACTION_TAG);
            
            if (existingFaction != null) {
                eventFaction = existingFaction;
                return eventFaction;
            }
            
            // Create new faction
            Method createFactionMethod = factionsClass.getMethod("createFaction");
            eventFaction = createFactionMethod.invoke(factionsInstance);
            
            // Set the tag
            Method setTagMethod = eventFaction.getClass().getMethod("setTag", String.class);
            setTagMethod.invoke(eventFaction, EVENT_FACTION_TAG);
            
            // Set as permanent
            try {
                Method setPermanentMethod = eventFaction.getClass().getMethod("setPermanent", boolean.class);
                setPermanentMethod.invoke(eventFaction, true);
            } catch (Exception e) {
                // Method might not exist, ignore
            }
            
            PandoraEventsPlugin.getInstance().getLogger().info("Created event faction: " + EVENT_FACTION_TAG);
            return eventFaction;
            
        } catch (Exception e) {
            PandoraEventsPlugin.getInstance().getLogger().severe("Failed to create event faction: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Claim area for the event faction
     */
    public boolean claimArea(Location center, int radiusChunks) {
        Object faction = getOrCreateEventFaction();
        if (faction == null) {
            return false;
        }
        
        try {
            // Access Board class
            Class<?> boardClass = Class.forName("com.massivecraft.factions.Board");
            Method getInstanceMethod = boardClass.getMethod("getInstance");
            Object boardInstance = getInstanceMethod.invoke(null);
            
            // Access FLocation class
            Class<?> flocationClass = Class.forName("com.massivecraft.factions.FLocation");
            Method wrapMethod = flocationClass.getMethod("wrap", Location.class);
            
            int claimed = 0;
            World world = center.getWorld();
            int centerChunkX = center.getChunk().getX();
            int centerChunkZ = center.getChunk().getZ();
            
            for (int x = -radiusChunks; x <= radiusChunks; x++) {
                for (int z = -radiusChunks; z <= radiusChunks; z++) {
                    try {
                        int chunkX = centerChunkX + x;
                        int chunkZ = centerChunkZ + z;
                        
                        // Create FLocation using wrap(String, int, int) - chunk coordinates
                        Method wrapWorldMethod = flocationClass.getMethod("wrap", String.class, int.class, int.class);
                        Object floc = wrapWorldMethod.invoke(null, world.getName(), chunkX, chunkZ);
                        
                        // Set faction at location
                        Method setFactionAtMethod = boardClass.getMethod("setFactionAt", 
                            Class.forName("com.massivecraft.factions.Faction"), 
                            flocationClass);
                        setFactionAtMethod.invoke(boardInstance, faction, floc);
                        claimed++;
                    } catch (Exception e) {
                        // Failed to claim this chunk, continue
                        PandoraEventsPlugin.getInstance().getLogger().warning("Failed to claim chunk: " + e.getMessage());
                    }
                }
            }
            
            PandoraEventsPlugin.getInstance().getLogger().info("Claimed " + claimed + " chunks for event faction");
            return claimed > 0;
            
        } catch (Exception e) {
            PandoraEventsPlugin.getInstance().getLogger().severe("Failed to claim area: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Set beacon location for event faction
     */
    public void setBeaconLocation(Location location) {
        Object faction = getOrCreateEventFaction();
        if (faction == null) {
            return;
        }
        
        try {
            Method setBeaconLocationMethod = faction.getClass().getMethod("setBeaconLocation", Location.class);
            setBeaconLocationMethod.invoke(faction, location);
            
            Method setHomeMethod = faction.getClass().getMethod("setHome", Location.class);
            setHomeMethod.invoke(faction, location);
        } catch (Exception e) {
            PandoraEventsPlugin.getInstance().getLogger().warning("Failed to set beacon location: " + e.getMessage());
        }
    }
    
    public Object getEventFaction() {
        return eventFaction;
    }
}

