package com.playpandora.levelenchant.listeners;

import com.playpandora.levelenchant.LevelEnchant;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;

public class CraftListener implements Listener {
    
    private final LevelEnchant plugin;
    
    public CraftListener(LevelEnchant plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Store the player's current XP level and progress
        int currentLevel = player.getLevel();
        float currentExp = player.getExp();
        
        // Schedule a task to restore XP after crafting
        // This prevents crafting from consuming levels
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // Only restore if we're displaying LevelPlugin level on XP bar
            if (plugin.getConfig().getBoolean("features.display-level-on-xp-bar", true)) {
                // The XP bar manager will update it properly
                if (plugin.getXPBarManager() != null) {
                    plugin.getXPBarManager().updatePlayer(player);
                }
            } else {
                // If not using XP bar display, restore vanilla XP
                player.setLevel(currentLevel);
                player.setExp(currentExp);
            }
        });
    }
}


