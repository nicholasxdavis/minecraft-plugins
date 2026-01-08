package com.playpandora.craftaccess;

import com.playpandora.craftaccess.commands.AnvilCommand;
import com.playpandora.craftaccess.commands.CraftCommand;
import com.playpandora.craftaccess.commands.EnchantCommand;
import com.playpandora.craftaccess.listeners.BlockPlaceListener;
import com.playpandora.craftaccess.listeners.PlayerListener;
import com.playpandora.craftaccess.storage.DataManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CraftAccess extends JavaPlugin {
    
    private static CraftAccess instance;
    private DataManager dataManager;
    private LevelPluginIntegration levelIntegration;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Save default config
            saveDefaultConfig();
            
            // Initialize managers (DataManager loads data in constructor)
            dataManager = new DataManager(this);
            levelIntegration = new LevelPluginIntegration(this);
            
            // Register commands with null checks
            if (getCommand("craft") != null) {
                getCommand("craft").setExecutor(new CraftCommand(this));
            } else {
                getLogger().warning("Command 'craft' not found in plugin.yml!");
            }
            if (getCommand("enchant") != null) {
                getCommand("enchant").setExecutor(new EnchantCommand(this));
            } else {
                getLogger().warning("Command 'enchant' not found in plugin.yml!");
            }
            if (getCommand("anvil") != null) {
                getCommand("anvil").setExecutor(new AnvilCommand(this));
            } else {
                getLogger().warning("Command 'anvil' not found in plugin.yml!");
            }
            
            // Register listeners
            getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
            getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
            
            getLogger().info("CraftAccess v" + getDescription().getVersion() + " has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable CraftAccess! " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            if (dataManager != null) {
                dataManager.close();
            }
            
            // Cancel all tasks
            getServer().getScheduler().cancelTasks(this);
            
            getLogger().info("CraftAccess has been disabled!");
        } catch (Exception e) {
            getLogger().severe("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static CraftAccess getInstance() {
        return instance;
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    public LevelPluginIntegration getLevelIntegration() {
        return levelIntegration;
    }
    
    public String getMessage(String key) {
        String message = getConfig().getString("messages." + key, "");
        String prefix = getConfig().getString("messages.prefix", "&e&lPandora");
        message = message.replace("{prefix}", prefix);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
}

