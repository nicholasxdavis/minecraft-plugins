package com.playpandora.pandoracrates.models;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Reward {
    
    public enum RewardType {
        ITEM,
        MONEY,
        LEVEL,
        COMMAND,
        PANDORA_ITEM_CANNON,
        PANDORA_ITEM_FARM,
        PANDORA_ITEM_PERK,
        PANDORA_ITEM_PET,
        PANDORA_ITEM_KIT,
        BANKNOTE,
        KIT,
        PERK,
        PET,
        SPAWNER,
        GODSET,
        CREEPER_EGG,
        ENCHANTED_GEAR,
        CRATE_KEY
    }
    
    private final RewardType type;
    private Material material;
    private String name;
    private List<String> lore;
    private Map<Enchantment, Integer> enchantments;
    private int amount; // For item amounts
    private double moneyAmount; // For money and banknote amounts
    private String command;
    private String itemKey; // For PandoraItems, kits, perks, pets
    
    // Constructor for programmatic creation
    public Reward(RewardType type, String itemKey, int amount) {
        this.type = type;
        this.itemKey = itemKey;
        this.amount = amount;
        this.moneyAmount = amount;
        this.material = Material.DIAMOND;
        this.name = null;
        this.lore = null;
        this.enchantments = new HashMap<>();
        this.command = null;
    }
    
    // Constructor for creeper eggs (no itemKey needed)
    public Reward(RewardType type, int amount) {
        this.type = type;
        this.itemKey = null;
        this.amount = amount;
        this.moneyAmount = amount;
        this.material = Material.DIAMOND;
        this.name = null;
        this.lore = null;
        this.enchantments = new HashMap<>();
        this.command = null;
    }
    
    public Reward(ConfigurationSection config) {
        String typeStr = config.getString("type", "ITEM").toUpperCase();
        RewardType tempType;
        try {
            tempType = RewardType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            tempType = RewardType.ITEM;
        }
        this.type = tempType;
        
        // For MONEY and BANKNOTE types, use double amount
        if (type == RewardType.MONEY || type == RewardType.BANKNOTE) {
            this.moneyAmount = config.getDouble("amount", 1.0);
            this.amount = (int) this.moneyAmount; // For display purposes
        } else {
            this.amount = config.getInt("amount", 1);
            this.moneyAmount = this.amount; // Default to int value
        }
        
        // Initialize defaults
        this.material = Material.DIAMOND;
        this.name = null;
        this.lore = null;
        this.enchantments = new HashMap<>();
        this.command = null;
        this.itemKey = null;
        
        if (type == RewardType.ITEM || type == RewardType.BANKNOTE) {
            String materialStr = config.getString("material", "DIAMOND");
            try {
                this.material = Material.valueOf(materialStr);
            } catch (IllegalArgumentException e) {
                this.material = Material.DIAMOND;
            }
            this.name = config.getString("name", this.material.name());
            this.lore = config.getStringList("lore");
            
            // Load enchantments
            ConfigurationSection enchantSection = config.getConfigurationSection("enchantments");
            if (enchantSection != null) {
                for (String enchantName : enchantSection.getKeys(false)) {
                    try {
                        Enchantment enchant = null;
                        // Try multiple methods to get enchantment (for compatibility)
                        try {
                            // Try by key first (newer method)
                            enchant = org.bukkit.enchantments.Enchantment.getByKey(
                                org.bukkit.NamespacedKey.minecraft(enchantName.toLowerCase()));
                        } catch (Exception e1) {
                            try {
                                // Try getByName (deprecated but still works)
                                enchant = Enchantment.getByName(enchantName.toUpperCase());
                            } catch (Exception e2) {
                                // Try to find by iterating through all enchantments
                                // Handle common aliases first
                                String searchName = enchantName.toUpperCase();
                                if (searchName.equals("DURABILITY")) {
                                    searchName = "UNBREAKING";
                                }
                                
                                for (Enchantment e : org.bukkit.enchantments.Enchantment.values()) {
                                    String keyName = e.getKey().getKey().toLowerCase();
                                    if (keyName.equalsIgnoreCase(searchName) ||
                                        (e.getName() != null && e.getName().equalsIgnoreCase(searchName))) {
                                        enchant = e;
                                        break;
                                    }
                                }
                            }
                        }
                        
                        if (enchant != null) {
                            enchantments.put(enchant, enchantSection.getInt(enchantName));
                        }
                    } catch (Exception e) {
                        // Ignore invalid enchantments - log will be handled by caller if needed
                    }
                }
            }
        }
        
        if (type == RewardType.COMMAND) {
            this.command = config.getString("command", "");
        }
        
        if (type == RewardType.PANDORA_ITEM_CANNON || 
            type == RewardType.PANDORA_ITEM_FARM ||
            type == RewardType.PANDORA_ITEM_PERK ||
            type == RewardType.PANDORA_ITEM_PET ||
            type == RewardType.PANDORA_ITEM_KIT ||
            type == RewardType.KIT ||
            type == RewardType.PERK ||
            type == RewardType.PET ||
            type == RewardType.SPAWNER ||
            type == RewardType.GODSET ||
            type == RewardType.CRATE_KEY) {
            this.itemKey = config.getString("item-key", "");
        }
        
        if (type == RewardType.ENCHANTED_GEAR) {
            // For ENCHANTED_GEAR, itemKey stores the gear type (e.g., "netherite_helmet", "diamond_chestplate")
            this.itemKey = config.getString("gear-type", "");
            // Load enchantments for gear
            ConfigurationSection enchantSection = config.getConfigurationSection("enchantments");
            if (enchantSection != null) {
                for (String enchantName : enchantSection.getKeys(false)) {
                    try {
                        Enchantment enchant = null;
                        try {
                            enchant = org.bukkit.enchantments.Enchantment.getByKey(
                                org.bukkit.NamespacedKey.minecraft(enchantName.toLowerCase()));
                        } catch (Exception e1) {
                            try {
                                enchant = Enchantment.getByName(enchantName.toUpperCase());
                            } catch (Exception e2) {
                                String searchName = enchantName.toUpperCase();
                                if (searchName.equals("DURABILITY")) {
                                    searchName = "UNBREAKING";
                                }
                                for (Enchantment e : org.bukkit.enchantments.Enchantment.values()) {
                                    String keyName = e.getKey().getKey().toLowerCase();
                                    if (keyName.equalsIgnoreCase(searchName) ||
                                        (e.getName() != null && e.getName().equalsIgnoreCase(searchName))) {
                                        enchant = e;
                                        break;
                                    }
                                }
                            }
                        }
                        if (enchant != null) {
                            enchantments.put(enchant, enchantSection.getInt(enchantName));
                        }
                    } catch (Exception e) {
                        // Ignore invalid enchantments
                    }
                }
            }
        }
    }
    
    public RewardType getType() {
        return type;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public String getName() {
        return name;
    }
    
    public List<String> getLore() {
        return lore;
    }
    
    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }
    
    public int getAmount() {
        return amount;
    }
    
    /**
     * Gets the amount as a double for money and banknote rewards
     * @return The amount as a double
     */
    public double getMoneyAmount() {
        if (type == RewardType.MONEY || type == RewardType.BANKNOTE) {
            return moneyAmount;
        }
        return amount;
    }
    
    public String getCommand() {
        return command;
    }
    
    public String getItemKey() {
        return itemKey;
    }
}

