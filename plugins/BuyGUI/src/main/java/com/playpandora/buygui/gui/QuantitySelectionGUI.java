package com.playpandora.buygui.gui;

import com.playpandora.buygui.BuyGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QuantitySelectionGUI implements Listener {
    
    private final BuyGUI plugin;
    private final Map<UUID, PurchaseData> pendingPurchases = new HashMap<>();
    
    // Slot positions
    private static final int SINGLE_SLOT = 11;
    private static final int STACK_32_SLOT = 13;
    private static final int STACK_64_SLOT = 15;
    private static final int CANCEL_SLOT = 31;
    private static final int ITEM_DISPLAY_SLOT = 4;
    
    public QuantitySelectionGUI(BuyGUI plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public void openQuantitySelection(Player player, ItemStack item, BuyShopGUI.ShopType shopType, String category, double pricePerUnit) {
        Inventory inv = Bukkit.createInventory(null, 36, 
            org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8Select Quantity"));
        
        // Store purchase data
        PurchaseData data = new PurchaseData(item, shopType, category, pricePerUnit);
        pendingPurchases.put(player.getUniqueId(), data);
        
        // Fill with glass panes
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, glass);
        }
        
        // Display item in center
        ItemStack displayItem = item.clone();
        ItemMeta displayMeta = displayItem.getItemMeta();
        if (displayMeta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Price per item: &6" + 
                plugin.getEconomyManager().formatMoney(pricePerUnit)));
            lore.add("");
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Select quantity to purchase"));
            displayMeta.setLore(lore);
            displayItem.setItemMeta(displayMeta);
        }
        inv.setItem(ITEM_DISPLAY_SLOT, displayItem);
        
        // Single item button
        inv.setItem(SINGLE_SLOT, createQuantityButton(1, pricePerUnit, Material.EMERALD));
        
        // 32 stack button
        inv.setItem(STACK_32_SLOT, createQuantityButton(32, pricePerUnit * 32, Material.GOLD_INGOT));
        
        // 64 stack button
        inv.setItem(STACK_64_SLOT, createQuantityButton(64, pricePerUnit * 64, Material.DIAMOND));
        
        // Cancel button
        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&cCancel"));
        List<String> cancelLore = new ArrayList<>();
        cancelLore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Return to shop"));
        cancelMeta.setLore(cancelLore);
        cancel.setItemMeta(cancelMeta);
        inv.setItem(CANCEL_SLOT, cancel);
        
        player.openInventory(inv);
    }
    
    private ItemStack createQuantityButton(int quantity, double totalPrice, Material icon) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
            "&aBuy &6" + quantity + " &7x"));
        List<String> lore = new ArrayList<>();
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Total: &6" + 
            plugin.getEconomyManager().formatMoney(totalPrice)));
        lore.add("");
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eClick to purchase!"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        if (!title.equals(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8Select Quantity"))) {
            return;
        }
        
        event.setCancelled(true);
        
        if (event.getClickedInventory() == null || 
            !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        
        int slot = event.getSlot();
        PurchaseData data = pendingPurchases.get(player.getUniqueId());
        
        if (data == null) {
            player.closeInventory();
            return;
        }
        
        if (slot == CANCEL_SLOT) {
            pendingPurchases.remove(player.getUniqueId());
            player.closeInventory();
            // Reopen shop
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getBuyShopGUI().openShop(player, data.shopType, data.category);
            }, 2L);
            return;
        }
        
        int quantity = 0;
        if (slot == SINGLE_SLOT) {
            quantity = 1;
        } else if (slot == STACK_32_SLOT) {
            quantity = 32;
        } else if (slot == STACK_64_SLOT) {
            quantity = 64;
        } else {
            return;
        }
        
        // Process purchase
        double totalPrice = data.pricePerUnit * quantity;
        double balance = plugin.getEconomyManager().getBalance(player);
        
        if (balance < totalPrice) {
            player.sendMessage(plugin.getMessage("insufficient-funds"));
            return;
        }
        
        // Withdraw money
        if (!plugin.getEconomyManager().withdrawMoney(player, totalPrice)) {
            player.sendMessage(plugin.getMessage("purchase-failed"));
            return;
        }
        
        // Give items - clear custom title/lore unless it's a spawner or special item
        ItemStack toGive = data.item.clone();
        toGive.setAmount(quantity);
        
        // Clear custom title/lore for regular items (keep for spawners and special items)
        Material mat = toGive.getType();
        boolean isSpawner = mat == Material.SPAWNER;
        boolean isSpecial = mat == Material.BEACON || mat == Material.NETHER_STAR || 
                           mat == Material.TOTEM_OF_UNDYING || mat == Material.DRAGON_EGG ||
                           mat == Material.ELYTRA || mat.name().contains("SHULKER_BOX");
        
        if (!isSpawner && !isSpecial) {
            ItemMeta meta = toGive.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(null);
                meta.setLore(null);
                toGive.setItemMeta(meta);
            }
        }
        
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(toGive);
        if (!leftover.isEmpty()) {
            // Inventory full, refund money
            try {
                plugin.getEconomyManager().depositMoney(player, totalPrice);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to refund money: " + e.getMessage());
            }
            player.sendMessage(plugin.getMessage("inventory-full"));
            pendingPurchases.remove(player.getUniqueId());
            player.closeInventory();
            return;
        }
        
        // Success
        String itemName = data.item.getType().name().toLowerCase().replace("_", " ");
        player.sendMessage(plugin.getMessage("purchased-item")
            .replace("{item}", quantity + "x " + itemName)
            .replace("{price}", plugin.getEconomyManager().formatMoney(totalPrice)));
        
        pendingPurchases.remove(player.getUniqueId());
        player.closeInventory();
        
        // Reopen shop after a short delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getBuyShopGUI().openShop(player, data.shopType, data.category);
        }, 5L);
    }
    
    private static class PurchaseData {
        final ItemStack item;
        final BuyShopGUI.ShopType shopType;
        final String category;
        final double pricePerUnit;
        
        PurchaseData(ItemStack item, BuyShopGUI.ShopType shopType, String category, double pricePerUnit) {
            this.item = item;
            this.shopType = shopType;
            this.category = category;
            this.pricePerUnit = pricePerUnit;
        }
    }
}


