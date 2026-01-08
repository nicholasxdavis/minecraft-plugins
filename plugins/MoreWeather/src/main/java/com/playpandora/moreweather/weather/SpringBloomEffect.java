package com.playpandora.moreweather.weather;

import com.playpandora.moreweather.MoreWeather;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Sheep;

import java.util.Random;

public class SpringBloomEffect extends WeatherEffect {
    
    private int tickCount = 0;
    private final Random random = new Random();
    
    public SpringBloomEffect(MoreWeather plugin, World world) {
        super(plugin, world);
    }
    
    @Override
    public void start() {
        active = true;
        plugin.getLogger().info("Spring Bloom started in " + world.getName());
    }
    
    @Override
    public void update() {
        if (!active) return;
        
        tickCount++;
        
        // Accelerate grass growth every 10 seconds
        if (tickCount % 200 == 0 && plugin.getConfig().getBoolean("weather.spring-bloom.grass-growth", true)) {
            accelerateGrassGrowth();
        }
        
        // Accelerate honey generation every 30 seconds
        if (tickCount % 600 == 0 && plugin.getConfig().getBoolean("weather.spring-bloom.honey-generation", true)) {
            accelerateHoneyGeneration();
        }
        
        // Make sheep regrow wool instantly when eating grass
        if (tickCount % 40 == 0 && plugin.getConfig().getBoolean("weather.spring-bloom.sheep-wool-regen", true)) {
            regrowSheepWool();
        }
        
        // Spread flowers every 20 seconds
        if (tickCount % 400 == 0 && plugin.getConfig().getBoolean("weather.spring-bloom.flower-spread", true)) {
            spreadFlowers();
        }
    }
    
    private void accelerateGrassGrowth() {
        int count = 0;
        for (int i = 0; i < 100 && count < 20; i++) {
            int x = random.nextInt(200) - 100;
            int z = random.nextInt(200) - 100;
            int y = world.getHighestBlockYAt(x, z);
            
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();
            
            if (type == Material.GRASS_BLOCK || type == Material.DIRT) {
                // Random tick to encourage growth
                block.randomTick();
                count++;
            }
        }
    }
    
    private void accelerateHoneyGeneration() {
        for (org.bukkit.entity.Bee bee : world.getEntitiesByClass(org.bukkit.entity.Bee.class)) {
            if (bee.getHive() != null && random.nextDouble() < 0.5) {
                // Increase honey level in hive
                org.bukkit.Location hiveLocation = bee.getHive();
                if (hiveLocation != null) {
                    org.bukkit.block.Block hiveBlock = world.getBlockAt(hiveLocation);
                    if (hiveBlock.getState() instanceof org.bukkit.block.Beehive) {
                        org.bukkit.block.Beehive beehive = (org.bukkit.block.Beehive) hiveBlock.getState();
                        // Simplified - would need to access hive data directly
                        // This is a placeholder
                    }
                }
            }
        }
    }
    
    private void regrowSheepWool() {
        for (Sheep sheep : world.getEntitiesByClass(Sheep.class)) {
            if (!sheep.isSheared() && sheep.isOnGround()) {
                Block block = sheep.getLocation().getBlock();
                if (block.getType() == Material.GRASS_BLOCK || 
                    block.getRelative(0, -1, 0).getType() == Material.GRASS_BLOCK) {
                // Sheep eats grass and regrows wool
                if (sheep.isSheared()) {
                    sheep.setSheared(false);
                }
                }
            }
        }
    }
    
    private void spreadFlowers() {
        int count = 0;
        for (int i = 0; i < 50 && count < 10; i++) {
            int x = random.nextInt(200) - 100;
            int z = random.nextInt(200) - 100;
            int y = world.getHighestBlockYAt(x, z) + 1;
            
            Block block = world.getBlockAt(x, y, z);
            if (block.getType() == Material.AIR) {
                Block below = block.getRelative(0, -1, 0);
                if (below.getType() == Material.GRASS_BLOCK) {
                    Material[] flowers = {
                        Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID,
                        Material.ALLIUM, Material.AZURE_BLUET, Material.RED_TULIP,
                        Material.ORANGE_TULIP, Material.WHITE_TULIP, Material.PINK_TULIP
                    };
                    block.setType(flowers[random.nextInt(flowers.length)]);
                    count++;
                }
            }
        }
    }
    
    @Override
    public void stop() {
        active = false;
        plugin.getLogger().info("Spring Bloom stopped in " + world.getName());
    }
}

