package com.playpandora.welcome.commands;

import com.playpandora.welcome.Welcome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TipsCommand implements CommandExecutor {
    
    private final Welcome plugin;
    
    public TipsCommand(Welcome plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&e&lPandora &7This command can only be used by players!"));
            return true;
        }
        
        boolean tipsEnabled = plugin.getTipsManager().toggleTips(player);
        
        if (tipsEnabled) {
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&e&lPandora &7Tips have been &aenabled&7!"));
        } else {
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&e&lPandora &7Tips have been &cdisabled&7!"));
        }
        
        return true;
    }
}


