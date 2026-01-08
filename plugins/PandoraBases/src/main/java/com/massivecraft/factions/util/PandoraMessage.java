package com.massivecraft.factions.util;

import org.bukkit.ChatColor;

/**
 * Utility class for Pandora message formatting
 * Provides consistent message formatting across the plugin
 */
public class PandoraMessage {

    // Color codes based on user specification:
    // p: &e&lPandora &8» &r (prefix)
    private static final String PREFIX = ChatColor.YELLOW + "" + ChatColor.BOLD + "Pandora " + 
                                         ChatColor.DARK_GRAY + "» " + ChatColor.RESET;
    // h: &e&l (header)
    private static final String HEADER = ChatColor.YELLOW + "" + ChatColor.BOLD;
    // v: &6 (values/highlight - gold/orange)
    private static final String HIGHLIGHT = ChatColor.GOLD + "";
    // t: &7 (text/gray)
    private static final String TEXT = ChatColor.GRAY + "";
    // e: &6 (error - changed from red to gold/orange)
    private static final String ERROR = ChatColor.GOLD + "";
    // s: &e (special/yellow)
    private static final String SPECIAL = ChatColor.YELLOW + "";
    
    // Additional color codes: &8, &a, &5, &d, &b
    public static final String DARK_GRAY = ChatColor.DARK_GRAY + "";
    public static final String GREEN = ChatColor.GREEN + "";
    public static final String DARK_PURPLE = ChatColor.DARK_PURPLE + "";
    public static final String LIGHT_PURPLE = ChatColor.LIGHT_PURPLE + "";
    public static final String AQUA = ChatColor.AQUA + "";

    /**
     * Format a message with Pandora prefix
     */
    public static String format(String message) {
        return PREFIX + message;
    }

    /**
     * Format a header message (bold yellow)
     */
    public static String header(String message) {
        return HEADER + message;
    }

    /**
     * Format a highlight message (orange/gold for values)
     */
    public static String highlight(String message) {
        return HIGHLIGHT + message;
    }

    /**
     * Format a text message (gray for normal text)
     */
    public static String text(String message) {
        return TEXT + message;
    }

    /**
     * Format an error message (red)
     */
    public static String error(String message) {
        return ERROR + message;
    }

    /**
     * Format a message with prefix and custom color
     */
    public static String formatWithPrefix(String message) {
        return PREFIX + message;
    }

    /**
     * Format a success/special message (yellow for special messages)
     */
    public static String success(String message) {
        return SPECIAL + message;
    }

    /**
     * Format a warning message (yellow)
     */
    public static String warning(String message) {
        return ChatColor.YELLOW + message;
    }

    /**
     * Format a value message (orange/gold for values)
     */
    public static String value(String message) {
        return HIGHLIGHT + message;
    }
}

