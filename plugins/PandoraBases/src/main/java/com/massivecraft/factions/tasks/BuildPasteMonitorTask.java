package com.massivecraft.factions.tasks;

import com.massivecraft.factions.*;
import com.massivecraft.factions.util.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

/**
 * Monitors for BuildPaste operations and blocks them in claimed territories
 * This is a backup system for when BuildPaste uses direct block setting methods
 */
public class BuildPasteMonitorTask extends BukkitRunnable {
    
    // Store previous block states to detect rapid changes (indicating BuildPaste)
    private static final Map<String, BlockState> previousStates = new HashMap<>();
    private static final int MONITOR_INTERVAL_TICKS = 1; // Check every tick for rapid changes
    
    private static class BlockState {
        Material material;
        byte data; // For legacy support
        
        BlockState(Material material, byte data) {
            this.material = material;
            this.data = data;
        }
    }
    
    @Override
    public void run() {
        // This task would monitor for rapid block changes
        // However, this is resource-intensive, so we rely mainly on event handlers
        // This is a placeholder for future enhancement if needed
    }
    
    /**
     * Check if a block change should be allowed in claimed territory
     * Called from various event handlers
     */
    public static boolean canChangeBlock(Location location) {
        if (location == null || location.getWorld() == null) {
            return true;
        }
        
        FLocation floc = FLocation.wrap(location);
        Faction faction = Board.getInstance().getFactionAt(floc);
        
        // Allow in wilderness, block in claimed areas
        if (faction != null && !faction.isWilderness()) {
            Logger.print("Block change blocked in claimed territory at " + 
                location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + 
                " (Faction: " + faction.getTag() + ")", Logger.PrefixType.DEFAULT);
            return false;
        }
        
        return true;
    }
}

