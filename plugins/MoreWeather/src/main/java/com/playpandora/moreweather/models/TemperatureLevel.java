package com.playpandora.moreweather.models;

public enum TemperatureLevel {
    FREEZING,    // Below -10
    COLD,        // -10 to 0
    COOL,        // 0 to 15
    NORMAL,      // 15 to 25
    WARM,        // 25 to 35
    HOT,         // 35 to 45
    BURNING;     // Above 45
    
    public static TemperatureLevel fromTemperature(double temp) {
        if (temp < -10) return FREEZING;
        if (temp < 0) return COLD;
        if (temp < 15) return COOL;
        if (temp < 25) return NORMAL;
        if (temp < 35) return WARM;
        if (temp < 45) return HOT;
        return BURNING;
    }
    
    public boolean isCold() {
        return this == FREEZING || this == COLD || this == COOL;
    }
    
    public boolean isHot() {
        return this == WARM || this == HOT || this == BURNING;
    }
}








