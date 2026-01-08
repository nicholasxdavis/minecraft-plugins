package com.playpandora.totemlimit;

import org.bukkit.plugin.java.JavaPlugin;

public class TotemLimit extends JavaPlugin {
    
    private static TotemLimit instance;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new TotemListener(this), this);
        
        getLogger().info("TotemLimit v" + getDescription().getVersion() + " has been enabled!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("TotemLimit has been disabled!");
    }
    
    public static TotemLimit getInstance() {
        return instance;
    }
}

