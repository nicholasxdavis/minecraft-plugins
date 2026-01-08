package com.playpandora.moreweather.models;

public enum Season {
    SPRING,
    SUMMER,
    FALL,
    WINTER;
    
    public Season next() {
        switch (this) {
            case SPRING: return SUMMER;
            case SUMMER: return FALL;
            case FALL: return WINTER;
            case WINTER: return SPRING;
            default: return SPRING;
        }
    }
    
    public double getTemperatureModifier() {
        switch (this) {
            case SPRING: return 0.0; // Neutral
            case SUMMER: return 10.0; // Hotter
            case FALL: return -5.0; // Cooler
            case WINTER: return -15.0; // Cold
            default: return 0.0;
        }
    }
}








