package com.playpandora.treasurehunt.managers;

import com.playpandora.treasurehunt.TreasureHunt;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class TreasureManager {
    
    private final TreasureHunt plugin;
    private Location currentTreasureLocation;
    private final Map<UUID, Integer> playerHintsClaimed = new HashMap<>();
    private final Map<UUID, Location> playerLastHintLocation = new HashMap<>();
    
    public TreasureManager(TreasureHunt plugin) {
        this.plugin = plugin;
        loadData();
    }
    
    public void startTreasureSpawnTask() {
        long interval = plugin.getConfig().getLong("treasure.spawn-interval", 36000L);
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            spawnNewTreasure();
        }, 0L, interval);
        
        // Start ambient particle effect task for treasure
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (currentTreasureLocation != null) {
                org.bukkit.Location particleLoc = currentTreasureLocation.clone().add(0.5, 1.5, 0.5);
                currentTreasureLocation.getWorld().spawnParticle(org.bukkit.Particle.ENCHANT, particleLoc, 5, 0.3, 0.3, 0.3, 0.1);
                currentTreasureLocation.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, particleLoc, 3, 0.2, 0.2, 0.2, 0.05);
            }
        }, 0L, 20L); // Every second
    }
    
    public void spawnNewTreasure() {
        // Find a random location in the overworld
        World world = plugin.getServer().getWorlds().get(0);
        if (world == null) {
            return;
        }
        
        Random random = new Random();
        int maxAttempts = 20;
        
        for (int i = 0; i < maxAttempts; i++) {
            int x = random.nextInt(10000) - 5000;
            int z = random.nextInt(10000) - 5000;
            int y = world.getHighestBlockYAt(x, z);
            
            Location loc = new Location(world, x, y, z);
            Block block = loc.getBlock();
            
            // Make sure it's a safe location
            if (block.getType() == Material.AIR || block.getType() == Material.WATER || block.getType() == Material.LAVA) {
                continue;
            }
            
            // Place treasure chest
            loc.add(0, 1, 0);
            loc.getBlock().setType(Material.CHEST);
            
            currentTreasureLocation = loc;
            playerHintsClaimed.clear();
            playerLastHintLocation.clear();
            
            // Create hologram above treasure chest
            if (plugin.getHologramManager() != null) {
                String hologramText = plugin.getConfig().getString("hologram.text", "&e&lTREASURE");
                plugin.getHologramManager().createHologram(loc, hologramText);
            }
            
            // Spawn particles at treasure location
            org.bukkit.Location particleLoc = loc.clone().add(0.5, 1.5, 0.5);
            world.spawnParticle(org.bukkit.Particle.ENCHANT, particleLoc, 50, 0.5, 0.5, 0.5, 0.3);
            world.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, particleLoc, 30, 0.3, 0.3, 0.3, 0.1);
            
            plugin.getLogger().info("New treasure spawned at " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
            break;
        }
    }
    
    public boolean claimHint(Player player) {
        if (currentTreasureLocation == null) {
            return false;
        }
        
        Location playerLoc = player.getLocation();
        if (!playerLoc.getWorld().equals(currentTreasureLocation.getWorld())) {
            return false;
        }
        
        double distance = playerLoc.distance(currentTreasureLocation);
        double maxDistance = plugin.getConfig().getDouble("treasure.hint-distance", 100.0);
        
        if (distance > maxDistance) {
            return false;
        }
        
        UUID uuid = player.getUniqueId();
        int hintsClaimed = playerHintsClaimed.getOrDefault(uuid, 0);
        int maxHints = plugin.getConfig().getInt("treasure.max-hints", 5);
        
        // Check if player has already claimed all hints
        if (hintsClaimed >= maxHints) {
            String prefix = "&e&lPandora &8» &r";
            player.sendMessage(plugin.formatMessage(prefix + " &7You have already claimed all hints! You can now claim the treasure."));
            return false;
        }
        
        // Player can claim another hint
        playerHintsClaimed.put(uuid, hintsClaimed + 1);
        playerLastHintLocation.put(uuid, playerLoc);
        
        // Add particles and sound effect
        org.bukkit.Location particleLoc = playerLoc.clone().add(0, 1, 0);
        player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, particleLoc, 30, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, particleLoc, 20, 0.3, 0.3, 0.3, 0.05);
        player.playSound(playerLoc, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        player.playSound(playerLoc, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
        
        // Give reward
        plugin.getRewardManager().giveHintReward(player);
        
        // Announce distance
        announceHintDistance(player);
        
        return true;
    }
    
    public boolean claimTreasure(Player player) {
        if (currentTreasureLocation == null) {
            return false;
        }
        
        Location playerLoc = player.getLocation();
        if (!playerLoc.getWorld().equals(currentTreasureLocation.getWorld())) {
            return false;
        }
        
        double distance = playerLoc.distance(currentTreasureLocation);
        if (distance > 5.0) { // Must be within 5 blocks
            return false;
        }
        
        // Check if player has claimed enough hints
        UUID uuid = player.getUniqueId();
        int hintsClaimed = playerHintsClaimed.getOrDefault(uuid, 0);
        int requiredHints = plugin.getConfig().getInt("treasure.max-hints", 5);
        
        if (hintsClaimed < requiredHints) {
            String prefix = "&e&lPandora &8» &r";
            player.sendMessage(plugin.formatMessage(prefix + " &cYou need to claim &6" + (requiredHints - hintsClaimed) + " &7more hints first!"));
            return false;
        }
        
        // Add spectacular particles and sounds
        org.bukkit.Location treasureLoc = currentTreasureLocation.clone();
        treasureLoc.add(0.5, 1.0, 0.5);
        player.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION, treasureLoc, 3, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().spawnParticle(org.bukkit.Particle.FIREWORK, treasureLoc, 50, 1.0, 1.0, 1.0, 0.3);
        player.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, treasureLoc, 100, 1.5, 1.5, 1.5, 0.1);
        player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, treasureLoc, 30, 0.8, 0.8, 0.8, 0.1);
        player.playSound(treasureLoc, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 0.8f);
        player.playSound(treasureLoc, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.5f, 0.6f);
        player.playSound(treasureLoc, org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
        
        // Give reward
        plugin.getRewardManager().giveTreasureReward(player);
        
        // Remove treasure
        treasureLoc.getBlock().setType(Material.AIR);
        
        // Remove hologram
        if (plugin.getHologramManager() != null) {
            plugin.getHologramManager().removeHologram(currentTreasureLocation);
        }
        
        currentTreasureLocation = null;
        playerHintsClaimed.clear();
        playerLastHintLocation.clear();
        
        return true;
    }
    
    public int getHintsRemaining(Player player) {
        if (currentTreasureLocation == null) {
            return -1;
        }
        
        UUID uuid = player.getUniqueId();
        int hintsClaimed = playerHintsClaimed.getOrDefault(uuid, 0);
        int maxHints = plugin.getConfig().getInt("treasure.max-hints", 5);
        return maxHints - hintsClaimed;
    }
    
    public int getHintsAway(Player player) {
        if (currentTreasureLocation == null) {
            return -1;
        }
        
        UUID uuid = player.getUniqueId();
        int hintsClaimed = playerHintsClaimed.getOrDefault(uuid, 0);
        int maxHints = plugin.getConfig().getInt("treasure.max-hints", 5);
        return Math.max(0, maxHints - hintsClaimed);
    }
    
    public void announceHintDistance(Player player) {
        if (!plugin.getConfig().getBoolean("announcements.hint-distance", true)) {
            return;
        }
        
        int hintsAway = getHintsAway(player);
        if (hintsAway > 0) {
            plugin.getIntegrationManager().announceHintDistance(player, hintsAway);
        }
    }
    
    public Location getCurrentTreasureLocation() {
        return currentTreasureLocation;
    }
    
    public boolean hasActiveTreasure() {
        return currentTreasureLocation != null;
    }
    
    public void saveData() {
        FileConfiguration config = plugin.getConfig();
        if (currentTreasureLocation != null) {
            config.set("treasure.location.world", currentTreasureLocation.getWorld().getName());
            config.set("treasure.location.x", currentTreasureLocation.getX());
            config.set("treasure.location.y", currentTreasureLocation.getY());
            config.set("treasure.location.z", currentTreasureLocation.getZ());
        } else {
            config.set("treasure.location", null);
        }
        plugin.saveConfig();
    }
    
    public void loadData() {
        try {
            FileConfiguration config = plugin.getConfig();
            if (config.contains("treasure.location.world")) {
                String worldName = config.getString("treasure.location.world");
                World world = plugin.getServer().getWorld(worldName);
                if (world != null) {
                    double x = config.getDouble("treasure.location.x");
                    double y = config.getDouble("treasure.location.y");
                    double z = config.getDouble("treasure.location.z");
                    currentTreasureLocation = new Location(world, x, y, z);
                    // Note: Hologram will be recreated after all managers are initialized
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load treasure data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Recreate hologram for existing treasure. Should be called after all managers are initialized.
     */
    public void recreateHologramIfNeeded() {
        if (currentTreasureLocation == null) {
            return;
        }
        
        if (plugin.getHologramManager() == null) {
            return;
        }
        
        try {
            // Check if the chest still exists at the location
            if (currentTreasureLocation.getBlock().getType() == Material.CHEST) {
                String hologramText = plugin.getConfig().getString("hologram.text", "&e&lTREASURE");
                plugin.getHologramManager().createHologram(currentTreasureLocation, hologramText);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to recreate hologram: " + e.getMessage());
        }
    }
}


