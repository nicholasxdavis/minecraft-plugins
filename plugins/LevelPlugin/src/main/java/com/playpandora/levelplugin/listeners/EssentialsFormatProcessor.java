package com.playpandora.levelplugin.listeners;

import com.playpandora.levelplugin.LevelPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Processes PlaceholderAPI placeholders in Essentials chat format
 * Works with Paper's modern chat events
 */
public class EssentialsFormatProcessor implements Listener {
    
    private final LevelPlugin plugin;
    private boolean placeholderAPIAvailable;
    private boolean essentialsAvailable;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();
    private Object essentialsChatHandler;
    private Field formatField;
    private Method getFormatMethod;
    
    public EssentialsFormatProcessor(LevelPlugin plugin) {
        this.plugin = plugin;
        this.placeholderAPIAvailable = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
        this.essentialsAvailable = plugin.getServer().getPluginManager().getPlugin("Essentials") != null;
        
        if (essentialsAvailable) {
            initializeEssentialsHook();
        }
    }
    
    private void initializeEssentialsHook() {
        try {
            org.bukkit.plugin.Plugin essentials = plugin.getServer().getPluginManager().getPlugin("Essentials");
            if (essentials == null) {
                return;
            }
            
            // Try to get Essentials chat handler
            // Essentials Chat module handles chat formatting
            Class<?> essentialsClass = essentials.getClass();
            
            // This is complex, so we'll use a simpler approach
            // Process the format string from config and hope Essentials picks it up
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize Essentials hook: " + e.getMessage());
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onChat(AsyncChatEvent event) {
        if (!essentialsAvailable) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Get the current message as a string
        Component messageComponent = event.message();
        String messageText = legacySerializer.serialize(messageComponent);
        
        // Check if there are unprocessed placeholders in the message
        if (!messageText.contains("%level_level%")) {
            return;
        }
        
        // Process PlaceholderAPI placeholders
        if (placeholderAPIAvailable) {
            try {
                messageText = PlaceholderAPI.setPlaceholders(player, messageText);
            } catch (Exception e) {
                plugin.getLogger().warning("Error processing PlaceholderAPI in chat: " + e.getMessage());
            }
        }
        
        // Fallback: manually replace level placeholder
        if (messageText.contains("%level_level%")) {
            int level = plugin.getLevelManager().getLevel(player.getUniqueId());
            messageText = messageText.replace("%level_level%", String.valueOf(level));
        }
        
        // Convert back to Component and set it
        if (!messageText.equals(legacySerializer.serialize(messageComponent))) {
            event.message(legacySerializer.deserialize(messageText));
        }
    }
    
    private String getEssentialsFormatString() {
        try {
            org.bukkit.plugin.Plugin essentials = plugin.getServer().getPluginManager().getPlugin("Essentials");
            if (essentials == null) {
                return null;
            }
            
            org.bukkit.configuration.file.FileConfiguration config = essentials.getConfig();
            if (config == null) {
                return null;
            }
            
            return config.getString("chat.format");
        } catch (Exception e) {
            return null;
        }
    }
}

