package com.playpandora.pandoraminer.listeners;

import com.playpandora.pandoraminer.PandoraMiner;
import com.playpandora.pandoraminer.integration.MinepacksIntegration;
import com.playpandora.pandoraminer.util.ColorUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

/**
 * Listener to prevent opening base chests while in miner mode
 */
public class MinerBaseChestListener implements Listener {
    
    private final PandoraMiner plugin;
    
    public MinerBaseChestListener(PandoraMiner plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Prevent opening chests and backpacks while in miner mode
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof org.bukkit.entity.Player)) {
            return;
        }
        
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getPlayer();
        
        if (!plugin.getMinerManager().isMiner(player)) {
            return;
        }
        
        Inventory inventory = event.getInventory();
        
        // Allow opening backpack inventories (minepack) - but MinerListener will prevent placing gold set/netherite pickaxe
        // Allow opening base chests - but MinerListener will prevent placing gold set/netherite pickaxe
        // Still prevent opening regular chests (not base chests) to avoid confusion
        if (MinepacksIntegration.isBackpackInventory(inventory)) {
            // Allow opening minepack/backpack inventories
            return;
        }
        
        // Check if this is a base chest inventory - allow it
        if (isBaseChest(inventory)) {
            // Allow opening base chests
            return;
        }
        
        // Prevent opening regular chests (not base chests)
        if (inventory.getType() == InventoryType.CHEST || 
            inventory.getType() == InventoryType.BARREL ||
            inventory.getType() == InventoryType.SHULKER_BOX) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.error("You cannot open chests while in miner mode! Use /miner to disable miner mode first."));
            return;
        }
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

