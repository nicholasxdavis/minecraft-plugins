package com.playpandora.hook;

import com.playpandora.hook.api.HookAPI;
import com.playpandora.hook.integrations.PluginIntegrations;
import com.playpandora.hook.listeners.PlayerListener;
import com.playpandora.hook.managers.NotificationManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Hook extends JavaPlugin {
    
    private static Hook instance;
    private NotificationManager notificationManager;
    private HookAPI api;
    private PluginIntegrations integrations;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Save default config
            saveDefaultConfig();
            
            // Initialize notification manager
            notificationManager = new NotificationManager(this);
            
            // Initialize API
            api = new HookAPI(this);
            
            // Initialize integrations
            integrations = new PluginIntegrations(this);
            
            // Register listeners
            getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
            
            // Initialize integrations
            integrations.initializeAll();
            
            getLogger().info("Hook v" + getDescription().getVersion() + " has been enabled! All plugins can now hook into the notification system!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable Hook! " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            if (integrations != null) {
                integrations.shutdownAll();
            }
            
            // Cancel all tasks
            getServer().getScheduler().cancelTasks(this);
            
            getLogger().info("Hook has been disabled!");
        } catch (Exception e) {
            getLogger().severe("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static Hook getInstance() {
        return instance;
    }
    
    public NotificationManager getNotificationManager() {
        return notificationManager;
    }
    
    public HookAPI getAPI() {
        return api;
    }
    
    public PluginIntegrations getIntegrations() {
        return integrations;
    }
}

