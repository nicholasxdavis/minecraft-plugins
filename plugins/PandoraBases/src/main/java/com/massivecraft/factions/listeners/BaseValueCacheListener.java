package com.massivecraft.factions.listeners;

import com.massivecraft.factions.*;
import com.massivecraft.factions.util.BaseStructureHelper;
import com.massivecraft.factions.util.BaseValueCalculator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;

/**
 * Listener to invalidate base value cache when blocks change
 */
public class BaseValueCacheListener implements Listener {
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        FLocation floc = FLocation.wrap(event.getBlock().getLocation());
        Faction faction = Board.getInstance().getFactionAt(floc);
        
        if (faction != null && faction.isNormal() && faction.hasBeacon()) {
            if (BaseStructureHelper.isInsideBaseCube(event.getBlock().getLocation(), faction)) {
                BaseValueCalculator.invalidateCache(faction);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        FLocation floc = FLocation.wrap(event.getBlock().getLocation());
        Faction faction = Board.getInstance().getFactionAt(floc);
        
        if (faction != null && faction.isNormal() && faction.hasBeacon()) {
            if (BaseStructureHelper.isInsideBaseCube(event.getBlock().getLocation(), faction)) {
                BaseValueCalculator.invalidateCache(faction);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChestClose(InventoryCloseEvent event) {
        if (event.getInventory().getType() == InventoryType.CHEST) {
            if (event.getInventory().getHolder() instanceof org.bukkit.block.BlockState) {
                org.bukkit.block.BlockState state = (org.bukkit.block.BlockState) event.getInventory().getHolder();
                org.bukkit.Location chestLoc = state.getLocation();
                FLocation floc = FLocation.wrap(chestLoc);
                Faction faction = Board.getInstance().getFactionAt(floc);
                
                if (faction != null && faction.isNormal() && faction.hasBeacon()) {
                    if (BaseStructureHelper.isInsideBaseCube(chestLoc, faction)) {
                        BaseValueCalculator.invalidateCache(faction);
                    }
                }
            }
        }
    }
}

