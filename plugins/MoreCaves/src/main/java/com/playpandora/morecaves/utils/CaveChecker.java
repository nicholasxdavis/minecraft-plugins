package com.playpandora.morecaves.utils;

import org.bukkit.Location;

public class CaveChecker {
    
    /**
     * Check if location is in a cave (underground and low light)
     */
    public static boolean isInCave(Location loc) {
        return DimensionChecker.isOverworld(loc) && 
               loc.getY() < 60 && 
               LightLevelChecker.isLowLight(loc);
    }
    
    /**
     * Check if location is deep underground
     */
    public static boolean isDeepCave(Location loc) {
        return isInCave(loc) && loc.getY() < 0;
    }
}

