package com.playpandora.buygui.commands;

import com.playpandora.buygui.BuyGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BuyCommand implements CommandExecutor {
    
    private final BuyGUI plugin;
    
    public BuyCommand(BuyGUI plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&e&lPandora &8» &r&7This command can only be used by players!"));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if economy is available
        if (plugin.getEconomy() == null) {
            player.sendMessage(plugin.getMessage("no-economy"));
            return true;
        }
        
        // Open shop selector
        plugin.getShopSelectorGUI().openSelector(player);
        
        return true;
    }
}

