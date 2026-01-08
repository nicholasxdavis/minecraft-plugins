package com.playpandora.farmshop;

import com.playpandora.farmshop.listeners.FarmPlacerListener;
import com.playpandora.farmshop.listeners.ShopListener;
import com.playpandora.farmshop.managers.EconomyManager;
import com.playpandora.farmshop.managers.LevelManager;
import com.playpandora.farmshop.managers.FarmManager;
import com.playpandora.farmshop.managers.ProtectionManager;
import com.playpandora.farmshop.storage.PlayerDataManager;
import org.bukkit.plugin.java.JavaPlugin;

public class FarmShop extends JavaPlugin {
    
    private static FarmShop instance;
    private EconomyManager economyManager;
    private LevelManager levelManager;
    private FarmManager farmManager;
    private PlayerDataManager playerDataManager;
    private ProtectionManager protectionManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Save default config
            saveDefaultConfig();
            
            // Initialize managers
            economyManager = new EconomyManager(this);
            levelManager = new LevelManager(this);
            farmManager = new FarmManager(this);
            playerDataManager = new PlayerDataManager(this);
            protectionManager = new ProtectionManager(this);
            
            // Register commands with null checks
            if (getCommand("farmshop") != null) {
                getCommand("farmshop").setExecutor(new com.playpandora.farmshop.commands.FarmShopCommand(this));
            } else {
                getLogger().warning("Command 'farmshop' not found in plugin.yml!");
            }
            
            // Register listeners
            getServer().getPluginManager().registerEvents(new ShopListener(this), this);
            getServer().getPluginManager().registerEvents(new FarmPlacerListener(this), this);
            
            getLogger().info("FarmShop v" + getDescription().getVersion() + " has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable FarmShop! " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            if (playerDataManager != null) {
                playerDataManager.close();
            }
            
            // Cancel all tasks
            getServer().getScheduler().cancelTasks(this);
            
            getLogger().info("FarmShop has been disabled!");
        } catch (Exception e) {
            getLogger().severe("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static FarmShop getInstance() {
        return instance;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public LevelManager getLevelManager() {
        return levelManager;
    }
    
    public FarmManager getFarmManager() {
        return farmManager;
    }
    
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }
}


