package com.playpandora.buygui.managers;

import com.playpandora.buygui.BuyGUI;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class EconomyManager {
    
    private final BuyGUI plugin;
    
    public EconomyManager(BuyGUI plugin) {
        this.plugin = plugin;
    }
    
    public boolean withdrawMoney(Player player, double amount) {
        return plugin.withdrawPlayer(player, amount);
    }
    
    public boolean depositMoney(Player player, double amount) {
        Object economy = plugin.getEconomy();
        if (economy == null) {
            return false;
        }
        try {
            Method depositMethod = economy.getClass().getMethod("depositPlayer", org.bukkit.OfflinePlayer.class, double.class);
            Object response = depositMethod.invoke(economy, player, amount);
            if (response != null) {
                try {
                    Method transactionSuccess = response.getClass().getMethod("transactionSuccess");
                    return (Boolean) transactionSuccess.invoke(response);
                } catch (Exception e) {
                    return true;
                }
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to deposit money: " + e.getMessage());
            return false;
        }
    }
    
    public double getBalance(Player player) {
        return plugin.getPlayerBalance(player);
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

