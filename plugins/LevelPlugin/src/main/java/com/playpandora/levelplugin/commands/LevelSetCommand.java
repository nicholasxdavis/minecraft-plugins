package com.playpandora.levelplugin.commands;

import com.playpandora.levelplugin.LevelPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LevelSetCommand implements CommandExecutor {
    
    private final LevelPlugin plugin;
    
    public LevelSetCommand(LevelPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("levelplugin.set")) {
            String prefix = "&e&lPandora &8» &r";
            String message = prefix + "&cYou don't have permission to use this command.";
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            return true;
        }
        
        // Check arguments
        if (args.length != 2) {
            String prefix = "&e&lPandora &8» &r";
            String message = prefix + "&7Usage: &6/levelset <player> <0-100>";
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            return true;
        }
        
        // Get target player
        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            String prefix = "&e&lPandora &8» &r";
            String message = prefix + "&7Player not found.";
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            return true;
        }
        
        // Parse level
        int level;
        try {
            level = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            String prefix = "&e&lPandora &8» &r";
            String message = prefix + "&cInvalid level! Must be a number between 0-100.";
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            return true;
        }
        
        // Validate level range
        if (level < 0 || level > 100) {
            String prefix = "&e&lPandora &8» &r";
            String message = prefix + "&cLevel must be between 0 and 100!";
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            return true;
        }
        
        // Set the level
        plugin.getLevelManager().setLevel(target.getUniqueId(), level);
        
        // Send confirmation messages
        String prefix = "&e&lPandora &8» &r";
        String senderMessage = prefix + "&7Set &6" + target.getName() + "&7's level to &6" + level + "&7.";
        sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', senderMessage));
        
        String targetMessage = prefix + "&7Your level has been set to &6" + level + "&7 by &6" + sender.getName() + "&7.";
        target.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', targetMessage));
        
        return true;
    }
}


