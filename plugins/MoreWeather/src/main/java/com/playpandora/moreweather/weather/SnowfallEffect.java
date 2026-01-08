package com.playpandora.moreweather.weather;

import com.playpandora.moreweather.MoreWeather;
import org.bukkit.World;

public class SnowfallEffect extends WeatherEffect {
    
    public SnowfallEffect(MoreWeather plugin, World world) {
        super(plugin, world);
    }
    
    @Override
    public void start() {
        active = true;
        plugin.getLogger().info("Snowfall started in " + world.getName());
    }
    
    @Override
    public void update() {
        // Light snowfall - mostly visual, lighter effects than blizzard
        if (!active) return;
    }
    
    @Override
    public void stop() {
        active = false;
        plugin.getLogger().info("Snowfall stopped in " + world.getName());
    }
}








