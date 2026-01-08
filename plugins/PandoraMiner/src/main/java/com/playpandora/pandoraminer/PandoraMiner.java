package com.playpandora.pandoraminer;

import com.playpandora.pandoraminer.commands.MinerCommand;
import com.playpandora.pandoraminer.listeners.MinerBaseChestListener;
import com.playpandora.pandoraminer.listeners.MinerCommandListener;
import com.playpandora.pandoraminer.listeners.MinerListener;
import com.playpandora.pandoraminer.listeners.MinerMovementListener;
import com.playpandora.pandoraminer.listeners.MinerProtectionListener;
import com.playpandora.pandoraminer.listeners.MinerQuitListener;
import com.playpandora.pandoraminer.managers.MinerManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for PandoraMiner
 */
public class PandoraMiner extends JavaPlugin {
    
    private static PandoraMiner instance;
    private MinerManager minerManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize miner manager
        minerManager = new MinerManager(this);
        
        // Register command
        getCommand("miner").setExecutor(new MinerCommand(this));
        getCommand("miner").setTabCompleter(new MinerCommand(this));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new MinerListener(this), this);
        getServer().getPluginManager().registerEvents(new MinerMovementListener(this), this);
        getServer().getPluginManager().registerEvents(new MinerProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new MinerBaseChestListener(this), this);
        getServer().getPluginManager().registerEvents(new MinerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new MinerCommandListener(this), this);
        
        getLogger().info("PandoraMiner v" + getDescription().getVersion() + " has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Restore all player inventories
        if (minerManager != null) {
            minerManager.restoreAllInventories();
        }
        
        getLogger().info("PandoraMiner has been disabled!");
    }
    
    public static PandoraMiner getInstance() {
        return instance;
    }
    
    public MinerManager getMinerManager() {
        return minerManager;
    }
}

