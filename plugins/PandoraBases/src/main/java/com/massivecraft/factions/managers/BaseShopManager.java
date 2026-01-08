package com.massivecraft.factions.managers;

import com.massivecraft.factions.FactionsPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseShopManager {
    
    private static final Map<String, ShopItem> shopItems = new HashMap<>();
    
    public static class ShopItem {
        private final String key;
        private final Material material;
        private final String displayName;
        private final List<String> lore;
        private final double price;
        private final Map<Enchantment, Integer> enchantments;
        private final double baseValue;
        
        public ShopItem(String key, Material material, String displayName, List<String> lore, 
                       double price, Map<Enchantment, Integer> enchantments, double baseValue) {
            this.key = key;
            this.material = material;
            this.displayName = displayName;
            this.lore = new ArrayList<>(lore);
            this.price = price;
            this.enchantments = new HashMap<>(enchantments);
            this.baseValue = baseValue;
        }
        
        public String getKey() { return key; }
        public Material getMaterial() { return material; }
        public String getDisplayName() { return displayName; }
        public List<String> getLore() { return lore; }
        public double getPrice() { return price; }
        public Map<Enchantment, Integer> getEnchantments() { return enchantments; }
        public double getBaseValue() { return baseValue; }
        
        public ItemStack createItemStack() {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName));
                List<String> translatedLore = new ArrayList<>();
                for (String line : lore) {
                    translatedLore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(translatedLore);
                item.setItemMeta(meta);
            }
            
            // Add enchantments
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                item.addUnsafeEnchantment(entry.getKey(), entry.getValue());
            }
            
            return item;
        }
    }
    
    public static void loadConfig() {
        shopItems.clear();
        
        File configFile = new File(FactionsPlugin.getInstance().getDataFolder(), "base-shop.yml");
        if (!configFile.exists()) {
            FactionsPlugin.getInstance().saveResource("base-shop.yml", false);
        }
        
        FileConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection itemsSection = config.getConfigurationSection("base-shop.items");
        
        if (itemsSection == null) {
            FactionsPlugin.getInstance().getLogger().warning("No items found in base-shop.yml!");
            return;
        }
        
        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null) continue;
            
            try {
                String materialName = itemSection.getString("material");
                Material material = Material.matchMaterial(materialName);
                if (material == null) {
                    FactionsPlugin.getInstance().getLogger().warning("Invalid material " + materialName + " for item " + key);
                    continue;
                }
                
                String displayName = itemSection.getString("display-name", key);
                List<String> lore = itemSection.getStringList("lore");
                double price = itemSection.getDouble("price", 0.0);
                double baseValue = itemSection.getDouble("base-value", 0.0);
                
                Map<Enchantment, Integer> enchantments = new HashMap<>();
                if (itemSection.contains("enchantments")) {
                    ConfigurationSection enchantSection = itemSection.getConfigurationSection("enchantments");
                    if (enchantSection != null) {
                        for (String enchantKey : enchantSection.getKeys(false)) {
                            try {
                                Enchantment enchant = Enchantment.getByName(enchantKey.toUpperCase());
                                if (enchant != null) {
                                    enchantments.put(enchant, enchantSection.getInt(enchantKey));
                                }
                            } catch (Exception e) {
                                FactionsPlugin.getInstance().getLogger().warning("Invalid enchantment " + enchantKey + " for item " + key);
                            }
                        }
                    }
                }
                
                ShopItem shopItem = new ShopItem(key, material, displayName, lore, price, enchantments, baseValue);
                shopItems.put(key, shopItem);
            } catch (Exception e) {
                FactionsPlugin.getInstance().getLogger().warning("Error loading shop item " + key + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        FactionsPlugin.getInstance().getLogger().info("Loaded " + shopItems.size() + " shop items from base-shop.yml");
    }
    
    public static Map<String, ShopItem> getAllShopItems() {
        return new HashMap<>(shopItems);
    }
    
    public static ShopItem getShopItem(String key) {
        return shopItems.get(key);
    }
    
    public static String getShopTitle() {
        File configFile = new File(FactionsPlugin.getInstance().getDataFolder(), "base-shop.yml");
        FileConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
        return config.getString("base-shop.title", "&8Base Shop");
    }
}

