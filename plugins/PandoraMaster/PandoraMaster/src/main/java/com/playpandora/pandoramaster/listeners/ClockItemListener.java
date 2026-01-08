package com.playpandora.pandoramaster.listeners;

import com.playpandora.pandoramaster.PandoraMaster;
import com.playpandora.pandoramaster.managers.ClockItemManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;

public class ClockItemListener implements Listener {
    
    private final PandoraMaster plugin;
    private final ClockItemManager clockItemManager;
    
    public ClockItemListener(PandoraMaster plugin) {
        this.plugin = plugin;
        this.clockItemManager = plugin.getClockItemManager();
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Give clock item on join (with a small delay to ensure inventory is ready)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            clockItemManager.giveClockToPlayer(event.getPlayer());
        }, 20L); // 1 second delay
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !clockItemManager.isClockItem(item)) {
            return;
        }
        
        // Only trigger on right-click (air or block)
        // Use HIGH priority so it runs AFTER Minepacks (which uses LOWEST)
        // This ensures Minepacks handles backpack items first
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Always open GUI on right-click, no matter what
            event.setCancelled(true);
            
            // Close any open inventory first
            if (event.getPlayer().getOpenInventory().getTopInventory().getType() != InventoryType.CRAFTING) {
                event.getPlayer().closeInventory();
            }
            
            // Open the hub GUI
            plugin.getHubGUI().openGUI(event.getPlayer());
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item != null && clockItemManager.isClockItem(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.formatMessage("messages.no-permission",
                "&e&lPandora &8» &r&7You cannot place the hub menu item!"));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        // Only handle clock item clicks, don't interfere with other items
        // Use HIGH priority so it runs AFTER Minepacks (which uses LOWEST)
        
        // First check if this is a backpack inventory - if so, don't interfere at all
        if (event.getInventory().getHolder() != null) {
            try {
                Class<?> backpackClass = Class.forName("at.pcgamingfreaks.Minepacks.Bukkit.API.Backpack");
                if (backpackClass.isInstance(event.getInventory().getHolder())) {
                    // This is a backpack inventory, let Minepacks handle everything
                    return;
                }
            } catch (Exception e) {
                // Backpack class not found, continue
            }
        }
        
        ItemStack item = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        
        // Check if clicking on clock item
        if (item != null && clockItemManager.isClockItem(item)) {
            // Only prevent moving if trying to move to a different inventory
            // Allow normal inventory management within player inventory
            if (event.getClickedInventory() != null && 
                event.getClickedInventory().getType() != InventoryType.PLAYER &&
                event.getWhoClicked().getInventory() != event.getClickedInventory()) {
                // Prevent moving clock item to non-player inventories (chests, etc.)
                if (event.getAction().toString().contains("MOVE_TO_OTHER_INVENTORY") ||
                    (cursor != null && clockItemManager.isClockItem(cursor))) {
                    event.setCancelled(true);
                }
            }
        }
        
        // Check if cursor is clock item
        if (cursor != null && clockItemManager.isClockItem(cursor)) {
            // Prevent putting clock item in crafting/result slots or other special inventories
            if (event.getSlotType() == InventoryType.SlotType.RESULT ||
                (event.getClickedInventory() != null && 
                 event.getClickedInventory().getType() != InventoryType.PLAYER &&
                 event.getWhoClicked().getInventory() != event.getClickedInventory())) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (clockItemManager.isClockItem(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.formatMessage("messages.no-permission",
                "&e&lPandora &8» &r&7You cannot drop the hub menu item!"));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Store if player had clock item before death
        boolean hadClock = false;
        for (ItemStack item : event.getEntity().getInventory().getContents()) {
            if (clockItemManager.isClockItem(item)) {
                hadClock = true;
                break;
            }
        }
        
        // Store in player metadata to restore on respawn
        if (hadClock) {
            event.getEntity().setMetadata("pandoramaster_had_clock", 
                new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Restore clock item after respawn if player had it before death
        if (event.getPlayer().hasMetadata("pandoramaster_had_clock")) {
            event.getPlayer().removeMetadata("pandoramaster_had_clock", plugin);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                clockItemManager.giveClockToPlayer(event.getPlayer());
            }, 5L); // Small delay to ensure inventory is ready
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase();
        // Check if player is clearing their own inventory
        if (command.startsWith("/clear") || command.startsWith("/ci")) {
            String[] args = command.split(" ");
            // If no target specified or target is self, restore clock after clear
            if (args.length == 1 || (args.length == 2 && args[1].equalsIgnoreCase(event.getPlayer().getName()))) {
                // Restore clock item after command executes
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    clockItemManager.giveClockToPlayer(event.getPlayer());
                }, 5L);
            }
        }
    }
}

