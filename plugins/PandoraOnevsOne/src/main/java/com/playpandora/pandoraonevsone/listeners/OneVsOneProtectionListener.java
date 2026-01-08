package com.playpandora.pandoraonevsone.listeners;

import com.playpandora.pandoraonevsone.PandoraOnevsOne;
import com.playpandora.pandoraonevsone.integration.MinepacksIntegration;
import com.playpandora.pandoraonevsone.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Protection listener for 1v1 duels
 * Prevents interaction with backpacks, base chests, sell, AH, etc.
 */
public class OneVsOneProtectionListener implements Listener {
    
    private final PandoraOnevsOne plugin;
    
    public OneVsOneProtectionListener(PandoraOnevsOne plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Prevent breaking chests and protected blocks
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getOneVsOneManager().isInDuel(player)) {
            return;
        }
        
        // Prevent breaking chests, barrels, shulker boxes in 1v1
        Material blockType = event.getBlock().getType();
        if (blockType == Material.CHEST || 
            blockType == Material.TRAPPED_CHEST ||
            blockType == Material.BARREL ||
            blockType == Material.SHULKER_BOX ||
            blockType == Material.BLACK_SHULKER_BOX ||
            blockType == Material.BLUE_SHULKER_BOX ||
            blockType == Material.BROWN_SHULKER_BOX ||
            blockType == Material.CYAN_SHULKER_BOX ||
            blockType == Material.GRAY_SHULKER_BOX ||
            blockType == Material.GREEN_SHULKER_BOX ||
            blockType == Material.LIGHT_BLUE_SHULKER_BOX ||
            blockType == Material.LIGHT_GRAY_SHULKER_BOX ||
            blockType == Material.LIME_SHULKER_BOX ||
            blockType == Material.MAGENTA_SHULKER_BOX ||
            blockType == Material.ORANGE_SHULKER_BOX ||
            blockType == Material.PINK_SHULKER_BOX ||
            blockType == Material.PURPLE_SHULKER_BOX ||
            blockType == Material.RED_SHULKER_BOX ||
            blockType == Material.WHITE_SHULKER_BOX ||
            blockType == Material.YELLOW_SHULKER_BOX) {
            
            event.setCancelled(true);
            player.sendMessage(ColorUtil.error("You cannot break chests during a 1v1 duel!"));
        }
    }
    
    /**
     * Prevent opening chests and backpacks
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        
        if (!plugin.getOneVsOneManager().isInDuel(player)) {
            return;
        }
        
        Inventory inventory = event.getInventory();
        
        // Prevent opening backpack inventories
        if (MinepacksIntegration.isBackpackInventory(inventory)) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.error("You cannot use backpacks during a 1v1 duel!"));
            return;
        }
        
        // Prevent opening regular chests (not just base chests)
        if (inventory.getType() == InventoryType.CHEST || 
            inventory.getType() == InventoryType.BARREL ||
            inventory.getType() == InventoryType.SHULKER_BOX) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.error("You cannot open chests during a 1v1 duel!"));
            return;
        }
    }
    
    /**
     * Prevent placing items in chests/minepacks and prevent using backpacks
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        if (!plugin.getOneVsOneManager().isInDuel(player)) {
            return;
        }
        
        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getInventory();
        
        // Check if this is a backpack inventory
        if (MinepacksIntegration.isBackpackInventory(topInventory)) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.error("You cannot use backpacks during a 1v1 duel!"));
            player.closeInventory();
            return;
        }
        
        // Prevent placing items in chests/minepacks
        
        // Check if clicking in a chest/minepack inventory
        if (clickedInventory != null && clickedInventory.getType() != InventoryType.PLAYER) {
            // This is a non-player inventory (chest, backpack, etc.)
            if (clickedInventory.getType() == InventoryType.CHEST || 
                clickedInventory.getType() == InventoryType.BARREL ||
                clickedInventory.getType() == InventoryType.SHULKER_BOX ||
                MinepacksIntegration.isBackpackInventory(clickedInventory)) {
                
                // Prevent placing ANY items from player inventory into chests/minepacks
                ItemStack cursorItem = event.getCursor();
                if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                    event.setCancelled(true);
                    player.sendMessage(ColorUtil.error("You cannot place items in chests or backpacks during a 1v1 duel!"));
                    return;
                }
                
                // Also prevent moving items from player inventory to chest (shift-click, etc.)
                if (event.getAction().toString().contains("MOVE_TO_OTHER_INVENTORY") ||
                    event.getAction().toString().contains("PLACE") ||
                    event.getAction().toString().contains("SWAP")) {
                    event.setCancelled(true);
                    player.sendMessage(ColorUtil.error("You cannot place items in chests or backpacks during a 1v1 duel!"));
                    return;
                }
            }
        }
        
        // Also check if clicking FROM player inventory TO a chest/minepack (top inventory)
        if (clickedInventory != null && clickedInventory.getType() == InventoryType.PLAYER) {
            // Clicking in player inventory
            if (topInventory.getType() == InventoryType.CHEST || 
                topInventory.getType() == InventoryType.BARREL ||
                topInventory.getType() == InventoryType.SHULKER_BOX ||
                MinepacksIntegration.isBackpackInventory(topInventory)) {
                
                // Prevent moving items from player inventory to chest/minepack
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                    if (event.getAction().toString().contains("MOVE_TO_OTHER_INVENTORY") ||
                        event.getAction().toString().contains("PLACE") ||
                        event.getAction().toString().contains("SWAP")) {
                        event.setCancelled(true);
                        player.sendMessage(ColorUtil.error("You cannot place items in chests or backpacks during a 1v1 duel!"));
                        return;
                    }
                }
            }
        }
        
        // Check if clicking backpack item
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        
        if (clickedItem != null && MinepacksIntegration.isBackpackItem(clickedItem)) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.error("You cannot use backpacks during a 1v1 duel!"));
            return;
        }

        if (cursorItem != null && MinepacksIntegration.isBackpackItem(cursorItem)) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.error("You cannot use backpacks during a 1v1 duel!"));
            return;
        }
    }
    
    /**
     * Prevent dropping ALL items during 1v1 (including loot items and backpacks)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getOneVsOneManager().isInDuel(player)) {
            return;
        }
        
        ItemStack item = event.getItemDrop().getItemStack();
        
        // Prevent dropping ALL items during 1v1 duel
        // This includes: backpack, armor, weapons, potions, etc.
        event.setCancelled(true);
        player.sendMessage(ColorUtil.error("You cannot drop items during a 1v1 duel!"));
    }
}


