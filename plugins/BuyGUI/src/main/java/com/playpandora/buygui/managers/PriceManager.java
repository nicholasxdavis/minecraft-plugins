package com.playpandora.buygui.managers;

import com.playpandora.buygui.BuyGUI;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public class PriceManager {
    
    private final BuyGUI plugin;
    private final Map<String, Map<Material, Double>> categoryPrices;
    
    public PriceManager(BuyGUI plugin) {
        this.plugin = plugin;
        this.categoryPrices = new HashMap<>();
        loadPrices();
    }
    
    private void loadPrices() {
        categoryPrices.clear();
        
        // Load prices from config by category - using lowercase keys for matching
        loadPriceSection("prices.general.building", "building");
        loadPriceSection("prices.general.tnt_redstone", "tnt_redstone");
        loadPriceSection("prices.general.food", "food");
        loadPriceSection("prices.general.misc", "misc");
        loadPriceSection("prices.general.misc.spawner_drops", "spawner_drops");
        
        int totalItems = categoryPrices.values().stream().mapToInt(Map::size).sum();
        plugin.getLogger().info("Loaded " + totalItems + " item prices across " + categoryPrices.size() + " categories");
    }
    
    private void loadPriceSection(String sectionPath, String category) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(sectionPath);
        if (section != null) {
            Map<Material, Double> prices = new HashMap<>();
            for (String key : section.getKeys(false)) {
                try {
                    Material material = Material.valueOf(key.toUpperCase());
                    double price = section.getDouble(key, 0.0);
                    if (price > 0) {
                        prices.put(material, price);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in config: " + key);
                }
            }
            if (!prices.isEmpty()) {
                categoryPrices.put(category, prices);
            }
        }
    }
    
    public double getPrice(Material material) {
        // Search all categories
        for (Map<Material, Double> prices : categoryPrices.values()) {
            if (prices.containsKey(material)) {
                return prices.get(material);
            }
        }
        return 0.0;
    }
    
    public double getPrice(Material material, String category) {
        if (category == null) {
            return getPrice(material);
        }
        Map<Material, Double> prices = categoryPrices.get(category);
        if (prices != null) {
            return prices.getOrDefault(material, 0.0);
        }
        return 0.0;
    }
    
    public boolean isBuyable(Material material) {
        return getPrice(material) > 0;
    }
    
    public Map<Material, Double> getAllPrices() {
        Map<Material, Double> allPrices = new HashMap<>();
        for (Map<Material, Double> prices : categoryPrices.values()) {
            allPrices.putAll(prices);
        }
        return allPrices;
    }
    
    public Map<Material, Double> getPricesByCategory(String category) {
        if (category == null) {
            return getAllPrices();
        }
        return new HashMap<>(categoryPrices.getOrDefault(category, new HashMap<>()));
    }
}
