package com.massivecraft.factions.listeners;

import com.massivecraft.factions.*;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.managers.TripwireAlarmManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Listener for tripwire alarm system
 * Detects when players enter claims with active alarms and notifies faction members
 */
public class TripwireAlarmListener implements Listener {
    
    private final FactionsPlugin plugin;
    private final TripwireAlarmManager alarmManager;
    private final Map<UUID, Faction> lastFactionMap = new HashMap<>();
    private final Map<UUID, Long> alarmCooldown = new HashMap<>();
    private static final long ALARM_COOLDOWN_MS = 5000; // 5 second cooldown between alarms
    
    public TripwireAlarmListener(FactionsPlugin plugin, TripwireAlarmManager alarmManager) {
        this.plugin = plugin;
        this.alarmManager = alarmManager;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (to == null || from == null) {
            return;
        }
        
        // Only check if player moved to a different block
        if (from.getBlockX() == to.getBlockX() && 
            from.getBlockY() == to.getBlockY() && 
            from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        
        FLocation toFloc = FLocation.wrap(to);
        Faction toFaction = Board.getInstance().getFactionAt(toFloc);
        
        FLocation fromFloc = FLocation.wrap(from);
        Faction fromFaction = Board.getInstance().getFactionAt(fromFloc);
        
        if (toFaction == null || toFaction.isWilderness()) {
            // Clear last faction if entering wilderness
            lastFactionMap.remove(player.getUniqueId());
            return;
        }
        
        // Check if player is entering from outside (wilderness or different faction)
        boolean isEnteringFromOutside = fromFaction == null || 
                                        fromFaction.isWilderness() || 
                                        (fromFaction.isNormal() && !fromFaction.getId().equals(toFaction.getId()));
        
        // Check if player is entering a different faction's territory
        Faction lastFaction = lastFactionMap.get(player.getUniqueId());
        boolean isNewEntry = lastFaction == null || !lastFaction.getId().equals(toFaction.getId());
        
        if (isEnteringFromOutside && isNewEntry) {
            // Player entered from outside into a new faction's territory
            lastFactionMap.put(player.getUniqueId(), toFaction);
            
            // Check if this faction has an active alarm and player is not a member
            if (alarmManager.hasActiveAlarm(toFaction)) {
                FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
                
                // Don't trigger alarm for faction members
                if (fPlayer == null || !fPlayer.hasFaction() || 
                    !fPlayer.getFaction().getId().equals(toFaction.getId())) {
                    
                    // Check cooldown
                    Long lastAlarm = alarmCooldown.get(player.getUniqueId());
                    if (lastAlarm != null && (System.currentTimeMillis() - lastAlarm) < ALARM_COOLDOWN_MS) {
                        return; // Still on cooldown
                    }
                    
                    // Trigger alarm
                    triggerAlarm(toFaction, player);
                    
                    // Update cooldown
                    alarmCooldown.put(player.getUniqueId(), System.currentTimeMillis());
                }
            }
        } else if (!isNewEntry) {
            // Player is still in the same faction's territory, just update map
            lastFactionMap.put(player.getUniqueId(), toFaction);
        }
    }
    
    private void triggerAlarm(Faction faction, Player intruder) {
        // Get Hook integration
        Plugin hookPlugin = Bukkit.getPluginManager().getPlugin("Hook");
        if (hookPlugin == null) {
            return;
        }
        
        try {
            // Get Hook API
            Class<?> hookClass = hookPlugin.getClass();
            Method getInstanceMethod = hookClass.getMethod("getInstance");
            Object hookInstance = getInstanceMethod.invoke(null);
            
            if (hookInstance == null) {
                return;
            }
            
            Method getAPIMethod = hookInstance.getClass().getMethod("getAPI");
            Object hookAPI = getAPIMethod.invoke(hookInstance);
            
            if (hookAPI == null) {
                return;
            }
            
            // Send notification to all faction members
            String intruderName = intruder.getName();
            String factionTag = faction.getTag();
            
            for (FPlayer member : faction.getFPlayers()) {
                if (member.isOnline() && member.getPlayer() != null) {
                    Player memberPlayer = member.getPlayer();
                    
                    // Send title via Hook
                    Method sendTitleMethod = hookAPI.getClass().getMethod("sendTitle", 
                        Player.class, String.class, String.class);
                    sendTitleMethod.invoke(hookAPI, memberPlayer, 
                        "⚠ ALARM TRIGGERED ⚠", 
                        intruderName + " entered " + factionTag + "!");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error triggering tripwire alarm: " + e.getMessage());
        }
    }
}

