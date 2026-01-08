package com.massivecraft.factions.tasks;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.util.ObsidianHealthManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

/**
 * Periodic task to regenerate obsidian block health
 * Runs every configured interval and regenerates blocks that haven't been damaged in 1 hour
 * OPTIMIZED: Only processes blocks near players to reduce server load
 */
public class ObsidianRegenTask extends BukkitRunnable {
    
    private static final double PLAYER_CHECK_RADIUS = 100.0; // Only regen blocks within 100 blocks of players

    @Override
    public void run() {
        if (!Conf.obsidianRegenEnabled) {
            return;
        }

        // Get all online players and their locations
        java.util.List<Player> onlinePlayers = new java.util.ArrayList<>(org.bukkit.Bukkit.getOnlinePlayers());
        
        // If no players online, skip regeneration entirely
        if (onlinePlayers.isEmpty()) {
            return;
        }

        ObsidianHealthManager manager = ObsidianHealthManager.getInstance();
        Map<Location, Block> toRegen = manager.getBlocksForRegeneration();

        int regenerated = 0;
        for (Map.Entry<Location, Block> entry : toRegen.entrySet()) {
            Block block = entry.getValue();
            Location blockLoc = block.getLocation();
            
            // Only process if a player is nearby
            boolean playerNearby = false;
            for (Player player : onlinePlayers) {
                if (player.getWorld() == blockLoc.getWorld() && 
                    player.getLocation().distance(blockLoc) <= PLAYER_CHECK_RADIUS) {
                    playerNearby = true;
                    break;
                }
            }
            
            // Skip if no players nearby
            if (!playerNearby) {
                continue;
            }
            
            // Verify block is still obsidian
            if (block.getType() == Material.OBSIDIAN) {
                int oldHP = manager.getBlockHP(block);
                manager.regenerateBlock(block);
                int newHP = manager.getBlockHP(block);
                
                if (newHP > oldHP) {
                    regenerated++;
                    
                    // Show regen feedback
                    if (Conf.obsidianShowDamageFeedback && Conf.obsidianShowParticles) {
                        block.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER,
                            block.getLocation().add(0.5, 0.5, 0.5), 5, 0.3, 0.3, 0.3, 0.1);
                    }
                }
            }
        }

        if (regenerated > 0 && Conf.logFactionCreate) {
            com.massivecraft.factions.util.Logger.print("Regenerated " + regenerated + 
                " obsidian blocks", com.massivecraft.factions.util.Logger.PrefixType.DEFAULT);
        }
    }
}

