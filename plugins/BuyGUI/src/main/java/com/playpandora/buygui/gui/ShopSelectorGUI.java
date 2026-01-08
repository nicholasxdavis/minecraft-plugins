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

public class ShopSelectorGUI implements Listener {
    
    private final BuyGUI plugin;
    
    private static final int GENERAL_SHOP_SLOT = 1;
    private static final int SPAWNER_SHOP_SLOT = 2;
    private static final int CANNON_SHOP_SLOT = 3;
    private static final int PET_SHOP_SLOT = 4;
    private static final int FARM_SHOP_SLOT = 5;
    private static final int PERK_SHOP_SLOT = 6;
    private static final int CLOSE_SLOT = 8; // Last slot in single row
    
    public ShopSelectorGUI(BuyGUI plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public void openSelector(Player player) {
        int size = 9; // Single row
        String title = "&8Shop Selector";
        
        Inventory inv = Bukkit.createInventory(null, size,
            org.bukkit.ChatColor.translateAlternateColorCodes('&', title));
        
        // Fill borders (left and right sides only for single row)
        fillBorders(inv, size);
        
        // Add shop options in single row
        inv.setItem(GENERAL_SHOP_SLOT, createGeneralShopButton());
        inv.setItem(SPAWNER_SHOP_SLOT, createSpawnerShopButton());
        inv.setItem(CANNON_SHOP_SLOT, createCannonShopButton());
        inv.setItem(PET_SHOP_SLOT, createPetShopButton());
        inv.setItem(FARM_SHOP_SLOT, createFarmShopButton());
        inv.setItem(PERK_SHOP_SLOT, createPerkShopButton());
        
        // Add close button at the end
        inv.setItem(CLOSE_SLOT, createCloseButton());
        
        // Fill remaining empty slots
        fillEmptySlots(inv, size);
        
        player.openInventory(inv);
    }
    
    private ItemStack createGeneralShopButton() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eGeneral Shop"));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7&m                              "));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Building blocks, materials,"));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7and general items!"));
            lore.add("");
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eClick to open!"));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7&m                              "));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createSpawnerShopButton() {
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eSpawner Shop"));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7&m                              "));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Purchase monster spawners"));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7from Pandora Spawners!"));
            lore.add("");
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eClick to open!"));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7&m                              "));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createCannonShopButton() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eCannon Shop"));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7&m                              "));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Purchase powerful cannons"));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7and special items!"));
            lore.add("");
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eClick to open!"));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7&m                              "));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createPetShopButton() {
        ItemStack item = new ItemStack(Material.BONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&ePet Shop"));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7&m                              "));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Purchase and manage pets!"));
            lore.add("");
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eClick to open!"));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7&m                              "));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createFarmShopButton() {
        ItemStack item = new ItemStack(Material.HAY_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eFarm Shop"));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7&m                              "));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Purchase automated farms!"));
            lore.add("");
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eClick to open!"));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7&m                              "));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createPerkShopButton() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&ePerk Shop"));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7&m                              "));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Purchase powerful perks"));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7and abilities!"));
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
        
        if (!title.equals(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8Shop Selector"))) {
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
        
        // Handle shop selection
        if (slot == GENERAL_SHOP_SLOT) {
            player.closeInventory();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getGeneralCategorySelectorGUI().openSelector(player);
            });
        } else if (slot == SPAWNER_SHOP_SLOT) {
            player.closeInventory();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getBuyShopGUI().openShop(player, BuyShopGUI.ShopType.SPAWNER);
            });
        } else if (slot == CANNON_SHOP_SLOT) {
            player.closeInventory();
            if (plugin.getShopManager().openCannonShop(player)) {
                // Shop opened successfully
            } else {
                player.sendMessage(plugin.getMessage("cannon-shop-unavailable"));
            }
        } else if (slot == PET_SHOP_SLOT) {
            player.closeInventory();
            if (plugin.getShopManager().openPetShop(player)) {
                // Shop opened successfully
            } else {
                player.sendMessage(plugin.getMessage("pet-shop-unavailable"));
            }
        } else if (slot == FARM_SHOP_SLOT) {
            player.closeInventory();
            if (plugin.getShopManager().openFarmShop(player)) {
                // Shop opened successfully
            } else {
                player.sendMessage(plugin.getMessage("farm-shop-unavailable"));
            }
        } else if (slot == PERK_SHOP_SLOT) {
            player.closeInventory();
            if (plugin.getShopManager().openPerkShop(player)) {
                // Shop opened successfully
            } else {
                player.sendMessage(plugin.getMessage("perk-shop-unavailable"));
            }
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

