package com.playpandora.pandoraonevsone.integration;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Integration with Minepacks for backpack protection
 */
public class MinepacksIntegration {
    
    private static Boolean enabled = null;
    
    public static boolean isEnabled() {
        if (enabled == null) {
            try {
                Class.forName("at.pcgamingfreaks.Minepacks.Bukkit.API.MinepacksPlugin");
                enabled = true;
            } catch (ClassNotFoundException e) {
                enabled = false;
            }
        }
        return enabled;
    }
    
    public static boolean isBackpackItem(ItemStack item) {
        if (!isEnabled() || item == null) {
            return false;
        }
        
        try {
            Class<?> minepacksPluginClass = Class.forName("at.pcgamingfreaks.Minepacks.Bukkit.API.MinepacksPlugin");
            Object minepacksPlugin = minepacksPluginClass.getMethod("getInstance").invoke(null);
            
            if (minepacksPlugin == null) {
                return false;
            }
            
            java.lang.reflect.Method isBackpackItemMethod = minepacksPluginClass.getMethod("isBackpackItem", ItemStack.class);
            Boolean result = (Boolean) isBackpackItemMethod.invoke(minepacksPlugin, item);
            
            return result != null && result;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static boolean isBackpackInventory(Inventory inventory) {
        if (!isEnabled() || inventory == null) {
            return false;
        }
        
        try {
            Class<?> backpackClass = Class.forName("at.pcgamingfreaks.Minepacks.Bukkit.API.Backpack");
            java.lang.reflect.Method isBackpackMethod = backpackClass.getMethod("isBackpack", Inventory.class);
            Boolean result = (Boolean) isBackpackMethod.invoke(null, inventory);
            
            if (result != null && result) {
                return true;
            }
            
            if (inventory.getHolder() != null) {
                return backpackClass.isInstance(inventory.getHolder());
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static boolean isBackpackHolder(Object holder) {
        if (!isEnabled() || holder == null) {
            return false;
        }
        
        try {
            Class<?> backpackClass = Class.forName("at.pcgamingfreaks.Minepacks.Bukkit.API.Backpack");
            return backpackClass.isInstance(holder);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get backpack item from player's inventory
     */
    public static ItemStack getBackpackItem(Player player) {
        if (!isEnabled()) {
            return null;
        }
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !item.getType().isAir() && isBackpackItem(item)) {
                return item.clone();
            }
        }
        
        return null;
    }
}


