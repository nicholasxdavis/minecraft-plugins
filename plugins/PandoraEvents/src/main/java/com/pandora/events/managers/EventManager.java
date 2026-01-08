package com.pandora.events.managers;

import com.pandora.events.PandoraEventsPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Manages active event state
 */
public class EventManager {
    
    private boolean eventActive = false;
    private Location eventBeaconLocation = null;
    private Object eventFaction = null;
    private String eventFactionId = null;
    
    /**
     * Check if an event is currently active
     */
    public boolean isEventActive() {
        return eventActive;
    }
    
    /**
     * Set event as active
     */
    public void setEventActive(boolean active) {
        this.eventActive = active;
    }
    
    /**
     * Set the event beacon location
     */
    public void setEventBeaconLocation(Location location) {
        this.eventBeaconLocation = location;
    }
    
    /**
     * Check if a location is the event beacon
     */
    public boolean isEventBeacon(Location location) {
        if (eventBeaconLocation == null || location == null) {
            return false;
        }
        return eventBeaconLocation.getWorld() == location.getWorld() &&
               eventBeaconLocation.getBlockX() == location.getBlockX() &&
               eventBeaconLocation.getBlockY() == location.getBlockY() &&
               eventBeaconLocation.getBlockZ() == location.getBlockZ();
    }
    
    /**
     * Set the event faction
     */
    public void setEventFaction(Object faction) {
        this.eventFaction = faction;
    }
    
    /**
     * Get the event faction
     */
    public Object getEventFaction() {
        return eventFaction;
    }
    
    /**
     * Set event faction ID
     */
    public void setEventFactionId(String factionId) {
        this.eventFactionId = factionId;
    }
    
    /**
     * Get event faction ID
     */
    public String getEventFactionId() {
        return eventFactionId;
    }
    
    /**
     * End the event
     */
    public void endEvent(Player winner) {
        this.eventActive = false;
        this.eventBeaconLocation = null;
        this.eventFaction = null;
        this.eventFactionId = null;
        
        // Save state immediately
        PandoraEventsPlugin.getInstance().saveEventState();
        
        PandoraEventsPlugin.getInstance().getLogger().info("Event ended by " + (winner != null ? winner.getName() : "system"));
    }
    
    /**
     * Get event beacon location
     */
    public Location getEventBeaconLocation() {
        return eventBeaconLocation;
    }
}

