package com.playpandora.pandoracrates.managers;

import com.playpandora.pandoracrates.PandoraCrates;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LocationDataManager {
    
    private final PandoraCrates plugin;
    private File dataFile;
    private FileConfiguration dataConfig;
    
    public LocationDataManager(PandoraCrates plugin) {
        this.plugin = plugin;
        setupDataFile();
    }
    
    private void setupDataFile() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        dataFile = new File(plugin.getDataFolder(), "locations.yml");
        
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create locations.yml: " + e.getMessage());
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
    
    public void saveCrateLocation(Location location, String crateId) {
        String key = locationToString(location);
        dataConfig.set("locations." + key + ".crate-id", crateId.toLowerCase());
        dataConfig.set("locations." + key + ".world", location.getWorld().getName());
        dataConfig.set("locations." + key + ".x", location.getBlockX());
        dataConfig.set("locations." + key + ".y", location.getBlockY());
        dataConfig.set("locations." + key + ".z", location.getBlockZ());
        // Don't save immediately - let auto-save handle it
    }
    
    public void removeCrateLocation(Location location) {
        String key = locationToString(location);
        dataConfig.set("locations." + key, null);
        // Don't save immediately - let auto-save handle it
    }
    
    public Map<Location, String> loadCrateLocations() {
        Map<Location, String> locations = new HashMap<>();
        
        if (dataConfig.getConfigurationSection("locations") == null) {
            return locations;
        }
        
        Set<String> keys = dataConfig.getConfigurationSection("locations").getKeys(false);
        
        for (String key : keys) {
            String worldName = dataConfig.getString("locations." + key + ".world");
            if (worldName == null) {
                continue;
            }
            
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("World '" + worldName + "' not found for crate location " + key + ". Skipping...");
                continue;
            }
            
            int x = dataConfig.getInt("locations." + key + ".x");
            int y = dataConfig.getInt("locations." + key + ".y");
            int z = dataConfig.getInt("locations." + key + ".z");
            String crateId = dataConfig.getString("locations." + key + ".crate-id");
            
            if (crateId == null) {
                continue;
            }
            
            Location location = new Location(world, x, y, z);
            locations.put(location, crateId.toLowerCase());
        }
        
        plugin.getLogger().info("Loaded " + locations.size() + " crate location(s) from locations.yml");
        return locations;
    }
    
    public void saveDataFile() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save locations.yml: " + e.getMessage());
        }
    }
    
    private String locationToString(Location location) {
        return location.getWorld().getName() + "_" + 
               location.getBlockX() + "_" + 
               location.getBlockY() + "_" + 
               location.getBlockZ();
    }
}


