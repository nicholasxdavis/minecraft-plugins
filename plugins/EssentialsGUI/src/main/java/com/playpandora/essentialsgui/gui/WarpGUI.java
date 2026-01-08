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

public class WarpGUI {
    
    private final EssentialsGUI plugin;
    
    public WarpGUI(EssentialsGUI plugin) {
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
        
        Collection<String> warps = hook.getWarps();
        if (warps == null || warps.isEmpty()) {
            plugin.getLogger().info("No warps found (warps: " + warps + ")");
            String message = "&e&lPandora &8» &7No warps available.";
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            return;
        }
        
        List<String> warpList = new ArrayList<>(warps);
        
        // Reorder: put spawn first if it exists
        List<String> reorderedWarps = new ArrayList<>();
        String spawnWarp = null;
        for (String warp : warpList) {
            if (warp.equalsIgnoreCase("spawn")) {
                spawnWarp = warp;
            } else {
                reorderedWarps.add(warp);
            }
        }
        if (spawnWarp != null) {
            reorderedWarps.add(0, spawnWarp);
        }
        warpList = reorderedWarps;
        
        plugin.getLogger().info("Opening warp GUI for " + player.getName() + " with " + warpList.size() + " warps");
        
        // Calculate optimal size (use smallest size needed)
        int itemCount = warpList.size();
        int size = calculateOptimalSizeWithNav(itemCount);
        
        String title = "&8Warps";
        Inventory inv = Bukkit.createInventory(null, size,
            org.bukkit.ChatColor.translateAlternateColorCodes('&', title));
        
        // Fill borders first
        fillBorders(inv, size, true, true, true);
        
        // Add close button (bottom right)
        int closeSlot = size - 1;
        inv.setItem(closeSlot, createCloseButton());
        
        // Add warp items - for size 9, use slot 1; for larger inventories, start at slot 10
        int currentSlot = size > 9 ? 10 : 1;
        int maxSlot = size > 9 ? size - 9 : size - 1;
        
        int itemsAdded = 0;
        for (String warpName : warpList) {
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
            
            ItemStack item = createWarpItem(warpName);
            if (item != null) {
                inv.setItem(currentSlot, item);
                itemsAdded++;
                plugin.getLogger().info("Added warp item '" + warpName + "' to slot " + currentSlot);
            } else {
                plugin.getLogger().warning("createWarpItem returned null for " + warpName);
            }
            
            currentSlot++;
            // Move to next row if we've filled a row (skip right border)
            if ((currentSlot % 9) == 8) {
                currentSlot += 2; // Skip to next row, past border
            }
        }
        
        plugin.getLogger().info("Total items added to warp GUI: " + itemsAdded + " out of " + warpList.size());
        
        // Fill remaining empty slots with borders
        fillEmptySlots(inv, size);
        
        player.openInventory(inv);
    }
    
    private ItemStack createWarpItem(String warpName) {
        String key = warpName.toLowerCase(java.util.Locale.ROOT);
        
        // Special warps with hardcoded styling
        if (key.equals("spawn") || key.equals("end") || key.equals("nether") || 
            key.equals("leaderboards") || key.equals("crates") || key.equals("tokens shop") || 
            key.equals("warp pvp") || key.equals("pvp")) {
            return createSpecialWarpItem(warpName, key);
        }
        
        // Default warp item
        Material material = Material.COMPASS;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta == null) {
            return item;
        }
        
        // Set display name
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&',
            "&6" + warpName));
        
        // Build lore
        List<String> lore = new ArrayList<>();
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Teleport to this location"));
        lore.add("");
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Click to warp"));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createSpecialWarpItem(String warpName, String key) {
        Material material = Material.COMPASS;
        String displayName = "&e" + warpName;
        List<String> lore = new ArrayList<>();
        
        switch (key) {
            case "spawn":
                material = Material.GRASS_BLOCK;
                displayName = "&e&lSpawn";
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Teleport to spawn"));
                lore.add("");
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Click to warp"));
                break;
            case "end":
                material = Material.ENDER_EYE;
                displayName = "&6&lWarp End";
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7World Size: 7kx7k"));
                lore.add("");
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Status: &c&lLocked"));
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Unlock Date"));
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&6Monday 5pm"));
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eApr &e20th"));
                lore.add("");
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7For more Info:"));
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7You can do &e/end"));
                break;
            case "nether":
                material = Material.BLAZE_POWDER;
                displayName = "&6&lWarp Nether";
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7World Size: 7kx7k"));
                lore.add("");
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Status: &c&lLocked"));
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Unlock Date"));
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&6Friday 5pm"));
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eFeb &e20th"));
                lore.add("");
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7For more Info:"));
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7You can do &e/end"));
                break;
            case "leaderboards":
                material = Material.GOLDEN_APPLE;
                displayName = "&e&lLeaderboards";
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7View server leaderboards"));
                lore.add("");
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Click to warp"));
                break;
            case "crates":
                material = Material.CHEST;
                displayName = "&e&lCrates";
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Open crates and get rewards"));
                lore.add("");
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7To buy keys visit &6store.pandoramc.net"));
                lore.add("");
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Click to warp"));
                break;
            case "tokens shop":
                material = Material.EMERALD;
                displayName = "&e&lTokens Shop";
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Spend your tokens"));
                lore.add("");
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Click to warp"));
                break;
            case "warp pvp":
            case "pvp":
                material = Material.DIAMOND_SWORD;
                displayName = "&e&lPvP Arena";
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Fight other players"));
                lore.add("");
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Click to warp"));
                break;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta == null) {
            return item;
        }
        
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName));
        meta.setLore(lore);
        item.setItemMeta(meta);
        
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
    
    public String getWarpNameBySlot(Inventory inv, int slot) {
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
        // Remove color codes and extract warp name
        String warpName = org.bukkit.ChatColor.stripColor(displayName);
        
        // Handle special warps that have "&e&l" prefix - extract original name
        // For special warps, we need to map back to the original warp name
        String lowerName = warpName.toLowerCase();
        if (lowerName.equals("spawn")) return "spawn";
        if (lowerName.equals("end")) return "end";
        if (lowerName.equals("nether")) return "nether";
        if (lowerName.equals("leaderboards")) return "leaderboards";
        if (lowerName.equals("crates")) return "crates";
        if (lowerName.equals("tokens shop")) return "tokens shop";
        if (lowerName.equals("pvp arena")) return "pvp";
        if (lowerName.equals("warp pvp")) return "warp pvp";
        
        return warpName;
    }
}

