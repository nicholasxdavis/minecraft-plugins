package com.playpandora.perkshop.commands;

import com.playpandora.perkshop.PerkShop;
import com.playpandora.perkshop.models.Perk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;

public class PerksCommand implements CommandExecutor {
    
    private final PerkShop plugin;
    
    public PerksCommand(PerkShop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7This command can only be used by players!"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("perkshop.reload")) {
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7You don't have permission to use this command!"));
                return true;
            }
            
            // Re-grant all owned perk permissions
            Set<String> ownedPerks = plugin.getDataManager().getPlayerPerks(player.getUniqueId());
            int granted = 0;
            
            for (String perkKey : ownedPerks) {
                Perk perk = plugin.getPerkManager().getPerk(perkKey);
                if (perk != null && perk.getPermission() != null && !perk.getPermission().isEmpty()) {
                    plugin.getPurchaseManager().grantPermission(player, perk.getPermission());
                    granted++;
                }
            }
            
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7Re-granted permissions for &6" + granted + " &7owned perks!"));
            return true;
        }
        
        // Show owned perks
        Set<String> ownedPerks = plugin.getDataManager().getPlayerPerks(player.getUniqueId());
        if (ownedPerks.isEmpty()) {
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7You don't own any perks yet. Use &6/shop &7to buy some!"));
            return true;
        }
        
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lYour Perks:"));
        for (String perkKey : ownedPerks) {
            Perk perk = plugin.getPerkManager().getPerk(perkKey);
            if (perk != null) {
                String status = player.hasPermission(perk.getPermission()) ? "&e✓" : "&7✗";
                // Strip color codes from perk name to avoid showing raw codes
                String perkName = perk.getName().replaceAll("&[0-9a-fk-orA-FK-OR]", "").replaceAll("§[0-9a-fk-orA-FK-OR]", "");
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "  " + status + " &7" + perkName));
            }
        }
        
        return true;
    }
}

