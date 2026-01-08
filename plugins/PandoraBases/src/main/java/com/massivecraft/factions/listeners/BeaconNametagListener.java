package com.massivecraft.factions.listeners;

import com.massivecraft.factions.event.FactionRenameEvent;
import com.massivecraft.factions.util.BeaconNametagManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Listens for faction rename events to update beacon nametags
 */
public class BeaconNametagListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFactionRename(FactionRenameEvent event) {
        // Update nametag when faction is renamed
        if (event.getFaction() != null && event.getFaction().hasBeacon()) {
            BeaconNametagManager.updateNametag(event.getFaction());
        }
    }
}





