package com.playpandora.esshats.commands;

import com.playpandora.esshats.EssHats;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HatCommand implements CommandExecutor {
    
    private final EssHats plugin;
    
    public HatCommand(EssHats plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("players-only"));
            return true;
        }
        
        // Check if player has permission to use hats
        if (!player.hasPermission("esshats.use")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        
        // If player has arguments, pass through to Essentials hat command
        if (args.length > 0 && !args[0].equalsIgnoreCase("gui")) {
            StringBuilder commandBuilder = new StringBuilder("hat");
            for (String arg : args) {
                commandBuilder.append(" ").append(arg);
            }
            
            try {
                org.bukkit.Bukkit.dispatchCommand(player, commandBuilder.toString());
            } catch (Exception e) {
                player.sendMessage(plugin.getConfigManager().getMessage("command-failed", "error", e.getMessage()));
            }
            return true;
        }
        
        // Check if player is wearing a hat
        org.bukkit.inventory.ItemStack helmet = player.getInventory().getHelmet();
        if (helmet != null && !helmet.getType().isAir()) {
            // Player is wearing a hat, remove it
            player.getInventory().setHelmet(null);
            player.sendMessage(plugin.getConfigManager().getMessage("hat-removed"));
            // Play sound effect
            float[] soundParams = plugin.getConfigManager().getSoundHatRemoveParams();
            player.playSound(player.getLocation(), plugin.getConfigManager().getSoundHatRemove(), 
                soundParams[0], soundParams[1]);
            return true;
        }
        
        // Player is not wearing a hat, open GUI
        plugin.getHatGUI().openGUI(player);
        return true;
    }
}

