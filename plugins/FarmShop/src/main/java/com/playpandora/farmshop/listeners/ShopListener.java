package com.playpandora.farmshop.listeners;

import com.playpandora.farmshop.FarmShop;
import com.playpandora.farmshop.managers.FarmManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ShopListener implements Listener {
    
    private final FarmShop plugin;
    private final Map<String, String> shopTitleMap = new HashMap<>();
    
    public ShopListener(FarmShop plugin) {
        this.plugin = plugin;
        String shopTitle = ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfig().getString("shop.title", "&8Farm Shop"));
        shopTitleMap.put(shopTitle, shopTitle);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        String title = event.getView().getTitle();
        if (!title.equals(ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfig().getString("shop.title", "&8Farm Shop")))) {
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
        
        // Find which farm was clicked
        FarmManager.FarmData farm = null;
        for (FarmManager.FarmData f : plugin.getFarmManager().getAllFarms().values()) {
            if (clicked.getType() == f.getMaterial()) {
                farm = f;
                break;
            }
        }
        
        if (farm == null) {
            return;
        }
        
        // Check if player already has the placer item
        if (plugin.getFarmManager().hasPlacerItem(player, farm.getKey())) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&ePandora");
            String message = plugin.getConfig().getString("messages.farm-already-placed",
                "{prefix} &7You already have a farm placer in your inventory!")
                .replace("{prefix}", prefix);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            player.closeInventory();
            return;
        }
        
        // Check level
        if (!plugin.getLevelManager().hasLevel(player, farm.getRequiredLevel())) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&ePandora");
            String message = plugin.getConfig().getString("messages.insufficient-level",
                "{prefix} &7You need to be level &6{level} &7to purchase this farm. You are level &6{current}")
                .replace("{prefix}", prefix)
                .replace("{level}", String.valueOf(farm.getRequiredLevel()))
                .replace("{current}", String.valueOf(plugin.getLevelManager().getLevel(player)));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            player.closeInventory();
            return;
        }
        
        // Check balance
        if (!plugin.getEconomyManager().hasEnough(player, farm.getPrice())) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&ePandora");
            String message = plugin.getConfig().getString("messages.insufficient-funds",
                "{prefix} &7You don't have enough money! You need &6${required} &7but you have &6${balance}")
                .replace("{prefix}", prefix)
                .replace("{required}", plugin.getEconomyManager().format(farm.getPrice()))
                .replace("{balance}", plugin.getEconomyManager().format(plugin.getEconomyManager().getBalance(player)));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            player.closeInventory();
            return;
        }
        
        // Purchase the farm
        if (plugin.getEconomyManager().withdraw(player, farm.getPrice())) {
            // Give placer item
            ItemStack placerItem = plugin.getFarmManager().createPlacerItem(farm.getKey());
            if (placerItem != null) {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(placerItem);
                if (!overflow.isEmpty()) {
                    // Inventory full, drop item
                    for (ItemStack item : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                }
            }
            
            // Save farm purchase to PlayerDataManager
            if (plugin.getPlayerDataManager() != null) {
                String farmId = farm.getKey() + "_" + System.currentTimeMillis(); // Unique farm ID
                plugin.getPlayerDataManager().addFarm(player.getUniqueId(), farmId);
            }
            
            // Log the purchase - the paste command will be executed when they right-click the item
            plugin.getLogger().info("Player " + player.getName() + " purchased farm: " + farm.getKey() + " (Paste ID: " + farm.getPasteId() + ")");
            
            // Send success message
            String prefix = plugin.getConfig().getString("messages.prefix", "&ePandora");
            String message = plugin.getConfig().getString("messages.purchase-success",
                "{prefix} &7You purchased &6{farm} &7farm! Check your inventory for the placer item.")
                .replace("{prefix}", prefix)
                .replace("{farm}", ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', farm.getName())));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            
            player.closeInventory();
        }
    }
}

