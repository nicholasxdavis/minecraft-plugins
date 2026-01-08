package com.playpandora.levelenchant;

import com.playpandora.levelenchant.listeners.EnchantListener;
import com.playpandora.levelenchant.listeners.CraftListener;
import com.playpandora.levelenchant.listeners.PlayerListener;
import com.playpandora.levelenchant.managers.LevelIntegration;
import com.playpandora.levelenchant.managers.XPBarManager;
import com.playpandora.levelenchant.storage.DataManager;
import org.bukkit.plugin.java.JavaPlugin;

public class LevelEnchant extends JavaPlugin {
    
    private static LevelEnchant instance;
    private LevelIntegration levelIntegration;
    private XPBarManager xpBarManager;
    private DataManager dataManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Save default config
            saveDefaultConfig();
            
            // Initialize data manager (loads data in constructor)
            dataManager = new DataManager(this);
            
            // Initialize level integration
            levelIntegration = new LevelIntegration(this);
            
            // Initialize XP bar manager if enabled
            if (getConfig().getBoolean("features.display-level-on-xp-bar", true)) {
                xpBarManager = new XPBarManager(this);
                if (xpBarManager != null) {
                    xpBarManager.start();
                }
            }
            
            // Register listeners
            PlayerListener playerListener = new PlayerListener(this);
            getServer().getPluginManager().registerEvents(playerListener, this);
            
            if (getConfig().getBoolean("features.use-level-for-enchanting", true)) {
                getServer().getPluginManager().registerEvents(new EnchantListener(this), this);
            }
            
            if (getConfig().getBoolean("features.prevent-crafting-xp-loss", true)) {
                getServer().getPluginManager().registerEvents(new CraftListener(this), this);
            }
            
            getLogger().info("LevelEnchant v" + getDescription().getVersion() + " has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable LevelEnchant! " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            if (xpBarManager != null) {
                xpBarManager.stop();
            }
            
            if (dataManager != null) {
                dataManager.close();
            }
            
            // Cancel all tasks
            getServer().getScheduler().cancelTasks(this);
            
            getLogger().info("LevelEnchant has been disabled!");
        } catch (Exception e) {
            getLogger().severe("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static LevelEnchant getInstance() {
        return instance;
    }
    
    public LevelIntegration getLevelIntegration() {
        return levelIntegration;
    }
    
    public XPBarManager getXPBarManager() {
        return xpBarManager;
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    public String getMessage(String key) {
        String message = getConfig().getString("messages." + key, "");
        String prefix = getConfig().getString("messages.prefix", "&e&lPandora");
        message = message.replace("{prefix}", prefix);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
}

