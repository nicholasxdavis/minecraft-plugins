package com.playpandora.hook.api;

import com.playpandora.hook.Hook;
import com.playpandora.hook.managers.NotificationManager;
import org.bukkit.entity.Player;

/**
 * Public API for other plugins to hook into the notification system
 */
public class HookAPI {
    
    private final Hook plugin;
    private final NotificationManager notificationManager;
    
    public HookAPI(Hook plugin) {
        this.plugin = plugin;
        this.notificationManager = plugin.getNotificationManager();
    }
    
    /**
     * Send a title notification to a player
     * @param player The player to send to
     * @param title Main title text
     * @param subtitle Subtitle text (can be null)
     */
    public void sendTitle(Player player, String title, String subtitle) {
        notificationManager.sendTitle(player, title, subtitle);
    }
    
    /**
     * Send a welcome message
     */
    public void sendWelcome(Player player, boolean isFirstJoin) {
        notificationManager.sendWelcome(player, isFirstJoin);
    }
    
    /**
     * Send a respawn notification
     */
    public void sendRespawn(Player player) {
        notificationManager.sendRespawn(player);
    }
    
    /**
     * Send a level up notification
     */
    public void sendLevelUp(Player player, int newLevel) {
        notificationManager.sendLevelUp(player, newLevel);
    }
    
    /**
     * Send a perk purchase notification
     */
    public void sendPerkPurchase(Player player, String perkName) {
        notificationManager.sendPerkPurchase(player, perkName);
    }
    
    /**
     * Send a sell notification
     */
    public void sendSell(Player player, double amount, int itemsSold) {
        notificationManager.sendSell(player, amount, itemsSold);
    }
    
    /**
     * Send a weather alert
     */
    public void sendWeatherAlert(Player player, String weatherType) {
        notificationManager.sendWeatherAlert(player, weatherType);
    }
    
    /**
     * Send a season change notification
     */
    public void sendSeasonChange(String season) {
        notificationManager.sendSeasonChange(season);
    }
    
    /**
     * Send a base event notification
     */
    public void sendBaseEvent(Player player, String eventType) {
        notificationManager.sendBaseEvent(player, eventType);
    }
    
    /**
     * Send a custom notification with all options
     */
    public void sendCustom(Player player, String title, String subtitle, 
                          int fadeInMs, int stayMs, int fadeOutMs) {
        notificationManager.sendCustom(player, title, subtitle, fadeInMs, stayMs, fadeOutMs);
    }
    
    /**
     * Broadcast title to all players
     */
    public void broadcastTitle(String title, String subtitle) {
        notificationManager.broadcastTitle(title, subtitle);
    }
    
    /**
     * Check if notifications are enabled
     */
    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("notifications.enabled", true);
    }
    
    /**
     * Send a treasure found notification
     */
    public void sendTreasureFound(org.bukkit.entity.Player player, String treasureName) {
        notificationManager.sendTreasureFound(player, treasureName);
    }
    
    /**
     * Send a hint claimed notification
     */
    public void sendHintClaimed(org.bukkit.entity.Player player, int hintsRemaining) {
        notificationManager.sendHintClaimed(player, hintsRemaining);
    }
    
    /**
     * Send a treasure distance announcement to public chat
     */
    public void sendTreasureDistance(org.bukkit.entity.Player player, int hintsAway) {
        notificationManager.sendTreasureDistance(player, hintsAway);
    }
    
    /**
     * Send a quest completion notification
     */
    public void sendQuestComplete(org.bukkit.entity.Player player, String questType, double reward, double xp) {
        notificationManager.sendQuestComplete(player, questType, reward, xp);
    }
    
    /**
     * Send a quest progress notification
     */
    public void sendQuestProgress(org.bukkit.entity.Player player, int progress, int goal, String questType) {
        notificationManager.sendQuestProgress(player, progress, goal, questType);
    }
    
    /**
     * Send a dimension entry notification
     */
    public void sendDimensionEntry(org.bukkit.entity.Player player, String dimensionName) {
        notificationManager.sendDimensionEntry(player, dimensionName);
    }
    
    /**
     * Send a rare mob kill notification
     */
    public void sendRareMobKill(org.bukkit.entity.Player player, String mobName) {
        notificationManager.sendRareMobKill(player, mobName);
    }
}




