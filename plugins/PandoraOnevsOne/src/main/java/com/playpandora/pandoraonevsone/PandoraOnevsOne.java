package com.playpandora.pandoraonevsone;

import com.playpandora.pandoraonevsone.commands.OneVsOneCommand;
import com.playpandora.pandoraonevsone.listeners.*;
import com.playpandora.pandoraonevsone.managers.OneVsOneManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for PandoraOnevsOne
 */
public class PandoraOnevsOne extends JavaPlugin {
    
    private static PandoraOnevsOne instance;
    private OneVsOneManager oneVsOneManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize manager
        oneVsOneManager = new OneVsOneManager(this);
        
        // Register commands
        getCommand("onevsone").setExecutor(new OneVsOneCommand(this));
        getCommand("onevsone").setTabCompleter(new OneVsOneCommand(this));
        getCommand("ovo").setExecutor(new OneVsOneCommand(this));
        getCommand("ovo").setTabCompleter(new OneVsOneCommand(this));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new OneVsOneListener(this), this);
        getServer().getPluginManager().registerEvents(new OneVsOneCommandListener(this), this);
        getServer().getPluginManager().registerEvents(new OneVsOneProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new OneVsOneBaseChestListener(this), this);
        getServer().getPluginManager().registerEvents(new OneVsOneArenaListener(this), this);
        getServer().getPluginManager().registerEvents(new OneVsOnePvPListener(this), this);
        
        getLogger().info("PandoraOnevsOne v" + getDescription().getVersion() + " has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // End all active duels and restore inventories
        if (oneVsOneManager != null) {
            oneVsOneManager.endAllDuels();
        }
        
        getLogger().info("PandoraOnevsOne has been disabled!");
    }
    
    public static PandoraOnevsOne getInstance() {
        return instance;
    }
    
    public OneVsOneManager getOneVsOneManager() {
        return oneVsOneManager;
    }
}


