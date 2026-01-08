package com.pandora.spawners.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.pandora.spawners.text.Text;
import com.pandora.spawners.utility.reflect.Reflect.RF;

public final class HookLevelPlugin implements HookInstance {
	
	private Plugin levelPlugin;
	private Object levelAPI;
	
	@Override
	public boolean exists() {
		return levelPlugin != null && levelAPI != null;
	}
	
	@Override
	public String message() {
		return "LevelPlugin has been found, custom levels enabled!";
	}

	@Override
	public void load() {
		Plugin plugin = Bukkit.getPluginManager().getPlugin("LevelPlugin");
		if(plugin == null) return;
		this.levelPlugin = plugin;
		
		try {
			// Get LevelAPI from LevelPlugin
			Class<?> apiClass = Class.forName("com.playpandora.levelplugin.api.LevelAPI");
			// LevelPlugin.getAPI() returns LevelAPI instance
			Object apiInstance = RF.order(plugin.getClass(), "getAPI").invoke(plugin);
			if(apiInstance != null) {
				this.levelAPI = apiInstance;
			}
		} catch (Exception e) {
			RF.debug(e);
		}
	}
	
	/**
	 * Get player's level from LevelPlugin
	 */
	public int getLevel(Player player) {
		if(!exists() || levelAPI == null) return 0;
		try {
			return (Integer) RF.order(levelAPI.getClass(), "getLevel", Player.class).invoke(levelAPI, player);
		} catch (Exception e) {
			RF.debug(e);
			return 0;
		}
	}
	
	/**
	 * Check if player has required level
	 */
	public boolean hasLevel(Player player, int requiredLevel) {
		if(!exists() || levelAPI == null) return false;
		try {
			return (Boolean) RF.order(levelAPI.getClass(), "hasLevel", Player.class, int.class).invoke(levelAPI, player, requiredLevel);
		} catch (Exception e) {
			RF.debug(e);
			return false;
		}
	}
	
	/**
	 * Remove levels from player by subtracting XP
	 */
	public void removeLevel(Player player, int amount) {
		if(!exists() || levelAPI == null) return;
		try {
			// Get LevelManager from LevelPlugin
			Object levelManager = RF.order(levelPlugin.getClass(), "getLevelManager").invoke(levelPlugin);
			if(levelManager != null) {
				// Get current level and XP
				int currentLevel = getLevel(player);
				double currentXP = (Double) RF.order(levelManager.getClass(), "getXP", java.util.UUID.class).invoke(levelManager, player.getUniqueId());
				
				// Calculate XP required for target level (current - amount)
				int targetLevel = Math.max(0, currentLevel - amount);
				double targetXP = (Double) RF.order(levelManager.getClass(), "getXPRequiredForLevel", int.class).invoke(levelManager, targetLevel);
				
				// Set XP to target level
				RF.order(levelManager.getClass(), "setXP", java.util.UUID.class, double.class).invoke(levelManager, player.getUniqueId(), targetXP);
			}
		} catch (Exception e) {
			RF.debug(e);
		}
	}

}

