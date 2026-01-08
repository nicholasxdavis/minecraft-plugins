package com.massivecraft.factions.listeners;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.util.SandFixManager;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

/**
 * Listens for falling block spawns to apply fixes
 */
public class SandFixListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onFallingBlockSpawn(EntitySpawnEvent event) {
        if (!Conf.sandFixEnabled) {
            return;
        }

        if (event.getEntity() instanceof FallingBlock) {
            FallingBlock block = (FallingBlock) event.getEntity();
            SandFixManager.getInstance().registerFallingBlock(block);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFallingBlockLand(EntityChangeBlockEvent event) {
        if (!Conf.sandFixEnabled) {
            return;
        }

        if (event.getEntity() instanceof FallingBlock) {
            FallingBlock block = (FallingBlock) event.getEntity();
            SandFixManager.getInstance().unregisterFallingBlock(block);
        }
    }
}







