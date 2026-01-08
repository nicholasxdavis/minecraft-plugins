package com.playpandora.sellgui;

import com.playpandora.sellgui.commands.SellCommand;
import com.playpandora.sellgui.gui.SellShopGUI;
import com.playpandora.sellgui.managers.EconomyManager;
import com.playpandora.sellgui.managers.LevelIntegration;
import com.playpandora.sellgui.managers.PriceManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class SellGUI extends JavaPlugin {
    
    private static SellGUI instance;
    private Object economy;
    private PriceManager priceManager;
    private EconomyManager economyManager;
    private LevelIntegration levelIntegration;
    private SellShopGUI sellShopGUI;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Save default config
            saveDefaultConfig();
            
            // Setup Vault economy
            if (!setupEconomy()) {
                getLogger().severe("Vault or an economy plugin is not installed! Disabling plugin.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Initialize managers
            priceManager = new PriceManager(this);
            economyManager = new EconomyManager(this);
            levelIntegration = new LevelIntegration(this);
            sellShopGUI = new SellShopGUI(this);
            
            // Register commands with null checks
            SellCommand sellCommand = new SellCommand(this);
            if (getCommand("sell") != null) {
                getCommand("sell").setExecutor(sellCommand);
            } else {
                getLogger().warning("Command 'sell' not found in plugin.yml!");
            }
            if (getCommand("sellall") != null) {
                getCommand("sellall").setExecutor(sellCommand);
            }
            
            getLogger().info("SellGUI v" + getDescription().getVersion() + " has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable SellGUI! " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            // Cancel all tasks
            getServer().getScheduler().cancelTasks(this);
            
            getLogger().info("SellGUI has been disabled!");
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
            RegisteredServiceProvider<?> rsp = getServer().getServicesManager().getRegistration(economyClass);
            if (rsp == null) {
                return false;
            }
            economy = rsp.getProvider();
            return economy != null;
        } catch (ClassNotFoundException e) {
            getLogger().warning("Vault Economy class not found: " + e.getMessage());
            return false;
        }
    }
    
    public static SellGUI getInstance() {
        return instance;
    }
    
    public Object getEconomy() {
        return economy;
    }
    
    public PriceManager getPriceManager() {
        return priceManager;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public LevelIntegration getLevelIntegration() {
        return levelIntegration;
    }
    
    public SellShopGUI getSellShopGUI() {
        return sellShopGUI;
    }
    
    public String getMessage(String key) {
        String message = getConfig().getString("messages." + key, "");
        String prefix = getConfig().getString("messages.prefix", "&ePandora");
        message = message.replace("{prefix}", prefix);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
}

