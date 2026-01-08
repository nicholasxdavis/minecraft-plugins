package com.playpandora.cannonshop.managers;

import com.playpandora.cannonshop.CannonShop;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CannonManager {
    
    private final CannonShop plugin;
    private final Map<String, CannonData> cannons = new HashMap<>();
    private final Map<String, SpecialItemData> specialItems = new HashMap<>();
    
    public CannonManager(CannonShop plugin) {
        this.plugin = plugin;
        loadCannons();
        loadSpecialItems();
    }
    
    private void loadCannons() {
        ConfigurationSection cannonsSection = plugin.getConfig().getConfigurationSection("cannons");
        if (cannonsSection == null) {
            return;
        }
        
        for (String key : cannonsSection.getKeys(false)) {
            ConfigurationSection cannonSection = cannonsSection.getConfigurationSection(key);
            if (cannonSection == null) {
                continue;
            }
            
            String name = cannonSection.getString("name", "Cannon");
            List<String> description = cannonSection.getStringList("description");
            double price = cannonSection.getDouble("price", 0.0);
            String pasteId = cannonSection.getString("paste-id", "");
            
            ConfigurationSection itemSection = cannonSection.getConfigurationSection("item");
            Material material = Material.TNT;
            String displayName = "Cannon Placer";
            List<String> lore = new ArrayList<>();
            
            if (itemSection != null) {
                try {
                    material = Material.valueOf(itemSection.getString("material", "TNT"));
                } catch (IllegalArgumentException e) {
                    material = Material.TNT;
                }
                displayName = itemSection.getString("display-name", "Cannon Placer");
                lore = itemSection.getStringList("lore");
            }
            
            cannons.put(key, new CannonData(key, name, description, price, pasteId, material, displayName, lore));
        }
    }
    
    private void loadSpecialItems() {
        ConfigurationSection specialSection = plugin.getConfig().getConfigurationSection("special-items");
        if (specialSection == null) {
            return;
        }
        
        for (String key : specialSection.getKeys(false)) {
            ConfigurationSection itemSection = specialSection.getConfigurationSection(key);
            if (itemSection == null) {
                continue;
            }
            
            String name = itemSection.getString("name", "Special Item");
            List<String> description = itemSection.getStringList("description");
            double price = itemSection.getDouble("price", 0.0);
            int amount = itemSection.getInt("amount", 1);
            
            ConfigurationSection itemConfigSection = itemSection.getConfigurationSection("item");
            Material material = Material.CREEPER_SPAWN_EGG;
            String displayName = "Special Item";
            List<String> lore = new ArrayList<>();
            
            if (itemConfigSection != null) {
                try {
                    material = Material.valueOf(itemConfigSection.getString("material", "CREEPER_SPAWN_EGG"));
                } catch (IllegalArgumentException e) {
                    material = Material.CREEPER_SPAWN_EGG;
                }
                displayName = itemConfigSection.getString("display-name", "Special Item");
                lore = itemConfigSection.getStringList("lore");
            }
            
            specialItems.put(key, new SpecialItemData(key, name, description, price, amount, material, displayName, lore));
        }
    }
    
    public CannonData getCannon(String key) {
        return cannons.get(key);
    }
    
    public Map<String, CannonData> getAllCannons() {
        return cannons;
    }
    
    public SpecialItemData getSpecialItem(String key) {
        return specialItems.get(key);
    }
    
    public Map<String, SpecialItemData> getAllSpecialItems() {
        return specialItems;
    }
    
    public ItemStack createPlacerItem(String cannonKey) {
        CannonData cannon = cannons.get(cannonKey);
        if (cannon == null) {
            return null;
        }
        
        ItemStack item = new ItemStack(cannon.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', cannon.getDisplayName()));
            
            List<String> lore = new ArrayList<>();
            for (String line : cannon.getLore()) {
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
            }
            lore.add(""); // Empty line
            lore.add(org.bukkit.ChatColor.GRAY + "Cannon: " + cannonKey);
            meta.setLore(lore);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    public ItemStack createSpecialItem(String itemKey) {
        SpecialItemData itemData = specialItems.get(itemKey);
        if (itemData == null) {
            return null;
        }
        
        // Check if it's creeper eggs and PandoraCeggs is loaded
        if (itemKey.equals("creeper-eggs")) {
            Plugin ceggsPlugin = Bukkit.getPluginManager().getPlugin("PandoraCeggs");
            if (ceggsPlugin != null) {
                // Use PandoraCeggs to create the egg
                try {
                    Class<?> utilClass = Class.forName("com.pandora.ceggs.util.CEggsUtil");
                    java.lang.reflect.Method createMethod = utilClass.getMethod("createCEgg");
                    ItemStack egg = (ItemStack) createMethod.invoke(null);
                    egg.setAmount(itemData.getAmount());
                    return egg;
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to create creeper egg from PandoraCeggs: " + e.getMessage());
                }
            }
        }
        
        // Fallback: create item manually
        ItemStack item = new ItemStack(itemData.getMaterial(), itemData.getAmount());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', itemData.getDisplayName()));
            
            List<String> lore = new ArrayList<>();
            for (String line : itemData.getLore()) {
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(lore);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    public boolean hasPlacerItem(Player player, String cannonKey) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasLore()) {
                    List<String> lore = meta.getLore();
                    if (lore != null && lore.contains(org.bukkit.ChatColor.GRAY + "Cannon: " + cannonKey)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    public void executePasteCommand(Player player, String pasteId) {
        // Execute console command to paste the cannon
        String command = "paste " + pasteId + " " + player.getName();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
    
    public static class CannonData {
        private final String key;
        private final String name;
        private final List<String> description;
        private final double price;
        private final String pasteId;
        private final Material material;
        private final String displayName;
        private final List<String> lore;
        
        public CannonData(String key, String name, List<String> description, double price,
                         String pasteId, Material material, String displayName, List<String> lore) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.price = price;
            this.pasteId = pasteId;
            this.material = material;
            this.displayName = displayName;
            this.lore = lore;
        }
        
        public String getKey() { return key; }
        public String getName() { return name; }
        public List<String> getDescription() { return description; }
        public double getPrice() { return price; }
        public String getPasteId() { return pasteId; }
        public Material getMaterial() { return material; }
        public String getDisplayName() { return displayName; }
        public List<String> getLore() { return lore; }
    }
    
    public static class SpecialItemData {
        private final String key;
        private final String name;
        private final List<String> description;
        private final double price;
        private final int amount;
        private final Material material;
        private final String displayName;
        private final List<String> lore;
        
        public SpecialItemData(String key, String name, List<String> description, double price,
                              int amount, Material material, String displayName, List<String> lore) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.price = price;
            this.amount = amount;
            this.material = material;
            this.displayName = displayName;
            this.lore = lore;
        }
        
        public String getKey() { return key; }
        public String getName() { return name; }
        public List<String> getDescription() { return description; }
        public double getPrice() { return price; }
        public int getAmount() { return amount; }
        public Material getMaterial() { return material; }
        public String getDisplayName() { return displayName; }
        public List<String> getLore() { return lore; }
    }
}

