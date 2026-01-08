package com.playpandora.treasurehunt;

import com.playpandora.treasurehunt.commands.TreasureCommand;
import com.playpandora.treasurehunt.listeners.TreasureListener;
import com.playpandora.treasurehunt.managers.TreasureManager;
import com.playpandora.treasurehunt.managers.RewardManager;
import com.playpandora.treasurehunt.managers.IntegrationManager;
import org.bukkit.plugin.java.JavaPlugin;

public class TreasureHunt extends JavaPlugin {
    
    private static TreasureHunt instance;
    private TreasureManager treasureManager;
    private RewardManager rewardManager;
    private IntegrationManager integrationManager;
    private com.playpandora.treasurehunt.managers.HologramManager hologramManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Save default config
            saveDefaultConfig();
            
            // Initialize managers (HologramManager must be initialized first as TreasureManager.loadData() uses it)
            hologramManager = new com.playpandora.treasurehunt.managers.HologramManager(this);
            treasureManager = new TreasureManager(this);
            rewardManager = new RewardManager(this);
            integrationManager = new IntegrationManager(this);
            
            // Initialize integrations
            integrationManager.initializeIntegrations();
            
            // Recreate hologram for existing treasure (if any) now that all managers are initialized
            treasureManager.recreateHologramIfNeeded();
            
            // Register commands
            if (getCommand("treasure") != null) {
                getCommand("treasure").setExecutor(new TreasureCommand(this));
            } else {
                getLogger().warning("Command 'treasure' not found in plugin.yml!");
            }
            
            // Register listeners
            getServer().getPluginManager().registerEvents(new TreasureListener(this), this);
            
            // Start treasure spawn task
            treasureManager.startTreasureSpawnTask();
            
            // Start reminder task
            startReminderTask();
            
            getLogger().info("TreasureHunt v" + getDescription().getVersion() + " has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable TreasureHunt! " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            // Remove all holograms
            if (hologramManager != null) {
                hologramManager.removeAllHolograms();
            }
            
            // Save data
            if (treasureManager != null) {
                treasureManager.saveData();
            }
            
            // Cancel all tasks
            getServer().getScheduler().cancelTasks(this);
            
            getLogger().info("TreasureHunt has been disabled!");
        } catch (Exception e) {
            getLogger().severe("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static TreasureHunt getInstance() {
        return instance;
    }
    
    public TreasureManager getTreasureManager() {
        return treasureManager;
    }
    
    public RewardManager getRewardManager() {
        return rewardManager;
    }
    
    public IntegrationManager getIntegrationManager() {
        return integrationManager;
    }
    
    public com.playpandora.treasurehunt.managers.HologramManager getHologramManager() {
        return hologramManager;
    }
    
    /**
     * Format a message by replacing {prefix} placeholder and translating color codes
     */
    public String formatMessage(String message) {
        if (message == null) {
            return "";
        }
        String prefix = getConfig().getString("messages.prefix", "&e&lPandora &8» &r");
        message = message.replace("{prefix}", prefix);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
    
    private void startReminderTask() {
        long reminderInterval = getConfig().getLong("announcements.reminder-interval", 6000L); // Default: 5 minutes
        if (reminderInterval <= 0) {
            return; // Disabled
        }
        
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (!getConfig().getBoolean("announcements.enabled", true)) {
                return;
            }
            
            if (!getConfig().getBoolean("announcements.reminder", true)) {
                return;
            }
            
            if (!treasureManager.hasActiveTreasure()) {
                return; // No active treasure
            }
            
            org.bukkit.Location treasureLoc = treasureManager.getCurrentTreasureLocation();
            if (treasureLoc == null) {
                return;
            }
            
            // Broadcast reminder to all online players
            String reminderMessage = getConfig().getString("messages.treasure-reminder",
                "{prefix} &7There is an active treasure hunt! The first hint is at &6X: %x%, Y: %y%, Z: %z% &7in &6%world%&7!");
            reminderMessage = reminderMessage.replace("%x%", String.valueOf(treasureLoc.getBlockX()))
                                            .replace("%y%", String.valueOf(treasureLoc.getBlockY()))
                                            .replace("%z%", String.valueOf(treasureLoc.getBlockZ()))
                                            .replace("%world%", treasureLoc.getWorld().getName());
            
            String formattedMessage = formatMessage(reminderMessage);
            
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                player.sendMessage(formattedMessage);
            }
        }, reminderInterval, reminderInterval);
    }
}

