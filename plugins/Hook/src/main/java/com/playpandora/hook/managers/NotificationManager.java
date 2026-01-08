package com.playpandora.hook.managers;

import com.playpandora.hook.Hook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NotificationManager {
    
    private final Hook plugin;
    
    // Color scheme: Gray, Orange, Yellow
    private static final TextColor COLOR_ORANGE = TextColor.color(255, 165, 0);  // #FFA500
    private static final TextColor COLOR_YELLOW = TextColor.color(255, 255, 0);  // #FFFF00
    private static final TextColor COLOR_GRAY = TextColor.color(128, 128, 128);  // #808080
    private static final TextColor COLOR_GOLD = TextColor.color(255, 215, 0);    // #FFD700 (for bold titles)
    
    // Default timings
    private static final Duration FADE_IN = Duration.ofMillis(500);
    private static final Duration STAY = Duration.ofMillis(3000);
    private static final Duration FADE_OUT = Duration.ofMillis(1000);
    
    // Track last notification times to prevent spam
    private final Map<UUID, Long> lastNotificationTimes = new HashMap<>();
    
    public NotificationManager(Hook plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Send a title notification to a player
     * @param player The player to send to
     * @param title Main title text
     * @param subtitle Subtitle text (can be null)
     */
    public void sendTitle(Player player, String title, String subtitle) {
        sendTitle(player, title, subtitle, FADE_IN, STAY, FADE_OUT);
    }
    
    /**
     * Send a title notification with custom timings
     */
    public void sendTitle(Player player, String title, String subtitle, 
                         Duration fadeIn, Duration stay, Duration fadeOut) {
        if (!plugin.getConfig().getBoolean("notifications.enabled", true)) {
            return;
        }
        
        // Check cooldown to prevent spam
        if (hasCooldown(player)) {
            return;
        }
        
        // Build title component with orange/gold color
        Component titleComponent = Component.text(title)
                .color(COLOR_GOLD);
        
        // Build subtitle component with gray color
        Component subtitleComponent = subtitle != null 
                ? Component.text(subtitle).color(COLOR_GRAY)
                : Component.empty();
        
        // Create title times
        Title.Times times = Title.Times.times(fadeIn, stay, fadeOut);
        
        // Create and show title
        Title titleObj = Title.title(titleComponent, subtitleComponent, times);
        player.showTitle(titleObj);
        
        // Update last notification time
        lastNotificationTimes.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    /**
     * Send a welcome message
     */
    public void sendWelcome(Player player, boolean isFirstJoin) {
        String title = isFirstJoin ? "Welcome!" : "Welcome Back!";
        String subtitle = "Enjoy your stay, " + player.getName();
        sendTitle(player, title, subtitle);
    }
    
    /**
     * Send a respawn notification
     */
    public void sendRespawn(Player player) {
        sendTitle(player, "You Respawned!", "Ready to continue your adventure?", 
                 FADE_IN, Duration.ofMillis(2500), FADE_OUT);
    }
    
    /**
     * Send a level up notification
     */
    public void sendLevelUp(Player player, int newLevel) {
        sendTitle(player, "Level Up!", "You are now level " + newLevel + "!", 
                 FADE_IN, Duration.ofMillis(4000), FADE_OUT);
    }
    
    /**
     * Send a perk purchase notification
     */
    public void sendPerkPurchase(Player player, String perkName) {
        sendTitle(player, "Perk Purchased!", "You unlocked: " + perkName, 
                 FADE_IN, Duration.ofMillis(3000), FADE_OUT);
    }
    
    /**
     * Send a sell notification
     */
    public void sendSell(Player player, double amount, int itemsSold) {
        String formattedAmount = String.format("%.2f", amount);
        sendTitle(player, "Items Sold!", "Sold " + itemsSold + " items for $" + formattedAmount, 
                 FADE_IN, Duration.ofMillis(3000), FADE_OUT);
    }
    
    /**
     * Send a weather alert
     */
    public void sendWeatherAlert(Player player, String weatherType) {
        sendTitle(player, "Weather Alert!", weatherType + " is approaching!", 
                 FADE_IN, Duration.ofMillis(3500), FADE_OUT);
    }
    
    /**
     * Send a season change notification
     */
    public void sendSeasonChange(String season) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            sendTitle(player, "Season Changed!", "It's now " + season, 
                     FADE_IN, Duration.ofMillis(4000), FADE_OUT);
        }
    }
    
    /**
     * Send a base event notification
     */
    public void sendBaseEvent(Player player, String eventType) {
        sendTitle(player, "Base Event!", eventType, 
                 FADE_IN, Duration.ofMillis(3000), FADE_OUT);
    }
    
    /**
     * Send a custom notification with all options
     */
    public void sendCustom(Player player, String title, String subtitle, 
                          int fadeInMs, int stayMs, int fadeOutMs) {
        sendTitle(player, title, subtitle, 
                 Duration.ofMillis(fadeInMs), 
                 Duration.ofMillis(stayMs), 
                 Duration.ofMillis(fadeOutMs));
    }
    
    /**
     * Check if player has notification cooldown
     */
    private boolean hasCooldown(Player player) {
        long cooldown = plugin.getConfig().getLong("notifications.cooldown-ms", 500L);
        Long lastTime = lastNotificationTimes.get(player.getUniqueId());
        
        if (lastTime == null) {
            return false;
        }
        
        return (System.currentTimeMillis() - lastTime) < cooldown;
    }
    
    /**
     * Clear cooldown for a player (useful for important notifications)
     */
    public void clearCooldown(Player player) {
        lastNotificationTimes.remove(player.getUniqueId());
    }
    
    /**
     * Send notification to all players
     */
    public void broadcastTitle(String title, String subtitle) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            sendTitle(player, title, subtitle);
        }
    }
    
    /**
     * Send a treasure found notification
     */
    public void sendTreasureFound(Player player, String treasureName) {
        sendTitle(player, "Treasure Found!", "You discovered: " + treasureName, 
                 FADE_IN, Duration.ofMillis(4000), FADE_OUT);
    }
    
    /**
     * Send a hint claimed notification
     */
    public void sendHintClaimed(Player player, int hintsRemaining) {
        sendTitle(player, "Hint Claimed!", hintsRemaining + " hints remaining to treasure!", 
                 FADE_IN, Duration.ofMillis(3000), FADE_OUT);
    }
    
    /**
     * Send a treasure distance announcement (for public chat)
     */
    public void sendTreasureDistance(Player player, int hintsAway) {
        // This will be sent to public chat, not as title
        String message = plugin.getConfig().getString("messages.treasure-distance", 
            "&e[Treasure] &7%player% is &6%hints% hints &7away from the treasure!");
        message = message.replace("%player%", player.getName())
                        .replace("%hints%", String.valueOf(hintsAway));
        plugin.getServer().broadcastMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
    }
    
    /**
     * Send a quest completion notification
     */
    public void sendQuestComplete(Player player, String questType, double reward, double xp) {
        String formattedReward = String.format("%.2f", reward);
        sendTitle(player, "Quest Complete!", 
            questType + " quest finished! +$" + formattedReward + " & +" + (int)xp + " XP", 
            FADE_IN, Duration.ofMillis(4000), FADE_OUT);
    }
    
    /**
     * Send a quest progress notification
     */
    public void sendQuestProgress(Player player, int progress, int goal, String questType) {
        int percent = (int)((double) progress / goal * 100);
        sendTitle(player, "Quest Progress", 
            questType + ": " + progress + "/" + goal + " (" + percent + "%)", 
            FADE_IN, Duration.ofMillis(2000), FADE_OUT);
    }
    
    /**
     * Send a dimension entry notification
     */
    public void sendDimensionEntry(Player player, String dimensionName) {
        String subtitle = getDimensionSubtitle(dimensionName);
        sendTitle(player, "Welcome to " + dimensionName + "!", subtitle, 
            FADE_IN, Duration.ofMillis(3000), FADE_OUT);
    }
    
    /**
     * Send a rare mob kill notification
     */
    public void sendRareMobKill(Player player, String mobName) {
        sendTitle(player, "Rare Kill!", 
            "You defeated a " + mobName, 
            FADE_IN, Duration.ofMillis(2500), FADE_OUT);
    }
    
    private String getDimensionSubtitle(String dimensionName) {
        switch (dimensionName.toLowerCase()) {
            case "nether":
                return "Beware the dangers that await...";
            case "end":
                return "The void calls...";
            case "caves":
                return "Watch your step...";
            default:
                return "Explore carefully!";
        }
    }
}




