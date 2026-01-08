package com.massivecraft.factions.listeners;

import com.massivecraft.factions.*;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.managers.TripwireAlarmManager;
import com.massivecraft.factions.util.PandoraMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Listener for tripwire placement and removal for alarm system
 */
public class TripwirePlaceListener implements Listener {
    
    private final FactionsPlugin plugin;
    private final TripwireAlarmManager alarmManager;
    
    public TripwirePlaceListener(FactionsPlugin plugin, TripwireAlarmManager alarmManager) {
        this.plugin = plugin;
        this.alarmManager = alarmManager;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTripwirePlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        
        // Check if placing a tripwire hook
        if (block.getType() != Material.TRIPWIRE_HOOK) {
            return;
        }
        
        Player player = event.getPlayer();
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        
        if (fPlayer == null || !fPlayer.hasFaction()) {
            return;
        }
        
        Faction faction = fPlayer.getFaction();
        if (faction == null || !faction.isNormal()) {
            return;
        }
        
        // Check if player placed it in their own claim
        Location loc = block.getLocation();
        FLocation floc = FLocation.wrap(loc);
        Faction atFaction = Board.getInstance().getFactionAt(floc);
        
        if (atFaction == null || !atFaction.isNormal() || !atFaction.getId().equals(faction.getId())) {
            // Not in their own claim
            return;
        }
        
        // Check if this is from an alarm kit (check item in hand)
        ItemStack item = event.getItemInHand();
        boolean isAlarmKit = false;
        
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String displayName = org.bukkit.ChatColor.stripColor(meta.getDisplayName());
                if (displayName.equals("Tripwire Alarm Kit") || displayName.contains("Tripwire Alarm")) {
                    isAlarmKit = true;
                }
            }
        }
        
        // Only process if it's from an alarm kit
        if (!isAlarmKit) {
            return;
        }
        
        // Try to add tripwire to alarm system
        boolean wasComplete = alarmManager.hasActiveAlarm(faction);
        boolean isComplete = alarmManager.addTripwire(faction, loc);
        
        int currentCount = alarmManager.getTripwireLocations(faction).size();
        
        if (isComplete && !wasComplete) {
            // Alarm system just became complete
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.success("✓ Alarm System Activated!")));
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("Your tripwire alarm is now active and will notify all members when intruders enter your claim.")));
            
            // Notify all faction members
            for (FPlayer member : faction.getFPlayers()) {
                if (member.isOnline() && member.getPlayer() != null && !member.equals(fPlayer)) {
                    member.getPlayer().sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.success("Alarm System Activated! Tripwire alarm is now protecting " + faction.getTag() + ".")));
                }
            }
        } else if (!isComplete) {
            // Still need more tripwires
            int remaining = 4 - currentCount;
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("Tripwire placed! (" + currentCount + "/4)")));
            
            if (currentCount > 1) {
                // Check if tripwire is far enough from others
                boolean tooClose = false;
                for (Location existing : alarmManager.getTripwireLocations(faction)) {
                    if (!existing.equals(loc) && existing.getWorld().equals(loc.getWorld())) {
                        double distance = loc.distance(existing);
                        if (distance < 20.0) {
                            tooClose = true;
                            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("This tripwire is too close to another! Place tripwires at least 20 blocks apart.")));
                            break;
                        }
                    }
                }
            }
            
            if (remaining > 0 && currentCount < 4) {
                player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("You need " + remaining + " more tripwire(s) placed at least 20 blocks apart to activate the alarm system.")));
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTripwireBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        // Check if breaking a tripwire hook
        if (block.getType() != Material.TRIPWIRE_HOOK) {
            return;
        }
        
        Location loc = block.getLocation();
        FLocation floc = FLocation.wrap(loc);
        Faction faction = Board.getInstance().getFactionAt(floc);
        
        if (faction == null || !faction.isNormal()) {
            return;
        }
        
        // Check if this tripwire is part of an alarm system
        java.util.List<Location> alarmTripwires = alarmManager.getTripwireLocations(faction);
        boolean isAlarmTripwire = false;
        
        for (Location alarmLoc : alarmTripwires) {
            if (alarmLoc.getWorld().equals(loc.getWorld()) &&
                alarmLoc.getBlockX() == loc.getBlockX() &&
                alarmLoc.getBlockY() == loc.getBlockY() &&
                alarmLoc.getBlockZ() == loc.getBlockZ()) {
                isAlarmTripwire = true;
                break;
            }
        }
        
        if (!isAlarmTripwire) {
            return; // Not part of alarm system
        }
        
        // Remove from alarm system
        boolean wasActive = alarmManager.hasActiveAlarm(faction);
        alarmManager.removeTripwire(faction, loc);
        boolean isActive = alarmManager.hasActiveAlarm(faction);
        
        Player player = event.getPlayer();
        
        if (wasActive && !isActive) {
            // Alarm system just became inactive
            if (player != null) {
                player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.warning("⚠ Alarm System Deactivated!")));
                player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("The tripwire alarm is no longer active. You need 4 tripwires placed 20 blocks apart.")));
            }
            
            // Notify all faction members
            for (FPlayer member : faction.getFPlayers()) {
                if (member.isOnline() && member.getPlayer() != null) {
                    member.getPlayer().sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.warning("Alarm System Deactivated! Tripwire alarm is no longer protecting " + faction.getTag() + ".")));
                }
            }
        } else {
            int remaining = alarmManager.getTripwireLocations(faction).size();
            if (player != null && remaining > 0) {
                player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("Tripwire removed! Alarm system has " + remaining + "/4 tripwires remaining.")));
            }
        }
    }
}

