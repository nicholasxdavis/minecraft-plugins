package com.playpandora.sellgui.commands;

import com.playpandora.sellgui.SellGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SellCommand implements CommandExecutor {
    
    private final SellGUI plugin;
    
    public SellCommand(SellGUI plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§e§lPandora §7This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Handle /sellall - open sell GUI
        if (label.equalsIgnoreCase("sellall") || label.equalsIgnoreCase("sellallitems")) {
            if (plugin.getEconomy() == null) {
                player.sendMessage(plugin.getMessage("no-economy"));
                return true;
            }
            plugin.getSellShopGUI().openShop(player);
            return true;
        }
        
        // Handle /sell (open GUI)
        if (plugin.getEconomy() == null) {
            player.sendMessage(plugin.getMessage("no-economy"));
            return true;
        }
        
        plugin.getSellShopGUI().openShop(player);
        
        return true;
    }
}


