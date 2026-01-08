package com.playpandora.buygui;

import com.playpandora.buygui.commands.BuyCommand;
import com.playpandora.buygui.gui.BuyShopGUI;
import com.playpandora.buygui.gui.GeneralCategorySelectorGUI;
import com.playpandora.buygui.gui.QuantitySelectionGUI;
import com.playpandora.buygui.gui.ShopSelectorGUI;
import com.playpandora.buygui.managers.EconomyManager;
import com.playpandora.buygui.managers.PriceManager;
import com.playpandora.buygui.managers.ShopManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class BuyGUI extends JavaPlugin {
    
    private static BuyGUI instance;
    private Object economy;
    private PriceManager priceManager;
    private EconomyManager economyManager;
    private ShopManager shopManager;
    private BuyShopGUI buyShopGUI;
    private ShopSelectorGUI shopSelectorGUI;
    private GeneralCategorySelectorGUI generalCategorySelectorGUI;
    private QuantitySelectionGUI quantitySelectionGUI;
    
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
            shopManager = new ShopManager(this);
            buyShopGUI = new BuyShopGUI(this);
            shopSelectorGUI = new ShopSelectorGUI(this);
            generalCategorySelectorGUI = new GeneralCategorySelectorGUI(this);
            quantitySelectionGUI = new QuantitySelectionGUI(this);
            
            // Register commands with null checks
            BuyCommand buyCommand = new BuyCommand(this);
            if (getCommand("buy") != null) {
                getCommand("buy").setExecutor(buyCommand);
            } else {
                getLogger().warning("Command 'buy' not found in plugin.yml!");
            }
            
            getLogger().info("BuyGUI v" + getDescription().getVersion() + " has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable BuyGUI! " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            // Cancel all tasks
            getServer().getScheduler().cancelTasks(this);
            
            getLogger().info("BuyGUI has been disabled!");
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
    
    public static BuyGUI getInstance() {
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
            java.lang.reflect.Method getBalance = economy.getClass().getMethod("getBalance", org.bukkit.OfflinePlayer.class);
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
            java.lang.reflect.Method withdraw = economy.getClass().getMethod("withdrawPlayer", org.bukkit.OfflinePlayer.class, double.class);
            Object response = withdraw.invoke(economy, player, amount);
            
            // Check if the response indicates success (EconomyResponse)
            if (response != null) {
                try {
                    java.lang.reflect.Method transactionSuccess = response.getClass().getMethod("transactionSuccess");
                    boolean success = (Boolean) transactionSuccess.invoke(response);
                    if (!success) {
                        java.lang.reflect.Method errorMessage = response.getClass().getMethod("errorMessage");
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
    
    public PriceManager getPriceManager() {
        return priceManager;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public ShopManager getShopManager() {
        return shopManager;
    }
    
    public BuyShopGUI getBuyShopGUI() {
        return buyShopGUI;
    }
    
    public ShopSelectorGUI getShopSelectorGUI() {
        return shopSelectorGUI;
    }
    
    public GeneralCategorySelectorGUI getGeneralCategorySelectorGUI() {
        return generalCategorySelectorGUI;
    }
    
    public QuantitySelectionGUI getQuantitySelectionGUI() {
        return quantitySelectionGUI;
    }
    
    public String getMessage(String key) {
        String message = getConfig().getString("messages." + key, "");
        String prefix = getConfig().getString("messages.prefix", "&e&lPandora &8» &r");
        message = message.replace("{prefix}", prefix);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
}

