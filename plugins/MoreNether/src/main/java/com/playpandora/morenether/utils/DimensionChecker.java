package com.playpandora.morenether.utils;

import org.bukkit.Location;
import org.bukkit.World;

public class DimensionChecker {
    
    public static boolean isNether(World world) {
        return world.getEnvironment() == World.Environment.NETHER;
    }
    
    public static boolean isEnd(World world) {
        return world.getEnvironment() == World.Environment.THE_END;
    }
    
    public static boolean isOverworld(World world) {
        return world.getEnvironment() == World.Environment.NORMAL;
    }
    
    public static boolean isNether(Location loc) {
        return isNether(loc.getWorld());
    }
    
    public static boolean isEnd(Location loc) {
        return isEnd(loc.getWorld());
    }
    
    public static boolean isOverworld(Location loc) {
        return isOverworld(loc.getWorld());
    }
}

