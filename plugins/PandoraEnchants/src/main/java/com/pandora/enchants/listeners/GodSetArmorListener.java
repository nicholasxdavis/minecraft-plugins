package com.pandora.enchants.listeners;

import com.pandora.enchants.util.ColorUtil;
import com.pandora.enchants.util.GodSetManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Listener to prevent equipping more than one piece of god set armor per set
 * Players can mix god set armor with normal armor, but can only wear one piece per set tier
 * Only blocks equipping - allows moving, dropping, storing in chests, etc.
 */
public class GodSetArmorListener implements Listener {
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        // Only check when actually EQUIPPING armor (placing item INTO armor slot)
        // Don't block removing, moving, or dropping items
        ItemStack itemToCheck = null;
        int armorSlot = -1;
        
        // Check if clicking on armor slot AND placing an item there
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            armorSlot = event.getSlot();
            // Only check if we're placing an item INTO the armor slot (not removing)
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                // Placing item from cursor into armor slot
                itemToCheck = event.getCursor();
            } else if (event.isShiftClick() && event.getCurrentItem() != null && 
                      event.getCurrentItem().getType() == Material.AIR) {
                // Shift-clicking from inventory to empty armor slot
                // This is handled by checking the clicked item in inventory
                return; // Let shift-click work normally, we'll catch it via PlayerInteractEvent
            }
        } else if (event.isShiftClick() && event.getCurrentItem() != null) {
            // Shift-click from inventory - check if it's armor going to armor slot
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && isArmor(clicked.getType())) {
                // Check if the target slot would be an armor slot
                // Only block if actually equipping (not just moving in inventory)
                armorSlot = getArmorSlot(clicked.getType());
                if (armorSlot != -1) {
                    itemToCheck = clicked;
                }
            }
        }
        
        // Only proceed if we're actually equipping armor
        if (itemToCheck == null || armorSlot == -1) {
            return;
        }
        
        // Check if the item being equipped is godset armor
        if (!GodSetManager.isGodsetArmor(itemToCheck)) {
            return;
        }
        
        String tier = GodSetManager.getGodsetTier(itemToCheck);
        if (tier == null) {
            return;
        }
        
        // Check if player already has another piece of godset armor from the same tier equipped
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            ItemStack armorPiece = armor[i];
            if (armorPiece == null || armorPiece.getType() == Material.AIR) {
                continue;
            }
            
            // Skip the slot being clicked (if swapping)
            if (i == getArmorIndex(armorSlot)) {
                continue;
            }
            
            if (GodSetManager.isGodsetArmor(armorPiece)) {
                String existingTier = GodSetManager.getGodsetTier(armorPiece);
                if (tier.equals(existingTier)) {
                    // Player already has a piece from this tier equipped
                    event.setCancelled(true);
                    String message = ColorUtil.error("You can only wear one piece of ") + 
                        ColorUtil.highlight(tier.toUpperCase() + " God Set") + 
                        ColorUtil.error(" armor at a time!");
                    player.sendMessage(message);
                    player.updateInventory();
                    return;
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Handle right-click equipping armor
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && 
            event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        
        // Check if it's armor
        if (!isArmor(item.getType())) {
            return;
        }
        
        // Check if it's godset armor
        if (!GodSetManager.isGodsetArmor(item)) {
            return;
        }
        
        String tier = GodSetManager.getGodsetTier(item);
        if (tier == null) {
            return;
        }
        
        // Check if player already has another piece of godset armor from the same tier equipped
        ItemStack[] armor = player.getInventory().getArmorContents();
        int targetSlot = getArmorIndex(getArmorSlot(item.getType()));
        
        for (int i = 0; i < armor.length; i++) {
            ItemStack armorPiece = armor[i];
            if (armorPiece == null || armorPiece.getType() == Material.AIR) {
                continue;
            }
            
            // Skip the slot this item would go to
            if (i == targetSlot) {
                continue;
            }
            
            if (GodSetManager.isGodsetArmor(armorPiece)) {
                String existingTier = GodSetManager.getGodsetTier(armorPiece);
                if (tier.equals(existingTier)) {
                    // Player already has a piece from this tier equipped
                    event.setCancelled(true);
                    String message = ColorUtil.error("You can only wear one piece of ") + 
                        ColorUtil.highlight(tier.toUpperCase() + " God Set") + 
                        ColorUtil.error(" armor at a time!");
                    player.sendMessage(message);
                    player.updateInventory();
                    return;
                }
            }
        }
    }
    
    /**
     * Checks if a material is armor
     */
    private boolean isArmor(Material material) {
        String name = material.name();
        return name.contains("HELMET") || name.contains("CHESTPLATE") || 
               name.contains("LEGGINGS") || name.contains("BOOTS");
    }
    
    /**
     * Gets the armor slot index (0-3) for a material
     */
    private int getArmorSlot(Material material) {
        String name = material.name();
        if (name.contains("HELMET")) return 39;
        if (name.contains("CHESTPLATE")) return 38;
        if (name.contains("LEGGINGS")) return 37;
        if (name.contains("BOOTS")) return 36;
        return -1;
    }
    
    /**
     * Converts armor slot number to armor array index
     */
    private int getArmorIndex(int slot) {
        // Armor slots: 39=helmet(0), 38=chestplate(1), 37=leggings(2), 36=boots(3)
        switch (slot) {
            case 39: return 0; // Helmet
            case 38: return 1; // Chestplate
            case 37: return 2; // Leggings
            case 36: return 3; // Boots
            default: return -1;
        }
    }
}

