package com.playpandora.hook.integrations;

import com.playpandora.hook.Hook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class MoreWeatherIntegration {
    
    private final Hook plugin;
    private Object moreWeather;
    
    public MoreWeatherIntegration(Hook plugin) {
        this.plugin = plugin;
    }
    
    public void hook() {
        org.bukkit.plugin.Plugin moreWeatherPlugin = Bukkit.getPluginManager().getPlugin("MoreWeather");
        
        if (moreWeatherPlugin == null) {
            return;
        }
        
        moreWeather = moreWeatherPlugin;
        
        // Monitor weather changes using reflection
        new BukkitRunnable() {
            private final java.util.Map<String, String> lastWeatherPerWorld = new java.util.HashMap<>();
            private final java.util.Map<String, Long> lastAlertTime = new java.util.HashMap<>();
            private static final long ALERT_COOLDOWN = 30000L; // 30 seconds cooldown between alerts
            
            @Override
            public void run() {
                if (moreWeather == null || !((org.bukkit.plugin.Plugin) moreWeather).isEnabled()) {
                    cancel();
                    return;
                }
                
                try {
                    // Use reflection to get weather manager
                    java.lang.reflect.Method getWeatherManagerMethod = moreWeather.getClass().getMethod("getWeatherManager");
                    Object weatherManager = getWeatherManagerMethod.invoke(moreWeather);
                    
                    // Check weather for each world
                    for (org.bukkit.World world : Bukkit.getWorlds()) {
                        if (world.getPlayers().isEmpty()) {
                            continue; // Skip worlds with no players
                        }
                        
                        java.lang.reflect.Method getCurrentWeatherMethod = weatherManager.getClass().getMethod("getCurrentWeather", org.bukkit.World.class);
                        Object currentWeather = getCurrentWeatherMethod.invoke(weatherManager, world);
                        
                        // Get weather type name using toString() or name() method
                        String weatherName;
                        try {
                            java.lang.reflect.Method nameMethod = currentWeather.getClass().getMethod("name");
                            weatherName = (String) nameMethod.invoke(currentWeather);
                        } catch (Exception e) {
                            weatherName = currentWeather.toString();
                        }
                        
                        String worldName = world.getName();
                        String lastWeather = lastWeatherPerWorld.getOrDefault(worldName, "");
                        
                        // If weather changed and it's significant, send alert (with cooldown)
                        if (!lastWeather.equals(weatherName) && !weatherName.equals("CLEAR")) {
                            long currentTime = System.currentTimeMillis();
                            Long lastAlert = lastAlertTime.get(worldName);
                            
                            // Check cooldown
                            if (lastAlert == null || (currentTime - lastAlert) >= ALERT_COOLDOWN) {
                                // Send to all players in that world
                                for (Player player : world.getPlayers()) {
                                    if (plugin.getConfig().getBoolean("integrations.moreweather.weather-alert", true)) {
                                        plugin.getAPI().sendWeatherAlert(player, formatWeatherType(weatherName));
                                    }
                                }
                                
                                lastAlertTime.put(worldName, currentTime);
                            }
                            
                            lastWeatherPerWorld.put(worldName, weatherName);
                        } else if (lastWeather.isEmpty()) {
                            // Initialize tracking
                            lastWeatherPerWorld.put(worldName, weatherName);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error checking weather: " + e.getMessage());
                }
            }
        }.runTaskTimer(plugin, 0L, 200L); // Check every 10 seconds (reduced frequency)
        
        // Hook into season changes using reflection
        new BukkitRunnable() {
            private String lastSeason = "";
            
            @Override
            public void run() {
                if (moreWeather == null || !((org.bukkit.plugin.Plugin) moreWeather).isEnabled()) {
                    cancel();
                    return;
                }
                
                try {
                    java.lang.reflect.Method getSeasonManagerMethod = moreWeather.getClass().getMethod("getSeasonManager");
                    Object seasonManager = getSeasonManagerMethod.invoke(moreWeather);
                    
                    java.lang.reflect.Method getCurrentSeasonMethod = seasonManager.getClass().getMethod("getCurrentSeason");
                    Object currentSeason = getCurrentSeasonMethod.invoke(seasonManager);
                    
                    String seasonName = currentSeason.getClass().getSimpleName();
                    
                    if (!lastSeason.equals(seasonName) && !lastSeason.isEmpty()) {
                        if (plugin.getConfig().getBoolean("integrations.moreweather.season-change-notification", true)) {
                            plugin.getAPI().sendSeasonChange(seasonName);
                        }
                        lastSeason = seasonName;
                    } else if (lastSeason.isEmpty()) {
                        lastSeason = seasonName;
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error checking season: " + e.getMessage());
                }
            }
        }.runTaskTimer(plugin, 0L, 600L); // Check every 30 seconds
        
        plugin.getLogger().info("MoreWeather integration enabled!");
    }
    
    private String formatWeatherType(String weatherType) {
        // Convert HEAVY_RAIN to "Heavy Rain", etc.
        String[] parts = weatherType.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(part.substring(0, 1).toUpperCase())
                  .append(part.substring(1).toLowerCase());
        }
        return result.toString();
    }
}

