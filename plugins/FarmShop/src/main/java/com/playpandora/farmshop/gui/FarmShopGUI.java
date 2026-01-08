package com.playpandora.farmshop.gui;

import com.playpandora.farmshop.FarmShop;
import com.playpandora.farmshop.managers.FarmManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class FarmShopGUI {
    
    private final FarmShop plugin;
    
    public FarmShopGUI(FarmShop plugin) {
        this.plugin = plugin;
    }
    
    public void openShop(Player player) {
        String title = plugin.getConfig().getString("shop.title", "&8Farm Shop");
        // Strip any bold formatting from the title
        title = org.bukkit.ChatColor.stripColor(org.bukkit.ChatColor.translateAlternateColorCodes('&', title)).replace(" ", "");
        title = "&8Farm Shop"; // Force &8 style (no bold)
        
        // Calculate optimal size based on number of farms
        int farmCount = plugin.getFarmManager().getAllFarms().size();
        int size = calculateOptimalSizeWithNav(farmCount);
        
        Inventory inv = Bukkit.createInventory(null, size, 
            org.bukkit.ChatColor.translateAlternateColorCodes('&', title));
        
        // Fill borders first
        fillBorders(inv, size, true, true, true);
        
        // Add close button (bottom right)
        int closeSlot = size - 1;
        inv.setItem(closeSlot, createCloseButton());
        
        // Start at slot 1 (after left border) for small inventories, or slot 10 for larger ones
        int startSlot = size > 9 ? 10 : 1;
        int currentSlot = startSlot;
        int maxSlot = size > 9 ? size - 9 : size - 1;
        
        for (FarmManager.FarmData farm : plugin.getFarmManager().getAllFarms().values()) {
            if (currentSlot >= maxSlot || currentSlot == closeSlot) {
                break; // Don't go into the bottom row or close button slot
            }
            
            // Skip border slots
            int col = currentSlot % 9;
            if (col == 0 || col == 8) {
                currentSlot++;
                continue;
            }
            
            ItemStack item = createFarmItem(farm, player);
            inv.setItem(currentSlot, item);
            
            currentSlot++;
            // Move to next row if we've filled a row (skip right border)
            if ((currentSlot % 9) == 8) {
                currentSlot += 2; // Skip to next row, past border
            }
        }
        
        // Fill remaining empty slots with borders
        fillEmptySlots(inv, size);
        
        player.openInventory(inv);
    }
    
    
    private ItemStack createFarmItem(FarmManager.FarmData farm, Player player) {
        Material material = farm.getMaterial();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set display name
            String displayName = farm.getName();
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName));
            
            // Build lore
            List<String> lore = new ArrayList<>();
            
            // Add description
            for (String line : farm.getDescription()) {
                String processed = line
                    .replace("${price}", plugin.getEconomyManager().format(farm.getPrice()))
                    .replace("{level}", String.valueOf(farm.getRequiredLevel()));
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', processed));
            }
            
            lore.add(""); // Empty line
            
            // Check if player can afford it
            boolean canAfford = plugin.getEconomyManager().hasEnough(player, farm.getPrice());
            boolean hasLevel = plugin.getLevelManager().hasLevel(player, farm.getRequiredLevel());
            boolean hasItem = plugin.getFarmManager().hasPlacerItem(player, farm.getKey());
            
            lore.add("");
            
            if (hasItem) {
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&a✓ OWNED"));
            } else if (!hasLevel) {
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                    "&c✗ LEVEL REQUIRED"));
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                    "&7Required: &6Level " + farm.getRequiredLevel()));
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                    "&7Your Level: &c" + plugin.getLevelManager().getLevel(player)));
            } else if (!canAfford) {
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                    "&c✗ INSUFFICIENT FUNDS"));
                double balance = plugin.getEconomyManager().getBalance(player);
                double needed = farm.getPrice() - balance;
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                    "&7You need: &6" + plugin.getEconomyManager().format(needed) + " &7more"));
            } else {
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8CLICK TO PURCHASE"));
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
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


