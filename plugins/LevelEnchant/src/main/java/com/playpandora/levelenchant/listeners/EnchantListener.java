package com.playpandora.levelenchant.listeners;

import com.playpandora.levelenchant.LevelEnchant;
import com.playpandora.levelenchant.managers.LevelIntegration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;

public class EnchantListener implements Listener {
    
    private final LevelEnchant plugin;
    private final LevelIntegration levelIntegration;
    
    public EnchantListener(LevelEnchant plugin) {
        this.plugin = plugin;
        this.levelIntegration = plugin.getLevelIntegration();
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        if (!levelIntegration.isAvailable()) {
            return;
        }
        
        Player player = event.getEnchanter();
        int playerLevel = levelIntegration.getLevel(player);
        
        // Get the required level for the enchantment (use the first available enchantment level)
        // We'll check if player has enough level for at least the minimum enchantment
        int[] levels = event.getExpLevelCostsOffered();
        if (levels != null && levels.length > 0) {
            int minRequiredLevel = levels[0];
            
            // Convert vanilla XP level requirement to LevelPlugin level requirement
            // For now, we'll use a 1:1 ratio, but you can adjust this
            int requiredLevel = minRequiredLevel;
            
            if (playerLevel < requiredLevel) {
                // Cancel the enchantment if player doesn't have enough level
                event.setCancelled(true);
                player.sendMessage(plugin.getMessage("not-enough-level")
                    .replace("{required_level}", String.valueOf(requiredLevel))
                    .replace("{player_level}", String.valueOf(playerLevel)));
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnchantItem(EnchantItemEvent event) {
        if (!levelIntegration.isAvailable()) {
            return;
        }
        
        Player player = event.getEnchanter();
        int cost = event.getExpLevelCost();
        
        // Prevent vanilla XP from being consumed
        // We'll use LevelPlugin levels instead, so restore vanilla XP immediately
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // Restore the vanilla XP level (we're using LevelPlugin levels instead)
            // The XP bar manager will update it to show LevelPlugin level
            if (plugin.getXPBarManager() != null) {
                plugin.getXPBarManager().updatePlayer(player);
            } else {
                // If XP bar manager isn't running, at least restore the level
                int playerLevel = levelIntegration.getLevel(player);
                player.setLevel(playerLevel);
            }
        });
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!levelIntegration.isAvailable()) {
            return;
        }
        
        // When player opens enchanting table, set their vanilla level to match LevelPlugin level
        // This ensures the enchanting table shows the correct enchantment options
        if (event.getInventory().getType() == InventoryType.ENCHANTING) {
            if (event.getPlayer() instanceof Player) {
                Player player = (Player) event.getPlayer();
                int playerLevel = levelIntegration.getLevel(player);
                
                // Set vanilla level to match LevelPlugin level temporarily
                // This allows the enchanting table to show correct enchantments
                player.setLevel(playerLevel);
                
                // Also update XP progress
                double currentXP = levelIntegration.getXPForCurrentLevel(player);
                double requiredXP = levelIntegration.getXPRequiredForNextLevel(player);
                float progress = 0.0f;
                if (requiredXP > 0) {
                    progress = (float) Math.min(1.0, currentXP / requiredXP);
                }
                player.setExp(progress);
            }
        }
    }
}

