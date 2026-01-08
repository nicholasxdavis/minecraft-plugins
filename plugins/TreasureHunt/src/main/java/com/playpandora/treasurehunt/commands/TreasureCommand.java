package com.playpandora.treasurehunt.commands;

import com.playpandora.treasurehunt.TreasureHunt;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TreasureCommand implements CommandExecutor {
    
    private final TreasureHunt plugin;
    
    public TreasureCommand(TreasureHunt plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7This command can only be used by players!"));
            return true;
        }
        
        Player player = (Player) sender;
        String prefix = "&e&lPandora &8» &r";
        
        if (args.length == 0) {
            // Show info
            if (!plugin.getTreasureManager().hasActiveTreasure()) {
                player.sendMessage(plugin.formatMessage(plugin.getConfig().getString("messages.no-treasure", "{prefix} &7No active treasure hunt!")));
                return true;
            }
            
            int hintsAway = plugin.getTreasureManager().getHintsAway(player);
            int hintsRemaining = plugin.getTreasureManager().getHintsRemaining(player);
            
            player.sendMessage(plugin.formatMessage(prefix + " &7You are &6" + hintsAway + " &7hints away from the treasure!"));
            player.sendMessage(plugin.formatMessage(prefix + " &7Hints remaining: &6" + hintsRemaining));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "hint":
            case "claimhint":
                if (!plugin.getTreasureManager().hasActiveTreasure()) {
                    player.sendMessage(plugin.formatMessage(plugin.getConfig().getString("messages.no-treasure", "{prefix} &7No active treasure hunt!")));
                    return true;
                }
                
                if (plugin.getTreasureManager().claimHint(player)) {
                    // Success message is sent in RewardManager
                } else {
                    player.sendMessage(plugin.formatMessage(plugin.getConfig().getString("messages.too-far", "{prefix} &7You are too far from the treasure location!")));
                }
                break;
                
            case "claim":
            case "claimtreasure":
                if (!plugin.getTreasureManager().hasActiveTreasure()) {
                    player.sendMessage(plugin.formatMessage(plugin.getConfig().getString("messages.no-treasure", "{prefix} &7No active treasure hunt!")));
                    return true;
                }
                
                if (plugin.getTreasureManager().claimTreasure(player)) {
                    // Success message is sent in RewardManager
                } else {
                    player.sendMessage(plugin.formatMessage(prefix + " &cYou cannot claim the treasure yet! You need to be close to it and have claimed all hints."));
                }
                break;
                
            case "info":
                if (!plugin.getTreasureManager().hasActiveTreasure()) {
                    player.sendMessage(plugin.formatMessage(plugin.getConfig().getString("messages.no-treasure", "{prefix} &7No active treasure hunt!")));
                    return true;
                }
                
                int hintsAway = plugin.getTreasureManager().getHintsAway(player);
                int hintsRemaining = plugin.getTreasureManager().getHintsRemaining(player);
                
                player.sendMessage(plugin.formatMessage(prefix + " &e&lInformation"));
                player.sendMessage(plugin.formatMessage(prefix + " &7Hints away: &6" + hintsAway));
                player.sendMessage(plugin.formatMessage(prefix + " &7Hints remaining: &6" + hintsRemaining));
                break;
                
            default:
                player.sendMessage(plugin.formatMessage(prefix + " &cUsage: &7/treasure [hint|claim|info]"));
                break;
        }
        
        return true;
    }
}




