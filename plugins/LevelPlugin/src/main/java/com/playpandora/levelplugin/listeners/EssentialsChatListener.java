package com.playpandora.levelplugin.listeners;

import com.playpandora.levelplugin.LevelPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import io.papermc.paper.event.player.AsyncChatEvent;

import java.lang.reflect.Method;

/**
 * Listener for Essentials chat format processing
 * Handles both legacy AsyncPlayerChatEvent and Paper's AsyncChatEvent
 * Processes PlaceholderAPI placeholders in chat format
 */
public class EssentialsChatListener implements Listener {
    
    private final LevelPlugin plugin;
    private boolean placeholderAPIAvailable;
    private boolean essentialsAvailable;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();
    
    public EssentialsChatListener(LevelPlugin plugin) {
        this.plugin = plugin;
        this.placeholderAPIAvailable = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        this.essentialsAvailable = plugin.getServer().getPluginManager().getPlugin("Essentials") != null;
    }
    
    // Handle legacy chat events (for compatibility)
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onLegacyChat(AsyncPlayerChatEvent event) {
        if (!essentialsAvailable) {
            return;
        }
        
        String format = event.getFormat();
        if (format == null || !format.contains("%")) {
            return;
        }
        
        // Process PlaceholderAPI placeholders
        if (placeholderAPIAvailable) {
            try {
                format = PlaceholderAPI.setPlaceholders(event.getPlayer(), format);
                event.setFormat(format);
                return;
            } catch (Exception e) {
                plugin.getLogger().warning("Error parsing PlaceholderAPI in chat: " + e.getMessage());
            }
        }
        
        // Fallback: manually replace level placeholder
        if (format.contains("%level_level%")) {
            int level = plugin.getLevelManager().getLevel(event.getPlayer().getUniqueId());
            format = format.replace("%level_level%", String.valueOf(level));
            event.setFormat(format);
        }
    }
    
    // Handle Paper's modern chat events (Essentials uses this when paper-chat-events: true)
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPaperChat(AsyncChatEvent event) {
        if (!essentialsAvailable) {
            return;
        }
        
        // Get the rendered message as a string to check for placeholders
        Component messageComponent = event.message();
        String messageText = legacySerializer.serialize(messageComponent);
        
        // Check if the message contains unprocessed placeholders
        // This shouldn't happen normally, but let's process it just in case
        if (messageText.contains("%level_level%")) {
            if (placeholderAPIAvailable) {
                try {
                    messageText = PlaceholderAPI.setPlaceholders(event.getPlayer(), messageText);
                    event.message(legacySerializer.deserialize(messageText));
                    return;
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            // Fallback
            int level = plugin.getLevelManager().getLevel(event.getPlayer().getUniqueId());
            messageText = messageText.replace("%level_level%", String.valueOf(level));
            event.message(legacySerializer.deserialize(messageText));
            return;
        }
        
        // The real issue: Essentials processes the format internally
        // We need to hook into Essentials' format processing
        // Let's try to get and process the format string from Essentials config
        
        try {
            // Get Essentials plugin instance
            org.bukkit.plugin.Plugin essentialsPlugin = plugin.getServer().getPluginManager().getPlugin("Essentials");
            if (essentialsPlugin == null) {
                return;
            }
            
            // Try to access Essentials' chat format using reflection
            // Essentials stores format in its config, but processes it internally
            // We need to intercept the format before it's applied
            
            // Since we can't easily intercept Essentials' internal processing,
            // we'll process placeholders in the format string from config
            // and hope Essentials picks it up
            
            // Actually, a better approach: use a chat event at HIGHEST priority
            // to modify the format before Essentials processes it
            // But Paper chat events don't have a format field...
            
            // The solution: We need to process the format string from Essentials config
            // and replace it before Essentials uses it
            // This requires hooking into Essentials' chat handler
            
            // For now, let's try processing the format string directly from config
            String formatString = getEssentialsFormatString();
            if (formatString != null && formatString.contains("%")) {
                // Process placeholders in the format
                if (placeholderAPIAvailable) {
                    formatString = PlaceholderAPI.setPlaceholders(event.getPlayer(), formatString);
                }
                
                // Replace level placeholder as fallback
                if (formatString.contains("%level_level%")) {
                    int level = plugin.getLevelManager().getLevel(event.getPlayer().getUniqueId());
                    formatString = formatString.replace("%level_level%", String.valueOf(level));
                }
                
                // Now we need to tell Essentials to use this processed format
                // But we can't easily do that with Paper chat events
                // So we'll need a different approach
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error processing Paper chat format: " + e.getMessage());
        }
    }
    
    private String getEssentialsFormatString() {
        try {
            org.bukkit.plugin.Plugin essentials = plugin.getServer().getPluginManager().getPlugin("Essentials");
            if (essentials == null) {
                return null;
            }
            
            // Get Essentials config
            org.bukkit.configuration.file.FileConfiguration config = essentials.getConfig();
            if (config == null) {
                return null;
            }
            
            // Get chat format from config
            String format = config.getString("chat.format");
            return format;
        } catch (Exception e) {
            return null;
        }
    }
}

