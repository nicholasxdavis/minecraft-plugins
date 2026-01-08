package com.massivecraft.factions.listeners;

import com.massivecraft.factions.*;
import com.massivecraft.factions.util.BaseStructureHelper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * Listener to control mob spawning in base cubes
 * - Prevents natural mob spawning in base cubes
 * - Allows spawner and egg spawns in base cubes
 * - Only allows natural spawning in wilderness
 * 
 * Compatibility Notes:
 * - Works with LevelledMobs: Uses HIGH priority to block natural spawns early,
 *   then LevelledMobs processes remaining spawns at MONITOR priority
 * - Spawner mobs are allowed and will be processed by LevelledMobs (set to level 1)
 * - Stacking from LevelledMobs works on spawner mobs after they spawn
 */
public class BaseMobSpawnListener implements Listener {
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Only handle natural spawns - allow all other spawn reasons
        // This allows spawner, egg, custom spawns which LevelledMobs will process
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return; // Allow spawner, egg, custom, etc. - LevelledMobs will handle these
        }
        
        org.bukkit.Location loc = event.getLocation();
        FLocation floc = FLocation.wrap(loc);
        Faction faction = Board.getInstance().getFactionAt(floc);
        
        // Only allow natural spawning in wilderness
        if (faction == null || !faction.isWilderness()) {
            event.setCancelled(true);
            return;
        }
        
        // If in wilderness, check if it's inside a base cube (shouldn't happen, but double-check)
        if (faction.isWilderness()) {
            // Check all factions to see if this location is inside any base cube
            for (Faction checkFaction : Factions.getInstance().getAllNormalFactions()) {
                if (checkFaction.hasBeacon() && 
                    BaseStructureHelper.isInsideBaseCube(loc, checkFaction)) {
                    // Inside a base cube, cancel natural spawn
                    event.setCancelled(true);
                    return;
                }
            }
        }
        
        // If we get here, it's wilderness and not in a base cube - allow natural spawn
        // This will work with LevelledMobs as it processes spawns after this
    }
}


