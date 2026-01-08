package com.playpandora.petplugin;

import com.playpandora.petplugin.commands.PetCommand;
import com.playpandora.petplugin.gui.PetShopGUI;
import com.playpandora.petplugin.managers.PetManager;
import com.playpandora.petplugin.managers.PurchaseManager;
import com.playpandora.petplugin.storage.DataManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

public class PetPlugin extends JavaPlugin {
    
    private static PetPlugin instance;
    private Object economy;
    private DataManager dataManager;
    private PetManager petManager;
    private PurchaseManager purchaseManager;
    private PetShopGUI shopGUI;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Save default config
            saveDefaultConfig();
            
            // Setup economy
            if (!setupEconomy()) {
                getLogger().warning("Vault not found! Economy features disabled.");
            }
            
            // Initialize managers
            dataManager = new DataManager(this);
            petManager = new PetManager(this);
            purchaseManager = new PurchaseManager(this);
            shopGUI = new PetShopGUI(this);
            
            // Load existing data
            if (dataManager != null) {
                dataManager.loadData();
            }
            
            // Register command with null check
            if (getCommand("pet") != null) {
                getCommand("pet").setExecutor(new PetCommand(this));
            } else {
                getLogger().warning("Command 'pet' not found in plugin.yml!");
            }
            
            // Register listeners
            getServer().getPluginManager().registerEvents(new com.playpandora.petplugin.listeners.PetListener(this), this);
            
            // Start health monitoring task
            if (petManager != null) {
                petManager.startHealthMonitoring();
            }
            
            getLogger().info("PetPlugin v" + getDescription().getVersion() + " has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable PetPlugin! " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            // Despawn all pets (health is already saved during monitoring)
            if (petManager != null) {
                petManager.despawnAllPets();
            }
            
            // Close data manager (saves all data and cancels auto-save)
            if (dataManager != null) {
                dataManager.close();
            }
            
            // Cancel all tasks
            getServer().getScheduler().cancelTasks(this);
            
            getLogger().info("PetPlugin has been disabled!");
        } catch (Exception e) {
            getLogger().severe("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            var rsp = getServer().getServicesManager().getRegistration(economyClass);
            if (rsp == null) {
                return false;
            }
            
            economy = rsp.getProvider();
            return economy != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    public static PetPlugin getInstance() {
        return instance;
    }
    
    public Object getEconomy() {
        return economy;
    }
    
    public double getPlayerBalance(org.bukkit.entity.Player player) {
        if (economy == null) {
            return 0.0;
        }
        
        try {
            Method getBalance = economy.getClass().getMethod("getBalance", org.bukkit.OfflinePlayer.class);
            return (Double) getBalance.invoke(economy, player);
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    public boolean withdrawPlayer(org.bukkit.entity.Player player, double amount) {
        if (economy == null) {
            getLogger().warning("Economy is null! Cannot withdraw money for " + player.getName());
            return false;
        }
        
        try {
            Method withdraw = economy.getClass().getMethod("withdrawPlayer", org.bukkit.OfflinePlayer.class, double.class);
            Object response = withdraw.invoke(economy, player, amount);
            
            // Check if the response indicates success (EconomyResponse)
            if (response != null) {
                try {
                    Method transactionSuccess = response.getClass().getMethod("transactionSuccess");
                    boolean success = (Boolean) transactionSuccess.invoke(response);
                    if (!success) {
                        Method errorMessage = response.getClass().getMethod("errorMessage");
                        String error = (String) errorMessage.invoke(response);
                        getLogger().warning("Failed to withdraw money for " + player.getName() + ": " + error);
                        return false;
                    }
                    return true;
                } catch (Exception e) {
                    // If response doesn't have transactionSuccess method, assume success
                    return true;
                }
            }
            return true;
        } catch (Exception e) {
            getLogger().severe("Failed to withdraw money for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    public PetManager getPetManager() {
        return petManager;
    }
    
    public PurchaseManager getPurchaseManager() {
        return purchaseManager;
    }
    
    public PetShopGUI getShopGUI() {
        return shopGUI;
    }
    
    /**
     * Formats a message from config by replacing {prefix} and translating color codes
     * @param messageKey The config key for the message (e.g., "messages.pet-despawned")
     * @param defaultMessage The default message if config value is not found
     * @param replacements Varargs of replacements in format: "key1", "value1", "key2", "value2", ...
     * @return Formatted message ready to send
     */
    public String formatMessage(String messageKey, String defaultMessage, String... replacements) {
        String message = getConfig().getString(messageKey, defaultMessage);
        String prefix = getConfig().getString("messages.prefix", "&e&lPandora &8» &r");
        
        // Replace {prefix} placeholder
        message = message.replace("{prefix}", prefix);
        
        // Apply replacements
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        
        // Translate color codes
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
}

