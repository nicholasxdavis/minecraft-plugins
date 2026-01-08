package com.playpandora.petplugin.gui;

import com.playpandora.petplugin.PetPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PetShopGUI {
    
    private final PetPlugin plugin;
    private final Map<UUID, Map<Integer, String>> playerSlotMappings = new HashMap<>();
    
    public PetShopGUI(PetPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void openShop(Player player) {
        String title = plugin.getConfig().getString("shop.title", "&8Pet Shop");
        // Strip any bold formatting from the title
        title = org.bukkit.ChatColor.stripColor(org.bukkit.ChatColor.translateAlternateColorCodes('&', title)).replace(" ", "");
        title = "&8Pet Shop"; // Force &8 style (no bold)
        
        ConfigurationSection petTypes = plugin.getConfig().getConfigurationSection("pet-types");
        if (petTypes == null) {
            player.sendMessage(plugin.formatMessage("messages.no-pets-configured",
                "{prefix}No pets configured!"));
            return;
        }
        
        // Clear previous slot mapping for this player
        playerSlotMappings.remove(player.getUniqueId());
        Map<Integer, String> slotMapping = new HashMap<>();
        
        // Count enabled pets
        int petCount = 0;
        for (String petType : petTypes.getKeys(false)) {
            ConfigurationSection petConfig = petTypes.getConfigurationSection(petType);
            if (petConfig != null && petConfig.getBoolean("enabled", true)) {
                petCount++;
            }
        }
        
        // Calculate optimal size
        int size = calculateOptimalSizeWithNav(petCount);
        
        Inventory inv = Bukkit.createInventory(null, size,
            org.bukkit.ChatColor.translateAlternateColorCodes('&', title));
        
        // Fill borders first
        fillBorders(inv, size, true, true, true);
        
        // Add close button (bottom right)
        int closeSlot = size - 1;
        inv.setItem(closeSlot, createCloseButton());
        
        // Add pet items
        // Start at slot 1 (after left border) for small inventories, or slot 10 for larger ones
        int startSlot = size > 9 ? 10 : 1;
        int currentSlot = startSlot;
        for (String petType : petTypes.getKeys(false)) {
            ConfigurationSection petConfig = petTypes.getConfigurationSection(petType);
            if (petConfig == null || !petConfig.getBoolean("enabled", true)) {
                continue;
            }
            
            // Use configured slot if valid, otherwise auto-place
            int slot = petConfig.getInt("slot", -1);
            if (slot < 0 || slot >= size || slot == closeSlot) {
                // Auto-place: find next available slot
                // For small inventories (size <= 9), use all slots except borders and close button
                // For larger inventories, skip bottom row
                int maxSlot = size > 9 ? size - 9 : size - 1;
                while (currentSlot < maxSlot) {
                    int row = currentSlot / 9;
                    int col = currentSlot % 9;
                    // Skip side borders (columns 0 and 8)
                    if (col == 0 || col == 8) {
                        currentSlot++;
                        continue;
                    }
                    // Skip top row borders (row 0)
                    if (row == 0 && (col == 0 || col == 8)) {
                        currentSlot++;
                        continue;
                    }
                    // Check if slot is available
                    ItemStack existing = inv.getItem(currentSlot);
                    if (existing == null || existing.getType() == Material.GRAY_STAINED_GLASS_PANE) {
                        slot = currentSlot;
                        currentSlot++;
                        break;
                    }
                    currentSlot++;
                }
                if (slot < 0) break; // No more space
            }
            
            // Store the mapping
            slotMapping.put(slot, petType);
            
            ItemStack item = createPetItem(petType, petConfig, player);
            inv.setItem(slot, item);
        }
        
        // Store the slot mapping for this player
        playerSlotMappings.put(player.getUniqueId(), slotMapping);
        
        // Fill remaining empty slots with borders
        fillEmptySlots(inv, size);
        
        player.openInventory(inv);
    }
    
    
    private ItemStack createPetItem(String petType, ConfigurationSection config, Player player) {
        String materialName = config.getString("material", "BARRIER");
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            material = Material.BARRIER;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta == null) {
            return item;
        }
        
        // Set display name
        String name = config.getString("name", petType);
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', name));
        
        // Build lore with Hypixel-style formatting
        List<String> lore = new ArrayList<>();
        
        // Add description
        List<String> description = config.getStringList("description");
        for (String line : description) {
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7" + line));
        }
        
        lore.add("");
        
        // Add price
        double price = config.getDouble("price", 0.0);
        if (price > 0) {
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&7Price: &6$" + String.format("%.2f", price)));
            lore.add("");
        }
        
        // Check if player owns this pet
        boolean ownsPet = plugin.getDataManager().hasPetType(player.getUniqueId(), petType);
        if (ownsPet) {
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&a&l✓ OWNED"));
        } else {
            boolean canPurchase = true;
            
            // Check level requirement
            int requiredLevel = config.getInt("required-level", 0);
            if (requiredLevel > 0) {
                int playerLevel = getPlayerLevel(player);
                if (playerLevel < requiredLevel) {
                    lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                        "&7Requires Level: &6" + requiredLevel));
                    lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                        "&7Your Level: &c" + playerLevel));
                    lore.add("");
                    canPurchase = false;
                }
            }
            
            lore.add("");
            
            // Check if player can afford it
            if (plugin.getEconomy() != null && price > 0) {
                double balance = plugin.getPlayerBalance(player);
                if (balance >= price && canPurchase) {
                    lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8CLICK TO PURCHASE"));
                } else {
                    if (balance < price) {
                        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&c&l✗ INSUFFICIENT FUNDS"));
                        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                            "&7You need: &6$" + String.format("%.2f", price - balance) + " &7more"));
                    } else {
                        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&c&l✗ REQUIREMENTS NOT MET"));
                    }
                }
            } else {
                if (canPurchase) {
                    lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8CLICK TO PURCHASE"));
                } else {
                    lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&c&l✗ REQUIREMENTS NOT MET"));
                }
            }
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    public String getPetTypeBySlot(Player player, int slot) {
        // First check the player's slot mapping (for auto-placed pets)
        Map<Integer, String> slotMapping = playerSlotMappings.get(player.getUniqueId());
        if (slotMapping != null && slotMapping.containsKey(slot)) {
            return slotMapping.get(slot);
        }
        
        // Fall back to config-based lookup
        ConfigurationSection petTypes = plugin.getConfig().getConfigurationSection("pet-types");
        if (petTypes == null) {
            return null;
        }
        
        for (String petType : petTypes.getKeys(false)) {
            ConfigurationSection petConfig = petTypes.getConfigurationSection(petType);
            if (petConfig != null && petConfig.getInt("slot", -1) == slot) {
                return petType;
            }
        }
        
        return null;
    }
    
    public void clearPlayerMapping(Player player) {
        playerSlotMappings.remove(player.getUniqueId());
    }
    
    private int getPlayerLevel(org.bukkit.entity.Player player) {
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
    
    // GUI utility methods (Hypixel standard)
    private int calculateOptimalSizeWithNav(int itemCount) {
        int neededSlots = itemCount + 1; // +1 for close button
        return calculateOptimalSize(neededSlots);
    }
    
    private int calculateOptimalSize(int itemCount) {
        if (itemCount <= 9) return 9;
        if (itemCount <= 18) return 18;
        if (itemCount <= 27) return 27;
        if (itemCount <= 36) return 36;
        if (itemCount <= 45) return 45;
        return 54;
    }
    
    private void fillBorders(Inventory inv, int size, boolean fillTop, boolean fillBottom, boolean fillSides) {
        ItemStack border = createBorder();
        int rows = size / 9;
        
        if (fillTop && rows > 0) {
            for (int i = 0; i < 9; i++) {
                if (inv.getItem(i) == null) {
                    inv.setItem(i, border);
                }
            }
        }
        
        if (fillBottom && rows > 0) {
            int startBottom = (rows - 1) * 9;
            for (int i = startBottom; i < size; i++) {
                if (inv.getItem(i) == null) {
                    inv.setItem(i, border);
                }
            }
        }
        
        if (fillSides && rows > 0) {
            for (int row = 0; row < rows; row++) {
                int leftSlot = row * 9;
                int rightSlot = row * 9 + 8;
                if (inv.getItem(leftSlot) == null) {
                    inv.setItem(leftSlot, border);
                }
                if (inv.getItem(rightSlot) == null) {
                    inv.setItem(rightSlot, border);
                }
            }
        }
    }
    
    private void fillEmptySlots(Inventory inv, int size) {
        ItemStack border = createBorder();
        for (int i = 0; i < size; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, border);
            }
        }
    }
    
    private ItemStack createBorder() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&c✖ Close"));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Click to close this menu"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}

