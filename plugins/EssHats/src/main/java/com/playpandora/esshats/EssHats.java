package com.playpandora.esshats;

import com.playpandora.esshats.commands.HatCommand;
import com.playpandora.esshats.commands.ReloadCommand;
import com.playpandora.esshats.config.ConfigManager;
import com.playpandora.esshats.gui.HatGUI;
import com.playpandora.esshats.listeners.GUIListener;
import com.playpandora.esshats.hooks.EssentialsHook;
import org.bukkit.plugin.java.JavaPlugin;

public class EssHats extends JavaPlugin {
    
    private static EssHats instance;
    private ConfigManager configManager;
    private HatGUI hatGUI;
    private EssentialsHook essentialsHook;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Load configuration
            configManager = new ConfigManager(this);
            
            // Check for EssentialsX (it may be registered as "Essentials" or "EssentialsX")
            org.bukkit.plugin.Plugin essPlugin = getServer().getPluginManager().getPlugin("EssentialsX");
            if (essPlugin == null) {
                essPlugin = getServer().getPluginManager().getPlugin("Essentials");
            }
            if (essPlugin == null || !essPlugin.isEnabled()) {
                getLogger().severe("EssentialsX is required but not found! Disabling plugin...");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Initialize Essentials hook
            essentialsHook = new EssentialsHook(this);
            
            if (!essentialsHook.isEssentialsAvailable()) {
                getLogger().warning("EssentialsX not found immediately. Will retry when GUI is opened...");
            } else {
                getLogger().info("Successfully hooked into EssentialsX!");
            }
            
            // Initialize GUI
            hatGUI = new HatGUI(this);
            
            // Register commands
            if (getCommand("hat") != null) {
                getCommand("hat").setExecutor(new HatCommand(this));
            } else {
                getLogger().warning("Command 'hat' not found in plugin.yml!");
            }
            
            if (getCommand("esshats") != null) {
                getCommand("esshats").setExecutor(new ReloadCommand(this));
            }
            
            // Register listeners
            getServer().getPluginManager().registerEvents(new GUIListener(this), this);
            
            getLogger().info("EssHats v" + getDescription().getVersion() + " has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable EssHats! " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            getServer().getScheduler().cancelTasks(this);
            getLogger().info("EssHats has been disabled!");
        } catch (Exception e) {
            getLogger().severe("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static EssHats getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public HatGUI getHatGUI() {
        return hatGUI;
    }
    
    public EssentialsHook getEssentialsHook() {
        return essentialsHook;
    }
}

