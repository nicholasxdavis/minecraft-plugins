package com.playpandora.moreweather.managers;

import com.playpandora.moreweather.MoreWeather;
import com.playpandora.moreweather.models.Season;
import com.playpandora.moreweather.models.WeatherType;
import com.playpandora.moreweather.weather.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.HashSet;
import java.util.Set;

public class WeatherManager {
    
    private final MoreWeather plugin;
    private final Map<World, WeatherType> currentWeather;
    private final Map<World, WeatherEffect> activeEffects;
    private final List<WeatherType> enabledWeatherTypes;
    private final Set<World> worldsBeingModified = new HashSet<>(); // Track worlds we're modifying
    
    public WeatherManager(MoreWeather plugin) {
        this.plugin = plugin;
        this.currentWeather = new HashMap<>();
        this.activeEffects = new HashMap<>();
        this.enabledWeatherTypes = new ArrayList<>();
        
        loadEnabledWeatherTypes();
    }
    
    private void loadEnabledWeatherTypes() {
        FileConfiguration config = plugin.getConfig();
        enabledWeatherTypes.clear();
        
        if (config.getBoolean("weather.heavy-rain.enabled", true)) {
            enabledWeatherTypes.add(WeatherType.HEAVY_RAIN);
        }
        if (config.getBoolean("weather.storm.enabled", true)) {
            enabledWeatherTypes.add(WeatherType.STORM);
            enabledWeatherTypes.add(WeatherType.THUNDER);
        }
        if (config.getBoolean("weather.snow.enabled", true)) {
            enabledWeatherTypes.add(WeatherType.SNOWFALL);
            enabledWeatherTypes.add(WeatherType.BLIZZARD);
        }
        if (config.getBoolean("weather.fog.enabled", true)) {
            enabledWeatherTypes.add(WeatherType.FOG);
        }
        if (config.getBoolean("weather.heatwave.enabled", true)) {
            enabledWeatherTypes.add(WeatherType.HEATWAVE);
        }
        if (config.getBoolean("weather.spring-bloom.enabled", true)) {
            enabledWeatherTypes.add(WeatherType.SPRING_BLOOM);
        }
    }
    
    public void setWeather(World world, WeatherType type) {
        if (!enabledWeatherTypes.contains(type) && type != WeatherType.CLEAR) {
            return;
        }
        
        // Prevent recursive calls
        if (worldsBeingModified.contains(world)) {
            return;
        }
        
        worldsBeingModified.add(world);
        try {
            // Stop current weather effect
            stopWeather(world);
            
            // Set new weather
            currentWeather.put(world, type);
            
            // Apply weather effect
            WeatherEffect effect = createWeatherEffect(world, type);
            if (effect != null) {
                activeEffects.put(world, effect);
                effect.start();
            }
            
            // Apply visual weather to world
            applyVisualWeather(world, type);
        } finally {
            worldsBeingModified.remove(world);
        }
    }
    
    private WeatherEffect createWeatherEffect(World world, WeatherType type) {
        switch (type) {
            case HEAVY_RAIN:
                return new HeavyRainEffect(plugin, world);
            case STORM:
            case THUNDER:
                return new StormEffect(plugin, world);
            case SNOWFALL:
                return new SnowfallEffect(plugin, world);
            case BLIZZARD:
                return new BlizzardEffect(plugin, world);
            case FOG:
                return new FogEffect(plugin, world);
            case HEATWAVE:
                return new HeatwaveEffect(plugin, world);
            case SPRING_BLOOM:
                return new SpringBloomEffect(plugin, world);
            default:
                return null;
        }
    }
    
    private void applyVisualWeather(World world, WeatherType type) {
        if (world == null) {
            return;
        }
        
        // We're already in worldsBeingModified, so this is safe
        // Apply weather regardless of chunk loading - it will apply when chunks load
        switch (type) {
            case HEAVY_RAIN:
            case STORM:
            case THUNDER:
                world.setStorm(true);
                world.setThundering(type == WeatherType.THUNDER || type == WeatherType.STORM);
                break;
            case SNOWFALL:
            case BLIZZARD:
                world.setStorm(false);
                world.setThundering(false);
                // Snow is handled by biome naturally
                break;
            case CLEAR:
                world.setStorm(false);
                world.setThundering(false);
                break;
            default:
                // Custom weather types don't change vanilla weather
                break;
        }
    }
    
    public void stopWeather(World world) {
        WeatherEffect effect = activeEffects.remove(world);
        if (effect != null) {
            effect.stop();
        }
        currentWeather.remove(world);
        
        // Only modify world weather if we're not already modifying it
        // This prevents triggering WeatherChangeEvent during our own weather changes
        if (!worldsBeingModified.contains(world)) {
            worldsBeingModified.add(world);
            try {
                if (world != null && world.isChunkLoaded(0, 0)) {
                    world.setStorm(false);
                    world.setThundering(false);
                }
            } finally {
                worldsBeingModified.remove(world);
            }
        }
    }
    
    public void stopAllWeather() {
        for (World world : new ArrayList<>(currentWeather.keySet())) {
            stopWeather(world);
        }
    }
    
