package com.playpandora.moreweather.managers;

import com.playpandora.moreweather.MoreWeather;
import com.playpandora.moreweather.models.Season;
import com.playpandora.moreweather.models.TemperatureLevel;
import com.playpandora.moreweather.models.WeatherType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class TemperatureManager {
    
    private final MoreWeather plugin;
    private final Map<Player, Double> playerTemperatures;
    
    // Base temperatures for biomes
    private static final Map<Biome, Double> BIOME_TEMPERATURES = new HashMap<>();
    
    static {
        // Hot biomes
        BIOME_TEMPERATURES.put(Biome.DESERT, 40.0);
        BIOME_TEMPERATURES.put(Biome.BADLANDS, 35.0);
        BIOME_TEMPERATURES.put(Biome.SAVANNA, 30.0);
        
        // Cold biomes
        BIOME_TEMPERATURES.put(Biome.ICE_SPIKES, -10.0);
        BIOME_TEMPERATURES.put(Biome.FROZEN_RIVER, -5.0);
        BIOME_TEMPERATURES.put(Biome.FROZEN_OCEAN, -8.0);
        BIOME_TEMPERATURES.put(Biome.SNOWY_TAIGA, -5.0);
        BIOME_TEMPERATURES.put(Biome.SNOWY_PLAINS, -3.0);
        BIOME_TEMPERATURES.put(Biome.SNOWY_BEACH, -2.0);
        
        // Normal biomes default to 20.0
    }
    
    public TemperatureManager(MoreWeather plugin) {
        this.plugin = plugin;
        this.playerTemperatures = new HashMap<>();
    }
    
    public void updateTemperatures() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            double temp = calculateTemperature(player);
            playerTemperatures.put(player, temp);
            
            // Apply temperature effects
            TemperatureLevel level = TemperatureLevel.fromTemperature(temp);
            applyTemperatureEffects(player, level, temp);
        }
    }
    
    private double calculateTemperature(Player player) {
        Location loc = player.getLocation();
        Biome biome = loc.getBlock().getBiome();
        
        // Start with biome base temperature
        double temp = BIOME_TEMPERATURES.getOrDefault(biome, 20.0);
        
        // Add season modifier
        if (plugin.getConfig().getBoolean("seasons.enabled", true)) {
            Season season = plugin.getSeasonManager().getCurrentSeason();
            temp += season.getTemperatureModifier();
        }
        
        // Add weather modifier
        WeatherType weather = plugin.getWeatherManager().getCurrentWeather(player.getWorld());
        temp += getWeatherTemperatureModifier(weather);
        
        // Underground is cooler
        if (loc.getY() < 63) {
            temp -= (63 - loc.getY()) * 0.1;
        }
        
        // Near water is cooler
        if (isNearWater(loc)) {
            temp -= 3.0;
        }
        
        // Daylight affects temperature
        long time = player.getWorld().getTime();
        if (time >= 0 && time < 6000) { // Night
            temp -= 5.0;
        } else if (time >= 6000 && time < 12000) { // Day
            temp += 3.0;
        }
        
        return temp;
    }
    
    private double getWeatherTemperatureModifier(WeatherType weather) {
        switch (weather) {
            case HEATWAVE:
                return 15.0;
            case BLIZZARD:
            case SNOWFALL:
                return -10.0;
            case HEAVY_RAIN:
            case STORM:
            case THUNDER:
                return -3.0;
            case SPRING_BLOOM:
                return 5.0;
            default:
                return 0.0;
        }
    }
    
    private boolean isNearWater(Location loc) {
        int radius = 3;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (loc.getWorld().getBlockAt(loc.getBlockX() + x, loc.getBlockY(), loc.getBlockZ() + z).isLiquid()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private void applyTemperatureEffects(Player player, TemperatureLevel level, double temp) {
        // Effects are handled by PlayerEffectManager
        // This just calculates and stores the temperature
    }
    
    public double getTemperature(Player player) {
        return playerTemperatures.getOrDefault(player, 20.0);
    }
    
    public TemperatureLevel getTemperatureLevel(Player player) {
        return TemperatureLevel.fromTemperature(getTemperature(player));
    }
    
    public void removePlayer(Player player) {
        playerTemperatures.remove(player);
    }
}








