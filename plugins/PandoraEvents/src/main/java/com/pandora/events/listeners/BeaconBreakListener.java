package com.pandora.events.listeners;

import com.pandora.events.PandoraEventsPlugin;
import com.pandora.events.managers.EventManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.lang.reflect.Method;

/**
 * Listener for beacon break events - ends event and awards winners
 */
public class BeaconBreakListener implements Listener {
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBeaconBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        if (block.getType() != Material.BEACON) {
            return;
        }
        
        // Check if this is an event beacon
        EventManager eventManager = PandoraEventsPlugin.getInstance().getEventManager();
        if (!eventManager.isEventActive() || !eventManager.isEventBeacon(block.getLocation())) {
            return;
        }
        
        Player breaker = event.getPlayer();
        
        // Show particles and sound effects
        showWinEffects(breaker, block.getLocation());
        
        // End the event and award winners
        eventManager.endEvent(breaker);
        
        // Award money
        awardWinners(breaker);
        
        // Teleport all players in the claim to spawn
        teleportPlayersInClaimToSpawn(block.getLocation());
    }
    
    /**
     * Show particles and sound effects on win (not too much)
     */
    private void showWinEffects(Player winner, org.bukkit.Location loc) {
        // Play sound at beacon location
        loc.getWorld().playSound(loc, org.bukkit.Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 1.0f);
        loc.getWorld().playSound(loc, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        // Show particles at beacon location (not too many)
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * 2 * Math.PI;
            double x = loc.getX() + Math.cos(angle) * 2;
            double z = loc.getZ() + Math.sin(angle) * 2;
            org.bukkit.Location particleLoc = new org.bukkit.Location(loc.getWorld(), x, loc.getY() + 1, z);
            loc.getWorld().spawnParticle(org.bukkit.Particle.FIREWORK, particleLoc, 1);
        }
        
        // Show particles at winner location (not too many)
        org.bukkit.Location winnerLoc = winner.getLocation();
        winnerLoc.getWorld().spawnParticle(org.bukkit.Particle.HEART, winnerLoc, 5, 0.5, 1, 0.5, 0.1);
        winnerLoc.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, winnerLoc, 10, 0.5, 1, 0.5, 0.1);
    }
    
    /**
     * Teleport all players in the event claim to spawn
     */
    private void teleportPlayersInClaimToSpawn(org.bukkit.Location beaconLoc) {
        try {
            EventManager eventManager = PandoraEventsPlugin.getInstance().getEventManager();
            Object eventFaction = eventManager.getEventFaction();
            if (eventFaction == null) {
                return;
            }
            
            // Get all online players
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                try {
                    // Check if player is in the event faction's claim
                    if (isInEventClaim(player.getLocation(), eventFaction)) {
                        // Teleport to world spawn
                        org.bukkit.Location spawnLoc = player.getWorld().getSpawnLocation();
                        player.teleport(spawnLoc);
                        player.sendMessage(com.pandora.events.util.PandoraMessage.formatWithPrefix(
                            com.pandora.events.util.PandoraMessage.text("You have been teleported to spawn as the event has ended.")
                        ));
                    }
                } catch (Exception e) {
                    // Skip this player if there's an error
                    continue;
                }
            }
        } catch (Exception e) {
            PandoraEventsPlugin.getInstance().getLogger().warning("Failed to teleport players: " + e.getMessage());
        }
    }
    
    /**
     * Check if a location is in the event faction's claim
     */
    private boolean isInEventClaim(org.bukkit.Location loc, Object eventFaction) {
        try {
            // Use reflection to check if location is in faction claim
            Class<?> boardClass = Class.forName("com.massivecraft.factions.Board");
            Method getInstanceMethod = boardClass.getMethod("getInstance");
            Object boardInstance = getInstanceMethod.invoke(null);
            
            Class<?> flocationClass = Class.forName("com.massivecraft.factions.FLocation");
            Method wrapMethod = flocationClass.getMethod("wrap", org.bukkit.Location.class);
            Object floc = wrapMethod.invoke(null, loc);
            
            Method getFactionAtMethod = boardClass.getMethod("getFactionAt", flocationClass);
            Object factionAtLoc = getFactionAtMethod.invoke(boardInstance, floc);
            
            // Check if the faction at this location is the event faction
            if (factionAtLoc == null || eventFaction == null) {
                return false;
            }
            
            Method getIdMethod = eventFaction.getClass().getMethod("getId");
            String eventFactionId = (String) getIdMethod.invoke(eventFaction);
            
            Method getIdMethod2 = factionAtLoc.getClass().getMethod("getId");
            String locFactionId = (String) getIdMethod2.invoke(factionAtLoc);
            
            return eventFactionId != null && eventFactionId.equals(locFactionId);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Award money to the winner(s)
     */
    private void awardWinners(Player breaker) {
        try {
            // Get player's faction
            Class<?> fplayersClass = Class.forName("com.massivecraft.factions.FPlayers");
            Method getInstanceMethod = fplayersClass.getMethod("getInstance");
            Object fplayersInstance = getInstanceMethod.invoke(null);
            
            Method getByPlayerMethod = fplayersClass.getMethod("getByPlayer", Player.class);
            Object fPlayer = getByPlayerMethod.invoke(fplayersInstance, breaker);
            
            if (fPlayer == null) {
                return;
            }
            
            // Check if player has a faction
            Method hasFactionMethod = fPlayer.getClass().getMethod("hasFaction");
            boolean hasFaction = (Boolean) hasFactionMethod.invoke(fPlayer);
            
            double rewardAmount;
            if (hasFaction) {
                // Player is in a faction - award $200k to each member
                rewardAmount = 200000.0;
                
                Method getFactionMethod = fPlayer.getClass().getMethod("getFaction");
                Object faction = getFactionMethod.invoke(fPlayer);
                
                if (faction != null) {
                    // Get all faction members
                    Method getFPlayersMethod = faction.getClass().getMethod("getFPlayers");
                    @SuppressWarnings("unchecked")
                    java.util.Set<Object> members = (java.util.Set<Object>) getFPlayersMethod.invoke(faction);
                    
                    // Award $200k to each member
                    int memberCount = 0;
                    for (Object member : members) {
                        Method getPlayerMethod = member.getClass().getMethod("getPlayer");
                        Player memberPlayer = (Player) getPlayerMethod.invoke(member);
                        if (memberPlayer != null && memberPlayer.isOnline()) {
                            awardPlayerMoney(memberPlayer, rewardAmount);
                            memberCount++;
                        }
                    }
                    
                    String factionTag = (String) faction.getClass().getMethod("getTag").invoke(faction);
                    String message = com.pandora.events.util.PandoraMessage.format(
                        com.pandora.events.util.PandoraMessage.success(breaker.getName() + " and their faction ") +
                        com.pandora.events.util.PandoraMessage.highlight(factionTag) +
                        com.pandora.events.util.PandoraMessage.text(" won the event! Each of the ") +
                        com.pandora.events.util.PandoraMessage.highlight(String.valueOf(memberCount)) +
                        com.pandora.events.util.PandoraMessage.text(" members received ") +
                        com.pandora.events.util.PandoraMessage.highlight("$200,000")
                    );
                    Bukkit.broadcastMessage(message);
                    
                    // Send Hook notification
                    com.pandora.events.integration.HookIntegration hook = com.pandora.events.PandoraEventsPlugin.getInstance().getHookIntegration();
                    if (hook != null && hook.isHookAvailable()) {
                        hook.sendEventWin(breaker, "You and your faction won!");
                    }
                }
            } else {
                // Solo player - award $100k
                rewardAmount = 100000.0;
                awardPlayerMoney(breaker, rewardAmount);
                String message = com.pandora.events.util.PandoraMessage.format(
                    com.pandora.events.util.PandoraMessage.success(breaker.getName() + " won the event solo! Received ") +
                    com.pandora.events.util.PandoraMessage.highlight("$100,000")
                );
                Bukkit.broadcastMessage(message);
                
                // Send Hook notification
                com.pandora.events.integration.HookIntegration hook = com.pandora.events.PandoraEventsPlugin.getInstance().getHookIntegration();
                if (hook != null && hook.isHookAvailable()) {
                    hook.sendEventWin(breaker, "You won solo!");
                }
            }
            
        } catch (Exception e) {
            PandoraEventsPlugin.getInstance().getLogger().severe("Failed to award winners: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Award money to a player using Vault (via reflection)
     */
    private void awardPlayerMoney(Player player, double amount) {
        try {
            if (org.bukkit.Bukkit.getPluginManager().getPlugin("Vault") == null) {
                return;
            }
            
            org.bukkit.plugin.ServicesManager services = org.bukkit.Bukkit.getServicesManager();
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            org.bukkit.plugin.RegisteredServiceProvider<?> rsp = services.getRegistration(economyClass);
            
            if (rsp != null) {
                Object econ = rsp.getProvider();
                Method depositMethod = econ.getClass().getMethod("depositPlayer", org.bukkit.OfflinePlayer.class, double.class);
                depositMethod.invoke(econ, player, amount);
            }
        } catch (Exception e) {
            PandoraEventsPlugin.getInstance().getLogger().warning("Failed to award money to player: " + e.getMessage());
        }
    }
}

