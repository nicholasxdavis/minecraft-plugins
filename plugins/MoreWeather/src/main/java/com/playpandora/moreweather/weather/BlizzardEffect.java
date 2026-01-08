package com.playpandora.moreweather.weather;

import com.playpandora.moreweather.MoreWeather;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Stray;
import org.bukkit.entity.Wolf;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class BlizzardEffect extends WeatherEffect {
    
    private int tickCount = 0;
    private final Random random = new Random();
    
    public BlizzardEffect(MoreWeather plugin, World world) {
        super(plugin, world);
    }
    
    @Override
    public void start() {
        active = true;
        plugin.getLogger().info("Blizzard started in " + world.getName());
    }
    
    @Override
    public void update() {
        if (!active) return;
        
        tickCount++;
        
        // Apply effects every second
        if (tickCount % 20 == 0) {
            world.getPlayers().forEach(player -> {
                // Check for leather or wool armor
                boolean hasColdProtection = hasColdProtection(player);
                
                if (!hasColdProtection && plugin.getConfig().getBoolean("weather.blizzard.movement-slow", true)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, true, false));
                }
                
                // Reduced visibility
                if (plugin.getConfig().getBoolean("weather.blizzard.reduce-visibility", true)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1, true, false));
                }
                
                // Cold damage if no protection
                if (!hasColdProtection && plugin.getConfig().getBoolean("weather.blizzard.cold-damage", false)) {
                    if (tickCount % 100 == 0) { // Every 5 seconds
                        player.damage(1.0);
                    }
                }
                
                // Torches/campfires reduce cold
                if (isNearHeatSource(player.getLocation())) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0, true, false));
                }
            });
            
            // Make wolves more aggressive
            for (Wolf wolf : world.getEntitiesByClass(Wolf.class)) {
                if (wolf.getTarget() == null && random.nextDouble() < 0.3) {
                    Player nearest = getNearestPlayer(wolf.getLocation());
                    if (nearest != null && nearest.getLocation().distance(wolf.getLocation()) < 20) {
                        wolf.setTarget(nearest);
                    }
                }
            }
        }
        
        // Build up snow layers every 5 seconds
        if (tickCount % 100 == 0 && plugin.getConfig().getBoolean("weather.blizzard.snow-buildup", true)) {
            buildSnowLayers();
        }
        
        // Spawn more strays
        if (tickCount % 200 == 0 && plugin.getConfig().getBoolean("weather.blizzard.more-strays", true)) {
            spawnStrays();
        }
    }
    
    private boolean hasColdProtection(Player player) {
        if (player.getInventory().getHelmet() != null) {
            Material helmet = player.getInventory().getHelmet().getType();
            if (helmet == Material.LEATHER_HELMET || helmet.name().contains("WOOL")) {
                return true;
            }
        }
        if (player.getInventory().getChestplate() != null) {
            Material chest = player.getInventory().getChestplate().getType();
            if (chest == Material.LEATHER_CHESTPLATE || chest.name().contains("WOOL")) {
                return true;
            }
        }
        if (player.getInventory().getLeggings() != null) {
            Material legs = player.getInventory().getLeggings().getType();
            if (legs == Material.LEATHER_LEGGINGS || legs.name().contains("WOOL")) {
                return true;
            }
        }
        if (player.getInventory().getBoots() != null) {
            Material boots = player.getInventory().getBoots().getType();
            if (boots == Material.LEATHER_BOOTS || boots.name().contains("WOOL")) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isNearHeatSource(Location loc) {
        int radius = 5;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = loc.getWorld().getBlockAt(
                        loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);
                    if (block.getType() == Material.TORCH || block.getType() == Material.CAMPFIRE ||
                        block.getType() == Material.SOUL_CAMPFIRE || block.getType() == Material.LAVA) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private Player getNearestPlayer(Location loc) {
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Player player : world.getPlayers()) {
            double dist = player.getLocation().distance(loc);
            if (dist < nearestDist && dist < 20) {
                nearestDist = dist;
                nearest = player;
            }
        }
        return nearest;
    }
    
    private void buildSnowLayers() {
        int count = 0;
        for (int i = 0; i < 30 && count < 5; i++) {
            int x = random.nextInt(200) - 100;
            int z = random.nextInt(200) - 100;
            int y = world.getHighestBlockYAt(x, z);
            
            org.bukkit.block.Block block = world.getBlockAt(x, y, z);
            if (block.getType() == Material.SNOW) {
                // Build up snow layer using BlockData API
                org.bukkit.block.data.BlockData blockData = block.getBlockData();
                if (blockData instanceof org.bukkit.block.data.Levelled) {
                    org.bukkit.block.data.Levelled levelled = (org.bukkit.block.data.Levelled) blockData;
                    int currentLevel = levelled.getLevel();
                    int maxLevel = levelled.getMaximumLevel();
                    if (currentLevel < maxLevel) {
                        levelled.setLevel(currentLevel + 1);
                        block.setBlockData(levelled);
                    }
                }
                count++;
            } else if (block.getType() == Material.AIR) {
                block.setType(Material.SNOW);
                count++;
            }
        }
    }
    
    private void spawnStrays() {
        if (world.getPlayers().isEmpty()) return;
        
        Player randomPlayer = world.getPlayers().get(random.nextInt(world.getPlayers().size()));
        int x = randomPlayer.getLocation().getBlockX() + random.nextInt(50) - 25;
        int z = randomPlayer.getLocation().getBlockZ() + random.nextInt(50) - 25;
        int y = world.getHighestBlockYAt(x, z) + 1;
        
        if (random.nextDouble() < 0.3) {
            world.spawnEntity(new Location(world, x, y, z), org.bukkit.entity.EntityType.STRAY);
        }
    }
    
    @Override
    public void stop() {
        active = false;
        plugin.getLogger().info("Blizzard stopped in " + world.getName());
    }
}

