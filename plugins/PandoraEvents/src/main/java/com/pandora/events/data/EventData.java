package com.pandora.events.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.Serializable;

/**
 * Data class for event persistence
 */
public class EventData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private boolean eventActive;
    private String eventBeaconWorld;
    private double eventBeaconX;
    private double eventBeaconY;
    private double eventBeaconZ;
    private String eventFactionId;
    private boolean autoEventsEnabled;
    private String firstEventTime;
    private String secondEventTime;
    private long lastFirstEventTime;
    private long lastSecondEventTime;
    
    public EventData() {
        this.eventActive = false;
        this.autoEventsEnabled = true;
        this.firstEventTime = "12:00";
        this.secondEventTime = "00:00";
        this.lastFirstEventTime = 0;
        this.lastSecondEventTime = 0;
    }
    
    // Getters and setters
    public boolean isEventActive() {
        return eventActive;
    }
    
    public void setEventActive(boolean eventActive) {
        this.eventActive = eventActive;
    }
    
    public Location getEventBeaconLocation() {
        if (eventBeaconWorld == null) {
            return null;
        }
        World world = Bukkit.getWorld(eventBeaconWorld);
        if (world == null) {
            return null;
        }
        return new Location(world, eventBeaconX, eventBeaconY, eventBeaconZ);
    }
    
    public void setEventBeaconLocation(Location location) {
        if (location == null) {
            this.eventBeaconWorld = null;
            this.eventBeaconX = 0;
            this.eventBeaconY = 0;
            this.eventBeaconZ = 0;
        } else {
            this.eventBeaconWorld = location.getWorld().getName();
            this.eventBeaconX = location.getX();
            this.eventBeaconY = location.getY();
            this.eventBeaconZ = location.getZ();
        }
    }
    
    public String getEventFactionId() {
        return eventFactionId;
    }
    
    public void setEventFactionId(String eventFactionId) {
        this.eventFactionId = eventFactionId;
    }
    
    public boolean isAutoEventsEnabled() {
        return autoEventsEnabled;
    }
    
    public void setAutoEventsEnabled(boolean autoEventsEnabled) {
        this.autoEventsEnabled = autoEventsEnabled;
    }
    
    public String getFirstEventTime() {
        return firstEventTime;
    }
    
    public void setFirstEventTime(String firstEventTime) {
        this.firstEventTime = firstEventTime;
    }
    
    public String getSecondEventTime() {
        return secondEventTime;
    }
    
    public void setSecondEventTime(String secondEventTime) {
        this.secondEventTime = secondEventTime;
    }
    
    public long getLastFirstEventTime() {
        return lastFirstEventTime;
    }
    
    public void setLastFirstEventTime(long lastFirstEventTime) {
        this.lastFirstEventTime = lastFirstEventTime;
    }
    
    public long getLastSecondEventTime() {
        return lastSecondEventTime;
    }
    
    public void setLastSecondEventTime(long lastSecondEventTime) {
        this.lastSecondEventTime = lastSecondEventTime;
    }
}

