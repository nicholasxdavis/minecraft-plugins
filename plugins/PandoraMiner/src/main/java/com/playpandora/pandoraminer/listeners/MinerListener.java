package com.playpandora.pandoraminer.listeners;

import com.playpandora.pandoraminer.PandoraMiner;
import com.playpandora.pandoraminer.integration.MinepacksIntegration;
import com.playpandora.pandoraminer.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Listener for miner protection
 */
public class MinerListener implements Listener {
    
    private final PandoraMiner plugin;
    
    public MinerListener(PandoraMiner plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Prevent removing miner armor
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        if (!plugin.getMinerManager().isMiner(player)) {
            return;
        }
        
        // Check if trying to remove armor
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        
        // Check if clicking in armor slot
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.error("You cannot remove your miner armor! Use /miner to disable miner mode."));
            return;
        }
        
        // PREVENT ALL movement of miner items (pickaxe, armor, backpack)
        // Also prevent putting items INTO miner item slots
        int clickedSlot = event.getSlot();
        
        // Check if trying to put item into pickaxe slot (main hand slot 0)
        if (clickedSlot == player.getInventory().getHeldItemSlot() || clickedSlot == 0) {
            if (cursorItem != null && !isMinerItem(cursorItem) && !MinepacksIntegration.isBackpackItem(cursorItem)) {
                // Trying to put non-miner item into pickaxe slot
                event.setCancelled(true);
                player.sendMessage(ColorUtil.error("You cannot put items in your pickaxe slot! Use /miner to disable miner mode."));
                return;
            }
        }
        
        if (clickedItem != null) {
            if (isMinerItem(clickedItem)) {
                // Prevent moving miner items AT ALL - they must stay in place
                event.setCancelled(true);
                player.sendMessage(ColorUtil.error("You cannot move your miner items! Use /miner to disable miner mode."));
                return;
            }
            if (MinepacksIntegration.isBackpackItem(clickedItem)) {
                // Prevent moving backpack in miner mode
                event.setCancelled(true);
                player.sendMessage(ColorUtil.error("You cannot move your backpack in miner mode!"));
                return;
            }
        }
        
        if (cursorItem != null) {
            if (isMinerItem(cursorItem)) {
                // Prevent moving miner items AT ALL
                event.setCancelled(true);
                player.sendMessage(ColorUtil.error("You cannot move your miner items! Use /miner to disable miner mode."));
                return;
            }
            if (MinepacksIntegration.isBackpackItem(cursorItem)) {
                // Prevent moving backpack
                event.setCancelled(true);
                player.sendMessage(ColorUtil.error("You cannot move your backpack in miner mode!"));
                return;
            }
        }
        
        // Prevent putting items into slots that contain miner items
        ItemStack slotItem = player.getInventory().getItem(clickedSlot);
        if (slotItem != null && isMinerItem(slotItem)) {
            // Trying to put something into a slot with miner item
            if (cursorItem != null && !isMinerItem(cursorItem)) {
                event.setCancelled(true);
                player.sendMessage(ColorUtil.error("You cannot replace your miner items! Use /miner to disable miner mode."));
                return;
            }
        }
        
        // Prevent placing miner items in chests/minepacks/base chests/AH/sell GUI
        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getInventory();
        
        // Check if clicking in a protected inventory (minepack/base chest/AH/sell GUI)
        if (clickedInventory != null && clickedInventory.getType() != InventoryType.PLAYER) {
            if (isProtectedInventory(clickedInventory)) {
                // Prevent placing miner items into protected inventories
                if (cursorItem != null && isMinerItem(cursorItem)) {
                    event.setCancelled(true);
                    player.sendMessage(ColorUtil.error("You cannot place miner equipment (gold set/netherite pickaxe) in chests, backpacks, AH, or sell GUI! Use /miner to disable miner mode."));
                    return;
                }
                
                // Also prevent moving miner items from player inventory to protected inventory
                if (clickedItem != null && isMinerItem(clickedItem)) {
                    if (event.getAction().toString().contains("MOVE_TO_OTHER_INVENTORY") ||
                        event.getAction().toString().contains("PLACE") ||
                        event.getAction().toString().contains("SWAP")) {
                        event.setCancelled(true);
                        player.sendMessage(ColorUtil.error("You cannot place miner equipment (gold set/netherite pickaxe) in chests, backpacks, AH, or sell GUI! Use /miner to disable miner mode."));
                        return;
                    }
                }
            }
        }
        
        // Also check if clicking FROM player inventory TO a protected inventory (top inventory)
        if (clickedInventory != null && clickedInventory.getType() == InventoryType.PLAYER) {
            // Clicking in player inventory
            if (isProtectedInventory(topInventory)) {
                // Prevent moving miner items from player inventory to protected inventory
                if (clickedItem != null && isMinerItem(clickedItem)) {
                    if (event.getAction().toString().contains("MOVE_TO_OTHER_INVENTORY") ||
                        event.getAction().toString().contains("PLACE") ||
                        event.getAction().toString().contains("SWAP")) {
                        event.setCancelled(true);
                        player.sendMessage(ColorUtil.error("You cannot place miner equipment (gold set/netherite pickaxe) in chests, backpacks, AH, or sell GUI! Use /miner to disable miner mode."));
                        return;
                    }
                }
            }
        }
        
        // Check if this is a backpack inventory holder - prevent placing miner items
        if (event.getInventory().getHolder() != null && 
            MinepacksIntegration.isBackpackHolder(event.getInventory().getHolder())) {
            // This is a backpack inventory - prevent placing miner items
            if (cursorItem != null && isMinerItem(cursorItem)) {
                event.setCancelled(true);
                player.sendMessage(ColorUtil.error("You cannot place miner equipment (gold set/netherite pickaxe) in backpacks! Use /miner to disable miner mode."));
                return;
            }
            if (clickedItem != null && isMinerItem(clickedItem)) {
                event.setCancelled(true);
                player.sendMessage(ColorUtil.error("You cannot place miner equipment (gold set/netherite pickaxe) in backpacks! Use /miner to disable miner mode."));
                return;
            }
        }
    }
    
    /**
     * Prevent dropping miner items and protect backpack
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getMinerManager().isMiner(player)) {
            return;
        }
        
        ItemStack item = event.getItemDrop().getItemStack();
        
        // Prevent dropping miner items
        if (isMinerItem(item)) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.error("You cannot drop your miner items! Use /miner to disable miner mode."));
            return;
        }
        
        // Prevent dropping backpack (protection)
        if (MinepacksIntegration.isBackpackItem(item)) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.error("You cannot drop your backpack!"));
        }
    }
    
    /**
     * Check if item is part of miner kit
     */
    private boolean isMinerItem(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        Material type = item.getType();
        
        // Check for gold armor
        if (type == Material.GOLDEN_HELMET ||
            type == Material.GOLDEN_CHESTPLATE ||
            type == Material.GOLDEN_LEGGINGS ||
            type == Material.GOLDEN_BOOTS) {
            
            // Check if it's enchanted (our miner armor is enchanted)
            if (item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
                return true;
            }
        }
        
        // Check for netherite pickaxe with Efficiency V
        if (type == Material.NETHERITE_PICKAXE) {
            if (item.hasItemMeta() && item.getItemMeta().hasEnchant(org.bukkit.enchantments.Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("efficiency")))) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if item is miner armor
     */
    private boolean isMinerArmor(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        Material type = item.getType();
        return type == Material.GOLDEN_HELMET ||
               type == Material.GOLDEN_CHESTPLATE ||
               type == Material.GOLDEN_LEGGINGS ||
               type == Material.GOLDEN_BOOTS;
    }
    
    /**
     * Check if an inventory is a protected inventory where miner items shouldn't be placed
     * This includes: minepacks, base chests, AH, sell GUI, but NOT regular chests (those are blocked from opening)
     */
    private boolean isProtectedInventory(Inventory inv) {
        if (inv == null) {
            return false;
        }
        
        // Check for minepack/backpack - allow opening but prevent placing miner items
        if (MinepacksIntegration.isBackpackInventory(inv)) {
            return true;
        }
        
        // Check for base chest - allow opening but prevent placing miner items
        if (isBaseChest(inv)) {
            return true;
        }
        
        // Check for AH/sell GUI - these are typically custom inventories
        // Check by title patterns for common AH/sell GUI plugins
        try {
            String title = null;
            if (!inv.getViewers().isEmpty()) {
                org.bukkit.entity.HumanEntity viewer = inv.getViewers().get(0);
                if (viewer != null && viewer.getOpenInventory() != null) {
                    title = viewer.getOpenInventory().getTitle();
                }
            }
            if (title != null) {
                String lowerTitle = title.toLowerCase();
                if (lowerTitle.contains("auction") || lowerTitle.contains("ah") || 
                    lowerTitle.contains("sell") || lowerTitle.contains("shop")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        
        return false;
    }
    
    /**
     * Check if an inventory is a base chest
     */
    private boolean isBaseChest(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        
        try {
            Class<?> baseChestManagerClass = Class.forName("com.massivecraft.factions.util.BaseChestManager");
            java.lang.reflect.Method isBaseChestMethod = baseChestManagerClass.getMethod("isBaseChest", Inventory.class);
            Boolean result = (Boolean) isBaseChestMethod.invoke(null, inventory);
            
            return result != null && result;
        } catch (Exception e) {
            // BaseChestManager not available or error
            // Fallback: check by title pattern
            try {
                // Try to get title from inventory viewers
                if (!inventory.getViewers().isEmpty()) {
                    org.bukkit.entity.HumanEntity viewer = inventory.getViewers().get(0);
                    if (viewer != null && viewer.getOpenInventory() != null) {
                        String title = viewer.getOpenInventory().getTitle();
                        if (title != null && title.startsWith("Base Chest - ")) {
                            return true;
                        }
                    }
                }
            } catch (Exception ex) {
                // Ignore
            }
            return false;
        }
    }
}

