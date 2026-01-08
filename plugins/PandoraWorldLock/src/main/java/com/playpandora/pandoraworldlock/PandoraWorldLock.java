package com.playpandora.pandoraworldlock;

import com.playpandora.pandoraworldlock.commands.WorldLockCommand;
import com.playpandora.pandoraworldlock.listeners.WorldChangeListener;
import com.playpandora.pandoraworldlock.listeners.WarpCommandListener;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class PandoraWorldLock extends JavaPlugin {
    
    private static PandoraWorldLock instance;
    private WorldLockManager worldLockManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize world lock manager
        worldLockManager = new WorldLockManager(this);
        
        // Register command
        WorldLockCommand commandHandler = new WorldLockCommand(this);
        org.bukkit.command.PluginCommand command = getCommand("pwl");
        if (command != null) {
            command.setExecutor(commandHandler);
            command.setTabCompleter(commandHandler);
        } else {
            getLogger().warning("Command 'pwl' not found in plugin.yml! Commands may not work.");
        }
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new WorldChangeListener(this), this);
        getServer().getPluginManager().registerEvents(new WarpCommandListener(this), this);
        
        // Start periodic check for players in locked worlds (runs every 2 seconds)
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    World currentWorld = player.getWorld();
                    
                    if (worldLockManager.isWorldLocked(currentWorld)) {
                        // Send message before teleporting
                        player.sendMessage(worldLockManager.getColoredLockedMessage(currentWorld));
                        
                        // Teleport to spawn
                        worldLockManager.teleportToSpawn(player);
                        getLogger().info("Teleported player " + player.getName() + " from locked world: " + currentWorld.getName());
                    }
                }
            }
        }.runTaskTimer(this, 20L, 40L); // Start after 1 second, then every 2 seconds
        
        // Log status
        getLogger().info("PandoraWorldLock v" + getDescription().getVersion() + " has been enabled!");
        int lockedCount = (int) getServer().getWorlds().stream()
            .filter(worldLockManager::isWorldLocked)
            .count();
        getLogger().info("Currently locking " + lockedCount + " world(s)");
        
        // Check for EssentialsX
        if (getServer().getPluginManager().getPlugin("Essentials") != null) {
            getLogger().info("EssentialsX detected - warp command blocking enabled");
        }
    }
    
    @Override
    public void onDisable() {
        getLogger().info("PandoraWorldLock has been disabled!");
    }
    
    public static PandoraWorldLock getInstance() {
        return instance;
    }
    
    public WorldLockManager getWorldLockManager() {
        return worldLockManager;
    }
}

