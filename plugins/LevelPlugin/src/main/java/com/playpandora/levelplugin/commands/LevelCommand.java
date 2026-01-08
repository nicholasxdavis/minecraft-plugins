package com.playpandora.levelplugin.commands;

import com.playpandora.levelplugin.LevelPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LevelCommand implements CommandExecutor {
    
    private final LevelPlugin plugin;
    
    public LevelCommand(LevelPlugin plugin) {
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
            
            showLevelInfo(player, player.getUniqueId());
            return true;
        }
        
        // Check permission for viewing other players
        if (!sender.hasPermission("levelplugin.view.others")) {
            String prefix = "&e&lPandora &8» &r";
            String message = prefix + "&cYou don't have permission to use this command.";
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            return true;
        }
        
        Player target = plugin.getServer().getPlayer(args[0]);
        if (target == null) {
            String prefix = "&e&lPandora &8» &r";
            String message = prefix + "&7Player not found.";
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            return true;
        }
        
        if (sender instanceof Player player) {
            showLevelInfo(player, target.getUniqueId(), target.getName());
        } else {
            // Console
            int level = plugin.getLevelManager().getLevel(target.getUniqueId());
            double xp = plugin.getLevelManager().getXP(target.getUniqueId());
            double required = plugin.getLevelManager().getXPRequiredForNextLevel(target.getUniqueId());
            String prefix = "&e&lPandora &8» &r";
            String message = prefix + "&6" + target.getName() + " &7- Level: &6" + level + 
                " &7| XP: &6" + String.format("%.1f", xp) + "&7/&6" + String.format("%.1f", required);
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
        }
        
        return true;
    }
    
    private void showLevelInfo(Player player, UUID uuid) {
        showLevelInfo(player, uuid, player.getName());
    }
    
    private void showLevelInfo(Player player, UUID uuid, String name) {
        int level = plugin.getLevelManager().getLevel(uuid);
        double currentXP = plugin.getLevelManager().getXPForCurrentLevel(uuid);
        double requiredXP = plugin.getLevelManager().getXPRequiredForNextLevel(uuid);
        double totalXP = plugin.getLevelManager().getXP(uuid);
        double percent = requiredXP > 0 ? (currentXP / requiredXP) * 100 : 100;
        
        String prefix = "&e&lPandora &8» &r";
        String message = prefix + "&7Level: &6" + level + " &7| XP: &6" + String.format("%.1f", currentXP) + 
            "&7/&6" + String.format("%.1f", requiredXP) + " &7(&6" + String.format("%.1f", percent) + "%&7)";
        
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
    }
}

