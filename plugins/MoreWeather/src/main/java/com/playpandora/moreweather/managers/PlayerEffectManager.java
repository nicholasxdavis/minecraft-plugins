package com.playpandora.moreweather.managers;

import com.playpandora.moreweather.MoreWeather;
import com.playpandora.moreweather.models.TemperatureLevel;
import com.playpandora.moreweather.models.WeatherType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class PlayerEffectManager {
    
    private final MoreWeather plugin;
    private final Map<Player, Set<PotionEffectType>> activeWeatherEffects = new HashMap<>();
    
    public PlayerEffectManager(MoreWeather plugin) {
        this.plugin = plugin;
    }
    
    public void updatePlayerEffects() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            WeatherType weather = plugin.getWeatherManager().getCurrentWeather(player.getWorld());
            TemperatureLevel tempLevel = plugin.getTemperatureManager().getTemperatureLevel(player);
            
            // Clear old weather effects
            clearWeatherEffects(player);
            
            // Apply weather-based effects
            applyWeatherEffects(player, weather);
            
            // Apply temperature-based effects
            applyTemperatureEffects(player, tempLevel);
            
            // Apply season-based effects
            if (plugin.getConfig().getBoolean("seasons.enabled", true)) {
                applySeasonEffects(player);
            }
        }
    }
    
    private void applyWeatherEffects(Player player, WeatherType weather) {
        switch (weather) {
            case SPRING_BLOOM:
                // Faster mining in spring rain
                if (plugin.getConfig().getBoolean("weather.spring-bloom.mining-bonus", true)) {
                    addWeatherEffect(player, PotionEffectType.HASTE, 0);
                }
                break;
                
            case HEAVY_RAIN:
                // Already handled by HeavyRainEffect
                break;
                
            case HEATWAVE:
                // Fire resistance already handled by HeatwaveEffect
                break;
                
            case FOG:
                // Night vision reduction already handled by FogEffect
                break;
        }
    }
    
    private void applyTemperatureEffects(Player player, TemperatureLevel tempLevel) {
        double temperature = plugin.getTemperatureManager().getTemperature(player);
        
        if (tempLevel.isCold()) {
            // Check for cold protection
            if (!hasColdProtection(player)) {
                if (temperature < -10) {
                    // Freezing - slow movement and damage
                    addWeatherEffect(player, PotionEffectType.SLOWNESS, 1);
                    if (plugin.getConfig().getBoolean("temperature.freezing-damage", false)) {
                        player.damage(0.5);
                    }
                } else if (temperature < 0) {
                    // Cold - slight slowness
                    addWeatherEffect(player, PotionEffectType.SLOWNESS, 0);
                }
            }
        } else if (tempLevel.isHot()) {
            // Check for heat protection
            if (!hasHeatProtection(player)) {
                if (temperature > 45) {
                    // Burning - damage over time
                    addWeatherEffect(player, PotionEffectType.HUNGER, 1);
                    if (plugin.getConfig().getBoolean("temperature.heat-damage", false)) {
                        player.damage(0.5);
                    }
                } else if (temperature > 35) {
                    // Hot - increased exhaustion
                    player.setExhaustion(player.getExhaustion() + 0.1f);
                }
            }
        }
    }
    
    private void applySeasonEffects(Player player) {
        com.playpandora.moreweather.models.Season season = 
            plugin.getSeasonManager().getCurrentSeason();
        
        switch (season) {
            case SPRING:
                // Spring buffs
                break;
            case SUMMER:
                // Summer effects
                break;
            case FALL:
                // Fall effects
                break;
            case WINTER:
                // Winter effects
                break;
        }
    }
    
    private boolean hasColdProtection(Player player) {
        // Check for leather/wool armor
        if (player.getInventory().getHelmet() != null) {
            Material helmet = player.getInventory().getHelmet().getType();
            if (helmet == Material.LEATHER_HELMET || helmet.name().contains("WOOL")) {
                return true;
            }
        }
        
        // Check for campfire/torch nearby
        return isNearHeatSource(player.getLocation());
    }
    
    private boolean hasHeatProtection(Player player) {
        // Check for gold armor
        return hasGoldArmor(player);
        
        // Also check for shade/water nearby
    }
    
    private boolean hasGoldArmor(Player player) {
        if (player.getInventory().getHelmet() != null && 
            player.getInventory().getHelmet().getType() == Material.GOLDEN_HELMET) {
            return true;
        }
        if (player.getInventory().getChestplate() != null && 
            player.getInventory().getChestplate().getType() == Material.GOLDEN_CHESTPLATE) {
            return true;
        }
        return false;
    }
    
    private boolean isNearHeatSource(org.bukkit.Location loc) {
        int radius = 5;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    org.bukkit.block.Block block = loc.getWorld().getBlockAt(
                        loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);
                    if (block.getType() == Material.TORCH || block.getType() == Material.CAMPFIRE ||
                        block.getType() == Material.SOUL_CAMPFIRE) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private void addWeatherEffect(Player player, PotionEffectType effect, int amplifier) {
        player.addPotionEffect(new PotionEffect(effect, 40, amplifier, true, false));
        activeWeatherEffects.computeIfAbsent(player, k -> new HashSet<>()).add(effect);
    }
    
    private void clearWeatherEffects(Player player) {
        Set<PotionEffectType> effects = activeWeatherEffects.get(player);
        if (effects != null) {
            for (PotionEffectType effect : effects) {
                player.removePotionEffect(effect);
            }
            effects.clear();
        }
    }
    
    public void clearAllEffects() {
        for (Player player : new ArrayList<>(activeWeatherEffects.keySet())) {
            clearWeatherEffects(player);
        }
        activeWeatherEffects.clear();
    }
    
    public void removePlayer(Player player) {
        clearWeatherEffects(player);
        activeWeatherEffects.remove(player);
    }
}








