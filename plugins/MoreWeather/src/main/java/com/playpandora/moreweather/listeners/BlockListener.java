package com.playpandora.moreweather.listeners;

import com.playpandora.moreweather.MoreWeather;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;

public class BlockListener implements Listener {
    
    private final MoreWeather plugin;
    
    public BlockListener(MoreWeather plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onBlockGrow(BlockGrowEvent event) {
        // Accelerate growth based on weather
        com.playpandora.moreweather.models.WeatherType weather = 
            plugin.getWeatherManager().getCurrentWeather(event.getBlock().getWorld());
        
        if (weather == com.playpandora.moreweather.models.WeatherType.HEAVY_RAIN ||
            weather == com.playpandora.moreweather.models.WeatherType.SPRING_BLOOM) {
            // Crops grow faster in rain/spring
            if (Math.random() < 0.3) { // 30% chance to double growth
                event.setCancelled(false);
            }
        }
    }
}








