package com.playpandora.levelenchant.managers;

import com.playpandora.levelenchant.LevelEnchant;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class XPBarManager {
    
    private final LevelEnchant plugin;
    private final LevelIntegration levelIntegration;
    private BukkitTask updateTask;
    private final ConcurrentHashMap<UUID, Integer> lastLevel = new ConcurrentHashMap<>();
    
    public XPBarManager(LevelEnchant plugin) {
        this.plugin = plugin;
        this.levelIntegration = plugin.getLevelIntegration();
    }
    
    public void start() {
        int interval = plugin.getConfig().getInt("xp-bar-update-interval", 20);
        
        updateTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                updateXPBar(player);
            }
        }, 0L, interval);
        
        plugin.getLogger().info("XP bar manager started!");
    }
    
    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }
    
    private void updateXPBar(Player player) {
        if (!levelIntegration.isAvailable()) {
            return;
        }
        
        int level = levelIntegration.getLevel(player);
        double currentXP = levelIntegration.getXPForCurrentLevel(player);
        double requiredXP = levelIntegration.getXPRequiredForNextLevel(player);
        
        // Update the level displayed on the XP bar
        player.setLevel(level);
        
        // Calculate progress (0.0 to 1.0)
        float progress = 0.0f;
        if (requiredXP > 0) {
            progress = (float) Math.min(1.0, currentXP / requiredXP);
        }
        
        // Set the XP bar progress
        player.setExp(progress);
        
        // Track level changes and save to DataManager
        UUID uuid = player.getUniqueId();
        Integer lastKnownLevel = lastLevel.get(uuid);
        if (lastKnownLevel == null || lastKnownLevel != level) {
            lastLevel.put(uuid, level);
            // Save player data when level changes
            if (plugin.getDataManager() != null) {
                double xp = levelIntegration.getXP(player);
                plugin.getDataManager().updatePlayerData(uuid, level, xp);
            }
        }
    }
    
    public void updatePlayer(Player player) {
        updateXPBar(player);
    }
}


