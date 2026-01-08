package com.playpandora.pandoraitems.managers;

import com.playpandora.pandoraitems.PandoraItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class FarmItemManager {
    
    private final PandoraItems plugin;
    
    public FarmItemManager(PandoraItems plugin) {
        this.plugin = plugin;
    }
    
    public ItemStack createFarmItem(String farmKey) {
        // Try to get farm from FarmShop plugin
        Object farmShop = Bukkit.getPluginManager().getPlugin("FarmShop");
        if (farmShop != null) {
            try {
                // Use reflection to get FarmManager
                Class<?> farmShopClass = farmShop.getClass();
                java.lang.reflect.Method getFarmManager = farmShopClass.getMethod("getFarmManager");
                Object farmManager = getFarmManager.invoke(farmShop);
                
                // Get farm data
                java.lang.reflect.Method getFarm = farmManager.getClass().getMethod("getFarm", String.class);
                Object farmData = getFarm.invoke(farmManager, farmKey);
                
                if (farmData != null) {
                    // Use FarmShop's createPlacerItem method
                    java.lang.reflect.Method createPlacerItem = farmManager.getClass().getMethod("createPlacerItem", String.class);
                    ItemStack item = (ItemStack) createPlacerItem.invoke(farmManager, farmKey);
                    
                    if (item != null) {
                        // Add PandoraItems identifier
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            List<String> lore = meta.getLore();
                            if (lore == null) {
                                lore = new ArrayList<>();
                            }
                            lore.add(ChatColor.GRAY + "PandoraItems:Farm:" + farmKey);
                            meta.setLore(lore);
                            item.setItemMeta(meta);
                        }
                        return item;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to create farm item from FarmShop: " + e.getMessage());
            }
        }
        
        // Fallback: create basic item
        return createFallbackFarmItem(farmKey);
    }
    
    private ItemStack createFallbackFarmItem(String farmKey) {
        ItemStack item = new ItemStack(Material.SUGAR_CANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6Farm Spawner: &e" + farmKey));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Right-click to place this farm");
            lore.add(ChatColor.GRAY + "at your current location!");
            lore.add(""); // Empty line
            lore.add(ChatColor.GRAY + "Farm: " + farmKey); // Format expected by FarmShop
            lore.add(ChatColor.GRAY + "PandoraItems:Farm:" + farmKey);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    public boolean isValidFarmKey(String key) {
        Object farmShop = Bukkit.getPluginManager().getPlugin("FarmShop");
        if (farmShop != null) {
            try {
                Class<?> farmShopClass = farmShop.getClass();
                java.lang.reflect.Method getFarmManager = farmShopClass.getMethod("getFarmManager");
                Object farmManager = getFarmManager.invoke(farmShop);
                
                java.lang.reflect.Method getFarm = farmManager.getClass().getMethod("getFarm", String.class);
                Object farmData = getFarm.invoke(farmManager, key);
                return farmData != null;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
    
    public int getRequiredLevel(String farmKey) {
        Object farmShop = Bukkit.getPluginManager().getPlugin("FarmShop");
        if (farmShop != null) {
            try {
                Class<?> farmShopClass = farmShop.getClass();
                java.lang.reflect.Method getFarmManager = farmShopClass.getMethod("getFarmManager");
                Object farmManager = getFarmManager.invoke(farmShop);
                
                java.lang.reflect.Method getFarm = farmManager.getClass().getMethod("getFarm", String.class);
                Object farmData = getFarm.invoke(farmManager, farmKey);
                
                if (farmData != null) {
                    // Get required level from farm data
                    java.lang.reflect.Method getRequiredLevel = farmData.getClass().getMethod("getRequiredLevel");
                    return (Integer) getRequiredLevel.invoke(farmData);
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return 0;
    }
}

