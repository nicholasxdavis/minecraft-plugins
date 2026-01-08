package com.playpandora.farmshop.commands;

import com.playpandora.farmshop.FarmShop;
import com.playpandora.farmshop.gui.FarmShopGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FarmShopCommand implements CommandExecutor {
    
    private final FarmShop plugin;
    
    public FarmShopCommand(FarmShop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&e&lPandora");
            String message = "{prefix} &7This command can only be used by players!"
                .replace("{prefix}", prefix);
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
            return true;
        }
        
        // Open the shop GUI
        FarmShopGUI gui = new FarmShopGUI(plugin);
        gui.openShop(player);
        
        return true;
    }
}


