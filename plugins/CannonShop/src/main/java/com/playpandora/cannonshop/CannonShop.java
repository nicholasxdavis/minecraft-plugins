package com.playpandora.cannonshop;

import com.playpandora.cannonshop.listeners.CannonPlacerListener;
import com.playpandora.cannonshop.listeners.ShopListener;
import com.playpandora.cannonshop.managers.CannonManager;
import com.playpandora.cannonshop.managers.EconomyManager;
import com.playpandora.cannonshop.managers.ProtectionManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CannonShop extends JavaPlugin {
    
    private static CannonShop instance;
    private EconomyManager economyManager;
    private CannonManager cannonManager;
    private ProtectionManager protectionManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Save default config
            saveDefaultConfig();
            
            // Initialize managers
            economyManager = new EconomyManager(this);
            cannonManager = new CannonManager(this);
            protectionManager = new ProtectionManager(this);
            
            // Register commands with null checks
            if (getCommand("cannon") != null) {
                getCommand("cannon").setExecutor(new com.playpandora.cannonshop.commands.CannonShopCommand(this));
            } else {
                getLogger().warning("Command 'cannon' not found in plugin.yml!");
            }
            
            // Register listeners
            getServer().getPluginManager().registerEvents(new ShopListener(this), this);
            getServer().getPluginManager().registerEvents(new CannonPlacerListener(this), this);
            
            getLogger().info("CannonShop v" + getDescription().getVersion() + " has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable CannonShop! " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            // Cancel all tasks
            getServer().getScheduler().cancelTasks(this);
            
            getLogger().info("CannonShop has been disabled!");
        } catch (Exception e) {
            getLogger().severe("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static CannonShop getInstance() {
        return instance;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public CannonManager getCannonManager() {
        return cannonManager;
    }
    
    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }
}

