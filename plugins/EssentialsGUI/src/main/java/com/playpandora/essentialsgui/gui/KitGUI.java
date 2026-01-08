package com.playpandora.essentialsgui.gui;

import com.playpandora.essentialsgui.EssentialsGUI;
import com.playpandora.essentialsgui.EssentialsHook;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class KitGUI {
    
    private final EssentialsGUI plugin;
    
    public KitGUI(EssentialsGUI plugin) {
        this.plugin = plugin;
    }
    
    public void openGUI(Player player) {
        EssentialsHook hook = plugin.getEssentialsHook();
        
        // Recheck Essentials availability in case it loaded after this plugin
        hook.recheckEssentials();
        
        if (!hook.isEssentialsAvailable()) {
            String message = "&e&lPandora &8» &7Essentials plugin not found!";
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            return;
        }
        
        Collection<String> kits = hook.getKits(player);
        if (kits == null || kits.isEmpty()) {
            plugin.getLogger().info("No kits found for player " + player.getName() + " (kits: " + kits + ")");
            String message = "&e&lPandora &8» &7No kits available.";
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            return;
        }
        
        List<String> kitList = new ArrayList<>(kits);
        plugin.getLogger().info("Opening kit GUI for " + player.getName() + " with " + kitList.size() + " kits");
        
        // Calculate optimal size
        int itemCount = kitList.size();
        int size = calculateOptimalSizeWithNav(itemCount);
        
        String title = "&8Kits";
        Inventory inv = Bukkit.createInventory(null, size,
            org.bukkit.ChatColor.translateAlternateColorCodes('&', title));
        
        // Fill borders first
        fillBorders(inv, size, true, true, true);
        
        // Add close button (bottom right)
        int closeSlot = size - 1;
        inv.setItem(closeSlot, createCloseButton());
        
        // Add kit items - for size 9, use slot 1; for larger inventories, start at slot 10
        int currentSlot = size > 9 ? 10 : 1;
        int maxSlot = size > 9 ? size - 9 : size - 1;
        
        int itemsAdded = 0;
        for (String kitName : kitList) {
            // Find next valid slot (skip borders and close button)
            while (currentSlot < maxSlot && currentSlot != closeSlot) {
                int col = currentSlot % 9;
                // Skip border columns (0 and 8)
                if (col != 0 && col != 8) {
                    break; // Found valid slot
                }
                currentSlot++;
            }
            
            if (currentSlot >= maxSlot || currentSlot == closeSlot) {
                break; // No more valid slots
            }
            
            ItemStack item = createKitItem(kitName);
            if (item != null) {
                inv.setItem(currentSlot, item);
                itemsAdded++;
                plugin.getLogger().info("Added kit item '" + kitName + "' to slot " + currentSlot);
            } else {
                plugin.getLogger().warning("createKitItem returned null for " + kitName);
            }
            
            currentSlot++;
            // Move to next row if we've filled a row (skip right border)
            if ((currentSlot % 9) == 8) {
                currentSlot += 2; // Skip to next row, past border
            }
        }
        
        plugin.getLogger().info("Total items added to kit GUI: " + itemsAdded + " out of " + kitList.size());
        
        // Fill remaining empty slots with borders
        fillEmptySlots(inv, size);
        
        player.openInventory(inv);
    }
    
    private ItemStack createKitItem(String kitName) {
        // Default icon
        Material material = Material.CHEST;
        
        // Try to determine a good icon based on kit name
        String key = kitName.toLowerCase(java.util.Locale.ROOT);
        if (key.contains("starter") || key.contains("beginner")) {
            material = Material.WOODEN_SWORD;
        } else if (key.contains("pvp") || key.contains("combat")) {
            material = Material.DIAMOND_SWORD;
        } else if (key.contains("food") || key.contains("eat")) {
            material = Material.APPLE;
        } else if (key.contains("tools") || key.contains("mining")) {
            material = Material.DIAMOND_PICKAXE;
        } else if (key.contains("builder") || key.contains("build")) {
            material = Material.BRICKS;
        } else if (key.contains("armor") || key.contains("gear")) {
            material = Material.IRON_CHESTPLATE;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta == null) {
            return item;
        }
        
        // Set display name
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&',
            "&6" + kitName));
        
        // Build lore
        List<String> lore = new ArrayList<>();
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Receive this kit"));
        lore.add("");
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8Click to claim"));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    public String getKitNameBySlot(Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        if (item == null || item.getType().isAir()) {
            return null;
        }
        
        // Skip border and close button
        if (item.getType() == Material.GRAY_STAINED_GLASS_PANE || item.getType() == Material.BARRIER) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return null;
        }
        
        String displayName = meta.getDisplayName();
        // Remove color codes and extract kit name
        String kitName = org.bukkit.ChatColor.stripColor(displayName);
        return kitName;
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


