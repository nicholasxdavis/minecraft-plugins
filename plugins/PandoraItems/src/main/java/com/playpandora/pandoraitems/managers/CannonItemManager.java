package com.playpandora.pandoraitems.managers;

import com.playpandora.pandoraitems.PandoraItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CannonItemManager {
    
    private final PandoraItems plugin;
    
    public CannonItemManager(PandoraItems plugin) {
        this.plugin = plugin;
    }
    
    public ItemStack createCannonItem(String cannonKey) {
        // Try to get cannon from CannonShop plugin
        Object cannonShop = Bukkit.getPluginManager().getPlugin("CannonShop");
        if (cannonShop != null) {
            try {
                // Use reflection to get CannonManager
                Class<?> cannonShopClass = cannonShop.getClass();
                java.lang.reflect.Method getCannonManager = cannonShopClass.getMethod("getCannonManager");
                Object cannonManager = getCannonManager.invoke(cannonShop);
                
                // Get cannon data
                java.lang.reflect.Method getCannon = cannonManager.getClass().getMethod("getCannon", String.class);
                Object cannonData = getCannon.invoke(cannonManager, cannonKey);
                
                if (cannonData != null) {
                    // Use CannonShop's createPlacerItem method
                    java.lang.reflect.Method createPlacerItem = cannonManager.getClass().getMethod("createPlacerItem", String.class);
                    ItemStack item = (ItemStack) createPlacerItem.invoke(cannonManager, cannonKey);
                    
                    if (item != null) {
                        // Add PandoraItems identifier
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            List<String> lore = meta.getLore();
                            if (lore == null) {
                                lore = new ArrayList<>();
                            }
                            lore.add(ChatColor.GRAY + "PandoraItems:Cannon:" + cannonKey);
                            meta.setLore(lore);
                            item.setItemMeta(meta);
                        }
                        return item;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to create cannon item from CannonShop: " + e.getMessage());
            }
        }
        
        // Fallback: create basic item
        return createFallbackCannonItem(cannonKey);
    }
    
    private ItemStack createFallbackCannonItem(String cannonKey) {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6Cannon Spawner: &e" + cannonKey));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Right-click to place this cannon");
            lore.add(ChatColor.GRAY + "at your current location!");
            lore.add(""); // Empty line
            lore.add(ChatColor.GRAY + "Cannon: " + cannonKey); // Format expected by CannonShop
            lore.add(ChatColor.GRAY + "PandoraItems:Cannon:" + cannonKey);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    public boolean isValidCannonKey(String key) {
        Object cannonShop = Bukkit.getPluginManager().getPlugin("CannonShop");
        if (cannonShop != null) {
            try {
                Class<?> cannonShopClass = cannonShop.getClass();
                java.lang.reflect.Method getCannonManager = cannonShopClass.getMethod("getCannonManager");
                Object cannonManager = getCannonManager.invoke(cannonShop);
                
                java.lang.reflect.Method getCannon = cannonManager.getClass().getMethod("getCannon", String.class);
                Object cannonData = getCannon.invoke(cannonManager, key);
                return cannonData != null;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
    
    public int getRequiredLevel(String cannonKey) {
        // Cannons don't have level requirements in CannonShop, return 0
        return 0;
    }
}

