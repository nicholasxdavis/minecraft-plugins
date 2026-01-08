package com.pandora.events.managers;

import com.pandora.events.PandoraEventsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Manages automatic event scheduling (twice daily by default)
 */
public class AutoEventScheduler {
    
    private boolean autoEnabled = true;
    private BukkitTask schedulerTask = null;
    private static final long TICKS_PER_DAY = 24000L; // Minecraft day
    private static final long TICKS_PER_HOUR = 1000L; // Minecraft hour
    
    // Default times: 12:00 PM and 12:00 AM (noon and midnight)
    private LocalTime firstEventTime = LocalTime.of(12, 0);
    private LocalTime secondEventTime = LocalTime.of(0, 0);
    
    // Track last event spawn times to prevent duplicates
    private long lastFirstEventTime = 0;
    private long lastSecondEventTime = 0;
    
    /**
     * Start the auto-event scheduler
     */
    public void start() {
        loadConfig();
        
        if (!autoEnabled) {
            PandoraEventsPlugin.getInstance().getLogger().info("Auto-event scheduler is disabled");
            return;
        }
        
        // Schedule events twice daily
        schedulerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!autoEnabled) {
                    return;
                }
                
                // Check if it's time for an event
                LocalTime now = LocalTime.now();
                long currentTime = System.currentTimeMillis();
                
                // Check first event time (with 1 minute tolerance)
                if (isTimeForEvent(now, firstEventTime)) {
                    // Only spawn if we haven't spawned in the last 30 minutes
                    if (currentTime - lastFirstEventTime > 30 * 60 * 1000) {
                        spawnScheduledEvent("first");
                        lastFirstEventTime = currentTime;
                        // Save immediately
                        PandoraEventsPlugin.getInstance().saveEventState();
                    }
                }
                
                // Check second event time (with 1 minute tolerance)
                if (isTimeForEvent(now, secondEventTime)) {
                    // Only spawn if we haven't spawned in the last 30 minutes
                    if (currentTime - lastSecondEventTime > 30 * 60 * 1000) {
                        spawnScheduledEvent("second");
                        lastSecondEventTime = currentTime;
                        // Save immediately
                        PandoraEventsPlugin.getInstance().saveEventState();
                    }
                }
            }
        }.runTaskTimer(PandoraEventsPlugin.getInstance(), 0L, 20L * 60L); // Check every minute
        
        PandoraEventsPlugin.getInstance().getLogger().info(
            "Auto-event scheduler started. Events will spawn at " + 
            firstEventTime.format(DateTimeFormatter.ofPattern("HH:mm")) + " and " +
            secondEventTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        );
    }
    
    /**
     * Check if current time matches event time (within 1 minute)
     */
    private boolean isTimeForEvent(LocalTime now, LocalTime eventTime) {
        int nowMinutes = now.getHour() * 60 + now.getMinute();
        int eventMinutes = eventTime.getHour() * 60 + eventTime.getMinute();
        
        return Math.abs(nowMinutes - eventMinutes) <= 1;
    }
    
    /**
     * Spawn a scheduled event (uses world spawn location)
     */
    private void spawnScheduledEvent(String eventType) {
        if (PandoraEventsPlugin.getInstance().getEventManager().isEventActive()) {
            // Event already active, skip
            return;
        }
        
        // Get a random world and use its spawn location
        org.bukkit.World world = getRandomWorld();
        if (world == null) {
            PandoraEventsPlugin.getInstance().getLogger().warning("No valid world found for scheduled event!");
            return;
        }
        
        Location spawnLocation = world.getSpawnLocation();
        
        PandoraEventsPlugin.getInstance().getLogger().info("Spawning scheduled " + eventType + " event at " + world.getName() + " spawn");
        Bukkit.broadcastMessage(com.pandora.events.util.PandoraMessage.format(
            com.pandora.events.util.PandoraMessage.highlight("A scheduled event is starting now at ") +
            com.pandora.events.util.PandoraMessage.text(world.getName() + " spawn!")
        ));
        
        // Spawn the event at world spawn
        PandoraEventsPlugin.getInstance().getEventSpawner().spawnEventBase(spawnLocation);
    }
    
    /**
     * Get a random world for auto-events
     */
    private org.bukkit.World getRandomWorld() {
        java.util.List<org.bukkit.World> validWorlds = new java.util.ArrayList<>();
        
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            org.bukkit.World.Environment env = world.getEnvironment();
            if (env == org.bukkit.World.Environment.NORMAL || 
                env == org.bukkit.World.Environment.NETHER || 
                env == org.bukkit.World.Environment.THE_END) {
                validWorlds.add(world);
            }
        }
        
        if (validWorlds.isEmpty()) {
            return Bukkit.getWorlds().get(0); // Fallback to first world
        }
        
        return validWorlds.get(new java.util.Random().nextInt(validWorlds.size()));
    }
    
    /**
     * Enable or disable auto-events
     */
    public void setAutoEnabled(boolean enabled) {
        this.autoEnabled = enabled;
        saveConfig();
        
        if (enabled && schedulerTask == null) {
            start();
        } else if (!enabled && schedulerTask != null) {
            schedulerTask.cancel();
            schedulerTask = null;
        }
        
        PandoraEventsPlugin.getInstance().getLogger().info(
            "Auto-event scheduler " + (enabled ? "enabled" : "disabled")
        );
    }
    
    /**
     * Check if auto-events are enabled
     */
    public boolean isAutoEnabled() {
        return autoEnabled;
    }
    
    /**
     * Load configuration (now uses EventDataManager)
     */
    private void loadConfig() {
        // Data is loaded by EventDataManager, this is just for backwards compatibility
        // The actual loading happens in loadEventState()
    }
    
    /**
     * Save configuration (now uses EventDataManager)
     */
    private void saveConfig() {
        // Data is saved by EventDataManager, this is just for backwards compatibility
        // The actual saving happens in saveEventState()
    }
    
    /**
     * Set event times
     */
    public void setEventTimes(String firstTime, String secondTime) {
        try {
            this.firstEventTime = LocalTime.parse(firstTime, DateTimeFormatter.ofPattern("HH:mm"));
            this.secondEventTime = LocalTime.parse(secondTime, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            PandoraEventsPlugin.getInstance().getLogger().warning("Invalid time format: " + e.getMessage());
        }
    }
    
    /**
     * Set last event times
     */
    public void setLastEventTimes(long firstTime, long secondTime) {
        this.lastFirstEventTime = firstTime;
        this.lastSecondEventTime = secondTime;
    }
    
    /**
     * Get first event time as string
     */
    public String getFirstEventTime() {
        return firstEventTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
    
    /**
     * Get second event time as string
     */
    public String getSecondEventTime() {
        return secondEventTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
    
    /**
     * Get last first event time
     */
    public long getLastFirstEventTime() {
        return lastFirstEventTime;
    }
    
    /**
     * Get last second event time
     */
    public long getLastSecondEventTime() {
        return lastSecondEventTime;
    }
    
    /**
     * Stop the scheduler
     */
    public void stop() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
            schedulerTask = null;
        }
    }
}

