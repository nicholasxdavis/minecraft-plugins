package com.pandora.events;

import com.pandora.events.commands.EventCommand;
import com.pandora.events.data.EventDataManager;
import com.pandora.events.listeners.BeaconBreakListener;
import com.pandora.events.listeners.FactionNameProtectionListener;
import com.pandora.events.managers.EventFactionManager;
import com.pandora.events.managers.EventManager;
import com.pandora.events.managers.EventSpawner;
import com.pandora.events.managers.AutoEventScheduler;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Method;

public class PandoraEventsPlugin extends JavaPlugin {
    
    private static PandoraEventsPlugin instance;
    private EventFactionManager eventFactionManager;
    private EventSpawner eventSpawner;
    private EventManager eventManager;
    private AutoEventScheduler autoEventScheduler;
    private EventDataManager eventDataManager;
    private BukkitRunnable autoSaveTask;
    private com.pandora.events.integration.HookIntegration hookIntegration;
    
    @Override
    public void onEnable() {
        getLogger().info("Starting PandoraEvents initialization...");
        instance = this;
        
        try {
            getLogger().info("Step 1: Ensuring data folder exists...");
            // Ensure data folder exists
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
                getLogger().info("Created data folder: " + getDataFolder().getAbsolutePath());
            }
            
            getLogger().info("Step 2: Checking for PandoraBases...");
            // Check if PandoraBases is loaded and enabled
            org.bukkit.plugin.Plugin pandoraBases = getServer().getPluginManager().getPlugin("PandoraBases");
            if (pandoraBases == null) {
                getLogger().severe("PandoraBases plugin not found! Disabling PandoraEvents...");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            if (!pandoraBases.isEnabled()) {
                getLogger().severe("PandoraBases is loaded but not enabled! Disabling PandoraEvents...");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            getLogger().info("PandoraBases found and enabled!");
            
            getLogger().info("Step 3: Verifying PandoraBases classes...");
            // Verify PandoraBases classes are available
            try {
                Class.forName("com.massivecraft.factions.Factions");
                getLogger().info("PandoraBases classes are available!");
            } catch (ClassNotFoundException e) {
                getLogger().severe("PandoraBases classes not found! Disabling PandoraEvents...");
                getLogger().severe("Error: " + e.getMessage());
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            getLogger().info("Step 4: Protecting event faction name...");
            // Protect event name first
            try {
                FactionNameProtectionListener.protectEventName();
                getLogger().info("Event faction name protected!");
            } catch (Exception e) {
                getLogger().warning("Failed to protect event name: " + e.getMessage());
                e.printStackTrace();
            }
            
            getLogger().info("Step 5: Initializing EventDataManager...");
            // Initialize data manager first (loads saved data)
            try {
                eventDataManager = new EventDataManager();
                getLogger().info("EventDataManager initialized!");
            } catch (Exception e) {
                getLogger().severe("Failed to initialize EventDataManager: " + e.getMessage());
                e.printStackTrace();
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            getLogger().info("Step 6: Initializing managers...");
            // Initialize managers
            try {
                eventFactionManager = new EventFactionManager();
                getLogger().info("EventFactionManager initialized!");
                eventSpawner = new EventSpawner();
                getLogger().info("EventSpawner initialized!");
                eventManager = new EventManager();
                getLogger().info("EventManager initialized!");
                autoEventScheduler = new AutoEventScheduler();
                getLogger().info("AutoEventScheduler initialized!");
            } catch (Exception e) {
                getLogger().severe("Failed to initialize managers: " + e.getMessage());
                e.printStackTrace();
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            getLogger().info("Step 7: Loading saved event state...");
            // Load saved event state
            try {
                loadEventState();
                getLogger().info("Event state loaded!");
            } catch (Exception e) {
                getLogger().warning("Failed to load event state: " + e.getMessage());
                e.printStackTrace();
            }
            
            getLogger().info("Step 8: Initializing Hook integration...");
            // Initialize Hook integration
            try {
                hookIntegration = new com.pandora.events.integration.HookIntegration(this);
                getLogger().info("Hook integration initialized!");
            } catch (Exception e) {
                getLogger().warning("Failed to initialize Hook integration: " + e.getMessage());
                e.printStackTrace();
            }
            
            getLogger().info("Step 9: Registering listeners...");
            // Register listeners
            try {
                // FactionNameProtectionListener is not needed - protection is handled via blacklist
                BeaconBreakListener beaconListener = new BeaconBreakListener();
                getServer().getPluginManager().registerEvents(beaconListener, this);
                
                // Register chest open listener
                getServer().getPluginManager().registerEvents(new com.pandora.events.listeners.ChestOpenListener(), this);
                
                getLogger().info("Listeners registered successfully!");
            } catch (Exception e) {
                getLogger().severe("Failed to register listeners: " + e.getMessage());
                getLogger().severe("Exception type: " + e.getClass().getName());
                e.printStackTrace();
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            getLogger().info("Step 10: Registering commands...");
            // Register commands
            try {
                org.bukkit.command.PluginCommand baseventCommand = getCommand("basevent");
                if (baseventCommand != null) {
                    baseventCommand.setExecutor(new EventCommand());
                    getLogger().info("Commands registered!");
                } else {
                    getLogger().severe("basevent command not found in plugin.yml! This is a critical error!");
                    getServer().getPluginManager().disablePlugin(this);
                    return;
                }
            } catch (Exception e) {
                getLogger().severe("Failed to register commands: " + e.getMessage());
                e.printStackTrace();
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            getLogger().info("Step 11: Starting auto-event scheduler...");
            // Start auto-event scheduler
            try {
                autoEventScheduler.start();
                getLogger().info("Auto-event scheduler started!");
            } catch (Exception e) {
                getLogger().warning("Failed to start auto-event scheduler: " + e.getMessage());
                e.printStackTrace();
            }
            
            getLogger().info("Step 12: Starting auto-save task...");
            // Start auto-save task (save every 5 minutes)
            try {
                startAutoSave();
                getLogger().info("Auto-save task started!");
            } catch (Exception e) {
                getLogger().warning("Failed to start auto-save task: " + e.getMessage());
                e.printStackTrace();
            }
            
            getLogger().info("PandoraEvents has been enabled successfully!");
        } catch (Throwable e) {
            getLogger().severe("Fatal error during plugin enable!");
            getLogger().severe("Error: " + e.getMessage());
            getLogger().severe("Class: " + e.getClass().getName());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    /**
     * Load event state from saved data
     */
    private void loadEventState() {
        try {
            com.pandora.events.data.EventData data = eventDataManager.getEventData();
            
            if (data == null) {
                getLogger().warning("EventData is null, using defaults");
                return;
            }
            
            // Restore event state
            if (data.isEventActive()) {
                eventManager.setEventActive(true);
                eventManager.setEventBeaconLocation(data.getEventBeaconLocation());
                eventManager.setEventFactionId(data.getEventFactionId());
                
                // Try to restore faction object
                if (data.getEventFactionId() != null) {
                    try {
                        Class<?> factionsClass = Class.forName("com.massivecraft.factions.Factions");
                        Method getInstanceMethod = factionsClass.getMethod("getInstance");
                        Object factionsInstance = getInstanceMethod.invoke(null);
                        Method getFactionByIdMethod = factionsClass.getMethod("getFactionById", String.class);
                        Object faction = getFactionByIdMethod.invoke(factionsInstance, data.getEventFactionId());
                        if (faction != null) {
                            eventManager.setEventFaction(faction);
                        }
                    } catch (Exception e) {
                        getLogger().warning("Could not restore event faction: " + e.getMessage());
                    }
                }
                
                getLogger().info("Restored active event state");
            }
            
            // Restore auto-event settings with null checks
            try {
                autoEventScheduler.setAutoEnabled(data.isAutoEventsEnabled());
            } catch (Exception e) {
                getLogger().warning("Failed to set auto-enabled: " + e.getMessage());
            }
            
            try {
                String firstTime = data.getFirstEventTime();
                String secondTime = data.getSecondEventTime();
                if (firstTime != null && secondTime != null) {
                    autoEventScheduler.setEventTimes(firstTime, secondTime);
                } else {
                    getLogger().info("Using default event times (12:00 and 00:00)");
                }
            } catch (Exception e) {
                getLogger().warning("Failed to set event times: " + e.getMessage());
            }
            
            try {
                autoEventScheduler.setLastEventTimes(data.getLastFirstEventTime(), data.getLastSecondEventTime());
            } catch (Exception e) {
                getLogger().warning("Failed to set last event times: " + e.getMessage());
            }
        } catch (Exception e) {
            getLogger().severe("Error loading event state: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Start auto-save task
     */
    private void startAutoSave() {
        autoSaveTask = new BukkitRunnable() {
            @Override
            public void run() {
                saveEventState();
            }
        };
        // Save every 5 minutes (6000 ticks)
        autoSaveTask.runTaskTimerAsynchronously(this, 6000L, 6000L);
    }
    
    /**
     * Save current event state
     */
    public void saveEventState() {
        com.pandora.events.data.EventData data = eventDataManager.getEventData();
        
        // Save event state
        data.setEventActive(eventManager.isEventActive());
        data.setEventBeaconLocation(eventManager.getEventBeaconLocation());
        
        // Save faction ID if available
        if (eventManager.getEventFaction() != null) {
            try {
                String factionId = (String) eventManager.getEventFaction().getClass().getMethod("getId").invoke(eventManager.getEventFaction());
                data.setEventFactionId(factionId);
            } catch (Exception e) {
                // Ignore
            }
        }
        
        // Save auto-event settings
        data.setAutoEventsEnabled(autoEventScheduler.isAutoEnabled());
        data.setFirstEventTime(autoEventScheduler.getFirstEventTime());
        data.setSecondEventTime(autoEventScheduler.getSecondEventTime());
        data.setLastFirstEventTime(autoEventScheduler.getLastFirstEventTime());
        data.setLastSecondEventTime(autoEventScheduler.getLastSecondEventTime());
        
        // Save to file
        eventDataManager.saveData();
    }
    
    @Override
    public void onDisable() {
        // Stop auto-save
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        
        // Save all data before shutdown
        saveEventState();
        
        if (autoEventScheduler != null) {
            autoEventScheduler.stop();
        }
        
        getLogger().info("PandoraEvents has been disabled!");
    }
    
    public static PandoraEventsPlugin getInstance() {
        return instance;
    }
    
    public EventFactionManager getEventFactionManager() {
        return eventFactionManager;
    }
    
    public EventSpawner getEventSpawner() {
        return eventSpawner;
    }
    
    public EventManager getEventManager() {
        return eventManager;
    }
    
    public AutoEventScheduler getAutoEventScheduler() {
        return autoEventScheduler;
    }
    
    public EventDataManager getEventDataManager() {
        return eventDataManager;
    }
    
    public com.pandora.events.integration.HookIntegration getHookIntegration() {
        return hookIntegration;
    }
}

