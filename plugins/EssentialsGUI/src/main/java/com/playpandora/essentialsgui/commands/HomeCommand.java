package com.playpandora.essentialsgui.commands;

import com.playpandora.essentialsgui.EssentialsGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HomeCommand implements CommandExecutor {
    
    private final EssentialsGUI plugin;
    
    public HomeCommand(EssentialsGUI plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // If no arguments or "open"/"gui", open GUI (only for players)
        if (args.length == 0 || (args.length == 1 && (args[0].equalsIgnoreCase("open") || args[0].equalsIgnoreCase("gui")))) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&e&lPandora &8» &7This command can only be used by players!"));
                return true;
            }
            plugin.getHomeGUI().openGUI(player);
            return true;
        }
        
        // For all other cases, delegate to Essentials by dispatching the command
        // This ensures Essentials commands like /home list, /home set, /home <name>, etc. all work
        StringBuilder cmdBuilder = new StringBuilder("essentials:home");
        for (String arg : args) {
            cmdBuilder.append(" ").append(arg);
        }
        org.bukkit.Bukkit.dispatchCommand(sender, cmdBuilder.toString());
        return true;
    }
}

