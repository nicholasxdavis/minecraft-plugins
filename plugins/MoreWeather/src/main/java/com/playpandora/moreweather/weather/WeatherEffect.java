package com.playpandora.moreweather.weather;

import org.bukkit.World;

public abstract class WeatherEffect {
    
    protected final com.playpandora.moreweather.MoreWeather plugin;
    protected final World world;
    protected boolean active;
    
    public WeatherEffect(com.playpandora.moreweather.MoreWeather plugin, World world) {
        this.plugin = plugin;
        this.world = world;
        this.active = false;
    }
    
    public abstract void start();
    public abstract void update();
    public abstract void stop();
    
    public boolean isActive() {
        return active;
    }
    
    protected World getWorld() {
        return world;
    }
}








