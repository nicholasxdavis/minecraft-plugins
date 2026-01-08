package com.playpandora.pandoraminer.commands;

import com.playpandora.pandoraminer.PandoraMiner;
import com.playpandora.pandoraminer.integration.PandoraBasesIntegration;
import com.playpandora.pandoraminer.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Command executor for /miner
 */
public class MinerCommand implements CommandExecutor, TabCompleter {
    
    private final PandoraMiner plugin;
    
    public MinerCommand(PandoraMiner plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.error("This command can only be used by players!"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("pandoraminer.use")) {
            player.sendMessage(ColorUtil.error("You don't have permission to use this command!"));
            return true;
        }
        
        // Check if trying to enable miner in claimed land (unless it's their own)
        if (!plugin.getMinerManager().isMiner(player)) {
            if (!PandoraBasesIntegration.canUseMiner(player, player.getLocation())) {
                player.sendMessage(ColorUtil.error("You cannot enable miner mode in claimed land!"));
                return true;
            }
        }
        
        // Toggle miner state
        plugin.getMinerManager().toggleMiner(player);
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>(); // No tab completion needed
    }
}


