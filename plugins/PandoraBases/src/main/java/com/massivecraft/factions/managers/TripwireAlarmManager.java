package com.massivecraft.factions.managers;

import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages tripwire alarm systems for factions
 * Requires 4 tripwires placed 20 blocks apart to activate
 */
public class TripwireAlarmManager {
    
    private final FactionsPlugin plugin;
    private final Map<String, AlarmSystem> alarmSystems = new HashMap<>();
    private File alarmDataFile;
    private FileConfiguration alarmDataConfig;
    
    // Required configuration
    private static final int REQUIRED_TRIPWIRES = 4;
    private static final double MIN_DISTANCE = 20.0; // 20 blocks minimum distance
    
    public TripwireAlarmManager(FactionsPlugin plugin) {
        this.plugin = plugin;
        loadAlarmData();
    }
    
    /**
     * Add a tripwire to a faction's alarm system
     * @return true if alarm system is now complete (all 4 tripwires placed)
     */
    public boolean addTripwire(Faction faction, Location tripwireLoc) {
        if (faction == null || !faction.isNormal()) {
            return false;
        }
        
        String factionId = faction.getId();
        AlarmSystem alarm = alarmSystems.get(factionId);
        
        if (alarm == null) {
            alarm = new AlarmSystem(factionId);
            alarmSystems.put(factionId, alarm);
        }
        
        // Check if tripwire is far enough from existing ones
        for (Location existing : alarm.getTripwireLocations()) {
            if (tripwireLoc.getWorld().equals(existing.getWorld())) {
                double distance = tripwireLoc.distance(existing);
                if (distance < MIN_DISTANCE) {
                    return false; // Too close to existing tripwire
                }
            }
        }
        
        // Add the tripwire
        alarm.addTripwire(tripwireLoc);
        
        // Check if system is complete
        boolean isComplete = alarm.getTripwireCount() >= REQUIRED_TRIPWIRES;
        
        // Save data
        saveAlarmData();
        
        return isComplete;
    }
    
    /**
     * Remove a tripwire from a faction's alarm system
     */
    public void removeTripwire(Faction faction, Location tripwireLoc) {
        if (faction == null || !faction.isNormal()) {
            return;
        }
        
        String factionId = faction.getId();
        AlarmSystem alarm = alarmSystems.get(factionId);
        
        if (alarm != null) {
            alarm.removeTripwire(tripwireLoc);
            
            if (alarm.getTripwireCount() == 0) {
                alarmSystems.remove(factionId);
            }
            
            saveAlarmData();
        }
    }
    
    /**
     * Check if a faction has an active alarm system
     */
    public boolean hasActiveAlarm(Faction faction) {
        if (faction == null || !faction.isNormal()) {
            return false;
        }
        
        String factionId = faction.getId();
        AlarmSystem alarm = alarmSystems.get(factionId);
        
        return alarm != null && alarm.isComplete();
    }
    
    /**
     * Get all tripwire locations for a faction
     */
    public List<Location> getTripwireLocations(Faction faction) {
        if (faction == null || !faction.isNormal()) {
            return Collections.emptyList();
        }
        
        String factionId = faction.getId();
        AlarmSystem alarm = alarmSystems.get(factionId);
        
        if (alarm != null) {
            return new ArrayList<>(alarm.getTripwireLocations());
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Remove all alarms for a faction (when disbanded)
     */
    public void removeAlarmSystem(Faction faction) {
        if (faction == null) {
            return;
        }
        
        String factionId = faction.getId();
        alarmSystems.remove(factionId);
        saveAlarmData();
    }
    
    private void loadAlarmData() {
        alarmDataFile = new File(plugin.getDataFolder(), "tripwire-alarms.yml");
        
        if (!alarmDataFile.exists()) {
            try {
                alarmDataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create tripwire-alarms.yml: " + e.getMessage());
                return;
            }
        }
        
        alarmDataConfig = YamlConfiguration.loadConfiguration(alarmDataFile);
        
        // Load all alarm systems
        if (alarmDataConfig.contains("alarms")) {
            for (String factionId : alarmDataConfig.getConfigurationSection("alarms").getKeys(false)) {
                String path = "alarms." + factionId + ".tripwires";
                
                if (alarmDataConfig.contains(path)) {
                    AlarmSystem alarm = new AlarmSystem(factionId);
                    
                    List<String> tripwireStrings = alarmDataConfig.getStringList(path);
                    for (String tripwireStr : tripwireStrings) {
                        try {
                            Location loc = parseLocation(tripwireStr);
                            if (loc != null) {
                                alarm.addTripwire(loc);
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to parse tripwire location: " + tripwireStr);
                        }
                    }
                    
                    if (alarm.getTripwireCount() > 0) {
                        alarmSystems.put(factionId, alarm);
                    }
                }
            }
        }
        
        plugin.getLogger().info("Loaded " + alarmSystems.size() + " tripwire alarm system(s).");
    }
    
    public void saveAlarmData() {
        if (alarmDataConfig == null || alarmDataFile == null) {
            return;
        }
        
        // Clear existing data
        alarmDataConfig.set("alarms", null);
        
        // Save all alarm systems
        for (Map.Entry<String, AlarmSystem> entry : alarmSystems.entrySet()) {
            String factionId = entry.getKey();
            AlarmSystem alarm = entry.getValue();
            
            String path = "alarms." + factionId + ".tripwires";
            List<String> tripwireStrings = new ArrayList<>();
            
            for (Location loc : alarm.getTripwireLocations()) {
                tripwireStrings.add(locationToString(loc));
            }
            
            alarmDataConfig.set(path, tripwireStrings);
        }
        
        try {
            alarmDataConfig.save(alarmDataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save tripwire-alarms.yml: " + e.getMessage());
        }
    }
    
    private String locationToString(Location loc) {
        if (loc.getWorld() == null) {
            return null;
        }
        return loc.getWorld().getName() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ();
    }
    
    private Location parseLocation(String str) {
        try {
            String[] parts = str.split(":");
            if (parts.length == 4) {
                String worldName = parts[0];
                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);
                
                org.bukkit.World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    return new Location(world, x, y, z);
                }
            }
        } catch (Exception e) {
            // Invalid format
        }
        return null;
    }
    
    /**
     * Internal class to represent an alarm system
     */
    private static class AlarmSystem {
        private final String factionId;
        private final Set<Location> tripwireLocations = new HashSet<>();
        private final Set<String> tripwireKeys = new HashSet<>(); // For quick lookup
        
        public AlarmSystem(String factionId) {
            this.factionId = factionId;
        }
        
        public void addTripwire(Location loc) {
            String key = locationToKey(loc);
            if (!tripwireKeys.contains(key)) {
                tripwireLocations.add(loc);
                tripwireKeys.add(key);
            }
        }
        
        public void removeTripwire(Location loc) {
            String key = locationToKey(loc);
            if (tripwireKeys.remove(key)) {
                tripwireLocations.removeIf(l -> locationToKey(l).equals(key));
            }
        }
        
        public int getTripwireCount() {
            return tripwireLocations.size();
        }
        
        public boolean isComplete() {
            return getTripwireCount() >= REQUIRED_TRIPWIRES;
        }
        
        public Set<Location> getTripwireLocations() {
            return Collections.unmodifiableSet(tripwireLocations);
        }
        
        private String locationToKey(Location loc) {
            if (loc.getWorld() == null) {
                return loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
            }
            return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
        }
    }
}

