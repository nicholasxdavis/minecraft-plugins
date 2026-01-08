package com.playpandora.moreend;

import com.playpandora.moreend.handlers.*;
import org.bukkit.plugin.java.JavaPlugin;

public class MoreEnd extends JavaPlugin {
    
    private static MoreEnd instance;
    private SpawnHandler spawnHandler;
    private DropHandler dropHandler;
    private BehaviorHandler behaviorHandler;
    private HorrorAtmosphereHandler horrorAtmosphereHandler;
    private EventHandler eventHandler;
    private AdvancedAIHandler advancedAIHandler;
    private com.playpandora.moreend.utils.IntegrationManager integrationManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Save default config
            saveDefaultConfig();
            
            // Initialize integration manager first
            integrationManager = new com.playpandora.moreend.utils.IntegrationManager(this);
            
            // Initialize handlers
            getLogger().info("Initializing MoreEnd handlers...");
            
            spawnHandler = new SpawnHandler(this);
            getServer().getPluginManager().registerEvents(spawnHandler, this);
            getLogger().info("Spawn handler enabled");
            
            dropHandler = new DropHandler(this);
            getServer().getPluginManager().registerEvents(dropHandler, this);
            getLogger().info("Drop handler enabled");
            
            behaviorHandler = new BehaviorHandler(this);
            getServer().getPluginManager().registerEvents(behaviorHandler, this);
            getLogger().info("Behavior handler enabled");
            
            if (getConfig().getBoolean("features.horror-atmosphere.enabled", true)) {
                try {
                    horrorAtmosphereHandler = new HorrorAtmosphereHandler(this);
                    getServer().getPluginManager().registerEvents(horrorAtmosphereHandler, this);
                    getLogger().info("Horror atmosphere enabled");
                } catch (Exception e) {
                    getLogger().warning("Failed to enable horror atmosphere: " + e.getMessage());
                }
            }
            
            if (getConfig().getBoolean("features.events.enabled", true)) {
                try {
                    eventHandler = new EventHandler(this);
                    getServer().getPluginManager().registerEvents(eventHandler, this);
                    getLogger().info("Event handler enabled");
                } catch (Exception e) {
                    getLogger().warning("Failed to enable event handler: " + e.getMessage());
                }
            }
            
            if (getConfig().getBoolean("features.advanced-ai.enabled", true)) {
                try {
                    advancedAIHandler = new AdvancedAIHandler(this);
                    getServer().getPluginManager().registerEvents(advancedAIHandler, this);
                    getLogger().info("Advanced AI enabled");
                } catch (Exception e) {
                    getLogger().warning("Failed to enable advanced AI: " + e.getMessage());
                }
            }
            
            getLogger().info("MoreEnd v" + getDescription().getVersion() + " has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable MoreEnd! " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            if (horrorAtmosphereHandler != null) {
                horrorAtmosphereHandler.cleanup();
            }
            if (eventHandler != null && eventHandler.getEventManager() != null) {
                eventHandler.getEventManager().cleanup();
            }
            
            // Cancel all tasks
            getServer().getScheduler().cancelTasks(this);
            
            getLogger().info("MoreEnd has been disabled!");
        } catch (Exception e) {
            getLogger().severe("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static MoreEnd getInstance() {
        return instance;
    }
    
    public SpawnHandler getSpawnHandler() {
        return spawnHandler;
    }
    
    public DropHandler getDropHandler() {
        return dropHandler;
    }
    
    public BehaviorHandler getBehaviorHandler() {
        return behaviorHandler;
    }
    
    public HorrorAtmosphereHandler getHorrorAtmosphereHandler() {
        return horrorAtmosphereHandler;
    }
    
    public EventHandler getEventHandler() {
        return eventHandler;
    }
    
    public AdvancedAIHandler getAdvancedAIHandler() {
        return advancedAIHandler;
    }
    
    public com.playpandora.moreend.utils.IntegrationManager getIntegrationManager() {
        return integrationManager;
    }
}

