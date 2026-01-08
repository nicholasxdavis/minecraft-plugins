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
        // Don't automatically give clock item on join - make it optional
        // Only give it if player uses /hubmenu or dies
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
        // Allow players to move clock item freely - including into chests
        // Only prevent placing in crafting result slots
        ItemStack cursor = event.getCursor();
        
        // Check if cursor is clock item - only prevent in crafting result slots
        if (cursor != null && clockItemManager.isClockItem(cursor)) {
            if (event.getSlotType() == InventoryType.SlotType.RESULT) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (clockItemManager.isClockItem(item)) {
            // Allow dropping but make it instantly despawn
            org.bukkit.entity.Item drop = event.getItemDrop();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (drop != null && !drop.isDead()) {
                    drop.remove();
                }
            }, 1L); // Remove on next tick (instantly)
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
        
        // Also check offhand
        if (!hadClock) {
            ItemStack offhand = event.getEntity().getInventory().getItemInOffHand();
            if (offhand != null && clockItemManager.isClockItem(offhand)) {
                hadClock = true;
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
                // Only give if player doesn't already have one
                boolean hasClock = false;
                for (ItemStack item : event.getPlayer().getInventory().getContents()) {
                    if (clockItemManager.isClockItem(item)) {
                        hasClock = true;
                        break;
                    }
                }
                if (!hasClock) {
                    ItemStack offhand = event.getPlayer().getInventory().getItemInOffHand();
                    if (offhand != null && clockItemManager.isClockItem(offhand)) {
                        hasClock = true;
                    }
                }
                if (!hasClock) {
                    clockItemManager.giveClockToPlayer(event.getPlayer());
                }
            }, 5L); // Small delay to ensure inventory is ready
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase().trim();
        
        // Check if player is holding the hub menu item
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        boolean holdingClock = (mainHand != null && clockItemManager.isClockItem(mainHand)) ||
                              (offHand != null && clockItemManager.isClockItem(offHand));
        
        if (holdingClock) {
            // Prevent ./sell, ./sellall, and ./ah when holding hub menu item
            if (command.startsWith("./sell") || command.startsWith("/sell")) {
                // Check if it's sellall specifically
                if (command.startsWith("./sellall") || command.startsWith("/sellall")) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.formatMessage("messages.no-permission",
                        "&e&lPandora &8» &r&7You cannot use /sellall while holding the hub menu item!"));
                    return;
                }
                // Regular ./sell command
                event.setCancelled(true);
                player.sendMessage(plugin.formatMessage("messages.no-permission",
                    "&e&lPandora &8» &r&7You cannot use /sell while holding the hub menu item!"));
                return;
            }
            
            // Prevent ./ah or /ah commands
            if (command.startsWith("./ah") || command.startsWith("/ah") ||
                command.startsWith("./auctionhouse") || command.startsWith("/auctionhouse") ||
                command.startsWith("./auction") || command.startsWith("/auction")) {
                event.setCancelled(true);
                player.sendMessage(plugin.formatMessage("messages.no-permission",
                    "&e&lPandora &8» &r&7You cannot use /ah while holding the hub menu item!"));
                return;
            }
        }
    }
}

