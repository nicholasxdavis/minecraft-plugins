package com.playpandora.pandoraworldlock.listeners;

import com.playpandora.pandoraworldlock.PandoraWorldLock;
import com.playpandora.pandoraworldlock.WorldLockManager;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class WarpCommandListener implements Listener {
    
    private final PandoraWorldLock plugin;
    private final WorldLockManager worldLockManager;
    
    public WarpCommandListener(PandoraWorldLock plugin) {
        this.plugin = plugin;
        this.worldLockManager = plugin.getWorldLockManager();
    }
    
    /**
     * Blocks warp commands to locked worlds (EssentialsX, etc.)
     * This is a backup - the main protection is in PlayerTeleportEvent
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        String command = message.toLowerCase().trim();
        
        // Handle all variations: /warp nether, /warp end, /essentials:warp nether, etc.
        if (!command.startsWith("/warp") && !command.contains(":warp")) {
            return;
        }
        
        // Parse the warp target (everything after "warp")
        String[] parts = message.split("\\s+");
        if (parts.length < 2) {
            return; // Not a warp command with target
        }
        
        String warpTarget = parts[parts.length - 1].toLowerCase().trim();
        
        // Find world by warp target name
        World targetWorld = findWorldByWarpName(warpTarget);
        
        if (targetWorld != null && worldLockManager.isWorldLocked(targetWorld)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(worldLockManager.getColoredLockedMessage(targetWorld));
            plugin.getLogger().info("Blocked warp command to locked world: " + targetWorld.getName() + 
                " (warp: " + warpTarget + ") for player: " + event.getPlayer().getName());
        }
    }
    
    /**
     * Find world by warp target name (handles nether, end, and world names)
     */
    private World findWorldByWarpName(String warpTarget) {
        // Check for nether variations
        if (warpTarget.equals("nether") || warpTarget.equals("the_nether") || 
            warpTarget.equals("the-nether") || warpTarget.contains("nether")) {
            return plugin.getServer().getWorlds().stream()
                .filter(w -> w.getEnvironment() == World.Environment.NETHER)
                .findFirst()
                .orElse(null);
        }
        
        // Check for end variations
        if (warpTarget.equals("end") || warpTarget.equals("the_end") || 
            warpTarget.equals("the-end") || warpTarget.equals("ender") || 
            warpTarget.equals("enderdragon") || warpTarget.contains("end")) {
            return plugin.getServer().getWorlds().stream()
                .filter(w -> w.getEnvironment() == World.Environment.THE_END)
                .findFirst()
                .orElse(null);
        }
        
        // Try to find by exact world name
        for (World world : plugin.getServer().getWorlds()) {
            if (world.getName().equalsIgnoreCase(warpTarget)) {
                return world;
            }
        }
        
        // Try partial match
        for (World world : plugin.getServer().getWorlds()) {
            if (world.getName().toLowerCase().contains(warpTarget) || 
                warpTarget.contains(world.getName().toLowerCase())) {
                return world;
            }
        }
        
        return null;
    }
}

