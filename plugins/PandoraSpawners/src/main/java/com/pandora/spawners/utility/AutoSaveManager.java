package com.pandora.spawners.utility;

import com.pandora.spawners.PandoraSpawners;
import com.pandora.spawners.configuration.location.LocationRegistry;
import com.tcoded.folialib.wrapper.task.WrappedTask;

/**
 * Manages automatic saving of all plugin data.
 */
public final class AutoSaveManager {
    
    private static WrappedTask autoSaveTask;
    private static final long AUTO_SAVE_INTERVAL = 20 * 60 * 5; // 5 minutes in ticks
    
    /**
     * Starts the auto-save task.
     */
    public static void start() {
        if (autoSaveTask != null && !autoSaveTask.isCancelled()) {
            autoSaveTask.cancel();
        }
        
        @SuppressWarnings("deprecation")
        WrappedTask task = PandoraSpawners.scheduler().runTimer(() -> {
            try {
                saveAll();
            } catch (Exception e) {
                PandoraSpawners.instance().getLogger().severe("Error during auto-save: " + e.getMessage());
                e.printStackTrace();
            }
        }, AUTO_SAVE_INTERVAL, AUTO_SAVE_INTERVAL);
        
        autoSaveTask = task;
        PandoraSpawners.instance().getLogger().info("Auto-save enabled (every 5 minutes)");
    }
    
    /**
     * Stops the auto-save task.
     */
    public static void stop() {
        if (autoSaveTask != null && !autoSaveTask.isCancelled()) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
    }
    
    /**
     * Saves all data immediately.
     */
    public static void saveAll() {
        try {
            // Save spawner data
            SpawnerDataStorage.saveAll();
            
            // Save drop limit data
            DropLimitStorage.saveAll();
            
            // Save player location data (LocationRegistry handles this)
            LocationRegistry.clear(); // This saves all player data
            
            PandoraSpawners.instance().getLogger().info("Auto-save completed");
        } catch (Exception e) {
            PandoraSpawners.instance().getLogger().severe("Error saving data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

