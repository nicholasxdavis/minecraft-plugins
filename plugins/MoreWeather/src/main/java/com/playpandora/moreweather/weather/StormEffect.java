package com.playpandora.moreweather.weather;

import com.playpandora.moreweather.MoreWeather;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

import java.util.*;

public class StormEffect extends WeatherEffect {
    
    private int tickCount = 0;
    private final Set<Block> chargedLightningRods = new HashSet<>();
    private final Map<Player, Integer> metalItemHeldTicks = new HashMap<>();
    private final Random random = new Random();
    
    public StormEffect(MoreWeather plugin, World world) {
        super(plugin, world);
    }
    
    @Override
    public void start() {
        active = true;
        plugin.getLogger().info("Storm started in " + world.getName());
    }
    
    @Override
    public void update() {
        if (!active) return;
        
        tickCount++;
        
        // Lightning strikes every 30-60 seconds
        if (tickCount % (600 + random.nextInt(600)) == 0) {
            spawnLightning();
        }
        
        // Check lightning rods
        if (tickCount % 20 == 0) {
            updateLightningRods();
        }
        
        // Check players holding metal items
        if (tickCount % 20 == 0) {
            checkMetalItemHolding();
        }
        
        // Lightning surge event (rare)
        if (random.nextDouble() < 0.001) { // 0.1% chance per tick
            triggerLightningSurge();
        }
        
        // Convert zombies to special variants during storm
        if (tickCount % 100 == 0) {
            convertZombies();
        }
    }
    
    private void spawnLightning() {
        if (world.getPlayers().isEmpty()) return;
        
        Player randomPlayer = world.getPlayers().get(random.nextInt(world.getPlayers().size()));
        int radius = plugin.getConfig().getInt("weather.storm.lightning-radius", 100);
        
        int x = randomPlayer.getLocation().getBlockX() + random.nextInt(radius * 2) - radius;
        int z = randomPlayer.getLocation().getBlockZ() + random.nextInt(radius * 2) - radius;
        int y = world.getHighestBlockYAt(x, z);
        
        Location strikeLoc = new Location(world, x, y, z);
        world.strikeLightning(strikeLoc);
        
        // Check for lightning rod
        Block block = world.getBlockAt(x, y - 1, z);
        if (block.getType() == Material.LIGHTNING_ROD) {
            chargedLightningRods.add(block);
            activateRedstoneNearRod(block);
        }
    }
    
    private void activateRedstoneNearRod(Block rod) {
        if (!plugin.getConfig().getBoolean("weather.storm.lightning-rods-power-redstone", true)) {
            return;
        }
        
        int radius = plugin.getConfig().getInt("weather.storm.lightning-rod-radius", 5);
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block nearby = rod.getRelative(x, y, z);
                    if (nearby.getType() == Material.REDSTONE_WIRE) {
                        // Power the redstone (simplified - in reality would need to use redstone API)
                        // This is a placeholder for the concept
                    }
                }
            }
        }
    }
    
    private void updateLightningRods() {
        chargedLightningRods.removeIf(block -> block.getType() != Material.LIGHTNING_ROD);
    }
    
    private void checkMetalItemHolding() {
        for (Player player : world.getPlayers()) {
            if (player.getInventory().getItemInMainHand() != null) {
                Material item = player.getInventory().getItemInMainHand().getType();
                if (isMetalItem(item)) {
                    metalItemHeldTicks.put(player, metalItemHeldTicks.getOrDefault(player, 0) + 1);
                    
                    // 0.1% chance per second if holding metal item
                    if (random.nextDouble() < 0.001) {
                        world.strikeLightning(player.getLocation());
                        // Send Hook notification instead of chat message
                        sendHookNotification(player, "Lightning Strike!", "&7You were struck by lightning!");
                        // Award XP for surviving lightning strike
                        if (plugin.getIntegrations() != null && plugin.getIntegrations().isLevelPluginEnabled()) {
                            plugin.getIntegrations().getLevelPlugin().awardWeatherXP(player, "survive-lightning");
                        }
                    }
                } else {
                    metalItemHeldTicks.remove(player);
                }
            }
        }
    }
    
    private boolean isMetalItem(Material item) {
        return item.name().contains("IRON") || item.name().contains("GOLD") || 
               item.name().contains("DIAMOND") || item.name().contains("NETHERITE") ||
               (item.name().contains("SWORD") || item.name().contains("AXE") || 
                item.name().contains("SHOVEL") || item.name().contains("HOE"));
    }
    
    private void triggerLightningSurge() {
        if (world.getPlayers().isEmpty()) return;
        
        Player randomPlayer = world.getPlayers().get(random.nextInt(world.getPlayers().size()));
        int chunkX = randomPlayer.getLocation().getChunk().getX();
        int chunkZ = randomPlayer.getLocation().getChunk().getZ();
        
        int strikes = 3 + random.nextInt(4); // 3-6 strikes
        
        for (int i = 0; i < strikes; i++) {
            int x = (chunkX * 16) + random.nextInt(16);
            int z = (chunkZ * 16) + random.nextInt(16);
            int y = world.getHighestBlockYAt(x, z);
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                world.strikeLightning(new org.bukkit.Location(world, x, y, z));
            }, i * 10L);
        }
        
        world.getPlayers().forEach(p -> {
            if (p.getLocation().getChunk().getX() == chunkX && 
                p.getLocation().getChunk().getZ() == chunkZ) {
                // Send Hook notification instead of chat message
                sendHookNotification(p, "Lightning Surge!", "&7Multiple strikes in your area!");
            }
        });
    }
    
    private void convertZombies() {
        for (Zombie zombie : world.getEntitiesByClass(Zombie.class)) {
            if (random.nextDouble() < 0.05) { // 5% chance
                // Convert to charged variant (if config enabled)
                if (plugin.getConfig().getBoolean("weather.storm.convert-zombies", true)) {
                    // Use lightning strike near zombie to charge it
                    world.strikeLightningEffect(zombie.getLocation());
                }
            }
        }
    }
    
    @Override
    public void stop() {
        active = false;
        chargedLightningRods.clear();
        metalItemHeldTicks.clear();
        plugin.getLogger().info("Storm stopped in " + world.getName());
    }
    
    /**
     * Send a Hook notification to a player
     */
    private void sendHookNotification(org.bukkit.entity.Player player, String title, String subtitle) {
        try {
            org.bukkit.plugin.Plugin hookPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("Hook");
            if (hookPlugin != null && hookPlugin.isEnabled()) {
                Object hookInstance = hookPlugin.getClass().getMethod("getInstance").invoke(null);
                Object hookAPI = hookInstance.getClass().getMethod("getAPI").invoke(hookInstance);
                hookAPI.getClass().getMethod("sendCustom", org.bukkit.entity.Player.class, 
                    String.class, String.class, int.class, int.class, int.class)
                    .invoke(hookAPI, player, title, subtitle, 300, 2500, 800);
            }
        } catch (Exception e) {
            // Silently fail if Hook is not available
        }
    }
}

