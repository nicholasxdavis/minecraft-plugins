package com.playpandora.petplugin.managers;

import com.playpandora.petplugin.PetPlugin;
import com.playpandora.petplugin.models.Pet;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

public class PurchaseManager {
    
    private final PetPlugin plugin;
    
    public PurchaseManager(PetPlugin plugin) {
        this.plugin = plugin;
    }
    
    public boolean purchasePet(Player player, String petType) {
        UUID uuid = player.getUniqueId();
        
        // Check if already owns this pet type (including permission-based)
        if (plugin.getDataManager().hasPetType(uuid, petType)) {
            String message = plugin.formatMessage("messages.pet-already-owned",
                "{prefix}You already own this pet type.");
            player.sendMessage(message);
            return false;
        }
        
        // Get pet config
        String configPath = "pet-types." + petType;
        if (!plugin.getConfig().contains(configPath)) {
            player.sendMessage(plugin.formatMessage("messages.invalid-pet-type",
                "{prefix}Invalid pet type!"));
            return false;
        }
        
        if (!plugin.getConfig().getBoolean(configPath + ".enabled", true)) {
            player.sendMessage(plugin.formatMessage("messages.pet-disabled",
                "{prefix}This pet type is disabled!"));
            return false;
        }
        
        // Check if player has permission-based unlock (bypasses all requirements)
        boolean hasPermissionUnlock = player.hasPermission("petplugin.pet." + petType) || 
                                      player.hasPermission("petplugin.pet.all");
        
        if (!hasPermissionUnlock) {
            // Check level requirement
            int requiredLevel = plugin.getConfig().getInt(configPath + ".required-level", 0);
            if (requiredLevel > 0) {
                int playerLevel = getPlayerLevel(player);
                if (playerLevel < requiredLevel) {
                    String message = plugin.formatMessage("messages.level-required",
                        "{prefix}You need to be level &6{required} &7to purchase this pet! You are level &6{current}&7.",
                        "required", String.valueOf(requiredLevel),
                        "current", String.valueOf(playerLevel));
                    player.sendMessage(message);
                    return false;
                }
            }
            
            // Check price
            double price = plugin.getConfig().getDouble(configPath + ".price", 0.0);
            if (price > 0) {
                if (plugin.getEconomy() == null) {
                    String message = plugin.formatMessage("messages.economy-not-available",
                        "{prefix}Economy system is not available! Cannot purchase pets.");
                    player.sendMessage(message);
                    plugin.getLogger().warning("Economy is null when trying to purchase pet for " + player.getName());
                    return false;
                }
                
                double balance = plugin.getPlayerBalance(player);
                if (balance < price) {
                    double needed = price - balance;
                    String message = plugin.formatMessage("messages.insufficient-funds",
                        "{prefix}Insufficient funds! You need &6${amount} &7more.",
                        "amount", String.format("%.2f", needed));
                    player.sendMessage(message);
                    return false;
                }
                
                // Withdraw money
                boolean withdrawn = plugin.withdrawPlayer(player, price);
                if (!withdrawn) {
                    String message = plugin.formatMessage("messages.purchase-failed",
                        "{prefix}Failed to process payment! Please try again.");
                    player.sendMessage(message);
                    plugin.getLogger().warning("Failed to withdraw money for " + player.getName() + " when purchasing " + petType);
                    return false;
                }
                
                // Verify withdrawal
                double newBalance = plugin.getPlayerBalance(player);
                if (newBalance >= balance) {
                    // Money wasn't actually withdrawn
                    String message = plugin.formatMessage("messages.purchase-failed",
                        "{prefix}Payment verification failed! Please try again.");
                    player.sendMessage(message);
                    plugin.getLogger().warning("Money withdrawal verification failed for " + player.getName() + " - balance didn't decrease");
                    return false;
                }
            }
        }
        
        // Generate pet name
        String generatedName = generatePetName(petType);
        
        // Create pet
        Pet pet = new Pet(uuid, petType, generatedName);
        
        // Set max health from config
        double maxHealth = plugin.getConfig().getDouble(configPath + ".max-health", 20.0);
        pet.setMaxHealth(maxHealth);
        
        // Save pet
        plugin.getDataManager().addPet(uuid, pet);
        
        // Send message
        String petName = plugin.getConfig().getString(configPath + ".name", petType);
        if (hasPermissionUnlock) {
            String message = plugin.formatMessage("messages.pet-purchased",
                "{prefix}You unlocked &6{pet_name} &7for free!",
                "pet_name", petName);
            player.sendMessage(message);
        } else {
            String message = plugin.formatMessage("messages.pet-purchased",
                "{prefix}You purchased &6{pet_name}&7!",
                "pet_name", petName);
            player.sendMessage(message);
        }
        
        return true;
    }
    
    private String generatePetName(String petType) {
        String[] prefixes = {"Fluffy", "Buddy", "Max", "Luna", "Charlie", "Bella", "Rocky", "Daisy", "Milo", "Zoe"};
        String[] suffixes = {"I", "II", "III", "IV", "V"};
        
        String prefix = prefixes[(int) (Math.random() * prefixes.length)];
        String suffix = suffixes[(int) (Math.random() * suffixes.length)];
        
        return prefix + " " + suffix;
    }
    
    private int getPlayerLevel(Player player) {
        // Check if LevelPlugin is available
        if (org.bukkit.Bukkit.getPluginManager().getPlugin("LevelPlugin") != null) {
            try {
                Class<?> levelPluginClass = Class.forName("com.playpandora.levelplugin.LevelPlugin");
                Method getInstanceMethod = levelPluginClass.getMethod("getInstance");
                Object levelPlugin = getInstanceMethod.invoke(null);
                
                Method getAPIMethod = levelPluginClass.getMethod("getAPI");
                Object api = getAPIMethod.invoke(levelPlugin);
                
                Method getLevelMethod = api.getClass().getMethod("getLevel", org.bukkit.entity.Player.class);
                return (Integer) getLevelMethod.invoke(api, player);
            } catch (Exception e) {
                // LevelPlugin not available or error
                return 0;
            }
        }
        return 0;
    }
}

