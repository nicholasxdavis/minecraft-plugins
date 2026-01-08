package com.playpandora.essentialsgui.commands;

import com.playpandora.essentialsgui.EssentialsGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KitCommand implements CommandExecutor {
    
    private final EssentialsGUI plugin;
    
    public KitCommand(EssentialsGUI plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // If no arguments or "open"/"gui"/"list", open GUI (only for players)
        if (args.length == 0 || (args.length == 1 && (args[0].equalsIgnoreCase("open") || 
                args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("list")))) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&e&lPandora &8» &7This command can only be used by players!"));
                return true;
            }
            plugin.getKitGUI().openGUI(player);
            return true;
        }
        
        // If one argument (kit name), delegate to Essentials
        // This ensures Essentials commands like /kit <kitname> still work
        if (args.length == 1) {
            StringBuilder cmdBuilder = new StringBuilder("essentials:kit");
            for (String arg : args) {
                cmdBuilder.append(" ").append(arg);
            }
            org.bukkit.Bukkit.dispatchCommand(sender, cmdBuilder.toString());
            return true;
        }
        
        // For all other cases, delegate to Essentials
        StringBuilder cmdBuilder = new StringBuilder("essentials:kit");
        for (String arg : args) {
            cmdBuilder.append(" ").append(arg);
        }
        org.bukkit.Bukkit.dispatchCommand(sender, cmdBuilder.toString());
        return true;
    }
}


