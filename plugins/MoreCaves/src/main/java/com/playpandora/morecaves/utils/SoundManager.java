package com.playpandora.morecaves.utils;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Random;

public class SoundManager {
    private static final Random random = new Random();
    
    /**
     * Play a sound at a location for all nearby players
     */
    public static void playSoundAtLocation(Location loc, Sound sound, SoundCategory category, 
                                          float volume, float pitch) {
        World world = loc.getWorld();
        if (world == null) return;
        
        world.playSound(loc, sound, category, volume, pitch);
    }
    
    /**
     * Play a sound for a specific player
     */
    public static void playSoundForPlayer(Player player, Sound sound, SoundCategory category,
                                         float volume, float pitch) {
        player.playSound(player.getLocation(), sound, category, volume, pitch);
    }
    
    /**
     * Play a sound at a random nearby location (illusion of distance)
     */
    public static void playDistantSound(Location center, Sound sound, SoundCategory category,
                                       float volume, float pitch, int radius) {
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = random.nextDouble() * radius;
        double x = center.getX() + Math.cos(angle) * distance;
        double z = center.getZ() + Math.sin(angle) * distance;
        
        Location soundLoc = new Location(center.getWorld(), x, center.getY(), z);
        playSoundAtLocation(soundLoc, sound, category, volume * 0.5f, pitch);
    }
}

