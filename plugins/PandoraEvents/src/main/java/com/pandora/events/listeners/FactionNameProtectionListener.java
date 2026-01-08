package com.pandora.events.listeners;

import com.pandora.events.PandoraEventsPlugin;
import org.bukkit.event.Listener;

/**
 * Listener to protect the "event" faction name from being used by players
 * Protection is handled via the blacklist in PandoraBases Conf class
 */
public class FactionNameProtectionListener implements Listener {
    
    private static final String PROTECTED_NAME = "event";
    
    /**
     * Add "event" to the blacklisted faction names in PandoraBases
     * This prevents players from creating or renaming factions to "event"
     */
    public static void protectEventName() {
        try {
            PandoraEventsPlugin plugin = PandoraEventsPlugin.getInstance();
            if (plugin == null) {
                return;
            }
            
            // Use reflection to access PandoraBases Conf class
            Class<?> confClass = Class.forName("com.massivecraft.factions.Conf");
            java.lang.reflect.Field blacklistField = confClass.getField("blacklistedFactionNames");
            @SuppressWarnings("unchecked")
            java.util.List<String> blacklist = (java.util.List<String>) blacklistField.get(null);
            
            if (blacklist != null && !blacklist.contains(PROTECTED_NAME)) {
                blacklist.add(PROTECTED_NAME);
                plugin.getLogger().info("Protected faction name 'event' from being used by players");
            }
        } catch (ClassNotFoundException e) {
            PandoraEventsPlugin plugin = PandoraEventsPlugin.getInstance();
            if (plugin != null) {
                plugin.getLogger().warning("PandoraBases Conf class not found - event name protection may not work");
            }
        } catch (Exception e) {
            PandoraEventsPlugin plugin = PandoraEventsPlugin.getInstance();
            if (plugin != null) {
                plugin.getLogger().warning("Failed to protect 'event' faction name: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}

