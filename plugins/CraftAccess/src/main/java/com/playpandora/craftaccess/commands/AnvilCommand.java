package com.playpandora.craftaccess.commands;

import com.playpandora.craftaccess.CraftAccess;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;

public class AnvilCommand implements CommandExecutor {
    
    private final CraftAccess plugin;
    
    public AnvilCommand(CraftAccess plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§e§lPandora §7This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if player has unlocked anvil
        if (!plugin.getDataManager().hasUnlocked(player.getUniqueId(), "anvil")) {
            String blockName = plugin.getConfig().getString("unlock-blocks.anvil", "ANVIL");
            player.sendMessage(plugin.getMessage("not-unlocked").replace("{block}", blockName.replace("_", " ")));
            return true;
        }
        
        // Check level requirement
        int requiredLevel = plugin.getConfig().getInt("level-requirements.anvil", 15);
        if (plugin.getLevelIntegration().isAvailable()) {
            int playerLevel = plugin.getLevelIntegration().getPlayerLevel(player);
            if (playerLevel < requiredLevel) {
                player.sendMessage(plugin.getMessage("level-required")
                    .replace("{level}", String.valueOf(requiredLevel))
                    .replace("{current}", String.valueOf(playerLevel)));
                return true;
            }
        }
        
        // Open anvil
        try {
            // Use the player's location to create a virtual anvil
            org.bukkit.Location loc = player.getLocation();
            org.bukkit.inventory.InventoryView view = player.openAnvil(loc, true);
            
            if (view != null) {
                String type = "Anvil";
                player.sendMessage(plugin.getMessage("opened").replace("{type}", type));
            } else {
                player.sendMessage(plugin.getMessage("error").replace("{type}", "anvil"));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error opening anvil for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(plugin.getMessage("error").replace("{type}", "anvil"));
        }
        
        return true;
    }
}

