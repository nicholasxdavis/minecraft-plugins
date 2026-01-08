package com.playpandora.moreweather.models;

public enum WeatherType {
    CLEAR,
    HEAVY_RAIN,
    STORM,
    THUNDER,
    SNOWFALL,
    BLIZZARD,
    FOG,
    HEATWAVE,
    SPRING_BLOOM;
    
    public boolean isPrecipitation() {
        return this == HEAVY_RAIN || this == STORM || this == THUNDER || this == SNOWFALL || this == BLIZZARD;
    }
    
    public boolean isCold() {
        return this == SNOWFALL || this == BLIZZARD;
    }
    
    public boolean isHot() {
        return this == HEATWAVE;
    }
    
    public boolean reducesVisibility() {
        return this == HEAVY_RAIN || this == STORM || this == THUNDER || this == FOG || this == BLIZZARD;
    }
    
    public boolean affectsCrops() {
        return this == HEAVY_RAIN || this == SPRING_BLOOM || this == HEATWAVE;
    }
}








