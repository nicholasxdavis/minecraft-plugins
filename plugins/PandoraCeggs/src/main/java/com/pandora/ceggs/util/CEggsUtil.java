package com.pandora.ceggs.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CEggsUtil {
    
    private static final String EGG_NAME = "§eCreeper Egg";
    private static final String NAMESPACE_KEY = "pandoraceggs:custom_egg";
    
    /**
     * Create a custom creeper egg item
     */
    public static ItemStack createCEgg() {
        ItemStack egg = new ItemStack(Material.CREEPER_SPAWN_EGG, 1);
        ItemMeta meta = egg.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(EGG_NAME);
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Right-click while facing air to throw");
            lore.add("§7the creeper egg. It will explode on impact");
            lore.add("§7and can destroy §6ANY blocks§7, including");
            lore.add("§7claimed territory.");
            lore.add("");
            lore.add("§e⚠ Warning: Use with caution!");
            
            meta.setLore(lore);
            
            // Set custom model data to identify it
            meta.setCustomModelData(1001);
            
            egg.setItemMeta(meta);
        }
        
        return egg;
    }
    
    /**
     * Check if an item is a custom creeper egg
     */
    public static boolean isCEgg(ItemStack item) {
        if (item == null || item.getType() != Material.CREEPER_SPAWN_EGG) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        // Check by custom model data
        if (meta.hasCustomModelData() && meta.getCustomModelData() == 1001) {
            return true;
        }
        
        // Check by display name as fallback
        if (meta.hasDisplayName() && meta.getDisplayName().equals(EGG_NAME)) {
            return true;
        }
        
        return false;
    }
}

