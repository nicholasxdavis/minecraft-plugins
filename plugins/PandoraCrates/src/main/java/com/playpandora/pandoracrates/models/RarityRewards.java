package com.playpandora.pandoracrates.models;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RarityRewards {
    
    private final String rarity;
    private final int chance;
    private final List<Reward> rewards;
    
    public RarityRewards(String rarity, ConfigurationSection config) {
        this.rarity = rarity;
        if (config != null) {
            this.chance = config.getInt("chance", 0);
        } else {
            this.chance = 0;
        }
        this.rewards = new ArrayList<>();
        
        if (config == null) {
            return; // Empty rarity rewards
        }
        
        // Handle both list and section formats
        if (config.isList("items")) {
            // List format: items: [item1, item2, ...]
            List<?> itemsList = config.getList("items");
            if (itemsList != null) {
                for (Object itemObj : itemsList) {
                    if (itemObj instanceof ConfigurationSection) {
                        Reward reward = new Reward((ConfigurationSection) itemObj);
                        rewards.add(reward);
                    } else if (itemObj instanceof Map) {
                        // Convert Map to ConfigurationSection
                        ConfigurationSection itemSection = config.createSection("temp_" + rewards.size());
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemMap = (Map<String, Object>) itemObj;
                        for (Map.Entry<String, Object> entry : itemMap.entrySet()) {
                            itemSection.set(entry.getKey(), entry.getValue());
                        }
                        Reward reward = new Reward(itemSection);
                        rewards.add(reward);
                    }
                }
            }
        } else {
            // Section format: items: { item1: {...}, item2: {...} }
            ConfigurationSection itemsSection = config.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String key : itemsSection.getKeys(false)) {
                    ConfigurationSection itemConfig = itemsSection.getConfigurationSection(key);
                    if (itemConfig != null) {
                        Reward reward = new Reward(itemConfig);
                        rewards.add(reward);
                    }
                }
            }
        }
    }
    
    public String getRarity() {
        return rarity;
    }
    
    public int getChance() {
        return chance;
    }
    
    public List<Reward> getRewards() {
        return rewards;
    }
}

