package com.playpandora.pandoraitems.managers;

import com.playpandora.pandoraitems.PandoraItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PerkItemManager {
    
    private final PandoraItems plugin;
    
    public PerkItemManager(PandoraItems plugin) {
        this.plugin = plugin;
    }
    
    public ItemStack createPerkItem(String perkKey) {
        // Try to get perk from PerkShop plugin
        Object perkShop = Bukkit.getPluginManager().getPlugin("PerkShop");
        if (perkShop != null) {
            try {
                // Use reflection to get PerkManager
                Class<?> perkShopClass = perkShop.getClass();
                java.lang.reflect.Method getPerkManager = perkShopClass.getMethod("getPerkManager");
                Object perkManager = getPerkManager.invoke(perkShop);
                
                // Get perk data
                java.lang.reflect.Method getPerk = perkManager.getClass().getMethod("getPerk", String.class);
                Object perkData = getPerk.invoke(perkManager, perkKey);
                
                if (perkData != null) {
                    // Get perk properties
                    java.lang.reflect.Method getName = perkData.getClass().getMethod("getName");
                    java.lang.reflect.Method getMaterial = perkData.getClass().getMethod("getMaterial");
                    java.lang.reflect.Method getDescription = perkData.getClass().getMethod("getDescription");
                    java.lang.reflect.Method getRequiredLevel = perkData.getClass().getMethod("getRequiredLevel");
                    
                    String name = (String) getName.invoke(perkData);
                    String materialStr = (String) getMaterial.invoke(perkData);
                    List<String> description = (List<String>) getDescription.invoke(perkData);
                    int requiredLevel = (Integer) getRequiredLevel.invoke(perkData);
                    
                    Material material = Material.CHEST;
                    try {
                        material = Material.valueOf(materialStr);
                    } catch (IllegalArgumentException e) {
                        material = Material.CHEST;
                    }
                    
                    ItemStack item = new ItemStack(material);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                        List<String> lore = new ArrayList<>();
                        for (String line : description) {
                            lore.add(ChatColor.translateAlternateColorCodes('&', line));
                        }
                        if (requiredLevel > 0) {
                            lore.add("");
                            lore.add(ChatColor.GRAY + "Required Level: " + ChatColor.GOLD + requiredLevel);
                        }
                        lore.add("");
                        lore.add(ChatColor.GRAY + "Right-click to claim this perk");
                        lore.add(ChatColor.GRAY + "PandoraItems:Perk:" + perkKey);
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                    return item;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to create perk item from PerkShop: " + e.getMessage());
            }
        }
        
        // Fallback: create basic item
        return createFallbackPerkItem(perkKey);
    }
    
    private ItemStack createFallbackPerkItem(String perkKey) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6Perk: &e" + perkKey));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Right-click to claim this perk");
            lore.add(ChatColor.GRAY + "PandoraItems:Perk:" + perkKey);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    public boolean isValidPerkKey(String key) {
        Object perkShop = Bukkit.getPluginManager().getPlugin("PerkShop");
        if (perkShop != null) {
            try {
                Class<?> perkShopClass = perkShop.getClass();
                java.lang.reflect.Method getPerkManager = perkShopClass.getMethod("getPerkManager");
                Object perkManager = getPerkManager.invoke(perkShop);
                
                java.lang.reflect.Method getPerk = perkManager.getClass().getMethod("getPerk", String.class);
                Object perkData = getPerk.invoke(perkManager, key);
                return perkData != null;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
    
    public int getRequiredLevel(String perkKey) {
        Object perkShop = Bukkit.getPluginManager().getPlugin("PerkShop");
        if (perkShop != null) {
            try {
                Class<?> perkShopClass = perkShop.getClass();
                java.lang.reflect.Method getPerkManager = perkShopClass.getMethod("getPerkManager");
                Object perkManager = getPerkManager.invoke(perkShop);
                
                java.lang.reflect.Method getPerk = perkManager.getClass().getMethod("getPerk", String.class);
                Object perkData = getPerk.invoke(perkManager, perkKey);
                
                if (perkData != null) {
                    java.lang.reflect.Method getRequiredLevel = perkData.getClass().getMethod("getRequiredLevel");
                    return (Integer) getRequiredLevel.invoke(perkData);
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return 0;
    }
}




