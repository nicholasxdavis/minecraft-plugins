package com.playpandora.cannonshop.listeners;

import com.playpandora.cannonshop.CannonShop;
import com.playpandora.cannonshop.managers.CannonManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class ShopListener implements Listener {
    
    private final CannonShop plugin;
    
    public ShopListener(CannonShop plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        String title = event.getView().getTitle();
        if (!title.equals(ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfig().getString("shop.title", "&8Cannon Shop")))) {
            return;
        }
        
        event.setCancelled(true);
        
        if (event.getCurrentItem() == null) {
            return;
        }
        
        int slot = event.getSlot();
        
        // Check if close button was clicked
        int inventorySize = event.getInventory().getSize();
        if (slot == inventorySize - 1) {
            player.closeInventory();
            return;
        }
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked.getType().isAir() || !clicked.hasItemMeta() || !clicked.getItemMeta().hasLore()) {
            return;
        }
        
        // Check if it's a cannon
        CannonManager.CannonData cannon = null;
        for (CannonManager.CannonData c : plugin.getCannonManager().getAllCannons().values()) {
            if (clicked.getType() == c.getMaterial()) {
                cannon = c;
                break;
            }
        }
        
        if (cannon != null) {
            handleCannonPurchase(player, cannon);
            return;
        }
        
        // Check if it's a special item
        CannonManager.SpecialItemData specialItem = null;
        for (CannonManager.SpecialItemData s : plugin.getCannonManager().getAllSpecialItems().values()) {
            if (clicked.getType() == s.getMaterial()) {
                specialItem = s;
                break;
            }
        }
        
        if (specialItem != null) {
            handleSpecialItemPurchase(player, specialItem);
            return;
        }
    }
    
    private void handleCannonPurchase(Player player, CannonManager.CannonData cannon) {
        // Check if player already has the placer item
        if (plugin.getCannonManager().hasPlacerItem(player, cannon.getKey())) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&ePandora");
            String message = plugin.getConfig().getString("messages.cannon-already-placed",
                "{prefix} &7You already have a cannon placer in your inventory!")
                .replace("{prefix}", prefix);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            player.closeInventory();
            return;
        }
        
        // Check balance
        if (!plugin.getEconomyManager().hasEnough(player, cannon.getPrice())) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&ePandora");
            String message = plugin.getConfig().getString("messages.insufficient-funds",
                "{prefix} &7You don't have enough money! You need &6${required} &7but you have &6${balance}")
                .replace("{prefix}", prefix)
                .replace("${required}", plugin.getEconomyManager().format(cannon.getPrice()))
                .replace("${balance}", plugin.getEconomyManager().format(plugin.getEconomyManager().getBalance(player)));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            player.closeInventory();
            return;
        }
        
        // Purchase the cannon
        if (plugin.getEconomyManager().withdraw(player, cannon.getPrice())) {
            // Give placer item
            ItemStack placerItem = plugin.getCannonManager().createPlacerItem(cannon.getKey());
            if (placerItem != null) {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(placerItem);
                if (!overflow.isEmpty()) {
                    // Inventory full, drop item
                    for (ItemStack item : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                }
            }
            
            // Log the purchase
            plugin.getLogger().info("Player " + player.getName() + " purchased cannon: " + cannon.getKey() + " (Paste ID: " + cannon.getPasteId() + ")");
            
            // Send success message
            String prefix = plugin.getConfig().getString("messages.prefix", "&ePandora");
            String message = plugin.getConfig().getString("messages.purchase-success",
                "{prefix} &7You purchased &6{item} &7! Check your inventory.")
                .replace("{prefix}", prefix)
                .replace("{item}", ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', cannon.getName())));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            
            player.closeInventory();
        }
    }
    
    private void handleSpecialItemPurchase(Player player, CannonManager.SpecialItemData specialItem) {
        // Check balance
        if (!plugin.getEconomyManager().hasEnough(player, specialItem.getPrice())) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&ePandora");
            String message = plugin.getConfig().getString("messages.insufficient-funds",
                "{prefix} &7You don't have enough money! You need &6${required} &7but you have &6${balance}")
                .replace("{prefix}", prefix)
                .replace("${required}", plugin.getEconomyManager().format(specialItem.getPrice()))
                .replace("${balance}", plugin.getEconomyManager().format(plugin.getEconomyManager().getBalance(player)));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            player.closeInventory();
            return;
        }
        
        // Check if it's creeper eggs and PandoraCeggs is loaded
        if (specialItem.getKey().equals("creeper-eggs")) {
            if (plugin.getServer().getPluginManager().getPlugin("PandoraCeggs") == null) {
                String prefix = plugin.getConfig().getString("messages.prefix", "&ePandora");
                String message = plugin.getConfig().getString("messages.pandoraceggs-not-found",
                    "{prefix} &7PandoraCeggs plugin not found! Creeper eggs unavailable.")
                    .replace("{prefix}", prefix);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                player.closeInventory();
                return;
            }
        }
        
        // Purchase the special item
        if (plugin.getEconomyManager().withdraw(player, specialItem.getPrice())) {
            // Give item
            ItemStack item = plugin.getCannonManager().createSpecialItem(specialItem.getKey());
            if (item != null) {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                if (!overflow.isEmpty()) {
                    // Inventory full, drop item
                    for (ItemStack droppedItem : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), droppedItem);
                    }
                }
            }
            
            // Log the purchase
            plugin.getLogger().info("Player " + player.getName() + " purchased special item: " + specialItem.getKey());
            
            // Send success message
            String prefix = plugin.getConfig().getString("messages.prefix", "&ePandora");
            String message = plugin.getConfig().getString("messages.purchase-success",
                "{prefix} &7You purchased &6{item} &7! Check your inventory.")
                .replace("{prefix}", prefix)
                .replace("{item}", ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', specialItem.getName())));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            
            player.closeInventory();
        }
    }
}

