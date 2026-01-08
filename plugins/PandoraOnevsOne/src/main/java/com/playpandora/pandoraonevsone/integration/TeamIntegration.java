package com.playpandora.pandoraonevsone.integration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Integration with team/friend systems to allow friends to 1v1
 * Supports Skript teams.sk and other friend systems
 */
public class TeamIntegration {
    
    private static Boolean skriptEnabled = null;
    
    /**
     * Check if Skript is available
     */
    public static boolean isSkriptEnabled() {
        if (skriptEnabled == null) {
            try {
                org.bukkit.plugin.Plugin skriptPlugin = Bukkit.getPluginManager().getPlugin("Skript");
                skriptEnabled = (skriptPlugin != null && skriptPlugin.isEnabled());
            } catch (Exception e) {
                skriptEnabled = false;
            }
        }
        return skriptEnabled;
    }
    
    /**
     * Check if two players are on the same team/friends
     * Returns true if they are friends/teammates
     */
    public static boolean areFriends(Player player1, Player player2) {
        if (player1 == null || player2 == null) {
            return false;
        }
        
        // Check Skript teams.sk
        if (isSkriptEnabled()) {
            try {
                // Check if players are on the same team using Skript variables
                // teams.sk uses variable: {teams::%player%} to store team name
                Object skriptExprManager = Class.forName("ch.njol.skript.Skript").getMethod("getExprManager").invoke(null);
                
                // Try to get team variable for both players
                String team1 = getTeamVariable(player1);
                String team2 = getTeamVariable(player2);
                
                if (team1 != null && team2 != null && team1.equals(team2)) {
                    return true; // Same team
                }
            } catch (Exception e) {
                // Skript integration failed - allow duels
            }
        }
        
        // By default, allow duels (friends can fight in 1v1)
        return false;
    }
    
    /**
     * Get team name for player from Skript
     */
    private static String getTeamVariable(Player player) {
        try {
            // Access Skript variables using reflection
            Class<?> variableManagerClass = Class.forName("ch.njol.skript.variables.Variables");
            Object variableManager = variableManagerClass.getMethod("getVariableManager").invoke(null);
            
            // Get variable: {teams::%player%}
            String varName = "teams::" + player.getName();
            Object variable = variableManagerClass.getMethod("getVariable", String.class).invoke(variableManager, varName);
            
            if (variable != null) {
                return variable.toString();
            }
        } catch (Exception e) {
            // Variable not found or error
        }
        return null;
    }
    
    /**
     * Check if players can duel (allow friends to duel in 1v1)
     * Returns true if duel is allowed
     * FRIENDS CAN ALWAYS 1V1 - this is intentional
     */
    public static boolean canDuel(Player player1, Player player2) {
        // Always allow duels in 1v1 arena - friends can fight
        // The 1v1 system is designed to allow friends to practice PvP
        return true;
    }
    
    /**
     * Check if challenge should be blocked (friends can always challenge each other)
     */
    public static boolean shouldBlockChallenge(Player challenger, Player target) {
        // Never block challenges - friends can always 1v1
        return false;
    }
}

