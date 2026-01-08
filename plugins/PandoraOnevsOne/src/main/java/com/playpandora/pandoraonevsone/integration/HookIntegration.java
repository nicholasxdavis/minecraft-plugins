package com.playpandora.pandoraonevsone.integration;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Integration with Hook plugin for notifications
 */
public class HookIntegration {
    
    private static Boolean enabled = null;
    private static Object hookAPI = null;
    
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
    
    public static void sendDuelStart(Player player1, Player player2) {
        if (!isEnabled() || hookAPI == null) {
            return;
        }
        
        try {
            String title = ChatColor.translateAlternateColorCodes('&', "&e&l1v1 Duel Started!");
            String subtitle = ChatColor.translateAlternateColorCodes('&', "&7Fight!");
            
            hookAPI.getClass().getMethod("sendCustom", Player.class, String.class, String.class, 
                int.class, int.class, int.class)
                .invoke(hookAPI, player1, title, subtitle, 300, 3000, 1000);
            
            hookAPI.getClass().getMethod("sendCustom", Player.class, String.class, String.class, 
                int.class, int.class, int.class)
                .invoke(hookAPI, player2, title, subtitle, 300, 3000, 1000);
        } catch (Exception e) {
            // Hook not available or error - silently fail
        }
    }
    
    public static void sendDuelEnd(Player player, String message) {
        if (!isEnabled() || hookAPI == null) {
            return;
        }
        
        try {
            String title = ChatColor.translateAlternateColorCodes('&', "&7Duel Ended");
            String subtitle = ChatColor.translateAlternateColorCodes('&', "&6" + message);
            
            hookAPI.getClass().getMethod("sendCustom", Player.class, String.class, String.class, 
                int.class, int.class, int.class)
                .invoke(hookAPI, player, title, subtitle, 300, 2000, 800);
        } catch (Exception e) {
            // Hook not available or error - silently fail
        }
    }
}


