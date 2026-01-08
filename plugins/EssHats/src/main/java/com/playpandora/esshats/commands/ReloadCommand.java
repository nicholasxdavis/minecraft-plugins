package com.playpandora.esshats.commands;

import com.playpandora.esshats.EssHats;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {
    
    private final EssHats plugin;
    
    public ReloadCommand(EssHats plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("esshats.reload")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        
        try {
            plugin.getConfigManager().reload();
            // Reinitialize GUI to reload tier labels
            plugin.getHatGUI().reinitialize();
            sender.sendMessage(plugin.getConfigManager().getPrefix() + " " + 
                org.bukkit.ChatColor.translateAlternateColorCodes('&', "&aConfiguration reloaded successfully!"));
            plugin.getLogger().info("Configuration reloaded by " + sender.getName());
        } catch (Exception e) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + " " + 
                org.bukkit.ChatColor.translateAlternateColorCodes('&', "&cFailed to reload configuration: &7" + e.getMessage()));
            plugin.getLogger().severe("Failed to reload configuration: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
}

