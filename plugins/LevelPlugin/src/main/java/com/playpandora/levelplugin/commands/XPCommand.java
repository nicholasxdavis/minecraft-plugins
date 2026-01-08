package com.playpandora.levelplugin.commands;

import com.playpandora.levelplugin.LevelPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class XPCommand implements CommandExecutor {
    
    private final LevelPlugin plugin;
    
    public XPCommand(LevelPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                String prefix = "&e&lPandora &8» &r";
                String message = prefix + "&7This command can only be used by players!";
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
                return true;
            }
            
            // Show own XP
            int level = plugin.getLevelManager().getLevel(player.getUniqueId());
            double required = plugin.getLevelManager().getXPRequiredForNextLevel(player.getUniqueId());
            double current = plugin.getLevelManager().getXPForCurrentLevel(player.getUniqueId());
            
            String prefix = "&e&lPandora &8» &r";
            String message = prefix + "&7Level: &6" + level + " &7| XP: &6" + 
                String.format("%.1f", current) + "&7/&6" + String.format("%.1f", required);
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            return true;
        }
        
        if (!sender.hasPermission("levelplugin.admin")) {
            String prefix = "&e&lPandora &8» &r";
            String message = prefix + "&cYou don't have permission to use this command.";
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            return true;
        }
        
        if (args.length < 3) {
            String prefix = "&e&lPandora &8» &r";
            String message = prefix + "&7Usage: &6/xp <give|set> <player> <amount>";
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            return true;
        }
        
        String action = args[0].toLowerCase();
        Player target = plugin.getServer().getPlayer(args[1]);
        
        if (target == null) {
            String prefix = "&e&lPandora &8» &r";
            String message = prefix + "&7Player not found.";
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            return true;
        }
        
        try {
            double amount = Double.parseDouble(args[2]);
            
            if (action.equals("give")) {
                plugin.getLevelManager().addXP(target.getUniqueId(), amount);
                String prefix = "&e&lPandora &8» &r";
                String message = prefix + "&eGave &6" + amount + " &eXP to &6" + target.getName();
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
                String targetMessage = prefix + "&eYou received &6" + amount + " &eXP!";
                target.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', targetMessage));
            } else if (action.equals("set")) {
                plugin.getLevelManager().setXP(target.getUniqueId(), amount);
                String prefix = "&e&lPandora &8» &r";
                String message = prefix + "&eSet &6" + target.getName() + "'s &eXP to &6" + amount;
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            } else {
                String prefix = "&e&lPandora &8» &r";
                String message = prefix + "&7Usage: &6/xp <give|set> <player> <amount>";
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            }
        } catch (NumberFormatException e) {
            String prefix = "&e&lPandora &8» &r";
            String message = prefix + "&cInvalid number: &6" + args[2];
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
        }
        
        return true;
    }
}

