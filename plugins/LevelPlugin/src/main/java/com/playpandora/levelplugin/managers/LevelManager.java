package com.playpandora.levelplugin.managers;

import com.playpandora.levelplugin.LevelPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LevelManager {
    
    private final LevelPlugin plugin;
    private final Map<UUID, Long> loginTimes = new HashMap<>();
    private final Map<UUID, Double> xpAccumulated = new HashMap<>(); // Track XP earned in last 5 minutes
    private BukkitTask playtimeTask;
    private BukkitTask xpSummaryTask;
    
    public LevelManager(LevelPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void startPlaytimeTracking() {
        // Track login times
        for (Player player : Bukkit.getOnlinePlayers()) {
            loginTimes.put(player.getUniqueId(), System.currentTimeMillis());
            xpAccumulated.put(player.getUniqueId(), 0.0);
        }
        
        // Start playtime tracking task
        playtimeTask = new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                for (Map.Entry<UUID, Long> entry : loginTimes.entrySet()) {
                    UUID uuid = entry.getKey();
                    long loginTime = entry.getValue();
                    long minutesOnline = (currentTime - loginTime) / 60000; // Convert to minutes
                    
                    if (minutesOnline >= 1) {
                        // Award playtime XP
                        double xpPerMinute = plugin.getConfig().getDouble("xp-sources.playtime.xp-per-minute", 2.0);
                        if (plugin.getConfig().getBoolean("xp-sources.playtime.enabled", true)) {
                            addXP(uuid, xpPerMinute, false); // Don't show message for playtime XP
                        }
                        
                        // Update playtime
                        plugin.getDataManager().addPlaytime(uuid, 1);
                        
                        // Reset login time
                        entry.setValue(currentTime);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1200L); // Every minute (60 seconds = 1200 ticks)
        
        // Start XP summary task (every 5 minutes)
        xpSummaryTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, Double> entry : xpAccumulated.entrySet()) {
                    UUID uuid = entry.getKey();
                    double xp = entry.getValue();
                    
                    if (xp > 0) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null && player.isOnline()) {
                            String prefix = "&e&lPandora &8» &r";
                            String message = prefix + "&7XP earned in last 5 minutes: &6" + String.format("%.1f", xp) + " &7XP";
                            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
                        }
                        // Reset accumulated XP
                        entry.setValue(0.0);
                    }
                }
            }
        }.runTaskTimer(plugin, 6000L, 6000L); // Every 5 minutes (6000 ticks = 5 minutes)
    }
    
    public void onPlayerJoin(Player player) {
        loginTimes.put(player.getUniqueId(), System.currentTimeMillis());
        xpAccumulated.put(player.getUniqueId(), 0.0);
    }
    
    public void onPlayerQuit(Player player) {
        loginTimes.remove(player.getUniqueId());
        xpAccumulated.remove(player.getUniqueId());
    }
    
    public int getLevel(UUID uuid) {
        double xp = plugin.getDataManager().getXP(uuid);
        return calculateLevel(xp);
    }
    
    public double getXP(UUID uuid) {
        return plugin.getDataManager().getXP(uuid);
    }
    
    public void addXP(UUID uuid, double amount) {
        addXP(uuid, amount, true);
    }
    
    public void addXP(UUID uuid, double amount, boolean accumulate) {
        int oldLevel = getLevel(uuid);
        plugin.getDataManager().addXP(uuid, amount);
        int newLevel = getLevel(uuid);
        
        // Check for level up
        if (newLevel > oldLevel) {
            onLevelUp(uuid, newLevel);
        }
        
        // Accumulate XP for summary message (if enabled)
        if (accumulate && amount > 0) {
            xpAccumulated.put(uuid, xpAccumulated.getOrDefault(uuid, 0.0) + amount);
        }
    }
    
    public void setXP(UUID uuid, double xp) {
        int oldLevel = getLevel(uuid);
        plugin.getDataManager().setXP(uuid, xp);
        int newLevel = getLevel(uuid);
        
        if (newLevel > oldLevel) {
            onLevelUp(uuid, newLevel);
        }
    }
    
    /**
     * Set a player's level directly (0-100)
     * This calculates the required XP for the target level and sets it
     */
    public void setLevel(UUID uuid, int targetLevel) {
        if (targetLevel < 0 || targetLevel > 100) {
            throw new IllegalArgumentException("Level must be between 0 and 100");
        }
        
        int oldLevel = getLevel(uuid);
        double requiredXP = getXPRequiredForLevel(targetLevel);
        plugin.getDataManager().setXP(uuid, requiredXP);
        
        // Check for level ups (in case we're going up multiple levels)
        if (targetLevel > oldLevel) {
            // Trigger level up notifications for each level gained
            for (int level = oldLevel + 1; level <= targetLevel; level++) {
                onLevelUp(uuid, level);
            }
        }
    }
    
    private void onLevelUp(UUID uuid, int level) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }
        
        // Send Hook notification
        sendLevelUpNotification(player, level);
        
        // Send level up message
        String prefix = "&e&lPandora &8» &r";
        String message = prefix + "&e&lLevel Up! &7Level &6" + level;
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
        
        // Play sound
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        // Give rewards
        plugin.getRewardManager().giveLevelReward(uuid, level);
    }
    
    private void sendLevelUpNotification(Player player, int level) {
        try {
            org.bukkit.plugin.Plugin hookPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("Hook");
            if (hookPlugin != null && hookPlugin.isEnabled()) {
                Object hookInstance = hookPlugin.getClass().getMethod("getInstance").invoke(null);
                Object hookAPI = hookInstance.getClass().getMethod("getAPI").invoke(hookInstance);
                
                // Check if it's a milestone level
                if (isMilestoneLevel(level)) {
                    hookAPI.getClass().getMethod("sendLevelUp", org.bukkit.entity.Player.class, int.class)
                        .invoke(hookAPI, player, level);
                } else {
                    // Still send notification but shorter duration for regular levels
                    hookAPI.getClass().getMethod("sendCustom", org.bukkit.entity.Player.class, 
                        String.class, String.class, int.class, int.class, int.class)
                        .invoke(hookAPI, player, "Level Up!", "Level " + level, 300, 2000, 800);
                }
            }
        } catch (Exception e) {
            // Silently fail if Hook is not available
        }
    }
    
    private boolean isMilestoneLevel(int level) {
        // Milestone levels: 5, 10, 15, 20, 25, 30, 50, 75, 100, and multiples of 25
        return level % 25 == 0 || level == 5 || level == 10 || level == 15 || level == 20 || level == 30;
    }
    
    public int calculateLevel(double xp) {
        double baseXP = plugin.getConfig().getDouble("level-system.base-xp", 100.0);
        double multiplier = plugin.getConfig().getDouble("level-system.multiplier", 1.15);
        int maxLevel = plugin.getConfig().getInt("level-system.max-level", 100);
        
        int level = 0;
        double totalXP = 0;
        
        while (level < maxLevel) {
            double xpForNextLevel = baseXP * Math.pow(multiplier, level);
            if (totalXP + xpForNextLevel > xp) {
                break;
            }
            totalXP += xpForNextLevel;
            level++;
        }
        
        return level;
    }
    
    public double getXPRequiredForLevel(int targetLevel) {
        double baseXP = plugin.getConfig().getDouble("level-system.base-xp", 100.0);
        double multiplier = plugin.getConfig().getDouble("level-system.multiplier", 1.15);
        
        double totalXP = 0;
        for (int i = 0; i < targetLevel; i++) {
            totalXP += baseXP * Math.pow(multiplier, i);
        }
        
        return totalXP;
    }
    
    public double getXPForCurrentLevel(UUID uuid) {
        int level = getLevel(uuid);
        if (level == 0) {
            return getXP(uuid);
        }
        
        double xpForLevel = getXPRequiredForLevel(level);
        return getXP(uuid) - xpForLevel;
    }
    
    public double getXPRequiredForNextLevel(UUID uuid) {
        int level = getLevel(uuid);
        return getXPRequiredForLevel(level + 1) - getXPRequiredForLevel(level);
    }
}

