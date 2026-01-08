package com.playpandora.homebuffs.commands;

import com.playpandora.homebuffs.HomeBuffs;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class HomeBuffsCommand implements CommandExecutor {
    
    private final HomeBuffs plugin;
    
    public HomeBuffsCommand(HomeBuffs plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&e&lPandora");
            String message = "{prefix} &7This command can only be used by players!"
                .replace("{prefix}", prefix);
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            return true;
        }
        
        UUID uuid = player.getUniqueId();
        
        if (args.length == 0 || args[0].equalsIgnoreCase("toggle")) {
            boolean currentlyEnabled = plugin.getBuffManager().areBuffsEnabled(uuid);
            plugin.getBuffManager().setBuffsEnabled(uuid, !currentlyEnabled);
            
            String prefix = plugin.getConfig().getString("messages.prefix", "&e&lPandora");
            String message;
            if (!currentlyEnabled) {
                message = plugin.getConfig().getString("messages.buffs-enabled",
                    "{prefix} &eHome buffs enabled! You'll receive buffs when near your home.")
                    .replace("{prefix}", prefix);
            } else {
                message = plugin.getConfig().getString("messages.buffs-disabled",
                    "{prefix} &7Home buffs disabled.")
                    .replace("{prefix}", prefix);
            }
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("status")) {
            boolean isNearHome = plugin.getBuffManager().isPlayerNearHome(player);
            String prefix = plugin.getConfig().getString("messages.prefix", "&e&lPandora");
            String message;
            
            if (isNearHome) {
                message = plugin.getConfig().getString("messages.status-near-home",
                    "{prefix} &eYou are near your home! Buffs are active.")
                    .replace("{prefix}", prefix);
            } else {
                message = plugin.getConfig().getString("messages.status-far-from-home",
                    "{prefix} &7You are not near any of your homes.")
                    .replace("{prefix}", prefix);
            }
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            return true;
        }
        
        // Show usage
        String prefix = plugin.getConfig().getString("messages.prefix", "&e&lPandora");
        String message = "{prefix} &7Usage: &6/homebuffs [toggle|status]"
            .replace("{prefix}", prefix);
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
        return true;
    }
}

