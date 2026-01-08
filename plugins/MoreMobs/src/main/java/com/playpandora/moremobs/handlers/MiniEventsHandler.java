package com.playpandora.moremobs.handlers;

import com.playpandora.moremobs.MoreMobs;
import com.playpandora.moremobs.util.WorldUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class MiniEventsHandler implements Listener {
    
    private final MoreMobs plugin;
    private final Random random = new Random();
    private final Map<World, Long> lastMiniRaidCheck = new HashMap<>();
    private final Map<World, Long> lastStriderHotspotCheck = new HashMap<>();
    private final Map<World, Long> lastAxolotlCaveCheck = new HashMap<>();
    private final Set<Location> activeSnifferNests = new HashSet<>();
    
    public MiniEventsHandler(MoreMobs plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        startMiniRaidsTimer();
        startStriderHotspotsTimer();
        startAxolotlCavesTimer();
        startSnifferNestManager();
    }
    
    // ========== Pillager Mini-Raids ==========
    
    private void startMiniRaidsTimer() {
        if (!plugin.getConfig().getBoolean("mini-events.pillager-mini-raids.enabled", true)) {
            return;
        }
        
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : plugin.getServer().getWorlds()) {
                    if (world.getEnvironment() != World.Environment.NORMAL) continue;
                    
                    long now = System.currentTimeMillis();
                    long lastCheck = lastMiniRaidCheck.getOrDefault(world, 0L);
                    long interval = plugin.getConfig().getInt("mini-events.pillager-mini-raids.check-interval-seconds", 300) * 1000L;
                    
                    if (now - lastCheck >= interval) {
                        checkAndSpawnMiniRaid(world);
                        lastMiniRaidCheck.put(world, now);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1200); // Check every minute
    }
    
    private void checkAndSpawnMiniRaid(World world) {
        double spawnChance = plugin.getConfig().getDouble("mini-events.pillager-mini-raids.spawn-chance", 0.15);
        if (random.nextDouble() >= spawnChance) return;
        
        // Find players and check for villages
        for (Player player : world.getPlayers()) {
            Location playerLoc = player.getLocation();
            
            // Check if near a village
            if (WorldUtils.isNearStructure(playerLoc, WorldUtils.StructureType.VILLAGE, 128)) {
                spawnMiniRaid(world, playerLoc);
                return; // Only spawn one per check
            }
        }
    }
    
    private void spawnMiniRaid(World world, Location nearLocation) {
        int minPillagers = plugin.getConfig().getInt("mini-events.pillager-mini-raids.min-pillagers", 3);
        int maxPillagers = plugin.getConfig().getInt("mini-events.pillager-mini-raids.max-pillagers", 5);
        int minDistance = plugin.getConfig().getInt("mini-events.pillager-mini-raids.min-distance-from-village", 32);
        int maxDistance = plugin.getConfig().getInt("mini-events.pillager-mini-raids.max-distance-from-village", 128);
        
        // Find spawn location at appropriate distance
        int distance = minDistance + random.nextInt(maxDistance - minDistance);
        double angle = random.nextDouble() * 2 * Math.PI;
        
        int x = (int) (nearLocation.getX() + Math.cos(angle) * distance);
        int z = (int) (nearLocation.getZ() + Math.sin(angle) * distance);
        int y = world.getHighestBlockYAt(x, z);
        
        Location spawnLoc = new Location(world, x + 0.5, y + 1, z + 0.5);
        spawnLoc = WorldUtils.findSafeSpawnLocation(spawnLoc, 8, EntityType.PILLAGER);
        
        if (spawnLoc == null) return;
        
        // Spawn pillagers
        int count = minPillagers + random.nextInt(maxPillagers - minPillagers + 1);
        for (int i = 0; i < count; i++) {
            Location individualSpawn = WorldUtils.findSafeSpawnLocation(spawnLoc, 8, EntityType.PILLAGER);
            if (individualSpawn != null) {
                world.spawnEntity(individualSpawn, EntityType.PILLAGER, CreatureSpawnEvent.SpawnReason.CUSTOM);
            }
        }
        
        plugin.getLogger().info("Spawned mini pillager raid with " + count + " pillagers near " + spawnLoc.getBlockX() + ", " + spawnLoc.getBlockZ());
    }
    
    // ========== Strider Hotspots ==========
    
    private void startStriderHotspotsTimer() {
        if (!plugin.getConfig().getBoolean("mini-events.strider-hotspots.enabled", true)) {
            return;
        }
        
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : plugin.getServer().getWorlds()) {
                    if (world.getEnvironment() != World.Environment.NETHER) continue;
                    
                    long now = System.currentTimeMillis();
                    long lastCheck = lastStriderHotspotCheck.getOrDefault(world, 0L);
                    long interval = plugin.getConfig().getInt("mini-events.strider-hotspots.check-interval-seconds", 600) * 1000L;
                    
                    if (now - lastCheck >= interval) {
                        checkAndSpawnStriderHotspot(world);
                        lastStriderHotspotCheck.put(world, now);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1200); // Check every minute
    }
    
    private void checkAndSpawnStriderHotspot(World world) {
        double spawnChance = plugin.getConfig().getDouble("mini-events.strider-hotspots.spawn-chance", 0.1);
        if (random.nextDouble() >= spawnChance) return;
        
        // Find players in Nether
        List<Player> players = world.getPlayers();
        if (players.isEmpty()) return;
        
        Player player = players.get(random.nextInt(players.size()));
        Location playerLoc = player.getLocation();
        
        // Find large lava lake
        Location lavaLake = findLargeLavaLake(playerLoc, world);
        if (lavaLake == null) return;
        
        spawnStriderHotspot(world, lavaLake);
    }
    
    private Location findLargeLavaLake(Location center, World world) {
        int minLavaBlocks = plugin.getConfig().getInt("mini-events.strider-hotspots.min-lava-blocks", 100);
        
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = center.getBlockX() + (random.nextInt(200) - 100);
            int z = center.getBlockZ() + (random.nextInt(200) - 100);
            
            // Find lava level
            int y = findLavaLevel(world, x, z);
            if (y == -1) continue;
            
            Location testLoc = new Location(world, x, y, z);
            int lavaCount = WorldUtils.countBlocksInArea(testLoc, Material.LAVA, 16, 8);
            
            if (lavaCount >= minLavaBlocks) {
                return testLoc;
            }
        }
        
        return null;
    }
    
    private int findLavaLevel(World world, int x, int z) {
        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType() == Material.LAVA) {
                return y;
            }
        }
        return -1;
    }
    
    private void spawnStriderHotspot(World world, Location center) {
        int minStriders = plugin.getConfig().getInt("mini-events.strider-hotspots.min-striders", 4);
        int maxStriders = plugin.getConfig().getInt("mini-events.strider-hotspots.max-striders", 6);
        int count = minStriders + random.nextInt(maxStriders - minStriders + 1);
        
        int spawned = 0;
        for (int i = 0; i < count * 3; i++) { // Try multiple times
            int x = center.getBlockX() + (random.nextInt(32) - 16);
            int z = center.getBlockZ() + (random.nextInt(32) - 16);
            int y = findLavaLevel(world, x, z);
            
            if (y == -1) continue;
            
            Location spawnLoc = new Location(world, x + 0.5, y + 1, z + 0.5);
            Block block = spawnLoc.getBlock();
            
            if (block.getType() == Material.LAVA || block.getRelative(0, -1, 0).getType() == Material.LAVA) {
                world.spawnEntity(spawnLoc, EntityType.STRIDER, CreatureSpawnEvent.SpawnReason.CUSTOM);
                spawned++;
                if (spawned >= count) break;
            }
        }
        
        plugin.getLogger().info("Spawned strider hotspot with " + spawned + " striders at " + center.getBlockX() + ", " + center.getBlockZ());
    }
    
    // ========== Axolotl Caves ==========
    
    private void startAxolotlCavesTimer() {
        if (!plugin.getConfig().getBoolean("mini-events.axolotl-caves.enabled", true)) {
            return;
        }
        
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : plugin.getServer().getWorlds()) {
                    if (world.getEnvironment() != World.Environment.NORMAL) continue;
                    
                    long now = System.currentTimeMillis();
                    long lastCheck = lastAxolotlCaveCheck.getOrDefault(world, 0L);
                    long interval = plugin.getConfig().getInt("mini-events.axolotl-caves.check-interval-seconds", 900) * 1000L;
                    
                    if (now - lastCheck >= interval) {
                        checkAndSpawnAxolotlCave(world);
                        lastAxolotlCaveCheck.put(world, now);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1200); // Check every minute
    }
    
    private void checkAndSpawnAxolotlCave(World world) {
        double spawnChance = plugin.getConfig().getDouble("mini-events.axolotl-caves.spawn-chance", 0.05);
        if (random.nextDouble() >= spawnChance) return;
        
        // Find players
        List<Player> players = world.getPlayers();
        if (players.isEmpty()) return;
        
        Player player = players.get(random.nextInt(players.size()));
        Location playerLoc = player.getLocation();
        
        // Find underground water pocket
        Location waterPocket = findWaterPocket(playerLoc, world);
        if (waterPocket == null) return;
        
        spawnAxolotlCave(world, waterPocket);
    }
    
    private Location findWaterPocket(Location center, World world) {
        int minWaterBlocks = plugin.getConfig().getInt("mini-events.axolotl-caves.min-water-blocks", 50);
        int maxY = plugin.getConfig().getInt("mini-events.axolotl-caves.max-y-level", 50);
        
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = center.getBlockX() + (random.nextInt(200) - 100);
            int z = center.getBlockZ() + (random.nextInt(200) - 100);
            
            // Search from surface down to maxY
            for (int y = world.getHighestBlockYAt(x, z) - 10; y >= maxY; y--) {
                Block block = world.getBlockAt(x, y, z);
                if (block.getType() == Material.WATER) {
                    Location testLoc = new Location(world, x, y, z);
                    int waterCount = WorldUtils.countBlocksInArea(testLoc, Material.WATER, 8, 4);
                    
                    if (waterCount >= minWaterBlocks) {
                        // Check if it's a pocket (surrounded by stone)
                        boolean isPocket = true;
                        for (int checkX = x - 4; checkX <= x + 4; checkX += 4) {
                            for (int checkZ = z - 4; checkZ <= z + 4; checkZ += 4) {
                                Block checkBlock = world.getBlockAt(checkX, y, checkZ);
                                if (checkBlock.getType() != Material.WATER && 
                                    checkBlock.getType() != Material.STONE && 
                                    checkBlock.getType() != Material.DEEPSLATE) {
                                    isPocket = false;
                                    break;
                                }
                            }
                        }
                        
                        if (isPocket) {
                            return testLoc;
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    private void spawnAxolotlCave(World world, Location center) {
        int minAxolotls = plugin.getConfig().getInt("mini-events.axolotl-caves.min-axolotls", 3);
        int maxAxolotls = plugin.getConfig().getInt("mini-events.axolotl-caves.max-axolotls", 5);
        int count = minAxolotls + random.nextInt(maxAxolotls - minAxolotls + 1);
        
        int spawned = 0;
        for (int i = 0; i < count * 3; i++) {
            int x = center.getBlockX() + (random.nextInt(16) - 8);
            int z = center.getBlockZ() + (random.nextInt(16) - 8);
            int y = center.getBlockY() + (random.nextInt(8) - 4);
            
            Block block = world.getBlockAt(x, y, z);
            if (block.getType() == Material.WATER) {
                Location spawnLoc = new Location(world, x + 0.5, y + 0.5, z + 0.5);
                world.spawnEntity(spawnLoc, EntityType.AXOLOTL, CreatureSpawnEvent.SpawnReason.CUSTOM);
                spawned++;
                if (spawned >= count) break;
            }
        }
        
        plugin.getLogger().info("Spawned axolotl cave with " + spawned + " axolotls at " + center.getBlockX() + ", " + center.getBlockY() + ", " + center.getBlockZ());
    }
    
    // ========== Sniffer Nests ==========
    
    private void startSnifferNestManager() {
        if (!plugin.getConfig().getBoolean("mini-events.sniffer-nests.enabled", true)) {
            return;
        }
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getConfig().getBoolean("mini-events.sniffer-nests.enabled-near-wild-sniffers", true)) {
                    return;
                }
                
                // Find all sniffers and create nests near them
                for (World world : plugin.getServer().getWorlds()) {
                    if (world.getEnvironment() != World.Environment.NORMAL) continue;
                    
                    for (Sniffer sniffer : world.getEntitiesByClass(Sniffer.class)) {
                        Location snifferLoc = sniffer.getLocation();
                        
                        // Check if already has nest nearby
                        boolean hasNearbyNest = false;
                        for (Location nestLoc : activeSnifferNests) {
                            if (nestLoc.getWorld() == world && nestLoc.distance(snifferLoc) < 16) {
                                hasNearbyNest = true;
                                break;
                            }
                        }
                        
                        if (!hasNearbyNest && random.nextDouble() < 0.1) {
                            createSnifferNest(world, snifferLoc);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 6000); // Check every 5 minutes
    }
    
    private void createSnifferNest(World world, Location nearLocation) {
        int radius = plugin.getConfig().getInt("mini-events.sniffer-nests.moss-patch-radius", 3);
        double flowerChance = plugin.getConfig().getDouble("mini-events.sniffer-nests.flower-spawn-chance", 0.3);
        
        Location nestCenter = WorldUtils.findSafeSpawnLocation(nearLocation, 8, EntityType.SNIFFER);
        if (nestCenter == null) return;
        
        int centerX = nestCenter.getBlockX();
        int centerY = nestCenter.getBlockY();
        int centerZ = nestCenter.getBlockZ();
        
        // Create moss patch with flowers
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                double distance = Math.sqrt((x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ));
                if (distance > radius) continue;
                
                Block block = world.getBlockAt(x, centerY, z);
                Block above = world.getBlockAt(x, centerY + 1, z);
                Block below = world.getBlockAt(x, centerY - 1, z);
                
                // Place moss
                if (below.getType().isSolid() && !block.getType().isSolid()) {
                    block.setType(Material.MOSS_BLOCK);
                }
                
                // Place flowers
                if (above.getType().isAir() && random.nextDouble() < flowerChance) {
                    Material[] flowers = {Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID,
                                         Material.ALLIUM, Material.AZURE_BLUET, Material.ORANGE_TULIP};
                    above.setType(flowers[random.nextInt(flowers.length)]);
                }
            }
        }
        
        activeSnifferNests.add(nestCenter);
        
        // Remove nest after 1 hour
        new BukkitRunnable() {
            @Override
            public void run() {
                activeSnifferNests.remove(nestCenter);
            }
        }.runTaskLater(plugin, 72000); // 1 hour in ticks
        
        plugin.getLogger().info("Created sniffer nest at " + centerX + ", " + centerY + ", " + centerZ);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onSnifferSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.SNIFFER) return;
        if (!plugin.getConfig().getBoolean("mini-events.sniffer-nests.enabled", true)) return;
        
        // Potentially create nest near newly spawned sniffer
        if (random.nextDouble() < 0.2) {
            createSnifferNest(event.getEntity().getWorld(), event.getEntity().getLocation());
        }
    }
    
    public void cleanup() {
        lastMiniRaidCheck.clear();
        lastStriderHotspotCheck.clear();
        lastAxolotlCaveCheck.clear();
        activeSnifferNests.clear();
    }
}

