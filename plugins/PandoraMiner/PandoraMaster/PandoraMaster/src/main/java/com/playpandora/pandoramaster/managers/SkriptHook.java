package com.playpandora.pandoramaster.managers;

import com.playpandora.pandoramaster.PandoraMaster;
import org.bukkit.OfflinePlayer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SkriptHook {
    
    private final PandoraMaster plugin;
    private final boolean skriptAvailable;
    
    public SkriptHook(PandoraMaster plugin) {
        this.plugin = plugin;
        this.skriptAvailable = plugin.getServer().getPluginManager().getPlugin("Skript") != null;
        
        if (!skriptAvailable) {
            plugin.getLogger().warning("Skript not found! Stats will not be available.");
        }
    }
    
    public boolean isSkriptAvailable() {
        return skriptAvailable;
    }
    
    private Object getSkriptVariable(String variableName, OfflinePlayer player) {
        if (!skriptAvailable) {
            return null;
        }
        
        try {
            String uuid = player.getUniqueId().toString();
            String fullPath = variableName + "::" + uuid;
            
            // Use Skript's variable system via reflection
            Class<?> variablesClass = Class.forName("ch.njol.skript.variables.Variables");
            Object value = variablesClass.getMethod("getVariable", String.class, Object.class, boolean.class)
                .invoke(null, fullPath, null, false);
            return value;
        } catch (Exception e) {
            // Try alternative method using PlaceholderAPI if available
            try {
                org.bukkit.plugin.Plugin placeholderAPIPlugin = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI");
                if (placeholderAPIPlugin != null) {
                    Class<?> placeholderAPIClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                    String uuid = player.getUniqueId().toString();
                    String placeholder = "%" + variableName + "::" + uuid + "%";
                    org.bukkit.entity.Player onlinePlayer = player instanceof org.bukkit.entity.Player ? 
                        (org.bukkit.entity.Player) player : null;
                    String result = (String) placeholderAPIClass.getMethod("setPlaceholders", 
                        org.bukkit.entity.Player.class, String.class)
                        .invoke(null, onlinePlayer, placeholder);
                    if (result != null && !result.equals(placeholder)) {
                        try {
                            return Double.parseDouble(result);
                        } catch (NumberFormatException ex) {
                            return result;
                        }
                    }
                }
            } catch (Exception ex) {
                // Ignore - PlaceholderAPI not available or method signature changed
            }
            
            plugin.getLogger().fine("Error getting Skript variable " + variableName + " for " + player.getName() + ": " + e.getMessage());
            return null;
        }
    }
    
    public int getKills(OfflinePlayer player) {
        Object value = getSkriptVariable("kills", player);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
    
    public int getDeaths(OfflinePlayer player) {
        Object value = getSkriptVariable("deaths", player);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
    
    public int getMobKills(OfflinePlayer player) {
        Object value = getSkriptVariable("mobkills", player);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }
    
    public double getTotalMoney(OfflinePlayer player) {
        Object value = getSkriptVariable("totalmoney", player);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
    
    public String getJoinedDate(OfflinePlayer player) {
        Object value = getSkriptVariable("joined", player);
        if (value instanceof Date) {
            SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
            return format.format((Date) value);
        } else if (value != null) {
            String str = value.toString();
            // Try to parse as date if it's a timestamp
            try {
                long timestamp = Long.parseLong(str);
                Date date = new Date(timestamp);
                SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
                return format.format(date);
            } catch (NumberFormatException e) {
                return str;
            }
        }
        // Fallback to first join date from Bukkit
        if (player.getFirstPlayed() > 0) {
            Date date = new Date(player.getFirstPlayed());
            SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
            return format.format(date);
        }
        return "Unknown";
    }
    
    public String formatMoney(double amount) {
        // Format with commas for thousands
        if (amount >= 1000000) {
            return String.format("%.2fM", amount / 1000000.0);
        } else if (amount >= 1000) {
            return String.format("%.2fK", amount / 1000.0);
        } else {
            return String.format("%.0f", amount);
        }
    }
}

