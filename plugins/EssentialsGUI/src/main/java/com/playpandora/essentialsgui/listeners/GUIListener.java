package com.playpandora.essentialsgui.listeners;

import com.playpandora.essentialsgui.EssentialsGUI;
import com.playpandora.essentialsgui.gui.HomeGUI;
import com.playpandora.essentialsgui.gui.KitGUI;
import com.playpandora.essentialsgui.gui.WarpGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class GUIListener implements Listener {
    
    private final EssentialsGUI plugin;
    
    public GUIListener(EssentialsGUI plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        Inventory inv = event.getView().getTopInventory();
        String title = event.getView().getTitle();
        
        // Check if it's a warp GUI
        if (title.contains("Warps") || title.equals(org.bukkit.ChatColor.translateAlternateColorCodes('&',
            plugin.getConfig().getString("gui.warp.title", "&8Warps")))) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) {
                return;
            }
            
            // Check for close button
            if (event.getCurrentItem().getType() == org.bukkit.Material.BARRIER) {
                player.closeInventory();
                return;
            }
            
            WarpGUI warpGUI = plugin.getWarpGUI();
            String warpName = warpGUI.getWarpNameBySlot(inv, event.getSlot());
            
            if (warpName != null) {
                player.closeInventory();
                
                // Send Hook notification
                sendTeleportNotification(player, "Warp", warpName);
                
                plugin.getEssentialsHook().teleportToWarp(player, warpName);
                
                String message = "&e&lPandora &8» &7Teleporting to &6" + warpName + "&7...";
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            }
            return;
        }
        
        // Check if it's a home GUI
        if (title.contains("Homes") || title.equals(org.bukkit.ChatColor.translateAlternateColorCodes('&',
            plugin.getConfig().getString("gui.home.title", "&8Homes")))) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) {
                return;
            }
            
            // Check for close button
            if (event.getCurrentItem().getType() == org.bukkit.Material.BARRIER) {
                player.closeInventory();
                return;
            }
            
            HomeGUI homeGUI = plugin.getHomeGUI();
            String homeName = homeGUI.getHomeNameBySlot(inv, event.getSlot());
            
            if (homeName != null) {
                player.closeInventory();
                
                // Send Hook notification
                sendTeleportNotification(player, "Home", homeName);
                
                plugin.getEssentialsHook().teleportToHome(player, homeName);
                
                String message = "&e&lPandora &8» &7Teleporting to &6" + homeName + "&7...";
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            }
            return;
        }
        
        // Check if it's a kit GUI
        if (title.contains("Kits") || title.equals(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8Kits"))) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) {
                return;
            }
            
            // Check for close button
            if (event.getCurrentItem().getType() == org.bukkit.Material.BARRIER) {
                player.closeInventory();
                return;
            }
            
            KitGUI kitGUI = plugin.getKitGUI();
            String kitName = kitGUI.getKitNameBySlot(inv, event.getSlot());
            
            if (kitName != null) {
                player.closeInventory();
                
                plugin.getEssentialsHook().giveKit(player, kitName);
                
                String message = "&e&lPandora &8» &7You have received the &6" + kitName + " &7kit!";
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            }
            return;
        }
        
        // Check if it's a rules GUI
        if (title.contains("Rules") || title.equals(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8Rules"))) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) {
                return;
            }
            
            // Check for close button
            if (event.getCurrentItem().getType() == org.bukkit.Material.BARRIER) {
                player.closeInventory();
                return;
            }
            
            // Rules GUI is read-only, no action needed
        }
    }
    
    private void sendTeleportNotification(Player player, String type, String destination) {
        try {
            org.bukkit.plugin.Plugin hookPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("Hook");
            if (hookPlugin != null && hookPlugin.isEnabled()) {
                Object hookInstance = hookPlugin.getClass().getMethod("getInstance").invoke(null);
                Object hookAPI = hookInstance.getClass().getMethod("getAPI").invoke(hookInstance);
                hookAPI.getClass().getMethod("sendCustom", org.bukkit.entity.Player.class, 
                    String.class, String.class, int.class, int.class, int.class)
                    .invoke(hookAPI, player, "Teleporting...", 
                        "Arrived at " + type + ": " + destination, 500, 2500, 1000);
            }
        } catch (Exception e) {
            // Silently fail if Hook is not available
        }
    }
}

