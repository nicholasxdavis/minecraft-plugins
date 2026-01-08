package com.playpandora.essentialsgui.commands;

import com.playpandora.essentialsgui.EssentialsGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RulesCommand implements CommandExecutor {
    
    private final EssentialsGUI plugin;
    
    public RulesCommand(EssentialsGUI plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&e&lPandora &8» &7This command can only be used by players!"));
            return true;
        }
        
        plugin.getRulesGUI().openGUI(player);
        return true;
    }
}


