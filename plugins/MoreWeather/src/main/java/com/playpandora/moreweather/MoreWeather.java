package com.playpandora.moreweather;

import com.playpandora.moreweather.commands.WeatherCommand;
import com.playpandora.moreweather.listeners.*;
import com.playpandora.moreweather.managers.*;
import com.playpandora.moreweather.integrations.PluginIntegrations;
import org.bukkit.plugin.java.JavaPlugin;

public class MoreWeather extends JavaPlugin {
    
    private static MoreWeather instance;
    private WeatherManager weatherManager;
    private SeasonManager seasonManager;
    private TemperatureManager temperatureManager;
    private MobBehaviorManager mobBehaviorManager;
    private PlayerEffectManager playerEffectManager;
    private PluginIntegrations integrations;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Save default config
            saveDefaultConfig();
            
            // Initialize integrations first
            integrations = new PluginIntegrations(this);
            
            // Initialize managers
            weatherManager = new WeatherManager(this);
            seasonManager = new SeasonManager(this);
            temperatureManager = new TemperatureManager(this);
            mobBehaviorManager = new MobBehaviorManager(this);
            playerEffectManager = new PlayerEffectManager(this);
            
            // Register commands with null checks
            if (getCommand("weather") != null) {
                getCommand("weather").setExecutor(new WeatherCommand(this));
            } else {
                getLogger().warning("Command 'weather' not found in plugin.yml!");
            }
            if (getCommand("season") != null) {
                getCommand("season").setExecutor(new com.playpandora.moreweather.commands.SeasonCommand(this));
            } else {
                getLogger().warning("Command 'season' not found in plugin.yml!");
            }
            
            // Register listeners
            getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
            getServer().getPluginManager().registerEvents(new BlockListener(this), this);
            getServer().getPluginManager().registerEvents(new EntityListener(this), this);
            getServer().getPluginManager().registerEvents(new WeatherListener(this), this);
            getServer().getPluginManager().registerEvents(new CropListener(this), this);
            
            // Start scheduled tasks
            startScheduledTasks();
            
            // Initialize weather for all worlds after a short delay
            getServer().getScheduler().runTaskLater(this, () -> {
                if (weatherManager != null) {
                    weatherManager.cycleWeather(); // Initialize weather on startup
                    getLogger().info("Initialized weather for all worlds!");
                }
            }, 100L); // 5 second delay to ensure everything is loaded
            
            getLogger().info("MoreWeather v" + getDescription().getVersion() + " has been enabled! Weather now matters!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable MoreWeather! " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            if (weatherManager != null) {
                weatherManager.stopAllWeather();
            }
            if (playerEffectManager != null) {
                playerEffectManager.clearAllEffects();
            }
            
            // Cancel all tasks
            getServer().getScheduler().cancelTasks(this);
            
            getLogger().info("MoreWeather has been disabled!");
        } catch (Exception e) {
            getLogger().severe("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void startScheduledTasks() {
        // Update weather effects every second
        getServer().getScheduler().runTaskTimer(this, () -> {
            try {
                if (weatherManager != null) {
                    weatherManager.updateWeatherEffects();
                }
                if (playerEffectManager != null) {
                    playerEffectManager.updatePlayerEffects();
                }
            } catch (Exception e) {
                getLogger().warning("Error updating weather effects: " + e.getMessage());
            }
        }, 0L, 20L); // 1 second
        
        // Update temperature every 5 seconds
        getServer().getScheduler().runTaskTimer(this, () -> {
            try {
                if (temperatureManager != null) {
                    temperatureManager.updateTemperatures();
                }
            } catch (Exception e) {
                getLogger().warning("Error updating temperatures: " + e.getMessage());
            }
        }, 0L, 100L); // 5 seconds
        
        // Cycle weather naturally every 10-15 minutes
        long weatherInterval = getConfig().getLong("weather.cycle-interval", 12000L);
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                if (weatherManager != null) {
                    weatherManager.cycleWeather();
                }
            } catch (Exception e) {
                getLogger().warning("Error cycling weather: " + e.getMessage());
            }
        }, weatherInterval, weatherInterval + (long)(Math.random() * 6000)); // 10-15 minutes
        
        // Update season if enabled
        if (getConfig().getBoolean("seasons.enabled", true)) {
            long seasonCheckInterval = getConfig().getLong("seasons.check-interval", 36000L);
            getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                try {
                    if (seasonManager != null) {
                        seasonManager.checkSeasonChange();
                    }
                } catch (Exception e) {
                    getLogger().warning("Error checking season change: " + e.getMessage());
                }
            }, 0L, seasonCheckInterval); // Check every configured interval
        }
    }
    
    public static MoreWeather getInstance() {
        return instance;
    }
    
    public WeatherManager getWeatherManager() {
        return weatherManager;
    }
    
    public SeasonManager getSeasonManager() {
        return seasonManager;
    }
    
    public TemperatureManager getTemperatureManager() {
        return temperatureManager;
    }
    
    public MobBehaviorManager getMobBehaviorManager() {
        return mobBehaviorManager;
    }
    
    public PlayerEffectManager getPlayerEffectManager() {
        return playerEffectManager;
    }
    
    public PluginIntegrations getIntegrations() {
        return integrations;
    }
}

