package com.playpandora.moreweather.weather;

import com.playpandora.moreweather.MoreWeather;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class HeatwaveEffect extends WeatherEffect {
    
    private int tickCount = 0;
    private final Random random = new Random();
    
    public HeatwaveEffect(MoreWeather plugin, World world) {
        super(plugin, world);
    }
    
    @Override
    public void start() {
        active = true;
        plugin.getLogger().info("Heatwave started in " + world.getName());
    }
    
    @Override
    public void update() {
        if (!active) return;
        
        tickCount++;
        
        // Apply effects every second
        if (tickCount % 20 == 0) {
            world.getPlayers().forEach(player -> {
                // Check for gold armor (heat resistance)
                boolean hasGoldArmor = hasGoldArmor(player);
                
                if (!hasGoldArmor && plugin.getConfig().getBoolean("weather.heatwave.fatigue", true)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 40, 0, true, false));
                    player.setExhaustion(player.getExhaustion() + 0.5f);
                }
                
                // Fire resistance benefits
                if (plugin.getConfig().getBoolean("weather.heatwave.fire-resistance", true)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40, 0, true, false));
                }
                
                // Increased brightness
                if (plugin.getConfig().getBoolean("weather.heatwave.increased-brightness", true)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 400, 0, true, false));
                }
            });
            
            // Horses run faster
            for (Horse horse : world.getEntitiesByClass(Horse.class)) {
                if (horse.isTamed() && horse.getOwner() instanceof Player) {
                    horse.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, true, false));
                }
            }
            
            // Creepers ignite faster
            for (Creeper creeper : world.getEntitiesByClass(Creeper.class)) {
                if (creeper.isIgnited() && plugin.getConfig().getBoolean("weather.heatwave.faster-creeper-ignition", true)) {
                    creeper.setMaxFuseTicks(creeper.getMaxFuseTicks() - 5);
                }
            }
        }
        
        // Evaporate water in exposed containers every 30 seconds
        if (tickCount % 600 == 0 && plugin.getConfig().getBoolean("weather.heatwave.evaporate-water", true)) {
            evaporateWater();
        }
    }
    
    private boolean hasGoldArmor(Player player) {
        if (player.getInventory().getHelmet() != null && 
            player.getInventory().getHelmet().getType() == Material.GOLDEN_HELMET) {
            return true;
        }
        if (player.getInventory().getChestplate() != null && 
            player.getInventory().getChestplate().getType() == Material.GOLDEN_CHESTPLATE) {
            return true;
        }
        if (player.getInventory().getLeggings() != null && 
            player.getInventory().getLeggings().getType() == Material.GOLDEN_LEGGINGS) {
            return true;
        }
        if (player.getInventory().getBoots() != null && 
            player.getInventory().getBoots().getType() == Material.GOLDEN_BOOTS) {
            return true;
        }
        return false;
    }
    
    private void evaporateWater() {
        int count = 0;
        for (int i = 0; i < 50 && count < 10; i++) {
            int x = random.nextInt(200) - 100;
            int z = random.nextInt(200) - 100;
            int y = random.nextInt(64) + 60; // Above ground level
            
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();
            
            if (type == Material.WATER_CAULDRON) {
                block.setType(Material.CAULDRON);
                count++;
            } else if (type == Material.WATER && block.getY() > world.getSeaLevel()) {
                // Check if exposed to sky
                if (world.getHighestBlockYAt(x, z) <= block.getY()) {
                    block.setType(Material.AIR);
                    count++;
                }
            }
        }
    }
    
    @Override
    public void stop() {
        active = false;
        plugin.getLogger().info("Heatwave stopped in " + world.getName());
    }
}








