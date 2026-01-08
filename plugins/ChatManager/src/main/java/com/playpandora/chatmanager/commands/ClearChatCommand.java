package com.playpandora.chatmanager.commands;

import com.playpandora.chatmanager.ChatManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClearChatCommand implements CommandExecutor {
    
    private final ChatManager plugin;
    
    public ClearChatCommand(ChatManager plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            // Check permission
            if (!sender.hasPermission("chatmanager.clearchat")) {
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                    "&e&lPandora &7You don't have permission to use this command!"));
                return true;
            }
            
            // Get number of lines from config or use default
            int lines = plugin.getConfig().getInt("clear-chat.lines", 100);
            if (lines < 1) {
                lines = 100; // Safety check
            }
            
            // Build clear message efficiently
            StringBuilder clearMessage = new StringBuilder();
            for (int i = 0; i < lines; i++) {
                clearMessage.append(" \n");
            }
            
            String clearedBy = sender instanceof Player ? ((Player) sender).getName() : "Console";
            String announcement = plugin.getConfig().getString("clear-chat.announcement", 
                "&e&lPandora &7Chat has been cleared by &6{player}&7.");
            announcement = announcement.replace("{player}", clearedBy);
            announcement = org.bukkit.ChatColor.translateAlternateColorCodes('&', announcement);
            
            // Send to all players
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.sendMessage(clearMessage.toString());
                player.sendMessage(announcement);
            }
            
            // Also send to console
            plugin.getLogger().info("Chat cleared by " + clearedBy);
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Error executing clearchat command: " + e.getMessage());
            sender.sendMessage(org.bukkit.ChatColor.RED + "An error occurred while clearing chat.");
            return true;
        }
    }
}

