package com.playpandora.pandoraminer.integration;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Integration with Hook plugin for notifications
 */
public class HookIntegration {
    
    private static Boolean enabled = null;
    private static Object hookAPI = null;
    
    /**
     * Check if Hook is available and initialize API
     */
    public static boolean isEnabled() {
        if (enabled == null) {
            try {
                org.bukkit.plugin.Plugin hookPlugin = Bukkit.getPluginManager().getPlugin("Hook");
                if (hookPlugin != null && hookPlugin.isEnabled()) {
                    Object hookInstance = hookPlugin.getClass().getMethod("getInstance").invoke(null);
                    hookAPI = hookInstance.getClass().getMethod("getAPI").invoke(hookInstance);
                    enabled = (hookAPI != null);
                } else {
                    enabled = false;
                }
            } catch (Exception e) {
                enabled = false;
            }
        }
        return enabled;
    }
    
    /**
     * Send miner enabled notification
     */
    public static void sendMinerEnabled(Player player) {
        if (!isEnabled() || hookAPI == null) {
            return;
        }
        
        try {
            // Send custom notification using Hook API
            String title = ChatColor.translateAlternateColorCodes('&', "&e&lMiner Mode");
            String subtitle = ChatColor.translateAlternateColorCodes('&', "&7Enabled");
            
            hookAPI.getClass().getMethod("sendCustom", Player.class, String.class, String.class, 
                int.class, int.class, int.class)
                .invoke(hookAPI, player, title, subtitle, 300, 2000, 800);
        } catch (Exception e) {
            // Hook not available or error - silently fail
        }
    }
    
    /**
     * Send miner disabled notification
     */
    public static void sendMinerDisabled(Player player) {
        if (!isEnabled() || hookAPI == null) {
            return;
        }
        
        try {
            // Send custom notification using Hook API
            String title = ChatColor.translateAlternateColorCodes('&', "&7Miner Mode");
            String subtitle = ChatColor.translateAlternateColorCodes('&', "&cDisabled");
            
            hookAPI.getClass().getMethod("sendCustom", Player.class, String.class, String.class, 
                int.class, int.class, int.class)
                .invoke(hookAPI, player, title, subtitle, 300, 2000, 800);
        } catch (Exception e) {
            // Hook not available or error - silently fail
        }
    }
}

