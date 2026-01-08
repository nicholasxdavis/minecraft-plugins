package com.playpandora.pandoracrates.commands;

import com.playpandora.pandoracrates.PandoraCrates;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CratesCommand implements CommandExecutor {
    
    private final PandoraCrates plugin;
    
    public CratesCommand(PandoraCrates plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.formatMessage("no-permission",
                "{prefix} &cThis command can only be used by players!"));
            return true;
        }
        
        // Execute /warp crates command
        Player player = (Player) sender;
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "warp crates " + player.getName());
        
        return true;
    }
}




