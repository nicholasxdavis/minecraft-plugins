package com.playpandora.chatmanager.listeners;

import com.playpandora.chatmanager.ChatManager;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import io.papermc.paper.event.player.AsyncChatEvent;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Handles chat formatting with PlaceholderAPI support
 * Works with both legacy AsyncPlayerChatEvent and Paper's AsyncChatEvent
 */
public class ChatListener implements Listener {
    
    private final ChatManager plugin;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();
    private boolean placeholderAPIAvailable;
    private boolean essentialsAvailable;
    private String formatString;
    private boolean usePlaceholderAPI;
    private boolean useEssentialsPlaceholders;
    private EventPriority eventPriority;
    private static boolean processingChat = false;
    
    // Anti-spam system
    private final java.util.Map<UUID, Long> lastMessageTime = new java.util.HashMap<>();
    private final java.util.Map<UUID, String> lastMessage = new java.util.HashMap<>();
    private final java.util.Map<UUID, Integer> spamCount = new java.util.HashMap<>();
    private long spamDelayMs = 1000; // 1 second between messages
    private int maxSpamCount = 3; // Max repeated messages
    private long spamResetTime = 5000; // Reset spam count after 5 seconds
    
    public ChatListener(ChatManager plugin) {
        this.plugin = plugin;
        this.placeholderAPIAvailable = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        this.essentialsAvailable = plugin.getServer().getPluginManager().getPlugin("Essentials") != null;
        
        // Load config
        reloadConfig();
    }
    
