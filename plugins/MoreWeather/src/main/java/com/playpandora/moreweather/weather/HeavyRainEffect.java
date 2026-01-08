package com.playpandora.moreweather.weather;

import com.playpandora.moreweather.MoreWeather;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class HeavyRainEffect extends WeatherEffect {
    
    private int tickCount = 0;
    private final Random random = new Random();
    
    public HeavyRainEffect(MoreWeather plugin, World world) {
        super(plugin, world);
    }
    
    @Override
    public void start() {
        active = true;
        plugin.getLogger().info("Heavy Rain started in " + world.getName());
    }
    
    @Override
    public void update() {
        if (!active) return;
        
        tickCount++;
        
        // Apply effects every second
        if (tickCount % 20 == 0) {
            // Apply movement slow and visibility effects to players
            world.getPlayers().forEach(player -> {
                // Check if player has leather boots
                boolean hasLeatherBoots = player.getInventory().getBoots() != null &&
                    player.getInventory().getBoots().getType() == Material.LEATHER_BOOTS;
                
                if (!hasLeatherBoots && plugin.getConfig().getBoolean("weather.heavy-rain.movement-slow", true)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, true, false));
                }
                
                // Reduced visibility
                if (plugin.getConfig().getBoolean("weather.heavy-rain.reduce-visibility", true)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, true, false));
                }
            });
            
            // Slow skeleton arrow velocity
            for (Entity entity : world.getEntitiesByClass(Skeleton.class)) {
                Skeleton skeleton = (Skeleton) entity;
                if (skeleton.getTarget() instanceof Player) {
                    // Modify arrow velocity when shooting
                    plugin.getMobBehaviorManager().applyRainSkeletonModifier(skeleton);
                }
            }
        }
        
        // Accelerate crop growth every 10 seconds
        if (tickCount % 200 == 0 && plugin.getConfig().getBoolean("weather.heavy-rain.crop-growth", true)) {
            accelerateCropGrowth();
        }
        
        // Increase fish spawns every 30 seconds
        if (tickCount % 600 == 0 && plugin.getConfig().getBoolean("weather.heavy-rain.more-fish", true)) {
            spawnExtraFish();
        }
    }
    
    private void accelerateCropGrowth() {
        int count = 0;
        for (int i = 0; i < 50 && count < 10; i++) {
            int x = random.nextInt(200) - 100;
            int z = random.nextInt(200) - 100;
            int y = world.getHighestBlockYAt(x, z);
            
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();
            
            if (type == Material.WHEAT || type == Material.CARROTS || type == Material.POTATOES ||
                type == Material.BEETROOTS || type == Material.MELON_STEM || type == Material.PUMPKIN_STEM) {
                block.randomTick();
                count++;
            }
        }
    }
    
    private void spawnExtraFish() {
        world.getPlayers().forEach(player -> {
            if (player.getLocation().getBlock().getBiome().toString().contains("OCEAN") ||
                player.getLocation().getBlock().getBiome().toString().contains("RIVER")) {
                if (random.nextDouble() < 0.3) {
                    world.spawnEntity(player.getLocation().add(
                        random.nextInt(10) - 5, 0, random.nextInt(10) - 5
                    ), EntityType.COD);
                }
            }
        });
    }
    
    @Override
    public void stop() {
        active = false;
        plugin.getLogger().info("Heavy Rain stopped in " + world.getName());
    }
}








