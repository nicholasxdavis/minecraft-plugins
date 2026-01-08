package com.playpandora.pandoracrates.managers;

import com.playpandora.pandoracrates.PandoraCrates;
import com.playpandora.pandoracrates.models.Crate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class CrateItemManager {
    
    private final PandoraCrates plugin;
    private static final String CRATE_TYPE_KEY = "pandoracrates_crate_type";
    
    public CrateItemManager(PandoraCrates plugin) {
        this.plugin = plugin;
    }
    
    public ItemStack createCrateItem(String crateId) {
        Crate crate = plugin.getCrateManager().getCrate(crateId);
        if (crate == null) {
            return null;
        }
        
        ItemStack crateItem = new ItemStack(crate.getBlockMaterial(), 1);
        ItemMeta meta = crateItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
                crate.getDisplayName() + " &7(Placeable)"));
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Place this crate block down"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7and use a key to open it!"));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&eRight-click to place"));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Visit &e/crates &7to view all crates!"));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Crate: &6" + crateId));
            
            meta.setLore(lore);
            
            // Store crate ID in NBT
            org.bukkit.persistence.PersistentDataContainer container = meta.getPersistentDataContainer();
            org.bukkit.NamespacedKey crateTypeKey = new org.bukkit.NamespacedKey(plugin, CRATE_TYPE_KEY);
            container.set(crateTypeKey, PersistentDataType.STRING, crateId.toLowerCase());
            
            // Make it identifiable
            meta.setCustomModelData(2000 + crateId.hashCode());
            
            crateItem.setItemMeta(meta);
        }
        
        return crateItem;
    }
    
    public String getCrateType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        org.bukkit.persistence.PersistentDataContainer container = meta.getPersistentDataContainer();
        org.bukkit.NamespacedKey crateTypeKey = new org.bukkit.NamespacedKey(plugin, CRATE_TYPE_KEY);
        
        if (container.has(crateTypeKey, PersistentDataType.STRING)) {
            return container.get(crateTypeKey, PersistentDataType.STRING);
        }
        
        return null;
    }
    
    public boolean isCrateItem(ItemStack item) {
        return getCrateType(item) != null;
    }
}

