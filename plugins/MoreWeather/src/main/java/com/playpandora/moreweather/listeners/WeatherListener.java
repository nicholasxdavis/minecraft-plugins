package com.playpandora.moreweather.listeners;

import com.playpandora.moreweather.MoreWeather;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;

public class WeatherListener implements Listener {
    
    private final MoreWeather plugin;
    
    public WeatherListener(MoreWeather plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        // Prevent vanilla weather from overriding our custom weather
        if (plugin.getConfig().getBoolean("weather.override-vanilla", true)) {
            // Check if we're currently modifying this world
            if (plugin.getWeatherManager() != null) {
                org.bukkit.World world = event.getWorld();
                
                // If we're modifying this world, allow the change (it's ours)
                // Otherwise, cancel it (it's vanilla)
                if (!plugin.getWeatherManager().isModifyingWorld(world)) {
                    event.setCancelled(true);
                }
            } else {
                // No weather manager - cancel all changes
                event.setCancelled(true);
            }
        }
    }
}

