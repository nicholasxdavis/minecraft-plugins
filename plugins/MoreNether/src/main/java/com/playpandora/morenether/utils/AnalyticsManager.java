package com.playpandora.morenether.utils;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AnalyticsManager {
    private final JavaPlugin plugin;
    private final Map<UUID, PlayerStats> playerStats = new HashMap<>();
    
    public AnalyticsManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void recordDeath(Player player, String cause) {
        PlayerStats stats = getStats(player);
        stats.deaths++;
        stats.lastDeathCause = cause;
    }
    
    public void recordKill(Player player, String mobType) {
        PlayerStats stats = getStats(player);
        stats.kills++;
        stats.mobKills.put(mobType, stats.mobKills.getOrDefault(mobType, 0) + 1);
    }
    
    public void recordProfit(Player player, double amount, String source) {
        PlayerStats stats = getStats(player);
        stats.totalProfit += amount;
        stats.profitBySource.put(source, stats.profitBySource.getOrDefault(source, 0.0) + amount);
    }
    
    public void recordEventParticipation(Player player, String eventType) {
        PlayerStats stats = getStats(player);
        stats.eventsParticipated.put(eventType, 
            stats.eventsParticipated.getOrDefault(eventType, 0) + 1);
    }
    
    public PlayerStats getStats(Player player) {
        return playerStats.computeIfAbsent(player.getUniqueId(), k -> new PlayerStats());
    }
    
    public static class PlayerStats {
        public int deaths = 0;
        public int kills = 0;
        public double totalProfit = 0.0;
        public String lastDeathCause = "";
        public Map<String, Integer> mobKills = new HashMap<>();
        public Map<String, Double> profitBySource = new HashMap<>();
        public Map<String, Integer> eventsParticipated = new HashMap<>();
    }
}

