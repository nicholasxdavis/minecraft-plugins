package com.playpandora.sellgui.managers;

import com.playpandora.sellgui.SellGUI;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

public class PriceManager {
    
    private final SellGUI plugin;
    private final Map<Material, Double> prices;
    
    public PriceManager(SellGUI plugin) {
        this.plugin = plugin;
        this.prices = new HashMap<>();
        loadPrices();
    }
    
    private void loadPrices() {
        prices.clear();
        
        // Load prices from config
        loadPriceSection("prices.crops");
        loadPriceSection("prices.ores");
        loadPriceSection("prices.raw_ores");
        loadPriceSection("prices.farmables");
        loadPriceSection("prices.fish");
        loadPriceSection("prices.wood");
        loadPriceSection("prices.spawner_drops");
        
        plugin.getLogger().info("Loaded " + prices.size() + " item prices");
    }
    
    private void loadPriceSection(String sectionPath) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(sectionPath);
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    Material material = Material.valueOf(key);
                    double price = section.getDouble(key, 0.0);
                    if (price > 0) {
                        prices.put(material, price);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material in config: " + key);
                }
            }
        }
    }
    
    public double getPrice(Material material) {
        return prices.getOrDefault(material, 0.0);
    }
    
    public boolean isSellable(Material material) {
        return prices.containsKey(material) && prices.get(material) > 0;
    }
    
    public Map<Material, Double> getAllPrices() {
        return new HashMap<>(prices);
    }
}


