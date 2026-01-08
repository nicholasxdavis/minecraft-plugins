package com.playpandora.pandoraonevsone.listeners;

import com.playpandora.pandoraonevsone.PandoraOnevsOne;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Listener to allow PvP between friends/teammates during 1v1 duels
 * Uses metadata to bypass friend/team protection systems during active duels
 */
public class OneVsOnePvPListener implements Listener {
    
    private final PandoraOnevsOne plugin;
    private static final String BYPASS_METADATA = "1v1_duel_bypass";
    
    public OneVsOnePvPListener(PandoraOnevsOne plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Set metadata on players in duel to bypass friend/team protection
     */
    public static void setBypassMetadata(Player player) {
        player.setMetadata(BYPASS_METADATA, new FixedMetadataValue(PandoraOnevsOne.getInstance(), true));
    }
    
    /**
     * Remove metadata when leaving duel
     */
    public static void removeBypassMetadata(Player player) {
        player.removeMetadata(BYPASS_METADATA, PandoraOnevsOne.getInstance());
    }
    
    /**
     * Check if player has bypass metadata
     */
    public static boolean hasBypassMetadata(Player player) {
        return player.hasMetadata(BYPASS_METADATA);
    }
    
    /**
     * Allow PvP damage between players in an active 1v1 duel
     * Bypasses friend/team protection systems by uncancelling at HIGHEST priority
     * This MUST run at HIGHEST to override teams.sk damage cancellation
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player victim = (Player) event.getEntity();
        Player attacker = null;
        
        // Get attacker player
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile) {
            org.bukkit.entity.Projectile projectile = (org.bukkit.entity.Projectile) event.getDamager();
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            }
        }
        
        if (attacker == null || attacker.equals(victim)) {
            return;
        }
        
        // Check if both players are in the same active duel
        if (plugin.getOneVsOneManager().isInDuel(attacker) && 
            plugin.getOneVsOneManager().isInDuel(victim)) {
            
            var activeDuel = plugin.getOneVsOneManager().getActiveDuel();
            if (activeDuel != null && 
                activeDuel.containsPlayer(attacker.getUniqueId()) &&
                activeDuel.containsPlayer(victim.getUniqueId())) {
                
                // Both players are in the same active duel - FORCE allow PvP damage
                // CRITICAL: Uncancel the event even if friend/team systems (teams.sk) cancelled it
                // This runs at HIGHEST priority to override teams.sk's damage cancellation
                event.setCancelled(false);
                
                // Set metadata for other systems that check it
                setBypassMetadata(attacker);
                setBypassMetadata(victim);
                
                plugin.getLogger().fine("1v1 PvP bypass: " + attacker.getName() + " -> " + victim.getName());
            }
        }
    }
}

