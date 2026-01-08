package com.playpandora.cannonshop.listeners;

import com.playpandora.cannonshop.CannonShop;
import com.playpandora.cannonshop.managers.CannonManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class CannonPlacerListener implements Listener {
    
    private final CannonShop plugin;
    
    public CannonPlacerListener(CannonShop plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return;
        }
        
        List<String> lore = meta.getLore();
        if (lore == null) {
            return;
        }
        
        // Check if this is a cannon placer item
        String cannonKey = null;
        for (String line : lore) {
            if (line.contains("Cannon: ")) {
                cannonKey = line.replace(ChatColor.GRAY.toString(), "").replace("Cannon: ", "").trim();
                break;
            }
        }
        
        if (cannonKey == null) {
            return;
        }
        
        CannonManager.CannonData cannon = plugin.getCannonManager().getCannon(cannonKey);
        if (cannon == null) {
            return;
        }
        
        event.setCancelled(true);
        
        // Check if location is protected (safe zone, war zone, or claimed territory)
        Location pasteLocation = player.getLocation();
        if (plugin.getProtectionManager() != null) {
            com.playpandora.cannonshop.managers.ProtectionManager.ProtectionResult result = 
                plugin.getProtectionManager().isLocationProtected(pasteLocation, player);
            
            if (result.isProtected()) {
                String prefix = plugin.getConfig().getString("messages.prefix", "&ePandora");
                String message = plugin.getConfig().getString("messages.cannot-paste-protected",
                    "{prefix} &c" + result.getMessage())
                    .replace("{prefix}", prefix);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                return;
            }
        }
        
        // Check if BuildPaste is available
        if (plugin.getServer().getPluginManager().getPlugin("BuildPaste") == null) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&ePandora");
            String message = plugin.getConfig().getString("messages.buildpaste-not-found",
                "{prefix} &7BuildPaste plugin not found! Please contact an administrator.")
                .replace("{prefix}", prefix);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            return;
        }
        
        // BuildPaste command format: /paste <paste-id>
        String command = "paste " + cannon.getPasteId();
        
        plugin.getLogger().info("Executing paste command as player: " + player.getName() + " - " + command);
        
        // Temporarily grant permission to use paste command
        String pastePermission = "buildpaste.paste";
        org.bukkit.permissions.PermissionAttachment attachment = null;
        boolean hadPermission = player.hasPermission(pastePermission);
        
        if (!hadPermission) {
            attachment = player.addAttachment(plugin);
            attachment.setPermission(pastePermission, true);
            plugin.getLogger().info("Temporarily granted " + pastePermission + " to " + player.getName());
        }
        
        try {
            // Execute command as player (not console)
            boolean success = plugin.getServer().dispatchCommand(player, command);
            
            // Remove permission immediately after command
            if (attachment != null) {
                attachment.remove();
                plugin.getLogger().info("Removed temporary permission from " + player.getName());
            }
            
            if (success) {
                // Remove one item from inventory
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    // Find the item in inventory and remove it
                    org.bukkit.inventory.PlayerInventory inv = player.getInventory();
                    if (inv.getItemInMainHand().equals(item)) {
                        inv.setItemInMainHand(null);
                    } else if (inv.getItemInOffHand().equals(item)) {
                        inv.setItemInOffHand(null);
                    } else {
                        // Search all slots
                        for (int i = 0; i < inv.getSize(); i++) {
                            ItemStack slotItem = inv.getItem(i);
                            if (slotItem != null && slotItem.equals(item)) {
                                inv.setItem(i, null);
                                break;
                            }
                        }
                    }
                }
                
                // Send success message
                String prefix = plugin.getConfig().getString("messages.prefix", "&ePandora");
                String message = plugin.getConfig().getString("messages.cannon-placed",
                    "{prefix} &7Cannon placed successfully at your location!")
                    .replace("{prefix}", prefix);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                
                plugin.getLogger().info("Successfully executed paste command for player: " + player.getName());
            } else {
                throw new Exception("Command dispatch returned false");
            }
            } catch (Exception e) {
            // Make sure to remove permission even if command fails
            if (attachment != null) {
                attachment.remove();
                plugin.getLogger().info("Removed temporary permission from " + player.getName() + " after error");
            }
            
            String prefix = plugin.getConfig().getString("messages.prefix", "&ePandora");
            String message = plugin.getConfig().getString("messages.console-command-error",
                "{prefix} &7Error executing cannon placement. Please contact an administrator.")
                .replace("{prefix}", prefix);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            plugin.getLogger().severe("Error executing paste command: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        // Check if this block placement is from BuildPaste in protected area
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        
        Location blockLoc = event.getBlockPlaced().getLocation();
        
        // Check if location is protected
        if (plugin.getProtectionManager() != null) {
            com.playpandora.cannonshop.managers.ProtectionManager.ProtectionResult result = 
                plugin.getProtectionManager().isLocationProtected(blockLoc, player);
            
            if (result.isProtected()) {
                event.setCancelled(true);
                String prefix = plugin.getConfig().getString("messages.prefix", "&ePandora");
                String message = plugin.getConfig().getString("messages.cannot-paste-protected",
                    "{prefix} &c" + result.getMessage())
                    .replace("{prefix}", prefix);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            }
        }
    }
}

