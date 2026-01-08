package com.playpandora.morecaves.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class LightLevelChecker {
    
    public static int getLightLevel(Location loc) {
        return loc.getBlock().getLightLevel();
    }
    
    public static boolean isLowLight(Location loc) {
        return getLightLevel(loc) <= 5;
    }
    
    public static boolean isLowLight(Player player) {
        return isLowLight(player.getLocation());
    }
}

