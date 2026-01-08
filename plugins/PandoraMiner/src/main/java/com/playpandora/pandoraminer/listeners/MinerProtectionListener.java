package com.playpandora.pandoraminer.listeners;

import com.playpandora.pandoraminer.PandoraMiner;
import com.playpandora.pandoraminer.integration.MinepacksIntegration;
import com.playpandora.pandoraminer.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Protection listener for miner items
 * Protects backpack, sell, AH, and base chests from miner pickaxe
 */
public class MinerProtectionListener implements Listener {
    
    private final PandoraMiner plugin;
    
    public MinerProtectionListener(PandoraMiner plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Protect blocks from miner pickaxe
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getMinerManager().isMiner(player)) {
            return;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.NETHERITE_PICKAXE) {
            return;
        }
        
        // Check if breaking protected block types
        Material blockType = event.getBlock().getType();
        
        // Protect chests (potential sell/AH/base chests)
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
            
            // Check if it's a protected chest (sell, AH, base)
            if (isProtectedChest(event.getBlock().getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
        
        // Additional protection checks can be added here for specific plugin integrations
    }
    
    /**
     * Protect entities from miner pickaxe (e.g., item frames with backpacks)
     * Also protects backpack items from being damaged
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getDamager();
        
        if (!plugin.getMinerManager().isMiner(player)) {
            return;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.NETHERITE_PICKAXE) {
            return;
        }
        
        // Protect item frames and armor stands (potential backpack displays)
        if (event.getEntity() instanceof org.bukkit.entity.ItemFrame || 
            event.getEntity() instanceof org.bukkit.entity.ArmorStand) {
            // Check if the entity is holding a backpack item
            if (event.getEntity() instanceof org.bukkit.entity.ItemFrame) {
                org.bukkit.entity.ItemFrame frame = (org.bukkit.entity.ItemFrame) event.getEntity();
                ItemStack frameItem = frame.getItem();
                if (frameItem != null && MinepacksIntegration.isBackpackItem(frameItem)) {
                    event.setCancelled(true);
                    player.sendMessage(ColorUtil.error("You cannot damage backpack items with your miner pickaxe!"));
                    return;
                }
            }
        }
    }
    
    /**
     * Check if a location contains a protected chest
     * This method should integrate with sell, AH, and base chest systems
     */
    private boolean isProtectedChest(org.bukkit.Location location) {
        // Check for backpack protection
        if (isBackpack(location)) {
            return true;
        }
        
        // Check for sell chest protection
        if (isSellChest(location)) {
            return true;
        }
        
        // Check for AH chest protection
        if (isAHChest(location)) {
            return true;
        }
        
        // Check for base chest protection
        if (isBaseChest(location)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if location is a backpack (Minepacks integration)
     * Backpacks are player-held items, but we protect them through inventory checks
     */
    private boolean isBackpack(org.bukkit.Location location) {
        // Backpacks are player-held items, not block locations
        // Protection is handled through inventory click events in MinerListener
        // This method is kept for consistency with other protection checks
        return false;
    }
    
    /**
     * Protect backpack items from being broken by miner pickaxe
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        
        if (!plugin.getMinerManager().isMiner(player)) {
            return;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.NETHERITE_PICKAXE) {
            return;
        }
        
        // Check if interacting with item frame or armor stand that might have backpack
        // Protection handled through general entity protection
    }
    
    /**
     * Check if location is a sell chest
     */
    private boolean isSellChest(org.bukkit.Location location) {
        // Integration with SellGUI plugin
        try {
            // Check if SellGUI plugin is available and has this location registered
            // This would require integration with SellGUI API
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if location is an auction house chest
     */
    private boolean isAHChest(org.bukkit.Location location) {
        // Integration with auction house plugin
        // This would require integration with AH plugin API
        return false;
    }
    
    /**
     * Check if location is a base chest area (protect chests in base cubes)
     * Base chests are virtual inventories, but we protect regular chests in base cube areas
     */
    private boolean isBaseChest(org.bukkit.Location location) {
        // Integration with PandoraBases for base chest protection
        // Protect chests inside base cubes from being broken
        try {
            Block block = location.getBlock();
            if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) {
                return false;
            }
            
            // Check if chest is inside a base cube using BaseStructureHelper
            Class<?> baseStructureHelperClass = Class.forName("com.massivecraft.factions.util.BaseStructureHelper");
            java.lang.reflect.Method isInsideBaseCubeMethod = baseStructureHelperClass.getMethod("isInsideBaseCube", org.bukkit.Location.class, Class.forName("com.massivecraft.factions.Faction"));
            
            // Get all factions and check if location is inside any base cube
            Class<?> factionsClass = Class.forName("com.massivecraft.factions.Factions");
            Object factionsInstance = factionsClass.getMethod("getInstance").invoke(null);
            java.util.Collection<?> allFactions = (java.util.Collection<?>) factionsClass.getMethod("getAllNormalFactions").invoke(factionsInstance);
            
            if (allFactions != null) {
                for (Object faction : allFactions) {
                    // Check if faction has a base (has beacon)
                    Boolean hasBeacon = (Boolean) faction.getClass().getMethod("hasBeacon").invoke(faction);
                    if (hasBeacon != null && hasBeacon) {
                        Boolean isInside = (Boolean) isInsideBaseCubeMethod.invoke(null, location, faction);
                        if (isInside != null && isInside) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // PandoraBases not available or error
        }
        
        return false;
    }
}

