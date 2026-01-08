package com.pandora.events.data;

import com.pandora.events.PandoraEventsPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Manages saving and loading event data
 */
public class EventDataManager {
    
    private static final String DATA_FILE_NAME = "event-data.yml";
    private File dataFile;
    private FileConfiguration dataConfig;
    private EventData eventData;
    
    public EventDataManager() {
        PandoraEventsPlugin plugin = PandoraEventsPlugin.getInstance();
        if (plugin == null) {
            throw new IllegalStateException("PandoraEventsPlugin instance is null!");
        }
        
        // Ensure data folder exists
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        this.dataFile = new File(dataFolder, DATA_FILE_NAME);
        loadData();
    }
    
    /**
     * Load event data from file
     */
    public void loadData() {
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                PandoraEventsPlugin.getInstance().getLogger().log(Level.SEVERE, "Failed to create data file", e);
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        eventData = new EventData();
        
        // Load data from config
        if (dataConfig.contains("event-active")) {
            eventData.setEventActive(dataConfig.getBoolean("event-active"));
        }
        
        if (dataConfig.contains("event-beacon")) {
            String world = dataConfig.getString("event-beacon.world");
            double x = dataConfig.getDouble("event-beacon.x");
            double y = dataConfig.getDouble("event-beacon.y");
            double z = dataConfig.getDouble("event-beacon.z");
            if (world != null) {
                org.bukkit.World w = org.bukkit.Bukkit.getWorld(world);
                if (w != null) {
                    eventData.setEventBeaconLocation(new org.bukkit.Location(w, x, y, z));
                }
            }
        }
        
        if (dataConfig.contains("event-faction-id")) {
            eventData.setEventFactionId(dataConfig.getString("event-faction-id"));
        }
        
        if (dataConfig.contains("auto-events-enabled")) {
            eventData.setAutoEventsEnabled(dataConfig.getBoolean("auto-events-enabled"));
        }
        
        if (dataConfig.contains("first-event-time")) {
            String firstTime = dataConfig.getString("first-event-time");
            if (firstTime != null && !firstTime.isEmpty()) {
                eventData.setFirstEventTime(firstTime);
            }
        }
        
        if (dataConfig.contains("second-event-time")) {
            String secondTime = dataConfig.getString("second-event-time");
            if (secondTime != null && !secondTime.isEmpty()) {
                eventData.setSecondEventTime(secondTime);
            }
        }
        
        if (dataConfig.contains("last-first-event-time")) {
            eventData.setLastFirstEventTime(dataConfig.getLong("last-first-event-time"));
        }
        
        if (dataConfig.contains("last-second-event-time")) {
            eventData.setLastSecondEventTime(dataConfig.getLong("last-second-event-time"));
        }
        
        PandoraEventsPlugin.getInstance().getLogger().info("Loaded event data from file");
    }
    
    /**
     * Save event data to file
     */
    public void saveData() {
        if (dataConfig == null || eventData == null) {
            return;
        }
        
        // Save all data
        dataConfig.set("event-active", eventData.isEventActive());
        
        org.bukkit.Location beaconLoc = eventData.getEventBeaconLocation();
        if (beaconLoc != null) {
            dataConfig.set("event-beacon.world", beaconLoc.getWorld().getName());
            dataConfig.set("event-beacon.x", beaconLoc.getX());
            dataConfig.set("event-beacon.y", beaconLoc.getY());
            dataConfig.set("event-beacon.z", beaconLoc.getZ());
        } else {
            dataConfig.set("event-beacon", null);
        }
        
        dataConfig.set("event-faction-id", eventData.getEventFactionId());
        dataConfig.set("auto-events-enabled", eventData.isAutoEventsEnabled());
        dataConfig.set("first-event-time", eventData.getFirstEventTime());
        dataConfig.set("second-event-time", eventData.getSecondEventTime());
        dataConfig.set("last-first-event-time", eventData.getLastFirstEventTime());
        dataConfig.set("last-second-event-time", eventData.getLastSecondEventTime());
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            PandoraEventsPlugin.getInstance().getLogger().log(Level.SEVERE, "Failed to save event data", e);
        }
    }
    
    /**
     * Get event data
     */
    public EventData getEventData() {
        if (eventData == null) {
            eventData = new EventData();
        }
        return eventData;
    }
    
    /**
     * Force save (synchronous)
     */
    public void forceSave() {
        saveData();
    }
}

