package com.playpandora.moremobs;

import com.playpandora.moremobs.handlers.*;
import org.bukkit.plugin.java.JavaPlugin;

public class MoreMobs extends JavaPlugin {
    
    private static MoreMobs instance;
    
    private SpawnLogicHandler spawnLogicHandler;
    private DropModifier dropModifier;
    private MobBuffHandler mobBuffHandler;
    private MiniEventsHandler miniEventsHandler;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Save default config
            saveDefaultConfig();
            
            // Initialize handlers
            getLogger().info("Initializing MoreMobs handlers...");
            
            if (getConfig().getBoolean("spawn-logic.enabled", true)) {
                try {
                    spawnLogicHandler = new SpawnLogicHandler(this);
                    getLogger().info("Spawn logic handler enabled");
                } catch (Exception e) {
                    getLogger().warning("Failed to enable spawn logic handler: " + e.getMessage());
                }
            }
            
            if (getConfig().getBoolean("drop-improvements.enabled", true)) {
                try {
                    dropModifier = new DropModifier(this);
                    getLogger().info("Drop improvements enabled");
                } catch (Exception e) {
                    getLogger().warning("Failed to enable drop improvements: " + e.getMessage());
                }
            }
            
            if (getConfig().getBoolean("mob-buffs.enabled", true)) {
                try {
                    mobBuffHandler = new MobBuffHandler(this);
                    getLogger().info("Mob buffs enabled");
                } catch (Exception e) {
                    getLogger().warning("Failed to enable mob buffs: " + e.getMessage());
                }
            }
            
            if (getConfig().getBoolean("mini-events.enabled", true)) {
                try {
                    miniEventsHandler = new MiniEventsHandler(this);
                    getLogger().info("Mini-events enabled");
                } catch (Exception e) {
                    getLogger().warning("Failed to enable mini-events: " + e.getMessage());
                }
            }
            
            getLogger().info("MoreMobs v" + getDescription().getVersion() + " has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable MoreMobs! " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            // Cleanup
            if (spawnLogicHandler != null) {
                spawnLogicHandler.cleanup();
            }
            
            if (dropModifier != null) {
                dropModifier.cleanup();
            }
            
            if (mobBuffHandler != null) {
                mobBuffHandler.cleanup();
            }
            
            if (miniEventsHandler != null) {
                miniEventsHandler.cleanup();
            }
            
            // Cancel all tasks
            getServer().getScheduler().cancelTasks(this);
            
            getLogger().info("MoreMobs has been disabled!");
        } catch (Exception e) {
            getLogger().severe("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static MoreMobs getInstance() {
        return instance;
    }
    
    public SpawnLogicHandler getSpawnLogicHandler() {
        return spawnLogicHandler;
    }
    
    public DropModifier getDropModifier() {
        return dropModifier;
    }
    
    public MobBuffHandler getMobBuffHandler() {
        return mobBuffHandler;
    }
    
    public MiniEventsHandler getMiniEventsHandler() {
        return miniEventsHandler;
    }
}

