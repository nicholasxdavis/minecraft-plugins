package com.playpandora.homebuffs;

import com.playpandora.homebuffs.listeners.PlayerListener;
import com.playpandora.homebuffs.managers.BuffManager;
import com.playpandora.homebuffs.managers.EssentialsHook;
import org.bukkit.plugin.java.JavaPlugin;

public class HomeBuffs extends JavaPlugin {
    
    private static HomeBuffs instance;
    private EssentialsHook essentialsHook;
    private BuffManager buffManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Save default config
            saveDefaultConfig();
            
            // Initialize Essentials hook
            essentialsHook = new EssentialsHook(this);
            if (!essentialsHook.isEssentialsAvailable()) {
                getLogger().warning("Essentials not found! This plugin requires Essentials to work.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Initialize buff manager
            buffManager = new BuffManager(this);
            
            // Register commands with null checks
            if (getCommand("homebuffs") != null) {
                getCommand("homebuffs").setExecutor(new com.playpandora.homebuffs.commands.HomeBuffsCommand(this));
            } else {
                getLogger().warning("Command 'homebuffs' not found in plugin.yml!");
            }
            
            // Register listeners
            getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
            
            // Start buff checking task
            if (buffManager != null) {
                buffManager.startBuffChecking();
            }
            
            getLogger().info("HomeBuffs v" + getDescription().getVersion() + " has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable HomeBuffs! " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            if (buffManager != null) {
                buffManager.stopBuffChecking();
            }
            
            // Cancel all tasks
            getServer().getScheduler().cancelTasks(this);
            
            getLogger().info("HomeBuffs has been disabled!");
        } catch (Exception e) {
            getLogger().severe("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static HomeBuffs getInstance() {
        return instance;
    }
    
    public EssentialsHook getEssentialsHook() {
        return essentialsHook;
    }
    
    public BuffManager getBuffManager() {
        return buffManager;
    }
}


