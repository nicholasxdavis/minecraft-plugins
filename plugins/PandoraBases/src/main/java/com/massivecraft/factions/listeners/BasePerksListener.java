package com.massivecraft.factions.listeners;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.event.FPlayerEnteredFactionEvent;
import com.massivecraft.factions.util.BasePerksManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class BasePerksListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Update invisible beacon visibility for the player
        com.massivecraft.factions.util.BeaconBeamManager.updateInvisibleBeaconForPlayer(player);
        
        // Apply perks after a short delay to ensure faction data is loaded
        new BukkitRunnable() {
            @Override
            public void run() {
                FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
                if (fPlayer != null && fPlayer.hasFaction() && fPlayer.getFaction().isNormal() && fPlayer.getFaction().hasBeacon()) {
                    BasePerksManager.applyPerks(player, fPlayer.getFaction());
                }
            }
        }.runTaskLater(com.massivecraft.factions.FactionsPlugin.getInstance(), 20L); // 1 second delay
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        BasePerksManager.removePerks(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerEnterFaction(FPlayerEnteredFactionEvent event) {
        FPlayer fPlayer = event.getfPlayer();
        if (fPlayer == null || !fPlayer.isOnline()) {
            return;
        }

        Player player = fPlayer.getPlayer();
        if (player == null) {
            return;
        }

        // Check if player entered their own base AND is a member
        if (fPlayer.hasFaction() && 
            event.getFactionTo() != null && 
            event.getFactionTo().equals(fPlayer.getFaction()) &&
            event.getFactionTo().isNormal() && 
            event.getFactionTo().hasBeacon() &&
            event.getFactionTo().getFPlayers().contains(fPlayer)) {
            // Player entered their own base and is a member - apply perks
            BasePerksManager.applyPerks(player, event.getFactionTo());
        } else if (event.getFactionFrom() != null && 
                   event.getFactionFrom().equals(fPlayer.getFaction()) &&
                   event.getFactionFrom().isNormal() && 
                   event.getFactionFrom().hasBeacon()) {
            // Player left their base - remove perks
            BasePerksManager.removePerks(player);
        } else if (event.getFactionTo() != null && 
                   event.getFactionTo().isNormal() && 
                   event.getFactionTo().hasBeacon() &&
                   !event.getFactionTo().equals(fPlayer.getFaction())) {
            // Player entered someone else's base - remove any perks they might have
            BasePerksManager.removePerks(player);
        }
    }
}

