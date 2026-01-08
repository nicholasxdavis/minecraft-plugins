package com.playpandora.pandoracavern.commands;

import com.playpandora.pandoracavern.PandoraCavern;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CavernCommand implements CommandExecutor {
    
    private final PandoraCavern plugin;
    
    public CavernCommand(PandoraCavern plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.formatMessage("{prefix} &cThis command can only be used by players!"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("pandoracavern.admin")) {
            player.sendMessage(plugin.formatMessage("{prefix} &cYou don't have permission to use this command!"));
            return true;
        }
        
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }
        
        if (args[0].equalsIgnoreCase("block")) {
            if (args.length < 2) {
                player.sendMessage(plugin.formatMessage("{prefix} &7Usage: &6/cavern block <add|remove>"));
                return true;
            }
            
            if (args[1].equalsIgnoreCase("add")) {
                // Give player 64 special netherite ore
                org.bukkit.inventory.ItemStack ore = plugin.getBlockManager().createCavernOre(64);
                player.getInventory().addItem(ore);
                player.sendMessage(plugin.formatMessage("{prefix} &7You have been given &664 &7special netherite ore blocks!"));
                return true;
            }
            
            if (args[1].equalsIgnoreCase("remove")) {
                // Give player removal pickaxe
                org.bukkit.inventory.ItemStack pickaxe = plugin.getBlockManager().createRemovalPickaxe();
                player.getInventory().addItem(pickaxe);
                player.sendMessage(plugin.formatMessage("{prefix} &7You have been given a &6removal pickaxe&7!"));
                player.sendMessage(plugin.formatMessage("{prefix} &7Mine cavern blocks with this pickaxe to remove them permanently."));
                return true;
            }
            
            player.sendMessage(plugin.formatMessage("{prefix} &7Usage: &6/cavern block <add|remove>"));
            return true;
        }
        
        sendHelpMessage(player);
        return true;
    }
    
    private void sendHelpMessage(Player player) {
        player.sendMessage(plugin.formatMessage("&e&l # Pandora Cavern Commands"));
        player.sendMessage(plugin.formatMessage("{prefix} &7/cavern block add &6- &7Get special netherite ore blocks"));
        player.sendMessage(plugin.formatMessage("{prefix} &7/cavern block remove &6- &7Get removal pickaxe"));
    }
}



