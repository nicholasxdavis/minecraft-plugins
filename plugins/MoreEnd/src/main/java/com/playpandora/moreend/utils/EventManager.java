package com.playpandora.moreend.utils;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class EventManager {
    private final JavaPlugin plugin;
    private final Map<String, ActiveEvent> activeEvents = new HashMap<>();
    
    public EventManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void startEvent(String eventId, EventType type, Location center, int durationSeconds) {
        if (activeEvents.containsKey(eventId)) {
            return;
        }
        
        ActiveEvent event = new ActiveEvent(eventId, type, center, durationSeconds);
        activeEvents.put(eventId, event);
        
        event.start(plugin);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                endEvent(eventId);
            }
        }.runTaskLater(plugin, durationSeconds * 20L);
    }
    
    public void endEvent(String eventId) {
        ActiveEvent event = activeEvents.remove(eventId);
        if (event != null) {
            event.end();
        }
    }
    
    public boolean isInEvent(Location loc, EventType type) {
        return activeEvents.values().stream()
            .anyMatch(e -> e.getType() == type && 
                    e.getCenter().distance(loc) <= e.getRadius());
    }
    
    public ActiveEvent getEventAt(Location loc) {
        return activeEvents.values().stream()
            .filter(e -> e.getCenter().distance(loc) <= e.getRadius())
            .findFirst()
            .orElse(null);
    }
    
    public void cleanup() {
        activeEvents.values().forEach(ActiveEvent::end);
        activeEvents.clear();
    }
    
    public enum EventType {
        END_STORM,
        SHULKER_INFESTATION,
        ENDERMAN_TIDE,
        VOID_GEYSER
    }
    
    public static class ActiveEvent {
        private final String id;
        private final EventType type;
        private final Location center;
        private final int durationSeconds;
        private final int radius;
        private final List<BukkitRunnable> tasks = new ArrayList<>();
        private boolean active = false;
        
        public ActiveEvent(String id, EventType type, Location center, int durationSeconds) {
            this.id = id;
            this.type = type;
            this.center = center;
            this.durationSeconds = durationSeconds;
            this.radius = getDefaultRadius(type);
        }
        
        public void start(JavaPlugin plugin) {
            active = true;
        }
        
        public void end() {
            active = false;
            tasks.forEach(BukkitRunnable::cancel);
            tasks.clear();
        }
        
        private int getDefaultRadius(EventType type) {
            switch (type) {
                case END_STORM:
                case SHULKER_INFESTATION:
                    return 100;
                case ENDERMAN_TIDE:
                    return 30;
                case VOID_GEYSER:
                    return 20;
                default:
                    return 30;
            }
        }
        
        public String getId() { return id; }
        public EventType getType() { return type; }
        public Location getCenter() { return center; }
        public int getRadius() { return radius; }
        public boolean isActive() { return active; }
        public void addTask(BukkitRunnable task) { tasks.add(task); }
    }
}

