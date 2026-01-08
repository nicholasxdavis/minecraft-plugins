package com.playpandora.pandoraitems;

import com.playpandora.pandoraitems.commands.ItemCommand;
import com.playpandora.pandoraitems.listeners.ItemUseListener;
import com.playpandora.pandoraitems.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

public class PandoraItems extends JavaPlugin {
    
    private static PandoraItems instance;
    private CannonItemManager cannonItemManager;
    private FarmItemManager farmItemManager;
    private KitItemManager kitItemManager;
    private PerkItemManager perkItemManager;
    private PetItemManager petItemManager;
    private LevelAPI levelAPI;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize LevelAPI
        initializeLevelAPI();
        
        // Initialize managers
        cannonItemManager = new CannonItemManager(this);
        farmItemManager = new FarmItemManager(this);
        kitItemManager = new KitItemManager(this);
        perkItemManager = new PerkItemManager(this);
        petItemManager = new PetItemManager(this);
        
        // Register command
        getCommand("pandoraitem").setExecutor(new ItemCommand(this));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new ItemUseListener(this), this);
        
        getLogger().info("PandoraItems v" + getDescription().getVersion() + " has been enabled!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("PandoraItems has been disabled!");
    }
    
    private void initializeLevelAPI() {
        if (getServer().getPluginManager().getPlugin("LevelPlugin") != null) {
            try {
                Class<?> levelPluginClass = Class.forName("com.playpandora.levelplugin.LevelPlugin");
                Object levelPlugin = getServer().getPluginManager().getPlugin("LevelPlugin");
                java.lang.reflect.Method getAPIMethod = levelPluginClass.getMethod("getAPI");
                Object levelPluginAPI = getAPIMethod.invoke(levelPlugin);
                levelAPI = new LevelAPI(levelPluginAPI);
                getLogger().info("LevelPlugin API loaded successfully!");
            } catch (Exception e) {
                getLogger().warning("Failed to load LevelPlugin API: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            getLogger().warning("LevelPlugin not found! Level requirements will not work.");
        }
    }
    
    public static PandoraItems getInstance() {
        return instance;
    }
    
    public CannonItemManager getCannonItemManager() {
        return cannonItemManager;
    }
    
    public FarmItemManager getFarmItemManager() {
        return farmItemManager;
    }
    
    public KitItemManager getKitItemManager() {
        return kitItemManager;
    }
    
    public PerkItemManager getPerkItemManager() {
        return perkItemManager;
    }
    
    public PetItemManager getPetItemManager() {
        return petItemManager;
    }
    
    public LevelAPI getLevelAPI() {
        return levelAPI;
    }
    
    public String formatMessage(String path, String defaultValue) {
        String message = getConfig().getString("messages." + path, defaultValue);
        message = message.replace("{prefix}", getConfig().getString("visuals.prefix", "&e&lPandora &8» &r"));
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public String formatMessage(String path, String defaultValue, String... replacements) {
        String message = formatMessage(path, defaultValue);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        return message;
    }
}

