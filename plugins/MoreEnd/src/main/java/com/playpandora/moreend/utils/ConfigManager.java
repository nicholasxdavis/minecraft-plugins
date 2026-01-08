package com.playpandora.moreend.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reloadConfig();
    }
    
    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
    
    public boolean isFeatureEnabled(String feature) {
        return config.getBoolean("features." + feature + ".enabled", true);
    }
    
    public double getMultiplier(String feature, String type) {
        return config.getDouble("features." + feature + "." + type, 1.0);
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
}

