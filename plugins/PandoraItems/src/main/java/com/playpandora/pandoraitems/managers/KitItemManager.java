package com.playpandora.pandoraitems.managers;

import com.playpandora.pandoraitems.PandoraItems;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KitItemManager {
    
    private final PandoraItems plugin;
    private Set<String> validKitKeys;
    
    public KitItemManager(PandoraItems plugin) {
        this.plugin = plugin;
        loadKits();
    }
    
    private void loadKits() {
        validKitKeys = new HashSet<>();
        FileConfiguration config = plugin.getConfig();
        
        // Load kits from config
        List<String> kits = config.getStringList("items.kits");
        if (kits != null && !kits.isEmpty()) {
            validKitKeys.addAll(kits);
            plugin.getLogger().info("Loaded " + kits.size() + " kit(s): " + String.join(", ", kits));
        } else {
            // Fallback: add default kits if config is empty
            validKitKeys.add("default");
            validKitKeys.add("onetime");
            validKitKeys.add("build");
            validKitKeys.add("keys");
            validKitKeys.add("cannon");
            validKitKeys.add("obsidian");
            validKitKeys.add("pandora");
            plugin.getLogger().info("No kits found in config, using default kits");
        }
    }
    
    public void reload() {
        loadKits();
    }
    
    public ItemStack createKitItem(String kitKey) {
        // Create a kit item that integrates with EssentialsX
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6Kit: &e" + kitKey));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Right-click to claim this kit"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Uses EssentialsX kit system"));
            lore.add("");
            lore.add(ChatColor.GRAY + "PandoraItems:Kit:" + kitKey);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    public boolean isValidKitKey(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        return validKitKeys.contains(key.toLowerCase());
    }
    
    public int getRequiredLevel(String kitKey) {
        // No level requirement for kits
        // Level requirements are handled by EssentialsX
        return 0;
    }
    
    public Set<String> getValidKitKeys() {
        return new HashSet<>(validKitKeys);
    }
}

