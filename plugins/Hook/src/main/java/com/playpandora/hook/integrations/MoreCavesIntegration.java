package com.playpandora.hook.integrations;

import com.playpandora.hook.Hook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MoreCavesIntegration implements Listener {
    
    private final Hook plugin;
    private final Map<UUID, Boolean> playerInCave = new HashMap<>();
    private final Map<UUID, Long> lastCaveNotification = new HashMap<>(); // Track last notification time
    private static final long COOLDOWN_MS = 30 * 60 * 1000; // 30 minutes in milliseconds
    
    public MoreCavesIntegration(Hook plugin) {
        this.plugin = plugin;
    }
    
    public void hook() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("integrations.morecaves.cave-entry-notification", true)) {
            return;
        }
        
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        
        // Check if in cave (underground and low light)
        boolean currentlyInCave = loc.getY() < 60 && 
            loc.getBlock().getLightLevel() <= 5 &&
            loc.getWorld().getEnvironment() == org.bukkit.World.Environment.NORMAL;
        
        Boolean wasInCave = playerInCave.get(player.getUniqueId());
        if (wasInCave == null || !wasInCave) {
            if (currentlyInCave) {
                // Check cooldown - only show if not shown in last 30 minutes
                UUID uuid = player.getUniqueId();
                Long lastNotification = lastCaveNotification.get(uuid);
                long currentTime = System.currentTimeMillis();
                
                if (lastNotification == null || (currentTime - lastNotification) >= COOLDOWN_MS) {
                    // Just entered a cave and cooldown has passed
                    plugin.getAPI().sendCustom(player, "Entered Deep Caves", 
                        "Watch your step...", 500, 2500, 1000);
                    lastCaveNotification.put(uuid, currentTime);
                }
                playerInCave.put(player.getUniqueId(), true);
            }
        } else if (!currentlyInCave) {
            playerInCave.put(player.getUniqueId(), false);
        }
    }
    
    @EventHandler
    public void onCaveMobKill(EntityDeathEvent event) {
        if (!plugin.getConfig().getBoolean("integrations.morecaves.cave-mob-kill-notification", true)) {
            return;
        }
        
        if (!(event.getEntity().getKiller() instanceof Player)) {
            return;
        }
        
        Player killer = event.getEntity().getKiller();
        Location loc = event.getEntity().getLocation();
        
        // Check if kill was in a cave
        if (loc.getY() >= 60 || loc.getBlock().getLightLevel() > 5) {
            return;
        }
        
        // Only notify for certain mobs
        org.bukkit.entity.EntityType type = event.getEntityType();
        if (type == org.bukkit.entity.EntityType.CREEPER ||
            type == org.bukkit.entity.EntityType.SKELETON ||
            type == org.bukkit.entity.EntityType.ZOMBIE) {
            plugin.getAPI().sendCustom(killer, "Cave Kill!", 
                "+" + (int)(Math.random() * 5 + 5) + " XP", 
                300, 2000, 800);
        }
    }
}



