package com.playpandora.pandoracrates.managers;

import com.playpandora.pandoracrates.PandoraCrates;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class IntegrationManager {
    
    private final PandoraCrates plugin;
    private Object economy; // Vault Economy interface
    
    public IntegrationManager(PandoraCrates plugin) {
        this.plugin = plugin;
        initializeIntegrations();
    }
    
    private void initializeIntegrations() {
        // Initialize Vault economy using reflection
        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            try {
                Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
                org.bukkit.plugin.RegisteredServiceProvider<?> rsp = 
                    plugin.getServer().getServicesManager().getRegistration(economyClass);
                if (rsp != null) {
                    economy = rsp.getProvider();
                    plugin.getLogger().info("Vault economy integration enabled!");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to initialize Vault economy: " + e.getMessage());
            }
        }
    }
    
    public void giveMoney(Player player, double amount) {
        if (economy != null) {
            try {
                java.lang.reflect.Method depositMethod = economy.getClass().getMethod("depositPlayer", Player.class, double.class);
                depositMethod.invoke(economy, player, amount);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to give money through Vault: " + e.getMessage());
                // Fallback: try to use command
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), 
                    "eco give " + player.getName() + " " + amount);
            }
        } else {
            // Fallback: try to use command
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), 
                "eco give " + player.getName() + " " + amount);
        }
    }
    
    public void giveLevels(Player player, int amount) {
        if (plugin.getServer().getPluginManager().getPlugin("LevelPlugin") != null) {
            try {
                Class<?> levelPluginClass = Class.forName("com.playpandora.levelplugin.LevelPlugin");
                Object levelPlugin = plugin.getServer().getPluginManager().getPlugin("LevelPlugin");
                java.lang.reflect.Method getAPIMethod = levelPluginClass.getMethod("getAPI");
                Object levelAPI = getAPIMethod.invoke(levelPlugin);
                
                // Add XP equivalent to levels
                // Assuming 1000 XP per level (adjust as needed)
                java.lang.reflect.Method addXPMethod = levelAPI.getClass().getMethod("addXP", 
                    java.util.UUID.class, double.class);
                addXPMethod.invoke(levelAPI, player.getUniqueId(), amount * 1000.0);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to give levels through LevelPlugin: " + e.getMessage());
            }
        }
    }
    
    public void givePandoraItem(Player player, String type, String itemKey) {
        if (plugin.getServer().getPluginManager().getPlugin("PandoraItems") != null) {
            String command = "pandoraitem give " + type + " " + itemKey + " " + player.getName();
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
        }
    }
    
    public void givePerk(Player player, String perkKey) {
        if (plugin.getServer().getPluginManager().getPlugin("PerkShop") != null) {
            try {
                Class<?> perkShopClass = Class.forName("com.playpandora.perkshop.PerkShop");
                Object perkShop = plugin.getServer().getPluginManager().getPlugin("PerkShop");
                java.lang.reflect.Method getDataManager = perkShopClass.getMethod("getDataManager");
                Object dataManager = getDataManager.invoke(perkShop);
                
                java.lang.reflect.Method addPerk = dataManager.getClass().getMethod("addPerk", 
                    java.util.UUID.class, String.class);
                addPerk.invoke(dataManager, player.getUniqueId(), perkKey);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to give perk through PerkShop: " + e.getMessage());
                // Fallback: use PandoraItems
                givePandoraItem(player, "perk", perkKey);
            }
        } else {
            // Fallback: use PandoraItems
            givePandoraItem(player, "perk", perkKey);
        }
    }
    
    public void givePet(Player player, String petKey) {
        if (plugin.getServer().getPluginManager().getPlugin("PetPlugin") != null) {
            try {
                Class<?> petPluginClass = Class.forName("com.playpandora.petplugin.PetPlugin");
                Object petPlugin = plugin.getServer().getPluginManager().getPlugin("PetPlugin");
                java.lang.reflect.Method getPurchaseManager = petPluginClass.getMethod("getPurchaseManager");
                Object purchaseManager = getPurchaseManager.invoke(petPlugin);
                
                java.lang.reflect.Method purchasePet = purchaseManager.getClass().getMethod("purchasePet", 
                    Player.class, String.class);
                purchasePet.invoke(purchaseManager, player, petKey);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to give pet through PetPlugin: " + e.getMessage());
                // Fallback: use PandoraItems
                givePandoraItem(player, "pet", petKey);
            }
        } else {
            // Fallback: use PandoraItems
            givePandoraItem(player, "pet", petKey);
        }
    }
    
    public void giveKit(Player player, String kitKey) {
        // Try to use PandoraItems first
        givePandoraItem(player, "kit", kitKey);
        
        // If there's a kit plugin, integrate with it here
        // This is a placeholder for future kit plugin integration
    }
    
    public void giveBanknote(Player player, double amount) {
        // Use banknotes plugin command: /note give <player> <amount>
        if (plugin.getServer().getPluginManager().getPlugin("PandoraBanknotes") != null) {
            try {
                // Dispatch the banknote command
                String command = "note give " + player.getName() + " " + amount;
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
                plugin.getLogger().info("Gave banknote of $" + amount + " to " + player.getName() + " via command");
                return;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to give banknote via command: " + e.getMessage());
                // Fallback to direct method call
                try {
                    org.bukkit.plugin.Plugin banknotesPlugin = plugin.getServer().getPluginManager().getPlugin("PandoraBanknotes");
                    if (banknotesPlugin != null && banknotesPlugin.isEnabled()) {
                        Class<?> notesPluginClass = Class.forName("com.sainttx.notes.NotesPlugin");
                        Object notesPluginInstance = banknotesPlugin;
                        
                        java.lang.reflect.Method createBanknoteMethod = notesPluginClass.getMethod("createBanknote", String.class, double.class);
                        org.bukkit.inventory.ItemStack banknote = (org.bukkit.inventory.ItemStack) createBanknoteMethod.invoke(
                            notesPluginInstance, 
                            "CONSOLE",
                            amount
                        );
                        
                        if (banknote != null) {
                            java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> overflow = player.getInventory().addItem(banknote);
                            if (!overflow.isEmpty()) {
                                for (org.bukkit.inventory.ItemStack overflowItem : overflow.values()) {
                                    player.getWorld().dropItemNaturally(player.getLocation(), overflowItem);
                                }
                            }
                            plugin.getLogger().info("Gave banknote of $" + amount + " to " + player.getName() + " via method");
                            return;
                        }
                    }
                } catch (Exception ex) {
                    plugin.getLogger().warning("Failed to give banknote through PandoraBanknotes: " + ex.getMessage());
                }
            }
        }
        
        // Fallback: give money if banknotes plugin is not available
        plugin.getLogger().warning("PandoraBanknotes plugin not found, giving money instead of banknote");
        giveMoney(player, amount);
    }
    
    public void sendNotification(Player player, String message) {
        // Placeholder for hook notifications integration
        player.sendMessage(message);
        
        // If hook plugin exists, send notification through it
        if (plugin.getServer().getPluginManager().getPlugin("Hook") != null) {
            // Integration code here
        }
    }
    
    public void giveSpawner(Player player, String spawnerType, int amount) {
        // Use PandoraSpawners command: /PandoraSpawners give <type> <amount> <player>
        if (plugin.getServer().getPluginManager().getPlugin("PandoraSpawners") != null) {
            // Command structure: PandoraSpawners give <type> <amount> <player>
            String command = "PandoraSpawners give " + spawnerType.toUpperCase() + " " + amount + " " + player.getName();
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
            plugin.getLogger().info("Gave " + amount + " " + spawnerType + " spawner(s) to " + player.getName() + " via command: " + command);
        } else {
            plugin.getLogger().warning("PandoraSpawners plugin not found, cannot give spawner");
        }
    }
    
    public void giveGodset(Player player, String tier) {
        // Use PandoraEnchants command: /pe godkit give <player> <iron|diamond|netherite>
        if (plugin.getServer().getPluginManager().getPlugin("PandoraEnchants") != null) {
            String command = "pe godkit give " + player.getName() + " " + tier.toLowerCase();
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
            plugin.getLogger().info("Gave " + tier + " godset to " + player.getName());
        } else {
            plugin.getLogger().warning("PandoraEnchants plugin not found, cannot give godset");
        }
    }
    
    public void giveCreeperEgg(Player player, int amount) {
        // Use PandoraCeggs command: /ceggs give <player> <amount>
        if (plugin.getServer().getPluginManager().getPlugin("PandoraCeggs") != null) {
            String command = "ceggs give " + player.getName() + " " + amount;
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
            plugin.getLogger().info("Gave " + amount + " creeper egg(s) to " + player.getName());
        } else {
            plugin.getLogger().warning("PandoraCeggs plugin not found, cannot give creeper eggs");
        }
    }
    
    public void giveEnchantedGear(Player player, String gearType, java.util.Map<org.bukkit.enchantments.Enchantment, Integer> enchantments) {
        // Parse gear type (e.g., "netherite_helmet", "diamond_chestplate")
        String[] parts = gearType.toLowerCase().split("_");
        if (parts.length < 2) {
            plugin.getLogger().warning("Invalid gear type: " + gearType);
            return;
        }
        
        String materialName = parts[0] + "_" + parts[1]; // e.g., "netherite_helmet"
        org.bukkit.Material material;
        try {
            material = org.bukkit.Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material: " + materialName);
            return;
        }
        
        // Create the item
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(material, 1);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Set display name
            String displayName = gearType.replace("_", " ");
            displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&6" + displayName));
            
            // Add lore
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Crate Reward"));
            if (enchantments != null && !enchantments.isEmpty()) {
                for (java.util.Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : enchantments.entrySet()) {
                    org.bukkit.enchantments.Enchantment enchant = entry.getKey();
                    int level = entry.getValue();
                    if (enchant != null && level > 0) {
                        try {
                            meta.addEnchant(enchant, level, true);
                            String enchantName = enchant.getKey().getKey().replace("_", " ");
                            enchantName = enchantName.substring(0, 1).toUpperCase() + enchantName.substring(1);
                            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                                "&7" + enchantName + " " + level));
                        } catch (Exception e) {
                            plugin.getLogger().warning("Could not apply enchantment " + enchant.getKey() + ": " + e.getMessage());
                        }
                    }
                }
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        // Give item to player
        java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> overflow = player.getInventory().addItem(item);
        if (!overflow.isEmpty()) {
            for (org.bukkit.inventory.ItemStack overflowItem : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), overflowItem);
            }
        }
        
        plugin.getLogger().info("Gave enchanted " + gearType + " to " + player.getName());
    }
}