    public void cycleWeather() {
        for (World world : Bukkit.getWorlds()) {
            // Initialize weather even if no players are online (for when they join)
            WeatherType oldWeather = currentWeather.getOrDefault(world, WeatherType.CLEAR);
            WeatherType newWeather = selectRandomWeather(world);
            setWeather(world, newWeather);
            
            // Notify players of weather change
            if (!world.getPlayers().isEmpty() && newWeather != oldWeather && newWeather != WeatherType.CLEAR) {
                plugin.getLogger().info("Weather changed to " + newWeather.name() + " in world " + world.getName());
                notifyWeatherChange(world, newWeather);
            }
        }
    }
    
    private void notifyWeatherChange(World world, WeatherType weather) {
        try {
            org.bukkit.plugin.Plugin hookPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("Hook");
            if (hookPlugin != null && hookPlugin.isEnabled()) {
                Object hookInstance = hookPlugin.getClass().getMethod("getInstance").invoke(null);
                Object hookAPI = hookInstance.getClass().getMethod("getAPI").invoke(hookInstance);
                
                String weatherName = formatWeatherName(weather);
                
                // Send notification to all players in the world
                for (org.bukkit.entity.Player player : world.getPlayers()) {
                    hookAPI.getClass().getMethod("sendWeatherAlert", org.bukkit.entity.Player.class, String.class)
                        .invoke(hookAPI, player, weatherName);
                }
            }
        } catch (Exception e) {
            // Silently fail if Hook is not available
        }
    }
    
    private String formatWeatherName(WeatherType weather) {
        switch (weather) {
            case HEAVY_RAIN: return "Heavy Rain";
            case STORM: return "Storm";
            case THUNDER: return "Thunderstorm";
            case SNOWFALL: return "Snowfall";
            case BLIZZARD: return "Blizzard";
            case FOG: return "Fog";
            case HEATWAVE: return "Heatwave";
            case SPRING_BLOOM: return "Spring Bloom";
            default: return "Clear";
        }
    }
    
    private WeatherType selectRandomWeather(World world) {
        if (enabledWeatherTypes.isEmpty()) {
            return WeatherType.CLEAR;
        }
        
        // Check season for weather pool
        if (plugin.getConfig().getBoolean("seasons.enabled", true)) {
            Season season = plugin.getSeasonManager().getCurrentSeason();
            List<WeatherType> seasonalWeather = getWeatherForSeason(season);
            if (!seasonalWeather.isEmpty()) {
                return seasonalWeather.get(new Random().nextInt(seasonalWeather.size()));
            }
        }
        
        // Fallback to random enabled weather
        return enabledWeatherTypes.get(new Random().nextInt(enabledWeatherTypes.size()));
    }
    
    private List<WeatherType> getWeatherForSeason(com.playpandora.moreweather.models.Season season) {
        List<WeatherType> weatherList = new ArrayList<>();
        
        switch (season) {
            case SPRING:
                if (enabledWeatherTypes.contains(WeatherType.HEAVY_RAIN)) weatherList.add(WeatherType.HEAVY_RAIN);
                if (enabledWeatherTypes.contains(WeatherType.SPRING_BLOOM)) weatherList.add(WeatherType.SPRING_BLOOM);
                if (enabledWeatherTypes.contains(WeatherType.STORM)) weatherList.add(WeatherType.STORM);
                break;
            case SUMMER:
                if (enabledWeatherTypes.contains(WeatherType.HEATWAVE)) weatherList.add(WeatherType.HEATWAVE);
                if (enabledWeatherTypes.contains(WeatherType.STORM)) weatherList.add(WeatherType.STORM);
                if (enabledWeatherTypes.contains(WeatherType.THUNDER)) weatherList.add(WeatherType.THUNDER);
                break;
            case FALL:
                if (enabledWeatherTypes.contains(WeatherType.HEAVY_RAIN)) weatherList.add(WeatherType.HEAVY_RAIN);
                if (enabledWeatherTypes.contains(WeatherType.FOG)) weatherList.add(WeatherType.FOG);
                if (enabledWeatherTypes.contains(WeatherType.STORM)) weatherList.add(WeatherType.STORM);
                break;
            case WINTER:
                if (enabledWeatherTypes.contains(WeatherType.SNOWFALL)) weatherList.add(WeatherType.SNOWFALL);
                if (enabledWeatherTypes.contains(WeatherType.BLIZZARD)) weatherList.add(WeatherType.BLIZZARD);
                if (enabledWeatherTypes.contains(WeatherType.FOG)) weatherList.add(WeatherType.FOG);
                break;
        }
        
        // Always include CLEAR as possibility
        weatherList.add(WeatherType.CLEAR);
        return weatherList;
    }
    
    public void updateWeatherEffects() {
        for (WeatherEffect effect : activeEffects.values()) {
            effect.update();
        }
    }
    
    public WeatherType getCurrentWeather(World world) {
        return currentWeather.getOrDefault(world, WeatherType.CLEAR);
    }
    
    public boolean isWeatherActive(World world, WeatherType type) {
        return currentWeather.getOrDefault(world, WeatherType.CLEAR) == type;
    }
    
    public List<WeatherType> getEnabledWeatherTypes() {
        return new ArrayList<>(enabledWeatherTypes);
    }
    
    /**
     * Check if we're currently modifying weather for this world
     * Used by WeatherListener to determine if a weather change is ours or vanilla's
     */
    public boolean isModifyingWorld(World world) {
        return worldsBeingModified.contains(world);
    }
}

