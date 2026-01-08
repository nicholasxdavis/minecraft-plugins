package com.playpandora.pandoramaster.Command;

import com.playpandora.pandoramaster.PandoraMaster;
import com.playpandora.pandoramaster.managers.ClockItemManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class HubMenuCommand implements CommandExecutor {
    
    private final PandoraMaster plugin;
    private final ClockItemManager clockItemManager;
    
    public HubMenuCommand(PandoraMaster plugin) {
        this.plugin = plugin;
        this.clockItemManager = plugin.getClockItemManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&e&lPandora &8» &r&7This command can only be used by players!"));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if player has the clock item in inventory
        boolean hasClock = false;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && clockItemManager.isClockItem(contents[i])) {
                hasClock = true;
                break;
            }
        }
        
        // Also check armor slots
        if (!hasClock) {
            ItemStack[] armor = player.getInventory().getArmorContents();
            for (int i = 0; i < armor.length; i++) {
                if (armor[i] != null && clockItemManager.isClockItem(armor[i])) {
                    hasClock = true;
                    break;
                }
            }
        }
        
        // Also check offhand
        if (!hasClock) {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (offhand != null && clockItemManager.isClockItem(offhand)) {
                hasClock = true;
            }
        }
        
        if (hasClock) {
            // Remove all clock items from player's inventory
            boolean removed = false;
            for (int i = 0; i < contents.length; i++) {
                if (contents[i] != null && clockItemManager.isClockItem(contents[i])) {
                    player.getInventory().setItem(i, null);
                    removed = true;
                }
            }
            
            // Also check armor slots
            ItemStack[] armor = player.getInventory().getArmorContents();
            for (int i = 0; i < armor.length; i++) {
                if (armor[i] != null && clockItemManager.isClockItem(armor[i])) {
                    armor[i] = null;
                    removed = true;
                }
            }
            if (removed) {
                player.getInventory().setArmorContents(armor);
            }
            
            // Also check offhand
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (offhand != null && clockItemManager.isClockItem(offhand)) {
                player.getInventory().setItemInOffHand(null);
                removed = true;
            }
            
            player.sendMessage(plugin.formatMessage("messages.clock-removed",
                "&e&lPandora &8» &r&7Hub menu item has been removed from your inventory!"));
        } else {
            // Add clock item to inventory
            clockItemManager.giveClockToPlayer(player);
        }
        
        return true;
    }
}

