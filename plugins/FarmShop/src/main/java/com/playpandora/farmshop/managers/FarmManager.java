package com.playpandora.farmshop.managers;

import com.playpandora.farmshop.FarmShop;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FarmManager {
    
    private final FarmShop plugin;
    private final Map<String, FarmData> farms = new HashMap<>();
    
    public FarmManager(FarmShop plugin) {
        this.plugin = plugin;
        loadFarms();
    }
    
    private void loadFarms() {
        ConfigurationSection farmsSection = plugin.getConfig().getConfigurationSection("farms");
        if (farmsSection == null) {
            return;
        }
        
        for (String key : farmsSection.getKeys(false)) {
            ConfigurationSection farmSection = farmsSection.getConfigurationSection(key);
            if (farmSection == null) {
                continue;
            }
            
            String name = farmSection.getString("name", "Farm");
            List<String> description = farmSection.getStringList("description");
            double price = farmSection.getDouble("price", 0.0);
            int requiredLevel = farmSection.getInt("required-level", 0);
            String pasteId = farmSection.getString("paste-id", "");
            
            ConfigurationSection itemSection = farmSection.getConfigurationSection("item");
            Material material = Material.SUGAR_CANE;
            String displayName = "Farm Placer";
            List<String> lore = new ArrayList<>();
            
            if (itemSection != null) {
                try {
                    material = Material.valueOf(itemSection.getString("material", "SUGAR_CANE"));
                } catch (IllegalArgumentException e) {
                    material = Material.SUGAR_CANE;
                }
                displayName = itemSection.getString("display-name", "Farm Placer");
                lore = itemSection.getStringList("lore");
            }
            
            farms.put(key, new FarmData(key, name, description, price, requiredLevel, pasteId, material, displayName, lore));
        }
    }
    
    public FarmData getFarm(String key) {
        return farms.get(key);
    }
    
    public Map<String, FarmData> getAllFarms() {
        return farms;
    }
    
    public ItemStack createPlacerItem(String farmKey) {
        FarmData farm = farms.get(farmKey);
        if (farm == null) {
            return null;
        }
        
        ItemStack item = new ItemStack(farm.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', farm.getDisplayName()));
            
            List<String> lore = new ArrayList<>();
            for (String line : farm.getLore()) {
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
            }
            lore.add(""); // Empty line
            lore.add(org.bukkit.ChatColor.GRAY + "Farm: " + farmKey);
            meta.setLore(lore);
            
            // Add custom NBT or persistent data to identify this as a farm placer
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    public boolean hasPlacerItem(Player player, String farmKey) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasLore()) {
                    List<String> lore = meta.getLore();
                    if (lore != null && lore.contains(org.bukkit.ChatColor.GRAY + "Farm: " + farmKey)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    public void executePasteCommand(Player player, String pasteId) {
        // Execute console command to paste the farm
        String command = "paste " + pasteId + " " + player.getName();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
    
    public static class FarmData {
        private final String key;
        private final String name;
        private final List<String> description;
        private final double price;
        private final int requiredLevel;
        private final String pasteId;
        private final Material material;
        private final String displayName;
        private final List<String> lore;
        
        public FarmData(String key, String name, List<String> description, double price, int requiredLevel,
                       String pasteId, Material material, String displayName, List<String> lore) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.price = price;
            this.requiredLevel = requiredLevel;
            this.pasteId = pasteId;
            this.material = material;
            this.displayName = displayName;
            this.lore = lore;
        }
        
        public String getKey() { return key; }
        public String getName() { return name; }
        public List<String> getDescription() { return description; }
        public double getPrice() { return price; }
        public int getRequiredLevel() { return requiredLevel; }
        public String getPasteId() { return pasteId; }
        public Material getMaterial() { return material; }
        public String getDisplayName() { return displayName; }
        public List<String> getLore() { return lore; }
    }
}


