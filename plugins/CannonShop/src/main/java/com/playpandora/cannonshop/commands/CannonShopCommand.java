package com.playpandora.cannonshop.commands;

import com.playpandora.cannonshop.CannonShop;
import com.playpandora.cannonshop.gui.CannonShopGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CannonShopCommand implements CommandExecutor {
    
    private final CannonShop plugin;
    
    public CannonShopCommand(CannonShop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&ePandora");
            String message = "{prefix} &7This command can only be used by players!"
                .replace("{prefix}", prefix);
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            return true;
        }
        
        // Open the shop GUI
        CannonShopGUI gui = new CannonShopGUI(plugin);
        gui.openShop(player);
        
        return true;
    }
}

