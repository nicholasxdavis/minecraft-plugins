package com.playpandora.sellgui.managers;

import com.playpandora.sellgui.SellGUI;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class EconomyManager {
    
    private final SellGUI plugin;
    
    public EconomyManager(SellGUI plugin) {
        this.plugin = plugin;
    }
    
    public void giveMoney(Player player, double amount) {
        Object economy = plugin.getEconomy();
        if (economy != null && amount > 0) {
            try {
                Method depositMethod = economy.getClass().getMethod("depositPlayer", org.bukkit.entity.Player.class, double.class);
                depositMethod.invoke(economy, player, amount);
            } catch (Exception e) {
                plugin.getLogger().warning("Error depositing money: " + e.getMessage());
            }
        }
    }
    
    public double getBalance(Player player) {
        Object economy = plugin.getEconomy();
        if (economy != null) {
            try {
                Method getBalanceMethod = economy.getClass().getMethod("getBalance", org.bukkit.entity.Player.class);
                return (Double) getBalanceMethod.invoke(economy, player);
            } catch (Exception e) {
                plugin.getLogger().warning("Error getting balance: " + e.getMessage());
            }
        }
        return 0.0;
    }
    
    public String formatMoney(double amount) {
        Object economy = plugin.getEconomy();
        if (economy != null) {
            try {
                Method formatMethod = economy.getClass().getMethod("format", double.class);
                return (String) formatMethod.invoke(economy, amount);
            } catch (Exception e) {
                plugin.getLogger().warning("Error formatting money: " + e.getMessage());
            }
        }
        return String.format("$%.2f", amount);
    }
}