    public void reloadConfig() {
        plugin.reloadConfig();
        formatString = plugin.getConfig().getString("format", "&7[&6%level_level%&7] &7{DISPLAYNAME} &7: &7{MESSAGE}");
        usePlaceholderAPI = plugin.getConfig().getBoolean("use-placeholderapi", true);
        useEssentialsPlaceholders = plugin.getConfig().getBoolean("use-essentials-placeholders", true);
        
        // Load anti-spam settings
        spamDelayMs = plugin.getConfig().getLong("anti-spam.delay-ms", 1000);
        maxSpamCount = plugin.getConfig().getInt("anti-spam.max-repeated", 3);
        spamResetTime = plugin.getConfig().getLong("anti-spam.reset-time-ms", 5000);
        
        String priorityStr = plugin.getConfig().getString("event-priority", "HIGHEST");
        try {
            eventPriority = EventPriority.valueOf(priorityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            eventPriority = EventPriority.HIGHEST;
            plugin.getLogger().warning("Invalid event priority: " + priorityStr + ". Using HIGHEST.");
        }
    }
    
    // Handle Paper's modern chat events (1.16.5+)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPaperChat(AsyncChatEvent event) {
        // Prevent duplicate processing
        if (processingChat) {
            event.setCancelled(true);
            return;
        }
        processingChat = true;
        
        Player player = event.getPlayer();
        
        // Get the original message
        Component originalMessage = event.message();
        String messageText = legacySerializer.serialize(originalMessage);
        
        // Strip color codes from message to get clean text for spam check
        String cleanMessage = org.bukkit.ChatColor.stripColor(messageText);
        
        // Check anti-spam
        if (!checkAntiSpam(player, cleanMessage)) {
            event.setCancelled(true);
            processingChat = false;
            return;
        }
        
        // Cancel the event to prevent Essentials and other plugins from processing it
        event.setCancelled(true);
        
        // Use clean message for formatting
        messageText = cleanMessage;
        
        // Get clean player name (no formatting)
        String displayName = player.getName();
        
        // Build the formatted message starting with our format
        String formatted = formatString;
        
        // Replace Essentials placeholders first
        if (useEssentialsPlaceholders) {
            formatted = replaceEssentialsPlaceholders(formatted, player, displayName, messageText);
        }
        
        // Process PlaceholderAPI placeholders
        if (usePlaceholderAPI && placeholderAPIAvailable) {
            try {
                formatted = PlaceholderAPI.setPlaceholders(player, formatted);
            } catch (Exception e) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().warning("Error processing PlaceholderAPI: " + e.getMessage());
                }
            }
        }
        
        // Fallback: manually replace level placeholder if PlaceholderAPI didn't work
        if (formatted.contains("%level_level%")) {
            int level = getPlayerLevel(player);
            formatted = formatted.replace("%level_level%", String.valueOf(level));
        }
        
        // Convert color codes from & to § format for proper rendering
        formatted = org.bukkit.ChatColor.translateAlternateColorCodes('&', formatted);
        
        // Convert to Component and broadcast manually
        Component formattedComponent = legacySerializer.deserialize(formatted);
        
        // Broadcast the formatted message to all players (including the sender)
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (onlinePlayer.canSee(player) || onlinePlayer.equals(player)) {
                onlinePlayer.sendMessage(formattedComponent);
            }
        }
        
        // Also log to console (strip colors for console)
        String consoleMessage = org.bukkit.ChatColor.stripColor(formatted);
        plugin.getLogger().info(consoleMessage);
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Formatted chat: " + formatted);
        }
        
        processingChat = false;
    }
    
    // Handle legacy chat events (for compatibility with older versions)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onLegacyChat(AsyncPlayerChatEvent event) {
        // If Paper's AsyncChatEvent is available, assume it's being handled there
        // and prevent duplicate processing by the legacy handler.
        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
            // If AsyncChatEvent class exists, we are on Paper and it will handle the chat.
            // So, we return early from the legacy handler.
            event.setCancelled(true);
            return;
        } catch (ClassNotFoundException e) {
            // AsyncChatEvent not found, proceed with legacy handling.
        }
        
        // Prevent duplicate processing
        if (processingChat) {
            event.setCancelled(true);
            return;
        }
        processingChat = true;
        
        Player player = event.getPlayer();
        String messageText = event.getMessage();
        
        // Check anti-spam
        if (!checkAntiSpam(player, messageText)) {
            event.setCancelled(true);
            processingChat = false;
            return;
        }
        
        // Cancel to prevent Essentials and other plugins from processing
        event.setCancelled(true);
        
        // Get clean player name (no formatting)
        String displayName = player.getName();
        
        // Build the formatted message
        String formatted = formatString;
        
        // Replace Essentials placeholders first
        if (useEssentialsPlaceholders) {
            formatted = replaceEssentialsPlaceholders(formatted, player, displayName, messageText);
        }
        
        // Process PlaceholderAPI placeholders
        if (usePlaceholderAPI && placeholderAPIAvailable) {
            try {
                formatted = PlaceholderAPI.setPlaceholders(player, formatted);
            } catch (Exception e) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().warning("Error processing PlaceholderAPI: " + e.getMessage());
                }
            }
        }
        
        // Fallback: manually replace level placeholder
        if (formatted.contains("%level_level%")) {
            int level = getPlayerLevel(player);
            formatted = formatted.replace("%level_level%", String.valueOf(level));
        }
        
        // Convert color codes from & to § format for proper rendering
        formatted = org.bukkit.ChatColor.translateAlternateColorCodes('&', formatted);
        
        // Convert to Component and broadcast
        Component formattedComponent = legacySerializer.deserialize(formatted);
        
        // Broadcast to all players (including the sender)
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (onlinePlayer.canSee(player) || onlinePlayer.equals(player)) {
                onlinePlayer.sendMessage(formattedComponent);
            }
        }
        
        // Log to console (strip colors for console)
        String consoleMessage = org.bukkit.ChatColor.stripColor(formatted);
        plugin.getLogger().info(consoleMessage);
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Formatted legacy chat: " + formatted);
        }
        
        processingChat = false;
    }
    
    private String replaceEssentialsPlaceholders(String format, Player player, String displayName, String message) {
        String result = format;
        
        // Replace common Essentials placeholders
        result = result.replace("{DISPLAYNAME}", displayName);
        result = result.replace("{MESSAGE}", message);
        result = result.replace("{USERNAME}", player.getName());
        result = result.replace("{NICKNAME}", player.getName()); // Could be enhanced to get actual nickname
        result = result.replace("{WORLDNAME}", player.getWorld().getName());
        
        // Try to get prefix/suffix from Vault or LuckPerms
        if (result.contains("{PREFIX}") || result.contains("{SUFFIX}")) {
            String prefix = getPrefix(player);
            String suffix = getSuffix(player);
            result = result.replace("{PREFIX}", prefix != null ? prefix : "");
            result = result.replace("{SUFFIX}", suffix != null ? suffix : "");
        }
        
        // Try to get group
        if (result.contains("{GROUP}")) {
            String group = getGroup(player);
            result = result.replace("{GROUP}", group != null ? group : "");
        }
        
        return result;
    }
    
    private String getPrefix(Player player) {
        try {
            // Try Vault using reflection
            if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
                Class<?> chatClass = Class.forName("net.milkbowl.vault.chat.Chat");
                org.bukkit.plugin.RegisteredServiceProvider<?> chatProvider = 
                    plugin.getServer().getServicesManager().getRegistration(chatClass);
                if (chatProvider != null) {
                    Object chat = chatProvider.getProvider();
                    Method getPrefixMethod = chatClass.getMethod("getPlayerPrefix", org.bukkit.entity.Player.class);
                    String prefix = (String) getPrefixMethod.invoke(chat, player);
                    if (prefix != null && !prefix.isEmpty()) {
                        return prefix;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    private String getSuffix(Player player) {
        try {
            // Try Vault using reflection
            if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
                Class<?> chatClass = Class.forName("net.milkbowl.vault.chat.Chat");
                org.bukkit.plugin.RegisteredServiceProvider<?> chatProvider = 
                    plugin.getServer().getServicesManager().getRegistration(chatClass);
                if (chatProvider != null) {
                    Object chat = chatProvider.getProvider();
                    Method getSuffixMethod = chatClass.getMethod("getPlayerSuffix", org.bukkit.entity.Player.class);
                    String suffix = (String) getSuffixMethod.invoke(chat, player);
                    if (suffix != null && !suffix.isEmpty()) {
                        return suffix;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    private String getGroup(Player player) {
        try {
            // Try Vault using reflection
            if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
                Class<?> permClass = Class.forName("net.milkbowl.vault.permission.Permission");
                org.bukkit.plugin.RegisteredServiceProvider<?> permProvider = 
                    plugin.getServer().getServicesManager().getRegistration(permClass);
                if (permProvider != null) {
                    Object perm = permProvider.getProvider();
                    Method getGroupMethod = permClass.getMethod("getPrimaryGroup", org.bukkit.entity.Player.class);
                    String group = (String) getGroupMethod.invoke(perm, player);
                    if (group != null && !group.isEmpty()) {
                        return group;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    private int getPlayerLevel(Player player) {
        // Try to get level from LevelPlugin
        try {
            org.bukkit.plugin.Plugin levelPlugin = plugin.getServer().getPluginManager().getPlugin("LevelPlugin");
            if (levelPlugin != null) {
                // Use reflection to get level
                Class<?> levelPluginClass = levelPlugin.getClass();
                Method getInstanceMethod = levelPluginClass.getMethod("getInstance");
                Object instance = getInstanceMethod.invoke(null);
                
                Method getAPIMethod = levelPluginClass.getMethod("getAPI");
                Object api = getAPIMethod.invoke(instance);
                
                Method getLevelMethod = api.getClass().getMethod("getLevel", org.bukkit.entity.Player.class);
                return (Integer) getLevelMethod.invoke(api, player);
            }
        } catch (Exception e) {
            // Fall through to vanilla level
        }
        
        // Fallback to vanilla level
        return player.getLevel();
    }
    
    /**
     * Checks if a message violates anti-spam rules
     * @param player The player sending the message
     * @param message The message content
     * @return true if message is allowed, false if it should be blocked
     */
    private boolean checkAntiSpam(Player player, String message) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check if player has bypass permission
        if (player.hasPermission("chatmanager.bypass.spam")) {
            return true;
        }
        
        // Get last message time
        Long lastTime = lastMessageTime.get(uuid);
        String lastMsg = lastMessage.get(uuid);
        
        // Check for rapid messages (delay check)
        if (lastTime != null) {
            long timeSinceLastMessage = currentTime - lastTime;
            if (timeSinceLastMessage < spamDelayMs) {
                long waitTime = (spamDelayMs - timeSinceLastMessage) / 1000 + 1;
                player.sendMessage("§6&lPandora §7Please wait §6" + waitTime + "s §7before sending another message!");
                return false;
            }
        }
        
        // Check for repeated messages
        if (lastMsg != null && lastMsg.equalsIgnoreCase(message)) {
            Integer count = spamCount.get(uuid);
            if (count == null) {
                count = 1;
            } else {
                count++;
            }
            
            spamCount.put(uuid, count);
            
            if (count >= maxSpamCount) {
                player.sendMessage("§6&lPandora §7Please do not repeat the same message!");
                return false;
            }
        } else {
            // Reset spam count if message is different
            spamCount.remove(uuid);
        }
        
        // Update last message info
        lastMessageTime.put(uuid, currentTime);
        lastMessage.put(uuid, message);
        
        // Reset spam count after reset time
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            Long storedTime = lastMessageTime.get(uuid);
            if (storedTime != null && (System.currentTimeMillis() - storedTime) >= spamResetTime) {
                spamCount.remove(uuid);
            }
        }, (spamResetTime / 50) + 1); // Convert ms to ticks
        
        return true;
    }
}

