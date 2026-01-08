package com.playpandora.morenether.utils;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class PerformanceOptimizer {
    private final JavaPlugin plugin;
    private final Map<Location, CachedData> cache = new HashMap<>();
    private final int cacheDuration;
    
    public PerformanceOptimizer(JavaPlugin plugin) {
        this.plugin = plugin;
        this.cacheDuration = plugin.getConfig().getInt("features.performance.cache-duration", 300);
    }
    
    public boolean isNetherCached(Location loc) {
        Location key = loc.getBlock().getLocation();
        CachedData data = cache.get(key);
        
        if (data != null && !data.isExpired()) {
            return data.isNether;
        }
        
        boolean isNether = DimensionChecker.isNether(loc);
        cache.put(key, new CachedData(isNether, System.currentTimeMillis()));
        
        cleanupCache();
        
        return isNether;
    }
    
    private void cleanupCache() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }
    
    private class CachedData {
        final boolean isNether;
        final long timestamp;
        
        CachedData(boolean isNether, long timestamp) {
            this.isNether = isNether;
            this.timestamp = timestamp;
        }
        
        boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }
        
        boolean isExpired(long now) {
            return (now - timestamp) > (cacheDuration * 50);
        }
    }
}

