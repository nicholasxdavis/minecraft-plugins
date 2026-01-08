package com.playpandora.pandoracrates;

import com.playpandora.pandoracrates.commands.CrateCommand;
import com.playpandora.pandoracrates.listeners.CrateListener;
import com.playpandora.pandoracrates.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

public class PandoraCrates extends JavaPlugin {
    
    private static PandoraCrates instance;
    private CrateManager crateManager;
    private KeyManager keyManager;
    private RewardManager rewardManager;
    private HologramManager hologramManager;
    private AnimationManager animationManager;
    private IntegrationManager integrationManager;
    private com.playpandora.pandoracrates.gui.PreviewGUI previewGUI;
    private com.playpandora.pandoracrates.gui.SpinnerGUI spinnerGUI;
    private com.playpandora.pandoracrates.managers.StatisticsManager statisticsManager;
    private com.playpandora.pandoracrates.managers.CooldownManager cooldownManager;
    private com.playpandora.pandoracrates.managers.CrateItemManager crateItemManager;
    private com.playpandora.pandoracrates.managers.LocationDataManager locationDataManager;
    private com.playpandora.pandoracrates.managers.SaveManager saveManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize managers (order matters - LocationDataManager must be before CrateManager)
        integrationManager = new IntegrationManager(this);
        locationDataManager = new com.playpandora.pandoracrates.managers.LocationDataManager(this);
        statisticsManager = new com.playpandora.pandoracrates.managers.StatisticsManager(this);
        cooldownManager = new com.playpandora.pandoracrates.managers.CooldownManager(this);
        crateManager = new CrateManager(this);
        keyManager = new KeyManager(this);
        rewardManager = new RewardManager(this);
        hologramManager = new HologramManager(this);
        animationManager = new AnimationManager(this);
        previewGUI = new com.playpandora.pandoracrates.gui.PreviewGUI(this);
        spinnerGUI = new com.playpandora.pandoracrates.gui.SpinnerGUI(this);
        crateItemManager = new com.playpandora.pandoracrates.managers.CrateItemManager(this);
        saveManager = new com.playpandora.pandoracrates.managers.SaveManager(this);
        
        // Register commands
        getCommand("pcrates").setExecutor(new CrateCommand(this));
        getCommand("crates").setExecutor(new com.playpandora.pandoracrates.commands.CratesCommand(this));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new CrateListener(this), this);
        getServer().getPluginManager().registerEvents(new com.playpandora.pandoracrates.listeners.GUIListener(this), this);
        
        // Load crates
        crateManager.loadCrates();
        
        // Initialize holograms
        hologramManager.initializeHolograms();
        
        getLogger().info("PandoraCrates v" + getDescription().getVersion() + " has been enabled!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Saving all data before shutdown...");
        
        // Stop auto-save
        if (saveManager != null) {
            saveManager.stopAutoSave();
            saveManager.saveAllSync();
        } else {
            // Fallback: save manually if SaveManager not available
            if (statisticsManager != null) {
                statisticsManager.saveStats();
            }
            if (locationDataManager != null) {
                locationDataManager.saveDataFile();
            }
            if (cooldownManager != null) {
                cooldownManager.saveCooldowns();
            }
        }
        
        if (hologramManager != null) {
            hologramManager.removeAllHolograms();
        }
        
        getLogger().info("All data saved! PandoraCrates has been disabled!");
    }
    
    public static PandoraCrates getInstance() {
        return instance;
    }
    
    public CrateManager getCrateManager() {
        return crateManager;
    }
    
    public KeyManager getKeyManager() {
        return keyManager;
    }
    
    public RewardManager getRewardManager() {
        return rewardManager;
    }
    
    public HologramManager getHologramManager() {
        return hologramManager;
    }
    
    public AnimationManager getAnimationManager() {
        return animationManager;
    }
    
    public IntegrationManager getIntegrationManager() {
        return integrationManager;
    }
    
    public com.playpandora.pandoracrates.gui.PreviewGUI getPreviewGUI() {
        return previewGUI;
    }
    
    public com.playpandora.pandoracrates.gui.SpinnerGUI getSpinnerGUI() {
        return spinnerGUI;
    }
    
    public com.playpandora.pandoracrates.managers.StatisticsManager getStatisticsManager() {
        return statisticsManager;
    }
    
    public com.playpandora.pandoracrates.managers.CooldownManager getCooldownManager() {
        return cooldownManager;
    }
    
    public com.playpandora.pandoracrates.managers.CrateItemManager getCrateItemManager() {
        return crateItemManager;
    }
    
    public com.playpandora.pandoracrates.managers.LocationDataManager getLocationDataManager() {
        return locationDataManager;
    }
    
    public com.playpandora.pandoracrates.managers.SaveManager getSaveManager() {
        return saveManager;
    }
    
    public String formatMessage(String path, String defaultValue) {
        String message = getConfig().getString("messages." + path, defaultValue);
        if (message == null || message.isEmpty()) {
            message = defaultValue;
        }
        String prefix = getConfig().getString("visuals.prefix", "&e&lPandora &8» &r");
        message = message.replace("{prefix}", prefix);
        // Translate color codes
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public String formatMessage(String path, String defaultValue, String... replacements) {
        String message = getConfig().getString("messages." + path, defaultValue);
        if (message == null || message.isEmpty()) {
            message = defaultValue;
        }
        String prefix = getConfig().getString("visuals.prefix", "&e&lPandora &8» &r");
        message = message.replace("{prefix}", prefix);
        
        // Replace all placeholders
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String placeholder = "{" + replacements[i] + "}";
                String value = replacements[i + 1];
                message = message.replace(placeholder, value);
            }
        }
        
        // Translate color codes after all replacements
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Formats a raw message string with color codes and placeholders
     * @param message The message to format
     * @param replacements Placeholder replacements (key, value pairs)
     * @return Formatted message with colors translated
     */
    public String formatRawMessage(String message, String... replacements) {
        String prefix = getConfig().getString("visuals.prefix", "&e&lPandora &8» &r");
        message = message.replace("{prefix}", prefix);
        
        // Replace all placeholders
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String placeholder = "{" + replacements[i] + "}";
                String value = replacements[i + 1];
                message = message.replace(placeholder, value);
            }
        }
        
        // Translate color codes after all replacements
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
}

