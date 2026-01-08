package com.playpandora.hook.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for creating Hypixel-style GUIs with consistent design
 * Color scheme: Gray (background), Orange (accents), Yellow (highlights)
 */
public class GUIUtils {
    
    // Border materials
    private static final Material BORDER_MATERIAL = Material.GRAY_STAINED_GLASS_PANE;
    private static final Material ACCENT_MATERIAL = Material.ORANGE_STAINED_GLASS_PANE;
    
    /**
     * Creates a decorative border item (gray glass pane)
     */
    public static ItemStack createBorder() {
        ItemStack item = new ItemStack(BORDER_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Creates an accent border item (orange glass pane)
     */
    public static ItemStack createAccentBorder() {
        ItemStack item = new ItemStack(ACCENT_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Creates a close button with Hypixel-style design
     */
    public static ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&c&l✖ Close"));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Click to close this menu"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Creates a back button
     */
    public static ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&l← Back"));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Click to go back"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Creates a next page button
     */
    public static ItemStack createNextButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lNext →"));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Click to go to the next page"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Creates a previous page button
     */
    public static ItemStack createPreviousButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&l← Previous"));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Click to go to the previous page"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Calculates the optimal inventory size based on number of items
     * Uses smallest size possible (9, 18, 27, 36, 45, 54)
     */
    public static int calculateOptimalSize(int itemCount) {
        if (itemCount <= 9) return 9;   // 1 row
        if (itemCount <= 18) return 18; // 2 rows
        if (itemCount <= 27) return 27; // 3 rows
        if (itemCount <= 36) return 36; // 4 rows
        if (itemCount <= 45) return 45; // 5 rows
        return 54; // 6 rows (max)
    }
    
    /**
     * Calculates the optimal size with space for navigation buttons
     */
    public static int calculateOptimalSizeWithNav(int itemCount) {
        // Add extra space for navigation (close button, etc.)
        int neededSlots = itemCount + 1; // +1 for close button
        return calculateOptimalSize(neededSlots);
    }
    
    /**
     * Fills borders of an inventory with decorative glass panes
     * @param inv The inventory to fill
     * @param size The size of the inventory
     * @param fillTop Whether to fill the top row
     * @param fillBottom Whether to fill the bottom row
     * @param fillSides Whether to fill the side columns
     */
    public static void fillBorders(org.bukkit.inventory.Inventory inv, int size, 
                                   boolean fillTop, boolean fillBottom, boolean fillSides) {
        ItemStack border = createBorder();
        int rows = size / 9;
        
        // Fill top row
        if (fillTop && rows > 0) {
            for (int i = 0; i < 9; i++) {
                if (inv.getItem(i) == null) {
                    inv.setItem(i, border);
                }
            }
        }
        
        // Fill bottom row
        if (fillBottom && rows > 0) {
            int startBottom = (rows - 1) * 9;
            for (int i = startBottom; i < size; i++) {
                if (inv.getItem(i) == null) {
                    inv.setItem(i, border);
                }
            }
        }
        
        // Fill side columns
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
    
    /**
     * Fills all empty slots with border items
     */
    public static void fillEmptySlots(org.bukkit.inventory.Inventory inv, int size) {
        ItemStack border = createBorder();
        for (int i = 0; i < size; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, border);
            }
        }
    }
    
    /**
     * Formats money with color codes
     */
    public static String formatMoney(double amount) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', 
            String.format("&6$%.2f", amount));
    }
    
    /**
     * Creates a separator line for lore
     */
    public static String createSeparator() {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7&m                              ");
    }
    
    /**
     * Creates a status line (owned, available, etc.)
     */
    public static String createStatusLine(String status, boolean positive) {
        String color = positive ? "&a" : "&c";
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', 
            color + "&l" + status);
    }
}








