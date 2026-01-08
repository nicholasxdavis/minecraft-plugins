package com.playpandora.levelplugin;

import com.playpandora.levelplugin.api.LevelAPI;
import com.playpandora.levelplugin.commands.LevelCommand;
import com.playpandora.levelplugin.commands.XPCommand;
import com.playpandora.levelplugin.expansions.LevelExpansion;
import com.playpandora.levelplugin.listeners.ChatListener;
import com.playpandora.levelplugin.listeners.XPListener;
import com.playpandora.levelplugin.managers.LevelManager;
import com.playpandora.levelplugin.managers.RewardManager;
import com.playpandora.levelplugin.storage.DataManager;
import org.bukkit.plugin.java.JavaPlugin;

public class LevelPlugin extends JavaPlugin {
    
    private static LevelPlugin instance;
    private DataManager dataManager;
    private LevelManager levelManager;
    private RewardManager rewardManager;
    private LevelAPI api;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Save default config
            saveDefaultConfig();
            
            // Initialize managers
            dataManager = new DataManager(this);
            levelManager = new LevelManager(this);
            rewardManager = new RewardManager(this);
            api = new LevelAPI(this);
            
            // DataManager loads data in constructor
            
            // Register commands with null checks
            if (getCommand("level") != null) {
                getCommand("level").setExecutor(new LevelCommand(this));
            } else {
                getLogger().warning("Command 'level' not found in plugin.yml!");
            }
            if (getCommand("xp") != null) {
                getCommand("xp").setExecutor(new XPCommand(this));
            } else {
                getLogger().warning("Command 'xp' not found in plugin.yml!");
            }
            if (getCommand("levelset") != null) {
                getCommand("levelset").setExecutor(new com.playpandora.levelplugin.commands.LevelSetCommand(this));
            } else {
                getLogger().warning("Command 'levelset' not found in plugin.yml!");
            }
            
            // Register listeners
            getServer().getPluginManager().registerEvents(new XPListener(this), this);
            getServer().getPluginManager().registerEvents(new ChatListener(this), this);
            getServer().getPluginManager().registerEvents(new com.playpandora.levelplugin.listeners.EssentialsChatListener(this), this);
            getServer().getPluginManager().registerEvents(new com.playpandora.levelplugin.listeners.EssentialsFormatProcessor(this), this);
            
            // Start playtime tracking
            if (levelManager != null) {
                levelManager.startPlaytimeTracking();
            }
            
            // Register PlaceholderAPI expansion
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                try {
                    new LevelExpansion(this).register();
                    getLogger().info("PlaceholderAPI expansion registered!");
                } catch (Exception e) {
                    getLogger().warning("Failed to register PlaceholderAPI expansion: " + e.getMessage());
                }
            } else {
                getLogger().warning("PlaceholderAPI not found! Level placeholder will not work.");
            }
            
            getLogger().info("LevelPlugin v" + getDescription().getVersion() + " has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable LevelPlugin! " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            // Stop auto-save task
            if (dataManager != null) {
                dataManager.stopAutoSave();
            }
            
            // Save all data one final time
            if (dataManager != null) {
                dataManager.saveAllData();
                getLogger().info("All player data saved successfully!");
            }
            
            // Cancel all tasks
            getServer().getScheduler().cancelTasks(this);
            
            getLogger().info("LevelPlugin has been disabled!");
        } catch (Exception e) {
            getLogger().severe("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static LevelPlugin getInstance() {
        return instance;
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    public LevelManager getLevelManager() {
        return levelManager;
    }
    
    public RewardManager getRewardManager() {
        return rewardManager;
    }
    
    public LevelAPI getAPI() {
        return api;
    }
}

