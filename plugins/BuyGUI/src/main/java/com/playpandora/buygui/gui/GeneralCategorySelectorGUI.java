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
import java.util.List;

public class GeneralCategorySelectorGUI implements Listener {
    
    private final BuyGUI plugin;
    
    private static final int BUILDING_SLOT = 1;
    private static final int TNT_REDSTONE_SLOT = 2;
    private static final int FOOD_SLOT = 3;
    private static final int MISC_SLOT = 4;
    private static final int BACK_SLOT = 6;
    private static final int CLOSE_SLOT = 8; // Last slot in single row
    
    public GeneralCategorySelectorGUI(BuyGUI plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public void openSelector(Player player) {
        int size = 9; // Single row
        String title = "&8General Shop";
        
        Inventory inv = Bukkit.createInventory(null, size,
            org.bukkit.ChatColor.translateAlternateColorCodes('&', title));
        
        // Fill borders (left and right sides only for single row)
        fillBorders(inv, size);
        
        // Add category options in single row
        inv.setItem(BUILDING_SLOT, createCategoryButton("building", Material.BRICKS, "&eBuilding Blocks", 
            "&7Building materials and blocks"));
        inv.setItem(TNT_REDSTONE_SLOT, createCategoryButton("tnt_redstone", Material.REDSTONE, "&eTNT/Redstone", 
            "&7TNT, redstone components and mechanisms"));
        inv.setItem(FOOD_SLOT, createCategoryButton("food", Material.APPLE, "&eFood", 
            "&7Food items and consumables"));
        inv.setItem(MISC_SLOT, createCategoryButton("misc", Material.COMPASS, "&eMiscellaneous", 
            "&7Various useful items"));
        
        // Add back button
        inv.setItem(BACK_SLOT, createBackButton());
        
        // Add close button at the end
        inv.setItem(CLOSE_SLOT, createCloseButton());
        
        // Fill remaining empty slots
        fillEmptySlots(inv, size);
        
        player.openInventory(inv);
    }
    
    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e« Back to Shop Selector"));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Click to return to the main shop selection."));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createCategoryButton(String category, Material material, String displayName, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7&m                              "));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', description));
            lore.add("");
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eClick to open!"));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7&m                              "));
            meta.setLore(lore);
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
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        if (!title.equals(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8General Shop"))) {
            return;
        }
        
        event.setCancelled(true);
        
        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        
        // Handle close button
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        
        // Handle back button
        if (slot == BACK_SLOT) {
            player.closeInventory();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getShopSelectorGUI().openSelector(player);
            });
            return;
        }
        
        // Handle category selection
        if (slot == BUILDING_SLOT) {
            player.closeInventory();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getBuyShopGUI().openShop(player, BuyShopGUI.ShopType.GENERAL, "building", 1);
            }, 2L);
        } else if (slot == TNT_REDSTONE_SLOT) {
            player.closeInventory();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player != null && player.isOnline()) {
                    // Open redstone shop directly - ensure category is passed correctly
                    plugin.getBuyShopGUI().openShop(player, BuyShopGUI.ShopType.GENERAL, "tnt_redstone", 1);
                }
            }, 2L);
        } else if (slot == FOOD_SLOT) {
            player.closeInventory();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getBuyShopGUI().openShop(player, BuyShopGUI.ShopType.GENERAL, "food", 1);
            }, 2L);
        } else if (slot == MISC_SLOT) {
            player.closeInventory();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getBuyShopGUI().openShop(player, BuyShopGUI.ShopType.GENERAL, "misc", 1);
            }, 2L);
        }
    }
    
    private void fillBorders(Inventory inv, int size) {
        ItemStack border = createBorder();
        // For single row, only fill left border (slot 0)
        if (size == 9) {
            if (inv.getItem(0) == null) {
                inv.setItem(0, border);
            }
            // Slot 8 has close button, don't fill as border
        } else {
            // For multi-row inventories (fallback)
            int rows = size / 9;
            
            // Top row
            for (int i = 0; i < 9; i++) {
                if (inv.getItem(i) == null) {
                    inv.setItem(i, border);
                }
            }
            
            // Bottom row
            int startBottom = (rows - 1) * 9;
            for (int i = startBottom; i < size; i++) {
                if (inv.getItem(i) == null) {
                    inv.setItem(i, border);
                }
            }
            
            // Left and right sides
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
}

