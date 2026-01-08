package de.lxca.slimeRanks.objects;

import de.lxca.slimeRanks.Main;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Professional rank manager with thread safety and caching
 * Hypixel-standard implementation
 */
public class RankManager {

    private static RankManager instance;
    private static final ArrayList<Rank> ranks = new ArrayList<>();
    private final ReadWriteLock ranksLock = new ReentrantReadWriteLock();
    
    // Cache for player ranks to reduce permission checks (cleared on rank changes)
    private final ConcurrentHashMap<java.util.UUID, Rank> rankCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<java.util.UUID, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 5000; // 5 second cache

    private RankManager() {
        reloadRanks();
    }

    public static synchronized RankManager getInstance() {
        if (instance == null) {
            instance = new RankManager();
        }

        return instance;
    }

    /**
     * Reload ranks from configuration with thread safety
     */
    public void reloadRanks() {
        ranksLock.writeLock().lock();
        try {
            ranks.clear();
            rankCache.clear(); // Clear cache when ranks are reloaded
            cacheTimestamps.clear();
            
            YamlConfiguration ranksYml = Main.getRanksYml().getYmlConfig();
            ConfigurationSection ranksSection = ranksYml.getConfigurationSection("Ranks");

            if (ranksSection == null) {
                Main.getLogger(this.getClass()).warn("Could not find ranks section in ranks.yml. Please check the file and recreate if necessary.");
                return;
            }

            for (String identifier : ranksSection.getKeys(false)) {
                Rank rank = new Rank(identifier);
                if (rank.exists()) {
                    ranks.add(rank);
                } else {
                    Main.getLogger(this.getClass()).warn("Failed to load rank: " + identifier);
                }
            }

            ranks.sort(Comparator.comparingInt(Rank::getRankPriority).reversed());
            Main.getLogger(this.getClass()).info("Loaded " + ranks.size() + " ranks successfully");
        } finally {
            ranksLock.writeLock().unlock();
        }
    }

    /**
     * Get all ranks (thread-safe copy)
     */
    public ArrayList<Rank> getRanks() {
        ranksLock.readLock().lock();
        try {
            return new ArrayList<>(ranks);
        } finally {
            ranksLock.readLock().unlock();
        }
    }

    public int getRankCount() {
        ranksLock.readLock().lock();
        try {
            return ranks.size();
        } finally {
            ranksLock.readLock().unlock();
        }
    }

    /**
     * Get player's rank with caching for performance
     * Hypixel-standard: Fast, cached, thread-safe
     */
    public @Nullable Rank getPlayerRank(@NotNull Player player) {
        if (player == null || !player.isOnline()) {
            return null;
        }
        
        java.util.UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        // Check cache first
        Rank cachedRank = rankCache.get(uuid);
        Long cacheTime = cacheTimestamps.get(uuid);
        
        if (cachedRank != null && cacheTime != null && (now - cacheTime) < CACHE_DURATION_MS) {
            // Verify cached rank is still valid
            if (cachedRank.getPermission() == null || player.hasPermission(cachedRank.getPermission())) {
                return cachedRank;
            }
        }
        
        // Cache miss or expired - calculate rank
        ranksLock.readLock().lock();
        try {
            for (Rank rank : ranks) {
                if (rank.getPermission() == null || player.hasPermission(rank.getPermission())) {
                    // Update cache
                    rankCache.put(uuid, rank);
                    cacheTimestamps.put(uuid, now);
                    return rank;
                }
            }
        } finally {
            ranksLock.readLock().unlock();
        }
        
        // No rank found - cache null to prevent repeated lookups
        rankCache.put(uuid, null);
        cacheTimestamps.put(uuid, now);
        return null;
    }
    
    /**
     * Clear rank cache for a specific player (called on rank changes)
     */
    public void clearPlayerRankCache(@NotNull java.util.UUID playerUUID) {
        rankCache.remove(playerUUID);
        cacheTimestamps.remove(playerUUID);
    }
    
    /**
     * Clear all rank caches (called on rank reloads)
     */
    public void clearAllRankCaches() {
        rankCache.clear();
        cacheTimestamps.clear();
    }

    /**
     * Reload displays for all players with proper error handling
     * Hypixel-standard: Smooth, glitch-free updates
     */
    public void reloadDisplays() {
        try {
            // Clear name tags first
            PlayerNameTag.clearPlayerNameTags();
            if (!Main.isFolia()) {
                for (World world : Bukkit.getWorlds()) {
                    PlayerNameTag.clearBuggyNameTags(world);
                }
            }

            // Update all online players
            for (Player player : new ArrayList<>(Bukkit.getOnlinePlayers())) {
                if (player == null || !player.isOnline()) {
                    continue;
                }
                
                try {
                    updatePlayerDisplay(player);
                } catch (Exception e) {
                    Main.getLogger(this.getClass()).warn("Failed to update display for player " + player.getName() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Main.getLogger(this.getClass()).error("Error reloading displays: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Update display for a single player (thread-safe)
     */
    public void updatePlayerDisplay(@NotNull Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        // Clear cache to force fresh lookup
        clearPlayerRankCache(player.getUniqueId());
        
        Rank rank = getPlayerRank(player);

        if (rank == null) {
            // Reset to default
            try {
                player.playerListName(player.name());
                player.setPlayerListOrder(0);
            } catch (Exception e) {
                Main.getLogger(this.getClass()).debug("Failed to reset player list name for " + player.getName());
            }
            return;
        }

        // Update tab list
        if (rank.tabIsActive()) {
            try {
                player.playerListName(rank.getTabFormat(player));
                player.setPlayerListOrder(rank.getTabPriority());
            } catch (Exception e) {
                Main.getLogger(this.getClass()).warn("Failed to update tab list for " + player.getName() + ": " + e.getMessage());
            }
        }

        // Update name tag
        if (rank.nameTagIsActive() && PlayerNameTag.shouldDisplayPlayerNameTag(player, true, true)) {
            try {
                player.getScheduler().run(
                        Main.getInstance(),
                        scheduledTask -> PlayerNameTag.getPlayerNameTag(player),
                        null
                );
            } catch (Exception e) {
                Main.getLogger(this.getClass()).warn("Failed to update name tag for " + player.getName() + ": " + e.getMessage());
            }
        }
    }
}
