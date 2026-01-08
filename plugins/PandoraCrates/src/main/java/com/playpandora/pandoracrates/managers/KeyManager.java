package com.playpandora.pandoracrates.managers;

import com.playpandora.pandoracrates.PandoraCrates;
import com.playpandora.pandoracrates.models.Crate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class KeyManager {
    
    private final PandoraCrates plugin;
    private static final String KEY_TYPE_KEY = "pandoracrates_key_type";
    
    public KeyManager(PandoraCrates plugin) {
        this.plugin = plugin;
    }
    
    public ItemStack createKey(String crateId, int amount) {
        Crate crate = plugin.getCrateManager().getCrate(crateId);
        if (crate == null) {
            return null;
        }
        
        ItemStack key = new ItemStack(crate.getKeyMaterial(), amount);
        ItemMeta meta = key.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', crate.getKeyName()));
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Used to open the &6" + crate.getDisplayName()));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Right-click on the crate to use"));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Visit &e/crates &7to view all crates!"));
            meta.setLore(lore);
            
            // Add enchantment glow to all keys
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            
            // Store crate ID in NBT
            org.bukkit.persistence.PersistentDataContainer container = meta.getPersistentDataContainer();
            org.bukkit.NamespacedKey keyTypeKey = new org.bukkit.NamespacedKey(plugin, KEY_TYPE_KEY);
            container.set(keyTypeKey, PersistentDataType.STRING, crateId.toLowerCase());
            
            // Make unstackable by setting custom model data
            meta.setCustomModelData(1000 + crateId.hashCode());
            
            key.setItemMeta(meta);
        }
        
        return key;
    }
    
    public String getKeyType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        
        org.bukkit.persistence.PersistentDataContainer container = meta.getPersistentDataContainer();
        org.bukkit.NamespacedKey keyTypeKey = new org.bukkit.NamespacedKey(plugin, KEY_TYPE_KEY);
        
        if (container.has(keyTypeKey, PersistentDataType.STRING)) {
            return container.get(keyTypeKey, PersistentDataType.STRING);
        }
        
        return null;
    }
    
    public boolean isKey(ItemStack item) {
        return getKeyType(item) != null;
    }
    
    public boolean isKeyForCrate(ItemStack item, String crateId) {
        String keyType = getKeyType(item);
        return keyType != null && keyType.equalsIgnoreCase(crateId);
    }
    
    public void giveKey(Player player, String crateId, int amount) {
        ItemStack key = createKey(crateId, amount);
        if (key != null) {
            java.util.HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(key);
            if (!overflow.isEmpty()) {
                for (ItemStack overflowItem : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), overflowItem);
                }
            }
        }
    }
}

