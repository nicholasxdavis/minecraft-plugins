package com.massivecraft.factions.listeners;

import com.massivecraft.factions.*;
import com.massivecraft.factions.util.ObsidianHealthManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Listens to explosions and applies damage to obsidian blocks
 */
public class ObsidianHealthListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!Conf.obsidianHealthEnabled) {
            return;
        }

        Entity entity = event.getEntity();
        Location explosionLoc = event.getLocation();
        
        // OPTIMIZATION: Skip processing if no players nearby (reduces server load)
        boolean playerNearby = false;
        for (org.bukkit.entity.Player player : explosionLoc.getWorld().getPlayers()) {
            if (player.getLocation().distance(explosionLoc) <= 150.0) { // 150 block radius
                playerNearby = true;
                break;
            }
        }
        
        if (!playerNearby) {
            return; // No players nearby, skip obsidian health processing
        }
        
        // Calculate damage based on entity type
        double baseDamage = getExplosionDamage(entity);
        if (baseDamage <= 0) {
            return; // Not a tracked explosion type
        }

        // Process obsidian blocks in explosion radius
        // Check blocks in the explosion's affected area
        List<Block> obsidianBlocks = new ArrayList<>();
        double explosionRadius = getExplosionRadius(entity);
        
        // First, check blocks already in blockList (these are blocks that would normally break)
        for (Block block : new ArrayList<>(event.blockList())) {
            if (block.getType() == Material.OBSIDIAN) {
                obsidianBlocks.add(block);
            }
        }
        
        // Also check nearby blocks that might be obsidian (obsidian normally doesn't break, so might not be in list)
        int radius = (int) Math.ceil(explosionRadius);
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = explosionLoc.getWorld().getBlockAt(
                        explosionLoc.getBlockX() + x,
                        explosionLoc.getBlockY() + y,
                        explosionLoc.getBlockZ() + z);
                    
                    if (block.getType() == Material.OBSIDIAN) {
                        double distance = block.getLocation().distance(explosionLoc);
                        if (distance <= explosionRadius && !obsidianBlocks.contains(block)) {
                            obsidianBlocks.add(block);
                        }
                    }
                }
            }
        }

        // Apply damage to each obsidian block
        for (Block block : obsidianBlocks) {
            // Calculate distance from explosion
            double distance = block.getLocation().distance(explosionLoc);
            
            // Apply damage
            boolean destroyed = ObsidianHealthManager.getInstance().damageBlock(block, baseDamage, distance);
            
            if (destroyed) {
                // Block HP reached 0 - break it
                // Remove from explosion block list to prevent double processing
                event.blockList().remove(block);
                block.breakNaturally();
                
                // Show feedback
                showBlockBreakFeedback(block);
            } else {
                // Prevent vanilla break - obsidian has HP > 0
                event.blockList().remove(block);
                
                // Show damage feedback
                showDamageFeedback(block, ObsidianHealthManager.getInstance().getBlockHP(block));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!Conf.obsidianHealthEnabled) {
            return;
        }

        Location explosionLoc = event.getBlock().getLocation();
        
        // OPTIMIZATION: Skip processing if no players nearby (reduces server load)
        boolean playerNearby = false;
        for (org.bukkit.entity.Player player : explosionLoc.getWorld().getPlayers()) {
            if (player.getLocation().distance(explosionLoc) <= 150.0) { // 150 block radius
                playerNearby = true;
                break;
            }
        }
        
        if (!playerNearby) {
            return; // No players nearby, skip obsidian health processing
        }
        
        double baseDamage = Conf.obsidianTntDamage; // TNT damage
        double explosionRadius = 4.0; // TNT radius
        
        // Process obsidian blocks in radius
        List<Block> obsidianBlocks = new ArrayList<>();
        
        // Check blocks in blockList first
        for (Block block : new ArrayList<>(event.blockList())) {
            if (block.getType() == Material.OBSIDIAN) {
                obsidianBlocks.add(block);
            }
        }
        
        // Also check nearby blocks (obsidian normally doesn't break)
        int radius = (int) Math.ceil(explosionRadius);
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = explosionLoc.getWorld().getBlockAt(
                        explosionLoc.getBlockX() + x,
                        explosionLoc.getBlockY() + y,
                        explosionLoc.getBlockZ() + z);
                    
                    if (block.getType() == Material.OBSIDIAN) {
                        double distance = block.getLocation().distance(explosionLoc);
                        if (distance <= explosionRadius && !obsidianBlocks.contains(block)) {
                            obsidianBlocks.add(block);
                        }
                    }
                }
            }
        }

        // Apply damage
        for (Block block : obsidianBlocks) {
            double distance = block.getLocation().distance(explosionLoc);
            boolean destroyed = ObsidianHealthManager.getInstance().damageBlock(block, baseDamage, distance);
            
            if (destroyed) {
                event.blockList().remove(block);
                block.breakNaturally();
                showBlockBreakFeedback(block);
            } else {
                event.blockList().remove(block); // Prevent vanilla break
                showDamageFeedback(block, ObsidianHealthManager.getInstance().getBlockHP(block));
            }
        }
    }

    /**
     * Get explosion radius based on entity type
     */
    private double getExplosionRadius(Entity entity) {
        if (entity instanceof TNTPrimed) {
            return 4.0; // TNT radius
        } else if (entity instanceof Creeper) {
            Creeper creeper = (Creeper) entity;
            return creeper.isPowered() ? 6.0 : 3.0; // Charged creeper has larger radius
        } else if (entity instanceof ExplosiveMinecart) {
            return 4.0;
        } else if (entity instanceof Fireball) {
            return 1.0; // Fireball radius
        } else if (entity instanceof WitherSkull) {
            return 1.0; // Wither skull radius
        }
        return 4.0; // Default
    }

    /**
     * Get explosion damage based on entity type (increased by 20%)
     */
    private double getExplosionDamage(Entity entity) {
        double baseDamage = 0.0;
        
        if (entity instanceof TNTPrimed) {
            baseDamage = Conf.obsidianTntDamage;
        } else if (entity instanceof Creeper) {
            Creeper creeper = (Creeper) entity;
            if (creeper.isPowered()) {
                baseDamage = Conf.obsidianChargedCreeperDamage;
            } else {
                // Check if spawned from egg (creeper egg)
                if (entity.hasMetadata("creeper_egg")) {
                    baseDamage = Conf.obsidianCreeperEggDamage;
                } else {
                    baseDamage = Conf.obsidianCreeperDamage;
                }
            }
        } else if (entity instanceof ExplosiveMinecart) {
            baseDamage = Conf.obsidianTntDamage; // Same as TNT
        } else if (entity instanceof Fireball) {
            baseDamage = Conf.obsidianFireballDamage;
        } else if (entity instanceof WitherSkull) {
            baseDamage = Conf.obsidianWitherSkullDamage;
        }
        
        if (baseDamage > 0) {
            return baseDamage;
        }
        
        return 0; // Unknown explosion type
    }

    /**
     * Show visual/audio feedback when block takes damage
     */
    private void showDamageFeedback(Block block, int currentHP) {
        if (!Conf.obsidianShowDamageFeedback) {
            return;
        }

        int maxHP = ObsidianHealthManager.getInstance().getMaxHP(block);
        double percent = (double) currentHP / maxHP * 100;

        // Show particles
        if (Conf.obsidianShowParticles) {
            org.bukkit.Particle particle = org.bukkit.Particle.BLOCK;
            block.getWorld().spawnParticle(particle, block.getLocation().add(0.5, 0.5, 0.5), 
                10, 0.3, 0.3, 0.3, 0.1, Material.OBSIDIAN.createBlockData());
        }

        // Show action bar to nearby players
        if (Conf.obsidianShowActionBar) {
            String color = percent > 50 ? ChatColor.GREEN.toString() : 
                        percent > 25 ? ChatColor.YELLOW.toString() : 
                        ChatColor.RED.toString();
            String message = ChatColor.GRAY + "Obsidian: " + color + currentHP + "/" + maxHP + " HP";
            
            for (org.bukkit.entity.Player player : block.getWorld().getPlayers()) {
                if (player.getLocation().distance(block.getLocation()) <= 10) {
                    // Use Adventure API for action bar
                    try {
                        String parsedMessage = com.massivecraft.factions.zcore.util.TextUtil.parse(message);
                        net.kyori.adventure.text.Component component = 
                            net.kyori.adventure.text.Component.text(parsedMessage);
                        if (com.massivecraft.factions.zcore.util.TextUtil.AUDIENCES != null) {
                            com.massivecraft.factions.zcore.util.TextUtil.AUDIENCES.player(player)
                                    .sendActionBar(component);
                        } else {
                            player.sendMessage(message);
                        }
                    } catch (Exception e) {
                        // Fallback to regular message
                        player.sendMessage(message);
                    }
                }
            }
        }
    }

    /**
     * Show feedback when block breaks
     */
    private void showBlockBreakFeedback(Block block) {
        if (!Conf.obsidianShowBreakFeedback) {
            return;
        }

        // Play sound
        if (Conf.obsidianPlayBreakSound) {
            block.getWorld().playSound(block.getLocation(), 
                org.bukkit.Sound.BLOCK_ANVIL_BREAK, 1.0f, 0.5f);
        }

        // Show particles
        if (Conf.obsidianShowParticles) {
            block.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION, 
                block.getLocation().add(0.5, 0.5, 0.5), 1);
        }
    }
}

