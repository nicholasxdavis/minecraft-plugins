package com.playpandora.moremobs.handlers;

import com.playpandora.moremobs.MoreMobs;
import com.playpandora.moremobs.util.WorldUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class SpawnLogicHandler implements Listener {
    
    private final MoreMobs plugin;
    private final Map<UUID, Long> playerLastSleepTime = new HashMap<>();
    private final Map<UUID, Integer> playerInsomniaTicks = new HashMap<>();
    private final Random random = new Random();
    
        // Track patrol spawns
    private final Map<World, Long> lastPatrolSpawn = new HashMap<>();
    
    public SpawnLogicHandler(MoreMobs plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Start tracking player sleep
        startSleepTracker();
        startPatrolTimer();
        startRareSnifferSpawner();
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.isCancelled()) return;
        
        LivingEntity entity = event.getEntity();
        Location loc = entity.getLocation();
        
        // Only handle natural spawns
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL &&
            event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER) {
            return;
        }
        
        // Handle specific mob types
        switch (entity.getType()) {
            case DOLPHIN:
                handleDolphinSpawn((Dolphin) entity, loc);
                break;
            case DROWNED:
                handleDrownedSpawn((Drowned) entity, loc);
                break;
            case PHANTOM:
                handlePhantomSpawn((Phantom) entity, loc);
                break;
            case STRIDER:
                handleStriderSpawn((Strider) entity, loc);
                break;
            case FROG:
                handleFrogSpawn((Frog) entity, loc);
                break;
            case SNIFFER:
                handleSnifferSpawn((Sniffer) entity, loc);
                break;
            default:
                break;
        }
    }
    
    private void handleDolphinSpawn(Dolphin dolphin, Location loc) {
        if (!plugin.getConfig().getBoolean("spawn-logic.dolphins.enabled", true)) return;
        
        // Check if near shipwreck
        if (WorldUtils.isNearStructure(loc, WorldUtils.StructureType.SHIPWRECK, 64)) {
            double multiplier = plugin.getConfig().getDouble("spawn-logic.dolphins.spawn-weight-near-shipwrecks", 1.5);
            if (random.nextDouble() < (multiplier - 1.0) * 0.1) { // Chance to spawn additional
                spawnAdditionalDolphins(loc, 1);
            }
        }
        
        // Increase curiosity range
        double curiosityMultiplier = plugin.getConfig().getDouble("spawn-logic.dolphins.curiosity-range-multiplier", 1.3);
        // Note: Dolphin behavior modification would require NMS or Paper API extensions
        // This is a conceptual implementation
    }
    
    private void handleDrownedSpawn(Drowned drowned, Location loc) {
        if (!plugin.getConfig().getBoolean("spawn-logic.drowned.enabled", true)) return;
        
        // More common around ocean ruins
        if (WorldUtils.isNearStructure(loc, WorldUtils.StructureType.OCEAN_RUINS, 48)) {
            double multiplier = plugin.getConfig().getDouble("spawn-logic.drowned.spawn-multiplier-near-ocean-ruins", 1.4);
            if (random.nextDouble() < (multiplier - 1.0) * 0.15) {
                spawnAdditionalDrowned(loc, 1);
            }
        }
        
        // Better trident carrier ratio
        double tridentBoost = plugin.getConfig().getDouble("spawn-logic.drowned.trident-carrier-ratio-boost", 0.1);
        if (random.nextDouble() < tridentBoost && !drowned.getEquipment().getItemInMainHand().getType().equals(Material.TRIDENT)) {
            // Give trident chance (vanilla is about 8% with NBT data, we'll approximate)
            if (random.nextDouble() < 0.15) { // Approximate boost
                drowned.getEquipment().setItemInMainHand(new org.bukkit.inventory.ItemStack(Material.TRIDENT));
            }
        }
    }
    
    private void handlePhantomSpawn(Phantom phantom, Location loc) {
        if (!plugin.getConfig().getBoolean("spawn-logic.phantoms.enabled", true)) return;
        
        // Find nearby players and check insomnia
        for (Player player : loc.getWorld().getPlayers()) {
            if (player.getLocation().distance(loc) <= 128) {
                Integer insomniaTicks = playerInsomniaTicks.get(player.getUniqueId());
                if (insomniaTicks != null && insomniaTicks > 0) {
                    // Scale spawn rate with insomnia duration
                    if (plugin.getConfig().getBoolean("spawn-logic.phantoms.spawn-rate-scales-with-insomnia", true)) {
                        // More phantoms for higher insomnia
                        double scale = Math.min(insomniaTicks / 72000.0, 2.0); // Max 2x at 1 hour insomnia
                        if (random.nextDouble() < scale * 0.2) {
                            spawnAdditionalPhantoms(loc, 1);
                        }
                    }
                    
                    // Check if player sleeps regularly
                    Long lastSleep = playerLastSleepTime.get(player.getUniqueId());
                    if (lastSleep != null && System.currentTimeMillis() - lastSleep < 120000) { // Slept in last 2 minutes
                        double intervalMultiplier = plugin.getConfig().getDouble("spawn-logic.phantoms.regular-sleeper-interval-multiplier", 1.5);
                        // Cancel this spawn with a chance based on multiplier
                        if (random.nextDouble() < (intervalMultiplier - 1.0) / intervalMultiplier) {
                            phantom.remove();
                            return;
                        }
                    }
                }
                break;
            }
        }
    }
    
    private void handleStriderSpawn(Strider strider, Location loc) {
        if (!plugin.getConfig().getBoolean("spawn-logic.striders.enabled", true)) return;
        
        // Extra spawns near major lava lakes
        if (WorldUtils.countBlocksInArea(loc, Material.LAVA, 32, 16) > 100) {
            if (plugin.getConfig().getBoolean("spawn-logic.striders.extra-spawns-near-lava-lakes", true)) {
                double multiplier = plugin.getConfig().getDouble("spawn-logic.striders.lava-lake-spawn-multiplier", 1.6);
                if (random.nextDouble() < (multiplier - 1.0) * 0.2) {
                    spawnAdditionalStriders(loc, 1);
                }
            }
        } else {
            // Reduced random stray spawns
            if (plugin.getConfig().getBoolean("spawn-logic.striders.reduced-random-spawns", true)) {
                if (random.nextDouble() < 0.3) { // 30% chance to cancel stray spawns
                    strider.remove();
                    return;
                }
            }
        }
    }
    
    private void handleFrogSpawn(Frog frog, Location loc) {
        if (!plugin.getConfig().getBoolean("spawn-logic.frogs.enabled", true)) return;
        
        // More night-time swamp spawns
        if (WorldUtils.isSwampBiome(loc)) {
            long time = loc.getWorld().getTime();
            boolean isNight = time > 13000 && time < 23000;
            
            if (isNight) {
                double multiplier = plugin.getConfig().getDouble("spawn-logic.frogs.night-time-swamp-spawn-multiplier", 1.5);
                if (random.nextDouble() < (multiplier - 1.0) * 0.2) {
                    spawnAdditionalFrogs(loc, 1);
                }
            }
        }
    }
    
    private void handleSnifferSpawn(Sniffer sniffer, Location loc) {
        if (!plugin.getConfig().getBoolean("spawn-logic.sniffer.enabled", true)) return;
        // Sniffer spawns are handled separately in the scheduled task
    }
    
    // Rare sniffer spawner
    private void startRareSnifferSpawner() {
        if (!plugin.getConfig().getBoolean("spawn-logic.sniffer.rare-passive-spawn", true)) {
            return;
        }
        
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : plugin.getServer().getWorlds()) {
                    if (world.getEnvironment() != World.Environment.NORMAL) continue;
                    
                    // Check random loaded chunks for sniffer spawns
                    for (Player player : world.getPlayers()) {
                        if (random.nextDouble() < 0.01) { // Check 1% of players per tick
                            checkAndSpawnRareSniffer(world, player.getLocation());
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 200); // Check every 10 seconds
    }
    
    private void checkAndSpawnRareSniffer(World world, Location center) {
        double chance = plugin.getConfig().getDouble("spawn-logic.sniffer.warm-biome-spawn-chance", 0.001);
        if (random.nextDouble() >= chance) return;
        
        // Find a warm biome location
        for (int attempt = 0; attempt < 5; attempt++) {
            int x = center.getBlockX() + (random.nextInt(1000) - 500);
            int z = center.getBlockZ() + (random.nextInt(1000) - 500);
            int y = world.getHighestBlockYAt(x, z);
            
            Location testLoc = new Location(world, x, y, z);
            if (WorldUtils.isWarmBiome(testLoc)) {
                Location spawnLoc = WorldUtils.findSafeSpawnLocation(testLoc, 8, EntityType.SNIFFER);
                if (spawnLoc != null) {
                    world.spawnEntity(spawnLoc, EntityType.SNIFFER, CreatureSpawnEvent.SpawnReason.CUSTOM);
                    plugin.getLogger().info("Spawned rare sniffer at " + spawnLoc.getBlockX() + ", " + spawnLoc.getBlockZ());
                    return;
                }
            }
        }
    }
    
    // Helper methods to spawn additional mobs
    private void spawnAdditionalDolphins(Location loc, int count) {
        for (int i = 0; i < count; i++) {
            Location spawnLoc = WorldUtils.findSafeSpawnLocation(loc, 16, EntityType.DOLPHIN);
            if (spawnLoc != null) {
                spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.DOLPHIN, CreatureSpawnEvent.SpawnReason.CUSTOM);
            }
        }
    }
    
    private void spawnAdditionalDrowned(Location loc, int count) {
        for (int i = 0; i < count; i++) {
            Location spawnLoc = WorldUtils.findSafeSpawnLocation(loc, 16, EntityType.DROWNED);
            if (spawnLoc != null) {
                spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.DROWNED, CreatureSpawnEvent.SpawnReason.CUSTOM);
            }
        }
    }
    
    private void spawnAdditionalPhantoms(Location loc, int count) {
        for (int i = 0; i < count; i++) {
            Location spawnLoc = WorldUtils.findSafeSpawnLocation(loc, 24, EntityType.PHANTOM);
            if (spawnLoc != null) {
                spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.PHANTOM, CreatureSpawnEvent.SpawnReason.CUSTOM);
            }
        }
    }
    
    private void spawnAdditionalStriders(Location loc, int count) {
        for (int i = 0; i < count; i++) {
            Location spawnLoc = WorldUtils.findSafeSpawnLocation(loc, 16, EntityType.STRIDER);
            if (spawnLoc != null && spawnLoc.getBlock().getType() == Material.LAVA) {
                spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.STRIDER, CreatureSpawnEvent.SpawnReason.CUSTOM);
            }
        }
    }
    
    private void spawnAdditionalFrogs(Location loc, int count) {
        for (int i = 0; i < count; i++) {
            Location spawnLoc = WorldUtils.findSafeSpawnLocation(loc, 16, EntityType.FROG);
            if (spawnLoc != null) {
                spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.FROG, CreatureSpawnEvent.SpawnReason.CUSTOM);
            }
        }
    }
    
    // Track player sleep
    private void startSleepTracker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    
                    // Check if player has phantom spawning conditions (insomnia)
                    int phantomTime = player.getStatistic(org.bukkit.Statistic.TIME_SINCE_REST);
                    playerInsomniaTicks.put(uuid, phantomTime);
                    
                    // Track sleep
                    if (player.isSleeping()) {
                        playerLastSleepTime.put(uuid, System.currentTimeMillis());
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 100); // Check every 5 seconds
    }
    
    // Patrol spawn timer
    private void startPatrolTimer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : plugin.getServer().getWorlds()) {
                    if (world.getEnvironment() != World.Environment.NORMAL) continue;
                    
                    long now = System.currentTimeMillis();
                    long lastSpawn = lastPatrolSpawn.getOrDefault(world, 0L);
                    long interval = plugin.getConfig().getInt("spawn-logic.pillagers.patrol-timer-minutes", 5) * 60000L;
                    long variance = plugin.getConfig().getInt("spawn-logic.pillagers.patrol-timer-variance-minutes", 2) * 60000L;
                    
                    if (now - lastSpawn >= interval + (random.nextLong() % (variance * 2)) - variance) {
                        spawnPillagerPatrol(world);
                        lastPatrolSpawn.put(world, now);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1200); // Check every minute
    }
    
    private void spawnPillagerPatrol(World world) {
        // Find a random player location
        List<Player> players = world.getPlayers();
        if (players.isEmpty()) return;
        
        Player player = players.get(random.nextInt(players.size()));
        Location playerLoc = player.getLocation();
        
        // Find spawn location near village or random area
        Location spawnLoc = WorldUtils.findSafeSpawnLocation(playerLoc, 64, EntityType.PILLAGER);
        if (spawnLoc != null) {
            int count = 3 + random.nextInt(3); // 3-5 pillagers
            for (int i = 0; i < count; i++) {
                Pillager pillager = (Pillager) spawnLoc.getWorld().spawnEntity(
                    WorldUtils.findSafeSpawnLocation(spawnLoc, 8, EntityType.PILLAGER),
                    EntityType.PILLAGER,
                    CreatureSpawnEvent.SpawnReason.CUSTOM
                );
            }
        }
    }
    
    public void cleanup() {
        playerLastSleepTime.clear();
        playerInsomniaTicks.clear();
        lastPatrolSpawn.clear();
    }
}

