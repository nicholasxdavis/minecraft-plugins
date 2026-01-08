package com.playpandora.moreweather.integrations;

import com.playpandora.moreweather.MoreWeather;

public class FarmShopIntegration {
    
    private final MoreWeather plugin;
    
    public FarmShopIntegration(MoreWeather plugin) {
        this.plugin = plugin;
    }
    
    public double getCropGrowthMultiplier() {
        // Return multiplier based on current weather
        // Heavy rain: 1.5x, Spring bloom: 2x, etc.
        return 1.0;
    }
}








