package com.playpandora.pandoraitems;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Wrapper for LevelPlugin API
 */
public class LevelAPI {
    
    private final Object levelPluginAPI;
    
    public LevelAPI(Object levelPluginAPI) {
        this.levelPluginAPI = levelPluginAPI;
    }
    
    public int getLevel(Player player) {
        return getLevel(player.getUniqueId());
    }
    
    public int getLevel(UUID uuid) {
        try {
            java.lang.reflect.Method getLevel = levelPluginAPI.getClass().getMethod("getLevel", UUID.class);
            return (Integer) getLevel.invoke(levelPluginAPI, uuid);
        } catch (Exception e) {
            return 0;
        }
    }
    
    public boolean hasLevel(Player player, int requiredLevel) {
        return hasLevel(player.getUniqueId(), requiredLevel);
    }
    
    public boolean hasLevel(UUID uuid, int requiredLevel) {
        try {
            java.lang.reflect.Method hasLevel = levelPluginAPI.getClass().getMethod("hasLevel", UUID.class, int.class);
            return (Boolean) hasLevel.invoke(levelPluginAPI, uuid, requiredLevel);
        } catch (Exception e) {
            return false;
        }
    }
}




