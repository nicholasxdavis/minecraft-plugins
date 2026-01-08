package com.playpandora.welcome;

import com.playpandora.welcome.commands.TipsCommand;
import com.playpandora.welcome.listeners.PlayerJoinListener;
import com.playpandora.welcome.managers.TipsManager;
import com.playpandora.welcome.managers.BookletManager;
import com.playpandora.welcome.managers.PlayerDataManager;
import com.playpandora.welcome.hooks.HookIntegration;
import org.bukkit.plugin.java.JavaPlugin;

public class Welcome extends JavaPlugin {
    
    private static Welcome instance;
    private TipsManager tipsManager;
    private BookletManager bookletManager;
    private PlayerDataManager playerDataManager;
    private HookIntegration hookIntegration;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Save default config
            saveDefaultConfig();
            
            // Initialize managers
            playerDataManager = new PlayerDataManager(this);
            tipsManager = new TipsManager(this);
            bookletManager = new BookletManager(this);
            
            // Initialize Hook integration
            hookIntegration = new HookIntegration(this);
            if (hookIntegration.isHookAvailable()) {
                getLogger().info("Hook integration enabled!");
            }
            
            // Register command
            if (getCommand("tips") != null) {
                getCommand("tips").setExecutor(new TipsCommand(this));
            } else {
                getLogger().warning("Command 'tips' not found in plugin.yml!");
            }
            
            // Register listeners
            getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
            
            getLogger().info("Welcome v" + getDescription().getVersion() + " has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable Welcome! " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            // Cancel all tasks
            if (tipsManager != null) {
                tipsManager.cancelAllTasks();
            }
            
            // Save all player data before shutdown
            if (playerDataManager != null) {
                playerDataManager.shutdown();
            }
            
            getLogger().info("Welcome has been disabled!");
        } catch (Exception e) {
            getLogger().severe("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static Welcome getInstance() {
        return instance;
    }
    
    public TipsManager getTipsManager() {
        return tipsManager;
    }
    
    public BookletManager getBookletManager() {
        return bookletManager;
    }
    
    public HookIntegration getHookIntegration() {
        return hookIntegration;
    }
    
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
}


