package com.playpandora.craftaccess.commands;

import com.playpandora.craftaccess.CraftAccess;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;

public class CraftCommand implements CommandExecutor {
    
    private final CraftAccess plugin;
    
    public CraftCommand(CraftAccess plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§e§lPandora §7This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if player has unlocked crafting table
        if (!plugin.getDataManager().hasUnlocked(player.getUniqueId(), "craft")) {
            String blockName = plugin.getConfig().getString("unlock-blocks.craft", "CRAFTING_TABLE");
            player.sendMessage(plugin.getMessage("not-unlocked").replace("{block}", blockName.replace("_", " ")));
            return true;
        }
        
        // Check level requirement
        int requiredLevel = plugin.getConfig().getInt("level-requirements.craft", 5);
        if (plugin.getLevelIntegration().isAvailable()) {
            int playerLevel = plugin.getLevelIntegration().getPlayerLevel(player);
            if (playerLevel < requiredLevel) {
                player.sendMessage(plugin.getMessage("level-required")
                    .replace("{level}", String.valueOf(requiredLevel))
                    .replace("{current}", String.valueOf(playerLevel)));
                return true;
            }
        }
        
        // Open crafting table
        try {
            InventoryView view = player.openWorkbench(null, true);
            if (view != null) {
                String type = "Crafting Table";
                player.sendMessage(plugin.getMessage("opened").replace("{type}", type));
            } else {
                player.sendMessage(plugin.getMessage("error").replace("{type}", "crafting table"));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error opening crafting table for " + player.getName() + ": " + e.getMessage());
            player.sendMessage(plugin.getMessage("error").replace("{type}", "crafting table"));
        }
        
        return true;
    }
}

