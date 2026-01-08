package com.playpandora.morenether.utils;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DifficultyScaler {
    private final JavaPlugin plugin;
    private final Map<UUID, PlayerDifficulty> playerDifficulties = new HashMap<>();
    
    public DifficultyScaler(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    public double getDifficultyMultiplier(Player player) {
        PlayerDifficulty diff = playerDifficulties.computeIfAbsent(
            player.getUniqueId(), 
            k -> new PlayerDifficulty(player)
        );
        
        return diff.calculateMultiplier();
    }
    
    public double getRewardMultiplier(Player player) {
        double difficulty = getDifficultyMultiplier(player);
        return 1.0 + (difficulty - 1.0) * 0.5;
    }
    
    private class PlayerDifficulty {
        private final UUID playerId;
        private int level;
        private long playTime;
        private int deaths;
        private int kills;
        
        public PlayerDifficulty(Player player) {
            this.playerId = player.getUniqueId();
            this.level = getPlayerLevel(player);
            this.playTime = getPlayTime(player);
            this.deaths = getDeaths(player);
            this.kills = getKills(player);
        }
        
        public double calculateMultiplier() {
            double base = 1.0;
            base += level * 0.01;
            double timeBonus = Math.min(playTime / 3600000.0 * 0.1, 0.5);
            base += timeBonus;
            double kdr = deaths > 0 ? (double) kills / deaths : kills;
            base += Math.min(kdr * 0.05, 0.3);
            return Math.min(base, 2.5);
        }
        
        private int getPlayerLevel(Player player) {
            return player.getLevel();
        }
        
        private long getPlayTime(Player player) {
            return player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE);
        }
        
        private int getDeaths(Player player) {
            return player.getStatistic(org.bukkit.Statistic.DEATHS);
        }
        
        private int getKills(Player player) {
            return player.getStatistic(org.bukkit.Statistic.MOB_KILLS);
        }
    }
}

