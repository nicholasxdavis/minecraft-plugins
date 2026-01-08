package com.playpandora.pandoracrates.managers;

import com.playpandora.pandoracrates.PandoraCrates;
import org.bukkit.scheduler.BukkitTask;

public class SaveManager {
    
    private final PandoraCrates plugin;
    private BukkitTask autoSaveTask;
    private long autoSaveInterval; // in ticks (20 ticks = 1 second)
    
    public SaveManager(PandoraCrates plugin) {
        this.plugin = plugin;
        // Auto-save every 5 minutes by default (6000 ticks)
        boolean enabled = plugin.getConfig().getBoolean("auto-save.enabled", true);
        this.autoSaveInterval = plugin.getConfig().getLong("auto-save.interval-ticks", 6000L);
        
        if (enabled) {
            startAutoSave();
        } else {
            plugin.getLogger().info("Auto-save is disabled in config");
        }
    }
    
    private void startAutoSave() {
        if (autoSaveInterval <= 0) {
            plugin.getLogger().info("Auto-save is disabled (interval <= 0)");
            return;
        }
        
        autoSaveTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            plugin.getLogger().info("Auto-saving all data...");
            saveAllSync();
            plugin.getLogger().info("Auto-save complete!");
        }, autoSaveInterval, autoSaveInterval);
        
        plugin.getLogger().info("Auto-save enabled (interval: " + (autoSaveInterval / 20) + " seconds)");
    }
    
    public void saveAll() {
        try {
            // Save all data synchronously on main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                try {
                    if (plugin.getStatisticsManager() != null) {
                        plugin.getStatisticsManager().saveStats();
                    }
                    
                    if (plugin.getLocationDataManager() != null) {
                        plugin.getLocationDataManager().saveDataFile();
                    }
                    
                    if (plugin.getCooldownManager() != null) {
                        plugin.getCooldownManager().saveCooldowns();
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error during save: " + e.getMessage());
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            plugin.getLogger().warning("Error during auto-save scheduling: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }
    
    public void saveAllSync() {
        // Save all data synchronously (call from main thread)
        try {
            if (plugin.getStatisticsManager() != null) {
                plugin.getStatisticsManager().saveStats();
            }
            
            if (plugin.getLocationDataManager() != null) {
                plugin.getLocationDataManager().saveDataFile();
            }
            
            if (plugin.getCooldownManager() != null) {
                plugin.getCooldownManager().saveCooldowns();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error during save: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }
    
    public void stopAutoSave() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
    }
    
    public void reload() {
        stopAutoSave();
        autoSaveInterval = plugin.getConfig().getLong("auto-save.interval-ticks", 6000L);
        startAutoSave();
    }
}

