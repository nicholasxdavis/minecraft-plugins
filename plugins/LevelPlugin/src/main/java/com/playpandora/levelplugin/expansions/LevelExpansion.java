package com.playpandora.levelplugin.expansions;

import com.playpandora.levelplugin.LevelPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LevelExpansion extends PlaceholderExpansion {
    
    private final LevelPlugin plugin;
    
    public LevelExpansion(LevelPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getIdentifier() {
        return "level";
    }
    
    @Override
    public String getAuthor() {
        return "PlayPandora";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) {
            return "";
        }
        
        UUID uuid = player.getUniqueId();
        
        switch (identifier.toLowerCase()) {
            case "level":
            case "lvl":
                return String.valueOf(plugin.getLevelManager().getLevel(uuid));
            case "xp":
            case "experience":
                return String.format("%.1f", plugin.getLevelManager().getXP(uuid));
            case "xp_current":
                return String.format("%.1f", plugin.getLevelManager().getXPForCurrentLevel(uuid));
            case "xp_required":
                return String.format("%.1f", plugin.getLevelManager().getXPRequiredForNextLevel(uuid));
            case "xp_percent":
                double current = plugin.getLevelManager().getXPForCurrentLevel(uuid);
                double required = plugin.getLevelManager().getXPRequiredForNextLevel(uuid);
                double percent = required > 0 ? (current / required) * 100 : 100;
                return String.format("%.1f", percent);
            default:
                return null;
        }
    }
}

