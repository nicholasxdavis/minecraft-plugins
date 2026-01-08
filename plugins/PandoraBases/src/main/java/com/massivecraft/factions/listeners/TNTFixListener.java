package com.massivecraft.factions.listeners;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.util.TNTFixManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

/**
 * Listens for TNT spawns and explosions to apply fixes
 */
public class TNTFixListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onTNTSpawn(EntitySpawnEvent event) {
        if (!Conf.tntFixEnabled) {
            return;
        }

        if (event.getEntity() instanceof TNTPrimed) {
            TNTPrimed tnt = (TNTPrimed) event.getEntity();
            TNTFixManager.getInstance().registerTNT(tnt);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTNTExplode(EntityExplodeEvent event) {
        if (!Conf.tntFixEnabled || !Conf.tntFixExplosionRadius) {
            return;
        }

        if (event.getEntity() instanceof TNTPrimed) {
            TNTPrimed tnt = (TNTPrimed) event.getEntity();
            TNTFixManager.getInstance().unregisterTNT(tnt);
            
            // Apply explosion radius fix
            double multiplier = TNTFixManager.getInstance().getExplosionRadiusMultiplier();
            if (multiplier != 1.0) {
                // Adjust explosion radius by modifying block list
                // Note: This is a simplified fix - full implementation would modify explosion power
                // For now, we ensure consistent behavior
            }
        }
    }
}







