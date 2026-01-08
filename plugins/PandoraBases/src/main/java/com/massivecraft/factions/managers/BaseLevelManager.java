package com.massivecraft.factions.managers;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.util.Logger;

/**
 * Manages base level system for factions
 * Base level affects obsidian health (+10% per level)
 */
public class BaseLevelManager {
    
    private static final int MAX_LEVEL = 5;
    // Increased costs: 3x, 4x, 5x, 6x, 7x the original costs
    private static final double[] LEVEL_COSTS = {450000.0, 1000000.0, 1750000.0, 2700000.0, 3850000.0};
    private static final double HEALTH_BONUS_PER_LEVEL = 0.11; // 11% per level (increased by 10%)
    
    /**
     * Get the base level for a faction
     */
    public static int getBaseLevel(Faction faction) {
        if (faction == null || !faction.isNormal()) {
            return 0;
        }
        
        int level = faction.getUpgrade("BaseLevel");
        return Math.min(MAX_LEVEL, Math.max(0, level));
    }
    
    /**
     * Set the base level for a faction
     */
    public static boolean setBaseLevel(Faction faction, int level) {
        if (faction == null || !faction.isNormal()) {
            return false;
        }
        
        if (level < 0 || level > MAX_LEVEL) {
            return false;
        }
        
        faction.setUpgrade("BaseLevel", level);
        return true;
    }
    
    /**
     * Get the cost to upgrade to the next level
     */
    public static double getUpgradeCost(int currentLevel) {
        if (currentLevel < 0 || currentLevel >= MAX_LEVEL) {
            return -1; // Invalid level or max level reached
        }
        return LEVEL_COSTS[currentLevel];
    }
    
    /**
     * Get max base level
     */
    public static int getMaxLevel() {
        return MAX_LEVEL;
    }
    
    /**
     * Calculate obsidian health multiplier based on base level
     * Returns multiplier (e.g., 1.0 for level 0, 1.1 for level 1, 1.5 for level 5)
     */
    public static double getObsidianHealthMultiplier(Faction faction) {
        int level = getBaseLevel(faction);
        return 1.0 + (level * HEALTH_BONUS_PER_LEVEL);
    }
    
    /**
     * Get effective obsidian max health for a faction
     */
    public static int getEffectiveObsidianHealth(Faction faction) {
        double baseHealth = com.massivecraft.factions.Conf.obsidianMaxHealth;
        double multiplier = getObsidianHealthMultiplier(faction);
        return (int) Math.round(baseHealth * multiplier);
    }
    
    /**
     * Get health bonus per level (as percentage, e.g., 11.0 for 11%)
     */
    public static double getHealthBonusPerLevelPercent() {
        return HEALTH_BONUS_PER_LEVEL * 100.0;
    }
}

