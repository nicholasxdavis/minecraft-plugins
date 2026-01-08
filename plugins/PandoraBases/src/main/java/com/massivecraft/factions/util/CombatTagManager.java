package com.massivecraft.factions.util;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.util.PandoraMessage;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages combat tagging system
 * Tracks which players are in combat and when their tag expires
 */
public class CombatTagManager {

    private static CombatTagManager instance;
    
    // Store tag expiration time for each player: UUID -> expiration timestamp (millis)
    private final Map<UUID, Long> combatTags = new ConcurrentHashMap<>();
    
    // Store last damage time for tag refresh: UUID -> timestamp
    private final Map<UUID, Long> lastDamageTime = new ConcurrentHashMap<>();

    private CombatTagManager() {
    }

    public static CombatTagManager getInstance() {
        if (instance == null) {
            instance = new CombatTagManager();
        }
        return instance;
    }

    /**
     * Tag a player (set combat tag expiration)
     */
    public void tagPlayer(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        long expirationTime = System.currentTimeMillis() + (Conf.combatTagDurationSeconds * 1000L);
        
        combatTags.put(uuid, expirationTime);
        lastDamageTime.put(uuid, System.currentTimeMillis());
        
        if (Conf.combatTagShowMessages) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("⚔ COMBAT TAGGED ⚔")));
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You are in combat for ") + 
                PandoraMessage.highlight(Conf.combatTagDurationSeconds + " seconds") + PandoraMessage.error("!")));
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("You cannot log out, teleport, or enter safezones.")));
        }
    }

    /**
     * Tag two players (both attacker and victim)
     */
    public void tagPlayers(Player attacker, Player victim) {
        tagPlayer(attacker);
        tagPlayer(victim);
    }

    /**
     * Check if a player is currently tagged
     */
    public boolean isTagged(Player player) {
        if (player == null) {
            return false;
        }
        
        UUID uuid = player.getUniqueId();
        Long expirationTime = combatTags.get(uuid);
        
        if (expirationTime == null) {
            return false;
        }
        
        // Check if tag has expired
        if (System.currentTimeMillis() >= expirationTime) {
            combatTags.remove(uuid);
            lastDamageTime.remove(uuid);
            return false;
        }
        
        return true;
    }

    /**
     * Get remaining tag time in seconds
     */
    public int getRemainingSeconds(Player player) {
        if (player == null) {
            return 0;
        }
        
        UUID uuid = player.getUniqueId();
        Long expirationTime = combatTags.get(uuid);
        
        if (expirationTime == null) {
            return 0;
        }
        
        long remaining = expirationTime - System.currentTimeMillis();
        return (int) Math.max(0, remaining / 1000);
    }

    /**
     * Remove tag from player (manually)
     */
    public void removeTag(Player player) {
        if (player == null) {
            return;
        }
        
        UUID uuid = player.getUniqueId();
        combatTags.remove(uuid);
        lastDamageTime.remove(uuid);
        
        if (Conf.combatTagShowMessages) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.success("You are no longer in combat.")));
        }
    }

    /**
     * Refresh tag (extend duration) - called when player takes new damage
     */
    public void refreshTag(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        UUID uuid = player.getUniqueId();
        
        // If not tagged, just tag normally
        if (!isTagged(player)) {
            tagPlayer(player);
            return;
        }
        
        // Already tagged - refresh the timer
        long expirationTime = System.currentTimeMillis() + (Conf.combatTagDurationSeconds * 1000L);
        combatTags.put(uuid, expirationTime);
        lastDamageTime.put(uuid, System.currentTimeMillis());
        
        if (Conf.combatTagShowMessages) {
            int remaining = getRemainingSeconds(player);
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.warning("Combat tag refreshed! ") + 
                PandoraMessage.highlight(remaining + " seconds remaining.")));
        }
    }

    /**
     * Check if player can be tagged (not in safezone, etc.)
     */
    public boolean canTagPlayer(Player player) {
        if (player == null) {
            return false;
        }
        
        // Check if PvP is enabled in world
        if (!player.getWorld().getPVP()) {
            return false;
        }
        
        // Check if player is in safezone (if configured to not tag in safezones)
        if (Conf.combatTagBlockInSafezone) {
            com.massivecraft.factions.FLocation floc = com.massivecraft.factions.FLocation.wrap(player.getLocation());
            com.massivecraft.factions.Faction faction = com.massivecraft.factions.Board.getInstance().getFactionAt(floc);
            if (faction != null && faction.isSafeZone()) {
                return false; // Don't tag in safezones
            }
        }
        
        return true;
    }

    /**
     * Clean up expired tags (called periodically)
     */
    public void cleanupExpiredTags() {
        long now = System.currentTimeMillis();
        combatTags.entrySet().removeIf(entry -> {
            if (entry.getValue() <= now) {
                UUID uuid = entry.getKey();
                lastDamageTime.remove(uuid);
                
                // Notify player if online
                Player player = FactionsPlugin.getInstance().getServer().getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    if (Conf.combatTagShowMessages) {
                        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.success("You are no longer in combat.")));
                    }
                }
                return true;
            }
            return false;
        });
    }

    /**
     * Get all currently tagged players
     */
    public Map<UUID, Long> getCombatTags() {
        return new ConcurrentHashMap<>(combatTags);
    }
}

