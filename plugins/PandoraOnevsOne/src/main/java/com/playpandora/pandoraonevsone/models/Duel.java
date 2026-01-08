package com.playpandora.pandoraonevsone.models;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Represents an active 1v1 duel
 */
public class Duel {
    
    private final UUID player1UUID;
    private final UUID player2UUID;
    private final long startTime;
    private final int maxTimeSeconds;
    private Location player1OriginalLocation;
    private Location player2OriginalLocation;
    private String player1Inventory;
    private String player2Inventory;
    
    public Duel(UUID player1, UUID player2, int maxTimeSeconds) {
        this.player1UUID = player1;
        this.player2UUID = player2;
        this.startTime = System.currentTimeMillis();
        this.maxTimeSeconds = maxTimeSeconds;
    }
    
    public UUID getPlayer1UUID() {
        return player1UUID;
    }
    
    public UUID getPlayer2UUID() {
        return player2UUID;
    }
    
    public Player getPlayer1() {
        return org.bukkit.Bukkit.getPlayer(player1UUID);
    }
    
    public Player getPlayer2() {
        return org.bukkit.Bukkit.getPlayer(player2UUID);
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public int getMaxTimeSeconds() {
        return maxTimeSeconds;
    }
    
    public long getElapsedTime() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }
    
    public boolean isTimeout() {
        return getElapsedTime() >= maxTimeSeconds;
    }
    
    public Location getPlayer1OriginalLocation() {
        return player1OriginalLocation;
    }
    
    public void setPlayer1OriginalLocation(Location location) {
        this.player1OriginalLocation = location;
    }
    
    public Location getPlayer2OriginalLocation() {
        return player2OriginalLocation;
    }
    
    public void setPlayer2OriginalLocation(Location location) {
        this.player2OriginalLocation = location;
    }
    
    public String getPlayer1Inventory() {
        return player1Inventory;
    }
    
    public void setPlayer1Inventory(String inventory) {
        this.player1Inventory = inventory;
    }
    
    public String getPlayer2Inventory() {
        return player2Inventory;
    }
    
    public void setPlayer2Inventory(String inventory) {
        this.player2Inventory = inventory;
    }
    
    public boolean containsPlayer(UUID uuid) {
        return player1UUID.equals(uuid) || player2UUID.equals(uuid);
    }
    
    public UUID getOpponent(UUID uuid) {
        if (player1UUID.equals(uuid)) {
            return player2UUID;
        } else if (player2UUID.equals(uuid)) {
            return player1UUID;
        }
        return null;
    }
}


