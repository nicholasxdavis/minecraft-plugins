package com.playpandora.moreweather.weather;

import com.playpandora.moreweather.MoreWeather;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class FogEffect extends WeatherEffect {
    
    private int tickCount = 0;
    private final Random random = new Random();
    
    public FogEffect(MoreWeather plugin, World world) {
        super(plugin, world);
    }
    
    @Override
    public void start() {
        active = true;
        plugin.getLogger().info("Fog started in " + world.getName());
    }
    
    @Override
    public void update() {
        if (!active) return;
        
        tickCount++;
        
        // Apply reduced visibility every second
        if (tickCount % 20 == 0) {
            world.getPlayers().forEach(player -> {
                // Drastically reduce visibility
                if (plugin.getConfig().getBoolean("weather.fog.reduce-visibility", true)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 2, true, false));
                }
                
                // Reduced night vision
                if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                    player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                }
            });
            
            // Make villagers hide indoors
            for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                if (!isIndoors(villager.getLocation()) && random.nextDouble() < 0.5) {
                    // Find nearest door/house and move towards it
                    // Simplified - would need pathfinding in full implementation
                }
            }
        }
        
        // Spawn phantoms even for players who slept
        if (tickCount % 200 == 0 && plugin.getConfig().getBoolean("weather.fog.spawn-phantoms", true)) {
            spawnPhantoms();
        }
        
        // Reduce mob detection range
        plugin.getMobBehaviorManager().setFogActive(true);
    }
    
    private boolean isIndoors(Location loc) {
        // Simple check - if blocks above, probably indoors
        return loc.getWorld().getHighestBlockYAt(loc) > loc.getBlockY() + 2;
    }
    
    private void spawnPhantoms() {
        for (Player player : world.getPlayers()) {
            if (random.nextDouble() < 0.3) {
                Location spawnLoc = player.getLocation().add(
                    random.nextInt(20) - 10, 30 + random.nextInt(10), random.nextInt(20) - 10
                );
                Phantom phantom = (Phantom) world.spawnEntity(spawnLoc, EntityType.PHANTOM);
                phantom.setTarget(player);
            }
        }
    }
    
    @Override
    public void stop() {
        active = false;
        plugin.getMobBehaviorManager().setFogActive(false);
        plugin.getLogger().info("Fog stopped in " + world.getName());
    }
}








