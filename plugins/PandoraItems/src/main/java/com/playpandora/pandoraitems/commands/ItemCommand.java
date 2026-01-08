package com.playpandora.pandoraitems.commands;

import com.playpandora.pandoraitems.PandoraItems;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ItemCommand implements CommandExecutor {
    
    private final PandoraItems plugin;
    
    public ItemCommand(PandoraItems plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pandoraitems.give")) {
            sender.sendMessage(plugin.formatMessage("no-permission", 
                "{prefix} &cYou don't have permission to use this command!"));
            return true;
        }
        
        if (args.length < 3) {
            sender.sendMessage(plugin.formatMessage("invalid-type",
                "{prefix} &cUsage: /pandoraitem give <cannon|farm|kit|perk|pet> <type> [player]"));
            return true;
        }
        
        if (!args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(plugin.formatMessage("invalid-type",
                "{prefix} &cUsage: /pandoraitem give <cannon|farm|kit|perk|pet> <type> [player]"));
            return true;
        }
        
        String itemType = args[1].toLowerCase();
        String itemKey = args[2];
        Player target = null;
        
        // Get target player
        if (args.length >= 4) {
            target = Bukkit.getPlayer(args[3]);
            if (target == null) {
                sender.sendMessage(plugin.formatMessage("player-not-found",
                    "{prefix} &cPlayer &6{player} &cnot found!", "player", args[3]));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(plugin.formatMessage("player-not-found",
                "{prefix} &cYou must specify a player when using this command from console!"));
            return true;
        }
        
        org.bukkit.inventory.ItemStack item = null;
        String itemName = itemKey;
        
        // Create item based on type
        switch (itemType) {
            case "cannon":
                if (!plugin.getCannonItemManager().isValidCannonKey(itemKey)) {
                    sender.sendMessage(plugin.formatMessage("invalid-item",
                        "{prefix} &cInvalid cannon key: &6{key}&7", "key", itemKey));
                    return true;
                }
                item = plugin.getCannonItemManager().createCannonItem(itemKey);
                itemName = "Cannon: " + itemKey;
                break;
                
            case "farm":
                if (!plugin.getFarmItemManager().isValidFarmKey(itemKey)) {
                    sender.sendMessage(plugin.formatMessage("invalid-item",
                        "{prefix} &cInvalid farm key: &6{key}&7", "key", itemKey));
                    return true;
                }
                item = plugin.getFarmItemManager().createFarmItem(itemKey);
                itemName = "Farm: " + itemKey;
                break;
                
            case "kit":
                if (!plugin.getKitItemManager().isValidKitKey(itemKey)) {
                    sender.sendMessage(plugin.formatMessage("invalid-item",
                        "{prefix} &cInvalid kit key: &6{key}&7", "key", itemKey));
                    return true;
                }
                item = plugin.getKitItemManager().createKitItem(itemKey);
                itemName = "Kit: " + itemKey;
                break;
                
            case "perk":
                if (!plugin.getPerkItemManager().isValidPerkKey(itemKey)) {
                    sender.sendMessage(plugin.formatMessage("invalid-item",
                        "{prefix} &cInvalid perk key: &6{key}&7", "key", itemKey));
                    return true;
                }
                item = plugin.getPerkItemManager().createPerkItem(itemKey);
                itemName = "Perk: " + itemKey;
                break;
                
            case "pet":
                if (!plugin.getPetItemManager().isValidPetKey(itemKey)) {
                    sender.sendMessage(plugin.formatMessage("invalid-item",
                        "{prefix} &cInvalid pet key: &6{key}&7", "key", itemKey));
                    return true;
                }
                item = plugin.getPetItemManager().createPetItem(itemKey);
                itemName = "Pet: " + itemKey;
                break;
                
            default:
                sender.sendMessage(plugin.formatMessage("invalid-type",
                    "{prefix} &cInvalid item type! Use: &6cannon&7, &6farm&7, &6kit&7, &6perk&7, or &6pet&7"));
                return true;
        }
        
        if (item == null) {
            sender.sendMessage(plugin.formatMessage("invalid-item",
                "{prefix} &cFailed to create item: &6{key}&7", "key", itemKey));
            return true;
        }
        
        // Give item to player
        if (target.getInventory().firstEmpty() == -1) {
            target.getWorld().dropItemNaturally(target.getLocation(), item);
        } else {
            target.getInventory().addItem(item);
        }
        
        // Send messages
        if (!sender.equals(target)) {
            sender.sendMessage(plugin.formatMessage("item-given",
                "{prefix} &7Gave &6{item} &7to &6{player}&7!", "item", itemName, "player", target.getName()));
        }
        target.sendMessage(plugin.formatMessage("item-received",
            "{prefix} &7You received &6{item}&7!", "item", itemName));
        
        return true;
    }
}

