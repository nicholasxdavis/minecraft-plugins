package com.playpandora.moreweather.listeners;

import com.playpandora.moreweather.MoreWeather;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class CropListener implements Listener {
    
    private final MoreWeather plugin;
    
    public CropListener(MoreWeather plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onCropBreak(BlockBreakEvent event) {
        // Integration with FarmShop - check if weather affects crop drops
        if (plugin.getIntegrations().isFarmShopEnabled()) {
            Material type = event.getBlock().getType();
            if (type == Material.WHEAT || type == Material.CARROTS || 
                type == Material.POTATOES || type == Material.BEETROOTS) {
                
                com.playpandora.moreweather.models.WeatherType weather = 
                    plugin.getWeatherManager().getCurrentWeather(event.getBlock().getWorld());
                
                if (weather == com.playpandora.moreweather.models.WeatherType.SPRING_BLOOM) {
                    // Bonus drops during spring bloom
                    // This would integrate with FarmShop reward system
                }
            }
        }
    }
}








