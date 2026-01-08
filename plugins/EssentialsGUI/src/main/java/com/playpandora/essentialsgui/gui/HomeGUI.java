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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HomeGUI {
    
    private final EssentialsGUI plugin;
    private final Map<UUID, Map<Integer, String>> playerSlotToHomeMap = new HashMap<>();
    
    public HomeGUI(EssentialsGUI plugin) {
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
        
        List<String> homes = hook.getHomes(player);
        if (homes == null || homes.isEmpty()) {
            plugin.getLogger().info("No homes found for player " + player.getName() + " (homes: " + homes + ")");
            String message = "&e&lPandora &8» &7You don't have any homes set. Use &6/sethome &7to create one.";
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            return;
        }
        
        plugin.getLogger().info("Opening home GUI for " + player.getName() + " with " + homes.size() + " homes");
        
        // Calculate optimal size
        int itemCount = homes.size();
        int size = calculateOptimalSizeWithNav(itemCount);
        
        String title = plugin.getConfig().getString("gui.home.title", "&8Homes");
        // Strip any bold formatting from the title
        title = org.bukkit.ChatColor.stripColor(org.bukkit.ChatColor.translateAlternateColorCodes('&', title)).replace(" ", "");
        title = "&8Homes"; // Force &8 style (no bold)
        Inventory inv = Bukkit.createInventory(null, size,
            org.bukkit.ChatColor.translateAlternateColorCodes('&', title));
        
        // Fill borders first
        fillBorders(inv, size, true, true, true);
        
        // Add close button (bottom right)
        int closeSlot = size - 1;
        inv.setItem(closeSlot, createCloseButton());
        
        // Determine if the player has a base home (BaseSystem)
        boolean hasBaseHome = false;
        String baseHomeName = null;
        try {
            org.bukkit.plugin.Plugin baseSystemPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("BaseSystem");
            if (baseSystemPlugin != null) {
                Class<?> baseSystemClass = Class.forName("com.playpandora.basesystem.BaseSystem");
                Object baseSystem = baseSystemClass.getMethod("getInstance").invoke(null);
                if (baseSystem != null) {
                    Object baseManager = baseSystemClass.getMethod("getBaseManager").invoke(baseSystem);
                    if (baseManager != null) {
                        Object base = baseManager.getClass().getMethod("getBase", java.util.UUID.class)
                            .invoke(baseManager, player.getUniqueId());
                        if (base != null) {
                            hasBaseHome = true;
                            // Use base name as the home name shown for the diamond sword
                            baseHomeName = (String) base.getClass().getMethod("getName").invoke(base);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // If BaseSystem is not present or any reflection fails, just skip base home
        }
        
        // Store slot-to-home mapping for this player
        Map<Integer, String> slotMap = new HashMap<>();
        playerSlotToHomeMap.put(player.getUniqueId(), slotMap);
        
        // Add home items - for size 9, use slot 1; for larger inventories, start at slot 10
        int currentSlot = size > 9 ? 10 : 1;
        int maxSlot = size > 9 ? size - 9 : size - 1;
        
        int itemsAdded = 0;
        for (String homeName : homes) {
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
            
            ItemStack item = createHomeItem(homeName, hasBaseHome, baseHomeName);
            if (item != null) {
                inv.setItem(currentSlot, item);
                slotMap.put(currentSlot, homeName); // Store mapping
                itemsAdded++;
                plugin.getLogger().info("Added home item '" + homeName + "' to slot " + currentSlot);
            } else {
                plugin.getLogger().warning("createHomeItem returned null for " + homeName);
            }
            
            currentSlot++;
            // Move to next row if we've filled a row (skip right border)
            if ((currentSlot % 9) == 8) {
                currentSlot += 2; // Skip to next row, past border
            }
        }
        
        plugin.getLogger().info("Total items added to home GUI: " + itemsAdded + " out of " + homes.size());
        
        // Fill remaining empty slots with borders
        fillEmptySlots(inv, size);
        
        player.openInventory(inv);
    }
    
    private ItemStack createHomeItem(String homeName, boolean hasBaseHome, String baseHomeName) {
        boolean isBaseHome = hasBaseHome && baseHomeName != null && baseHomeName.equalsIgnoreCase(homeName);
        
        ItemStack item;
        if (isBaseHome) {
            item = new ItemStack(Material.DIAMOND_SWORD);
        } else {
            item = new ItemStack(Material.RED_BED);
        }
        ItemMeta meta = item.getItemMeta();
        
        if (meta == null) {
            return item;
        }
        
        // Set display name
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&',
            isBaseHome ? "&e&lBase: &6" + homeName : "&6" + homeName));
        
        // Build lore
        List<String> lore = new ArrayList<>();
        if (isBaseHome) {
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Teleport to your base home"));
        } else {
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Teleport to your home"));
        }
        lore.add("");
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Click to teleport"));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    
    public String getHomeNameBySlot(Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        if (item == null || item.getType().isAir()) {
            return null;
        }
        
        // Skip border and close button
        if (item.getType() == Material.GRAY_STAINED_GLASS_PANE || item.getType() == Material.BARRIER) {
            return null;
        }
        
        // Only process bed or diamond sword items (homes)
        if (item.getType() != Material.RED_BED && item.getType() != Material.DIAMOND_SWORD) {
            return null;
        }
        
        // Try to get from stored mapping first (most reliable)
        if (!inv.getViewers().isEmpty() && inv.getViewers().get(0) instanceof org.bukkit.entity.Player player) {
            Map<Integer, String> slotMap = playerSlotToHomeMap.get(player.getUniqueId());
            if (slotMap != null && slotMap.containsKey(slot)) {
                return slotMap.get(slot);
            }
        }
        
        // Fallback: extract from display name
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return null;
        }
        
        String displayName = meta.getDisplayName();
        // Remove all color codes and extract home name
        String homeName = org.bukkit.ChatColor.stripColor(displayName);
        
        // Handle base home format: "Base: HomeName" or just "HomeName"
        if (homeName.startsWith("Base: ")) {
            homeName = homeName.substring(6).trim(); // Remove "Base: " prefix
        }
        
        return homeName.isEmpty() ? null : homeName;
    }
    
    public void clearPlayerMapping(UUID playerUUID) {
        playerSlotToHomeMap.remove(playerUUID);
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


