package com.playpandora.levelplugin.api;

import com.playpandora.levelplugin.LevelPlugin;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Public API for LevelPlugin
 * Other plugins can use this to check player levels and XP
 */
public class LevelAPI {
    
    private final LevelPlugin plugin;
    
    public LevelAPI(LevelPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Get a player's current level
     * @param player The player
     * @return The player's level (0 if not found)
     */
    public int getLevel(Player player) {
        return getLevel(player.getUniqueId());
    }
    
    /**
     * Get a player's current level by UUID
     * @param uuid The player's UUID
     * @return The player's level (0 if not found)
     */
    public int getLevel(UUID uuid) {
        return plugin.getLevelManager().getLevel(uuid);
    }
    
    /**
     * Get a player's current XP
     * @param player The player
     * @return The player's current XP
     */
    public double getXP(Player player) {
        return getXP(player.getUniqueId());
    }
    
    /**
     * Get a player's current XP by UUID
     * @param uuid The player's UUID
     * @return The player's current XP
     */
    public double getXP(UUID uuid) {
        return plugin.getLevelManager().getXP(uuid);
    }
    
    /**
     * Get XP required for next level
     * @param player The player
     * @return XP required for next level
     */
    public double getXPRequired(Player player) {
        return getXPRequired(player.getUniqueId());
    }
    
    /**
     * Get XP required for next level by UUID
     * @param uuid The player's UUID
     * @return XP required for next level
     */
    public double getXPRequired(UUID uuid) {
        int level = getLevel(uuid);
        return plugin.getLevelManager().getXPRequiredForLevel(level + 1);
    }
    
    /**
     * Check if a player meets a level requirement
     * @param player The player
     * @param requiredLevel The required level
     * @return true if player meets the requirement
     */
    public boolean hasLevel(Player player, int requiredLevel) {
        return hasLevel(player.getUniqueId(), requiredLevel);
    }
    
    /**
     * Check if a player meets a level requirement by UUID
     * @param uuid The player's UUID
     * @param requiredLevel The required level
     * @return true if player meets the requirement
     */
    public boolean hasLevel(UUID uuid, int requiredLevel) {
        return getLevel(uuid) >= requiredLevel;
    }
    
    /**
     * Add XP to a player
     * @param player The player
     * @param amount The amount of XP to add
     */
    public void addXP(Player player, double amount) {
        addXP(player.getUniqueId(), amount);
    }
    
    /**
     * Add XP to a player by UUID
     * @param uuid The player's UUID
     * @param amount The amount of XP to add
     */
    public void addXP(UUID uuid, double amount) {
        plugin.getLevelManager().addXP(uuid, amount);
    }
}


