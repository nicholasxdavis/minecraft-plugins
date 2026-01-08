package com.playpandora.pandoramaster;

import com.playpandora.pandoramaster.Command.HubMenuCommand;
import com.playpandora.pandoramaster.gui.HubGUI;
import com.playpandora.pandoramaster.listeners.ClockItemListener;
import com.playpandora.pandoramaster.managers.ClockItemManager;
import com.playpandora.pandoramaster.managers.SkriptHook;
import org.bukkit.plugin.java.JavaPlugin;

public class PandoraMaster extends JavaPlugin {
    
    private static PandoraMaster instance;
    private ClockItemManager clockItemManager;
    private HubGUI hubGUI;
    private SkriptHook skriptHook;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Save default config
            saveDefaultConfig();
            
            // Initialize managers
            skriptHook = new SkriptHook(this);
            clockItemManager = new ClockItemManager(this);
            hubGUI = new HubGUI(this);
            
            // Register listeners
            getServer().getPluginManager().registerEvents(new ClockItemListener(this), this);
            getServer().getPluginManager().registerEvents(new com.playpandora.pandoramaster.listeners.GUIListener(this), this);
            
            // Register commands
            getCommand("hubmenu").setExecutor(new HubMenuCommand(this));
            
            // Give clock item to all online players
            getServer().getScheduler().runTaskLater(this, () -> {
                clockItemManager.giveClockToAllOnline();
            }, 20L); // 1 second delay
            
            getLogger().info("PandoraMaster v" + getDescription().getVersion() + " has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable PandoraMaster! " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            // Cancel all tasks
            getServer().getScheduler().cancelTasks(this);
            
            getLogger().info("PandoraMaster has been disabled!");
        } catch (Exception e) {
            getLogger().severe("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static PandoraMaster getInstance() {
        return instance;
    }
    
    public ClockItemManager getClockItemManager() {
        return clockItemManager;
    }
    
    public HubGUI getHubGUI() {
        return hubGUI;
    }
    
    public SkriptHook getSkriptHook() {
        return skriptHook;
    }
    
    public String formatMessage(String path, String defaultMessage) {
        String message = getConfig().getString(path, defaultMessage);
        String prefix = getConfig().getString("messages.prefix", "&e&lPandora &8» &r");
        message = message.replace("{prefix}", prefix);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
}

