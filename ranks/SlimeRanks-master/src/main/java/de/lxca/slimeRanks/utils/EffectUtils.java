package de.lxca.slimeRanks.utils;

import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.Random;

/**
 * Utility class for playing effects (fireworks, sounds) for rank changes and kit claims
 */
public class EffectUtils {
    
    private static final Random random = new Random();
    
    /**
     * Spawn fireworks at player location for rank promotion
     */
    public static void spawnRankFireworks(Player player, int count) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return;
        
        for (int i = 0; i < count; i++) {
            Bukkit.getScheduler().runTaskLater(
                de.lxca.slimeRanks.Main.getInstance(),
                () -> {
                    Location fireworkLoc = loc.clone().add(
                        (random.nextDouble() - 0.5) * 3,
                        random.nextDouble() * 2,
                        (random.nextDouble() - 0.5) * 3
                    );
                    
                    Firework firework = world.spawn(fireworkLoc, Firework.class);
                    FireworkMeta meta = firework.getFireworkMeta();
                    
                    // Create colorful firework effect
                    org.bukkit.FireworkEffect.Builder builder = org.bukkit.FireworkEffect.builder();
                    builder.with(org.bukkit.FireworkEffect.Type.BURST);
                    builder.withColor(
                        org.bukkit.Color.fromRGB(255, 215, 0),  // Gold
                        org.bukkit.Color.fromRGB(255, 255, 0),  // Yellow
                        org.bukkit.Color.fromRGB(255, 165, 0)   // Orange
                    );
                    builder.withFade(org.bukkit.Color.WHITE);
                    builder.withFlicker();
                    builder.withTrail();
                    
                    meta.addEffect(builder.build());
                    meta.setPower(1);
                    firework.setFireworkMeta(meta);
                    
                    // Detonate after a short delay
                    Bukkit.getScheduler().runTaskLater(
                        de.lxca.slimeRanks.Main.getInstance(),
                        () -> firework.detonate(),
                        5L
                    );
                },
                i * 5L
            );
        }
    }
    
    /**
     * Spawn fireworks at player location for kit claim (different style)
     */
    public static void spawnKitFireworks(Player player, int count) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return;
        
        for (int i = 0; i < count; i++) {
            Bukkit.getScheduler().runTaskLater(
                de.lxca.slimeRanks.Main.getInstance(),
                () -> {
                    Location fireworkLoc = loc.clone().add(
                        (random.nextDouble() - 0.5) * 2,
                        random.nextDouble() * 1.5,
                        (random.nextDouble() - 0.5) * 2
                    );
                    
                    Firework firework = world.spawn(fireworkLoc, Firework.class);
                    FireworkMeta meta = firework.getFireworkMeta();
                    
                    // Create colorful firework effect (different colors for kits)
                    org.bukkit.FireworkEffect.Builder builder = org.bukkit.FireworkEffect.builder();
                    builder.with(org.bukkit.FireworkEffect.Type.STAR);
                    builder.withColor(
                        org.bukkit.Color.fromRGB(255, 255, 0),  // Yellow
                        org.bukkit.Color.fromRGB(128, 128, 128) // Gray
                    );
                    builder.withFade(org.bukkit.Color.WHITE);
                    builder.withTrail();
                    
                    meta.addEffect(builder.build());
                    meta.setPower(0);
                    firework.setFireworkMeta(meta);
                    
                    // Detonate after a short delay
                    Bukkit.getScheduler().runTaskLater(
                        de.lxca.slimeRanks.Main.getInstance(),
                        () -> firework.detonate(),
                        3L
                    );
                },
                i * 3L
            );
        }
    }
    
    /**
     * Play rank promotion sound
     */
    public static void playRankSound(Player player) {
        try {
            // Try new sound name format first
            player.playSound(
                player.getLocation(),
                "entity.firework_rocket.launch",
                SoundCategory.PLAYERS,
                1.0f,
                1.2f
            );
            
            // Also play level up sound
            Bukkit.getScheduler().runTaskLater(
                de.lxca.slimeRanks.Main.getInstance(),
                () -> {
                    try {
                        player.playSound(
                            player.getLocation(),
                            "entity.player.levelup",
                            SoundCategory.PLAYERS,
                            1.0f,
                            1.0f
                        );
                    } catch (Exception e) {
                        // Fallback
                        player.getWorld().playSound(
                            player.getLocation(),
                            Sound.ENTITY_PLAYER_LEVELUP,
                            SoundCategory.PLAYERS,
                            1.0f,
                            1.0f
                        );
                    }
                },
                10L
            );
        } catch (Exception e) {
            // Fallback to enum
            try {
                player.playSound(
                    player.getLocation(),
                    Sound.ENTITY_FIREWORK_ROCKET_LAUNCH,
                    SoundCategory.PLAYERS,
                    1.0f,
                    1.2f
                );
                player.playSound(
                    player.getLocation(),
                    Sound.ENTITY_PLAYER_LEVELUP,
                    SoundCategory.PLAYERS,
                    1.0f,
                    1.0f
                );
            } catch (Exception ex) {
                // Last resort - use world sound
                player.getWorld().playSound(
                    player.getLocation(),
                    "entity.player.levelup",
                    SoundCategory.PLAYERS,
                    1.0f,
                    1.0f
                );
            }
        }
    }
    
    /**
     * Play kit claim sound (different from rank sound)
     */
    public static void playKitSound(Player player) {
        try {
            // Try new sound name format first
            player.playSound(
                player.getLocation(),
                "entity.experience_orb.pickup",
                SoundCategory.PLAYERS,
                0.8f,
                1.4f
            );
            
            // Also play item pickup sound
            Bukkit.getScheduler().runTaskLater(
                de.lxca.slimeRanks.Main.getInstance(),
                () -> {
                    try {
                        player.playSound(
                            player.getLocation(),
                            "entity.item.pickup",
                            SoundCategory.PLAYERS,
                            0.6f,
                            1.2f
                        );
                    } catch (Exception e) {
                        // Fallback
                        player.getWorld().playSound(
                            player.getLocation(),
                            Sound.ENTITY_ITEM_PICKUP,
                            SoundCategory.PLAYERS,
                            0.6f,
                            1.2f
                        );
                    }
                },
                5L
            );
        } catch (Exception e) {
            // Fallback to enum
            try {
                player.playSound(
                    player.getLocation(),
                    Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    SoundCategory.PLAYERS,
                    0.8f,
                    1.4f
                );
                player.playSound(
                    player.getLocation(),
                    Sound.ENTITY_ITEM_PICKUP,
                    SoundCategory.PLAYERS,
                    0.6f,
                    1.2f
                );
            } catch (Exception ex) {
                // Last resort
                player.getWorld().playSound(
                    player.getLocation(),
                    "entity.experience_orb.pickup",
                    SoundCategory.PLAYERS,
                    0.8f,
                    1.4f
                );
            }
        }
    }
}






