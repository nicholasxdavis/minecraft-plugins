package com.playpandora.moreweather.managers;

import com.playpandora.moreweather.MoreWeather;
import com.playpandora.moreweather.models.Season;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class SeasonManager {
    
    private final MoreWeather plugin;
    private Season currentSeason;
    private long seasonStartTime;
    
    public SeasonManager(MoreWeather plugin) {
        this.plugin = plugin;
        loadSeason();
    }
    
    private void loadSeason() {
        FileConfiguration config = plugin.getConfig();
        
        if (!config.getBoolean("seasons.enabled", true)) {
            // If seasons disabled, default to current season or none
            String lockedSeason = config.getString("seasons.locked-season", "");
            if (!lockedSeason.isEmpty()) {
                try {
                    currentSeason = Season.valueOf(lockedSeason.toUpperCase());
                } catch (IllegalArgumentException e) {
                    currentSeason = Season.SPRING;
                }
            } else {
                currentSeason = Season.SPRING;
            }
            return;
        }
        
        // Load from config or calculate based on time
        String savedSeason = config.getString("seasons.current", "");
        if (!savedSeason.isEmpty()) {
            try {
                currentSeason = Season.valueOf(savedSeason.toUpperCase());
            } catch (IllegalArgumentException e) {
                currentSeason = calculateSeasonFromTime();
            }
        } else {
            currentSeason = calculateSeasonFromTime();
        }
        
        seasonStartTime = config.getLong("seasons.start-time", System.currentTimeMillis());
        
        // Check if season should have changed
        checkSeasonChange();
    }
    
    private Season calculateSeasonFromTime() {
        int month = LocalDateTime.now().getMonthValue();
        if (month >= 3 && month <= 5) return Season.SPRING;
        if (month >= 6 && month <= 8) return Season.SUMMER;
        if (month >= 9 && month <= 11) return Season.FALL;
        return Season.WINTER;
    }
    
    public void checkSeasonChange() {
        if (!plugin.getConfig().getBoolean("seasons.enabled", true)) {
            return;
        }
        
        long seasonDuration = plugin.getConfig().getLong("seasons.duration-ticks", 7200000L); // 10 hours default
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - seasonStartTime >= seasonDuration) {
            advanceSeason();
        }
    }
    
    public void advanceSeason() {
        Season oldSeason = currentSeason;
        currentSeason = currentSeason.next();
        seasonStartTime = System.currentTimeMillis();
        
        // Save to config
        plugin.getConfig().set("seasons.current", currentSeason.name());
        plugin.getConfig().set("seasons.start-time", seasonStartTime);
        plugin.saveConfig();
        
        // Broadcast season change via Hook
        try {
            org.bukkit.plugin.Plugin hookPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("Hook");
            if (hookPlugin != null && hookPlugin.isEnabled()) {
                Object hookInstance = hookPlugin.getClass().getMethod("getInstance").invoke(null);
                Object hookAPI = hookInstance.getClass().getMethod("getAPI").invoke(hookInstance);
                hookAPI.getClass().getMethod("sendSeasonChange", String.class)
                    .invoke(hookAPI, currentSeason.name());
            }
        } catch (Exception e) {
            // Fallback to broadcast message if Hook not available (using proper color scheme)
            String prefix = plugin.getConfig().getString("messages.prefix", "&eMoreWeather");
            String message = plugin.getConfig().getString("messages.season-change", 
                "{prefix} &7Season changed to &6{season}")
                .replace("{prefix}", prefix)
                .replace("{season}", currentSeason.name());
            plugin.getServer().broadcastMessage(
                org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
        }
        
        plugin.getLogger().info("Season changed from " + oldSeason + " to " + currentSeason);
    }
    
    public Season getCurrentSeason() {
        return currentSeason;
    }
    
    public void setSeason(Season season) {
        this.currentSeason = season;
        this.seasonStartTime = System.currentTimeMillis();
        plugin.getConfig().set("seasons.current", season.name());
        plugin.getConfig().set("seasons.start-time", seasonStartTime);
        plugin.saveConfig();
    }
    
    public long getSeasonDuration() {
        return plugin.getConfig().getLong("seasons.duration-ticks", 7200000L);
    }
}







