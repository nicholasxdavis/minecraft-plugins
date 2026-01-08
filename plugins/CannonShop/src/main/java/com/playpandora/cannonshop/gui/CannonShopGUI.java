package com.playpandora.cannonshop.gui;

import com.playpandora.cannonshop.CannonShop;
import com.playpandora.cannonshop.managers.CannonManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CannonShopGUI {
    
    private final CannonShop plugin;
    
    public CannonShopGUI(CannonShop plugin) {
        this.plugin = plugin;
    }
    
    public void openShop(Player player) {
        String title = plugin.getConfig().getString("shop.title", "&8Cannon Shop");
        title = "&8Cannon Shop"; // Force &8 style (no bold)
        
        // Calculate optimal size based on number of items
        int cannonCount = plugin.getCannonManager().getAllCannons().size();
        int specialCount = plugin.getCannonManager().getAllSpecialItems().size();
        int totalCount = cannonCount + specialCount;
        int size = calculateOptimalSizeWithNav(totalCount);
        
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
        
        // Add cannons first
        for (CannonManager.CannonData cannon : plugin.getCannonManager().getAllCannons().values()) {
            if (currentSlot >= maxSlot || currentSlot == closeSlot) {
                break;
            }
            
            // Skip border slots
            int col = currentSlot % 9;
            if (col == 0 || col == 8) {
                currentSlot++;
                continue;
            }
            
            ItemStack item = createCannonItem(cannon, player);
            inv.setItem(currentSlot, item);
            
            currentSlot++;
            // Move to next row if we've filled a row (skip right border)
            if ((currentSlot % 9) == 8) {
                currentSlot += 2; // Skip to next row, past border
            }
        }
        
        // Add special items
        for (CannonManager.SpecialItemData specialItem : plugin.getCannonManager().getAllSpecialItems().values()) {
            if (currentSlot >= maxSlot || currentSlot == closeSlot) {
                break;
            }
            
            // Skip border slots
            int col = currentSlot % 9;
            if (col == 0 || col == 8) {
                currentSlot++;
                continue;
            }
            
            ItemStack item = createSpecialItem(specialItem, player);
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
    
    private ItemStack createCannonItem(CannonManager.CannonData cannon, Player player) {
        Material material = cannon.getMaterial();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set display name
            String displayName = cannon.getName();
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName));
            
            // Build lore
            List<String> lore = new ArrayList<>();
            
            // Add description
            for (String line : cannon.getDescription()) {
                String processed = line.replace("${price}", plugin.getEconomyManager().format(cannon.getPrice()));
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', processed));
            }
            
            lore.add(""); // Empty line
            
            // Check if player can afford it
            boolean canAfford = plugin.getEconomyManager().hasEnough(player, cannon.getPrice());
            boolean hasItem = plugin.getCannonManager().hasPlacerItem(player, cannon.getKey());
            
            lore.add("");
            
            if (hasItem) {
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&a✓ OWNED"));
            } else if (!canAfford) {
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                    "&c✗ INSUFFICIENT FUNDS"));
                double balance = plugin.getEconomyManager().getBalance(player);
                double needed = cannon.getPrice() - balance;
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
    
    private ItemStack createSpecialItem(CannonManager.SpecialItemData specialItem, Player player) {
        Material material = specialItem.getMaterial();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set display name
            String displayName = specialItem.getName();
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName));
            
            // Build lore
            List<String> lore = new ArrayList<>();
            
            // Add description
            for (String line : specialItem.getDescription()) {
                String processed = line.replace("${price}", plugin.getEconomyManager().format(specialItem.getPrice()));
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', processed));
            }
            
            lore.add(""); // Empty line
            
            // Check if player can afford it
            boolean canAfford = plugin.getEconomyManager().hasEnough(player, specialItem.getPrice());
            
            lore.add("");
            
            if (!canAfford) {
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                    "&c✗ INSUFFICIENT FUNDS"));
                double balance = plugin.getEconomyManager().getBalance(player);
                double needed = specialItem.getPrice() - balance;
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

