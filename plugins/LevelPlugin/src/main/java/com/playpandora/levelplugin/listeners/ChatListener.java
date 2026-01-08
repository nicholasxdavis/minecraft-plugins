package com.playpandora.levelplugin.listeners;

import com.playpandora.levelplugin.LevelPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    
    private final LevelPlugin plugin;
    private boolean placeholderAPIAvailable;
    
    public ChatListener(LevelPlugin plugin) {
        this.plugin = plugin;
        this.placeholderAPIAvailable = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        String format = event.getFormat();
        if (format == null || !format.contains("%")) {
            return;
        }
        
        // Try PlaceholderAPI first
        if (placeholderAPIAvailable) {
            try {
                format = PlaceholderAPI.setPlaceholders(event.getPlayer(), format);
                event.setFormat(format);
                return;
            } catch (Exception e) {
                plugin.getLogger().warning("Error parsing PlaceholderAPI placeholders: " + e.getMessage());
            }
        }
        
        // Fallback: manually replace level placeholder
        if (format.contains("%level_level%")) {
            int level = plugin.getLevelManager().getLevel(event.getPlayer().getUniqueId());
            format = format.replace("%level_level%", String.valueOf(level));
            event.setFormat(format);
        }
    }
}

