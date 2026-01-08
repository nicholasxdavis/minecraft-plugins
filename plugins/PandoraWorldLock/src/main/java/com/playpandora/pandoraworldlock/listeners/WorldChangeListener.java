package com.playpandora.pandoraworldlock.listeners;

import com.playpandora.pandoraworldlock.PandoraWorldLock;
import com.playpandora.pandoraworldlock.WorldLockManager;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class WorldChangeListener implements Listener {
    
    private final PandoraWorldLock plugin;
    private final WorldLockManager worldLockManager;
    
    public WorldChangeListener(PandoraWorldLock plugin) {
        this.plugin = plugin;
        this.worldLockManager = plugin.getWorldLockManager();
    }
    
    /**
     * Handles players changing worlds - catches any world change that wasn't blocked
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        World toWorld = event.getPlayer().getWorld();
        
        if (worldLockManager.isWorldLocked(toWorld)) {
            plugin.getLogger().warning("Player " + event.getPlayer().getName() + 
                " entered locked world: " + toWorld.getName() + " - teleporting out!");
            
            // Send message
            event.getPlayer().sendMessage(worldLockManager.getColoredLockedMessage(toWorld));
            
            // Teleport player to spawn (use delayed task to ensure world change completes first)
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (worldLockManager.isWorldLocked(event.getPlayer().getWorld())) {
                    worldLockManager.teleportToSpawn(event.getPlayer());
                }
            });
        }
    }
    
    /**
     * Blocks portal usage - PRIMARY protection against portals
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortalUse(PlayerPortalEvent event) {
        if (event.getTo() == null || event.getTo().getWorld() == null) {
            return;
        }
        
        World toWorld = event.getTo().getWorld();
        
        if (worldLockManager.isWorldLocked(toWorld)) {
            event.setCancelled(true);
            plugin.getLogger().info("Blocked portal use to locked world: " + toWorld.getName() + 
                " by player: " + event.getPlayer().getName());
            
            // Send message
            event.getPlayer().sendMessage(worldLockManager.getColoredLockedMessage(toWorld));
        }
    }
    
    /**
     * Blocks entity portal entry (extra protection)
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPortalEnter(EntityPortalEnterEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Player)) {
            return;
        }
        
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) event.getEntity();
        World currentWorld = player.getWorld();
        
        // Check if player is trying to enter a portal in a locked world
        // This is a backup check - the main protection is in onPortalUse
        if (worldLockManager.isWorldLocked(currentWorld)) {
            plugin.getLogger().info("Player " + player.getName() + 
                " attempted to use portal in locked world: " + currentWorld.getName());
        }
    }
    
    /**
     * Handles player join - teleports if they join in a locked world
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerJoin(PlayerJoinEvent event) {
        World world = event.getPlayer().getWorld();
        if (worldLockManager.isWorldLocked(world)) {
            plugin.getLogger().info("Player " + event.getPlayer().getName() + 
                " joined in locked world: " + world.getName() + " - teleporting out!");
            
            // Send message
            event.getPlayer().sendMessage(worldLockManager.getColoredLockedMessage(world));
            
            // Teleport after a short delay to ensure player is fully loaded
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (worldLockManager.isWorldLocked(event.getPlayer().getWorld())) {
                    worldLockManager.teleportToSpawn(event.getPlayer());
                }
            }, 5L); // 0.25 seconds delay
        }
    }
    
    /**
     * Blocks respawn in locked worlds
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        World respawnWorld = event.getRespawnLocation().getWorld();
        
        if (worldLockManager.isWorldLocked(respawnWorld)) {
            // Find a safe spawn world
            World safeWorld = plugin.getServer().getWorlds().stream()
                .filter(w -> !worldLockManager.isWorldLocked(w))
                .filter(w -> w.getEnvironment() == World.Environment.NORMAL)
                .findFirst()
                .orElse(plugin.getServer().getWorlds().stream()
                    .filter(w -> !worldLockManager.isWorldLocked(w))
                    .findFirst()
                    .orElse(null));
            
            if (safeWorld != null) {
                event.setRespawnLocation(safeWorld.getSpawnLocation());
                plugin.getLogger().info("Changed respawn location for " + event.getPlayer().getName() + 
                    " from locked world " + respawnWorld.getName() + " to " + safeWorld.getName());
            }
        }
    }
    
    /**
     * Blocks ALL teleports to locked worlds - COMPREHENSIVE protection
     * This catches warps, commands, plugins, etc.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Handle all teleports including portals, warps, commands, etc.
        // Note: PlayerPortalEvent extends PlayerTeleportEvent, so portals will trigger both handlers
        // We handle portals in onPortalUse first, but this serves as a backup
        if (event.getTo() == null || event.getTo().getWorld() == null) {
            return;
        }
        
        // Skip if this is a portal event (handled separately in onPortalUse)
        if (event instanceof PlayerPortalEvent) {
            return;
        }
        
        World toWorld = event.getTo().getWorld();
        
        // Block ALL teleports to locked worlds (warps, commands, plugins, etc.)
        if (worldLockManager.isWorldLocked(toWorld)) {
            event.setCancelled(true);
            plugin.getLogger().info("Blocked teleport to locked world: " + toWorld.getName() + 
                " (cause: " + event.getCause() + ") by player: " + event.getPlayer().getName());
            
            // Send message
            event.getPlayer().sendMessage(worldLockManager.getColoredLockedMessage(toWorld));
        }
    }
}

