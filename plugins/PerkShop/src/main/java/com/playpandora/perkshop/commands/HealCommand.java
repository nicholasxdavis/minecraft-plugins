package com.playpandora.perkshop.commands;

import com.playpandora.perkshop.PerkShop;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class HealCommand implements CommandExecutor {
    
    private final PerkShop plugin;
    private static final long HEAL_COOLDOWN_MS = 3600000; // 1 hour in milliseconds
    
    public HealCommand(PerkShop plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7This command can only be used by players!"));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if player has the heal perk
        if (!plugin.getPurchaseManager().hasPerk(player.getUniqueId(), "heal")) {
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7You need to purchase the &6Heal &7perk from the shop first!"));
            return true;
        }
        
        // Check cooldown (using config cooldown in seconds)
        int cooldownSeconds = plugin.getConfig().getInt("perks.heal.cooldown", 300);
        UUID uuid = player.getUniqueId();
        
        if (plugin.getPlayerDataManager() != null && 
            plugin.getPlayerDataManager().isOnCooldown(uuid, "heal", cooldownSeconds)) {
            long remaining = plugin.getPlayerDataManager().getCooldownRemaining(uuid, "heal", cooldownSeconds);
            long minutes = remaining / 60;
            long seconds = remaining % 60;
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&e&lPandora &8» &r&7You must wait &6" + minutes + "m " + seconds + "s &7before using heal again!"));
            return true;
        }
        
        // Also check old cooldown system for backward compatibility
        long lastUsed = plugin.getDataManager().getLastHealUse(player.getUniqueId());
        long currentTime = System.currentTimeMillis();
        long timeRemaining = (lastUsed + HEAL_COOLDOWN_MS) - currentTime;
        
        if (timeRemaining > 0) {
            long minutes = timeRemaining / 60000;
            long seconds = (timeRemaining % 60000) / 1000;
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&e&lPandora &8» &r&7You must wait &6" + minutes + "m " + seconds + "s &7before using heal again!"));
            return true;
        }
        
        // Heal the player
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setFireTicks(0);
        
        // Update cooldown in both systems
        long currentTimeSeconds = System.currentTimeMillis() / 1000;
        if (plugin.getPlayerDataManager() != null) {
            plugin.getPlayerDataManager().setCooldown(uuid, "heal", currentTimeSeconds);
        }
        plugin.getDataManager().setLastHealUse(player.getUniqueId(), currentTime);
        
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7You have been healed!"));
        
        return true;
    }
}


