package com.massivecraft.factions.util;

/**
 * Utility class to replace color codes in chat messages
 * Replaces: &a (light green) -> &e, &2 (dark green) -> &6, &f (white) -> &7
 */
public class ColorCodeReplacer {
    
    /**
     * Replace color codes in a message
     * @param message The message to process
     * @return The message with replaced color codes
     */
    public static String replaceColorCodes(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        // Replace &a (light green) with &e (yellow)
        message = message.replace("&a", "&e");
        message = message.replace("§a", "§e");
        
        // Replace &2 (dark green) with &6 (gold/orange)
        message = message.replace("&2", "&6");
        message = message.replace("§2", "§6");
        
        // Replace &f (white) with &7 (gray)
        message = message.replace("&f", "&7");
        message = message.replace("§f", "§7");
        
        return message;
    }
    
    /**
     * Replace color codes in a list of messages
     * @param messages The list of messages to process
     * @return The list with replaced color codes
     */
    public static java.util.List<String> replaceColorCodes(java.util.List<String> messages) {
        if (messages == null) {
            return messages;
        }
        
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String message : messages) {
            result.add(replaceColorCodes(message));
        }
        return result;
    }
}





