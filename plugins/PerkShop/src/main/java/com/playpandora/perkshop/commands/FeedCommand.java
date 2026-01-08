package com.playpandora.perkshop.commands;

import com.playpandora.perkshop.PerkShop;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FeedCommand implements CommandExecutor {
    
    private final PerkShop plugin;
    
    public FeedCommand(PerkShop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7This command can only be used by players!"));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if player has the feed perk
        if (!plugin.getPurchaseManager().hasPerk(player.getUniqueId(), "feed")) {
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7You need to purchase the &6Feed &7perk from the shop first!"));
            return true;
        }
        
        // Feed the player
        player.setFoodLevel(20);
        player.setSaturation(20);
        
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7You have been fed!"));
        
        return true;
    }
}


