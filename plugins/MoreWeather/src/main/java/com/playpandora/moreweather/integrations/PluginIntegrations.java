package com.playpandora.moreweather.integrations;

import com.playpandora.moreweather.MoreWeather;
import org.bukkit.Bukkit;

public class PluginIntegrations {
    
    private final MoreWeather plugin;
    private BaseSystemIntegration baseSystem;
    private HomeBuffsIntegration homeBuffs;
    private FarmShopIntegration farmShop;
    private LevelPluginIntegration levelPlugin;
    
    public PluginIntegrations(MoreWeather plugin) {
        this.plugin = plugin;
        initializeIntegrations();
    }
    
    private void initializeIntegrations() {
        // BaseSystem integration
        if (Bukkit.getPluginManager().getPlugin("BaseSystem") != null) {
            baseSystem = new BaseSystemIntegration(plugin);
            plugin.getLogger().info("BaseSystem integration enabled!");
        }
        
        // HomeBuffs integration
        if (Bukkit.getPluginManager().getPlugin("HomeBuffs") != null) {
            homeBuffs = new HomeBuffsIntegration(plugin);
            plugin.getLogger().info("HomeBuffs integration enabled!");
        }
        
        // FarmShop integration
        if (Bukkit.getPluginManager().getPlugin("FarmShop") != null) {
            farmShop = new FarmShopIntegration(plugin);
            plugin.getLogger().info("FarmShop integration enabled!");
        }
        
        // LevelPlugin integration
        if (Bukkit.getPluginManager().getPlugin("LevelPlugin") != null) {
            levelPlugin = new LevelPluginIntegration(plugin);
            plugin.getLogger().info("LevelPlugin integration enabled!");
        }
    }
    
    public boolean isBaseSystemEnabled() {
        return baseSystem != null;
    }
    
    public boolean isHomeBuffsEnabled() {
        return homeBuffs != null;
    }
    
    public boolean isFarmShopEnabled() {
        return farmShop != null;
    }
    
    public boolean isLevelPluginEnabled() {
        return levelPlugin != null;
    }
    
    public BaseSystemIntegration getBaseSystem() {
        return baseSystem;
    }
    
    public HomeBuffsIntegration getHomeBuffs() {
        return homeBuffs;
    }
    
    public FarmShopIntegration getFarmShop() {
        return farmShop;
    }
    
    public LevelPluginIntegration getLevelPlugin() {
        return levelPlugin;
    }
}








