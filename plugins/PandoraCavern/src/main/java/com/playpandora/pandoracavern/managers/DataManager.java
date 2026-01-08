package com.playpandora.pandoracavern.managers;

import com.playpandora.pandoracavern.PandoraCavern;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class DataManager {
    
    private final PandoraCavern plugin;
    private File dataFile;
    private FileConfiguration dataConfig;
    private Set<Location> blockLocations;
    
    public DataManager(PandoraCavern plugin) {
        this.plugin = plugin;
        this.blockLocations = new HashSet<>();
        setupDataFile();
    }
    
    private void setupDataFile() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        dataFile = new File(plugin.getDataFolder(), "blocks.yml");
        
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create blocks.yml: " + e.getMessage());
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
    
    public void addBlockLocation(Location location) {
        blockLocations.add(location);
        saveBlockLocation(location);
    }
    
    public void removeBlockLocation(Location location) {
        blockLocations.remove(location);
        removeBlockLocationFromConfig(location);
    }
    
    public boolean isCavernBlock(Location location) {
        return blockLocations.contains(location);
    }
    
    public Set<Location> getBlockLocations() {
        return new HashSet<>(blockLocations);
    }
    
    private void saveBlockLocation(Location location) {
        String key = locationToString(location);
        dataConfig.set("blocks." + key + ".world", location.getWorld().getName());
        dataConfig.set("blocks." + key + ".x", location.getBlockX());
        dataConfig.set("blocks." + key + ".y", location.getBlockY());
        dataConfig.set("blocks." + key + ".z", location.getBlockZ());
        saveDataFile();
    }
    
    private void removeBlockLocationFromConfig(Location location) {
        String key = locationToString(location);
        dataConfig.set("blocks." + key, null);
        saveDataFile();
    }
    
    public void loadBlocks() {
        blockLocations.clear();
        
        if (dataConfig.getConfigurationSection("blocks") == null) {
            plugin.getLogger().info("No saved blocks found.");
            return;
        }
        
        Set<String> keys = dataConfig.getConfigurationSection("blocks").getKeys(false);
        
        for (String key : keys) {
            String worldName = dataConfig.getString("blocks." + key + ".world");
            if (worldName == null) {
                continue;
            }
            
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("World '" + worldName + "' not found for block location " + key + ". Skipping...");
                continue;
            }
            
            int x = dataConfig.getInt("blocks." + key + ".x");
            int y = dataConfig.getInt("blocks." + key + ".y");
            int z = dataConfig.getInt("blocks." + key + ".z");
            
            Location location = new Location(world, x, y, z);
            blockLocations.add(location);
        }
        
        plugin.getLogger().info("Loaded " + blockLocations.size() + " cavern block location(s) from blocks.yml");
    }
    
    public void saveBlocks() {
        // Save all current block locations
        for (Location location : blockLocations) {
            saveBlockLocation(location);
        }
        saveDataFile();
    }
    
    private void saveDataFile() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save blocks.yml: " + e.getMessage());
        }
    }
    
    private String locationToString(Location location) {
        return location.getWorld().getName() + "_" + 
               location.getBlockX() + "_" + 
               location.getBlockY() + "_" + 
               location.getBlockZ();
    }
}



