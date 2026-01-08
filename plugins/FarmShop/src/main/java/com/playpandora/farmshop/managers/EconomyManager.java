package com.playpandora.farmshop.managers;

import com.playpandora.farmshop.FarmShop;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {
    
    private final FarmShop plugin;
    private Economy economy;
    private boolean economyAvailable;
    
    public EconomyManager(FarmShop plugin) {
        this.plugin = plugin;
        setupEconomy();
    }
    
    private void setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            economyAvailable = false;
            return;
        }
        
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            economyAvailable = false;
            return;
        }
        
        economy = rsp.getProvider();
        economyAvailable = true;
    }
    
    public boolean isEconomyAvailable() {
        return economyAvailable && economy != null;
    }
    
    public double getBalance(Player player) {
        if (!isEconomyAvailable()) {
            return 0.0;
        }
        return economy.getBalance(player);
    }
    
    public boolean hasEnough(Player player, double amount) {
        return getBalance(player) >= amount;
    }
    
    public boolean withdraw(Player player, double amount) {
        if (!isEconomyAvailable()) {
            return false;
        }
        if (!hasEnough(player, amount)) {
            return false;
        }
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }
    
    public String format(double amount) {
        if (!isEconomyAvailable()) {
            return String.format("%.2f", amount);
        }
        return economy.format(amount);
    }
}


