package com.playpandora.morecaves.handlers;

import com.playpandora.morecaves.MoreCaves;
import com.playpandora.morecaves.utils.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class EventHandler implements Listener {
    private final MoreCaves plugin;
    private final ConfigManager config;
    private final EventManager eventManager;
    private final Random random = new Random();
    
    public EventHandler(MoreCaves plugin) {
        this.plugin = plugin;
        this.config = new ConfigManager(plugin);
        this.eventManager = new EventManager(plugin);
        
        startEventSystems();
    }
    
    private void startEventSystems() {
        if (!config.isFeatureEnabled("events")) return;
        
        if (config.isFeatureEnabled("events.swarm-room")) {
            startSwarmRoomSystem();
        }
        
        if (config.isFeatureEnabled("events.creeper-trap")) {
            startCreeperTrapSystem();
        }
        
        if (config.isFeatureEnabled("events.bat-flock")) {
            startBatFlockSystem();
        }
    }
    
    private void startSwarmRoomSystem() {
        double chance = config.getConfig().getDouble("features.events.swarm-room.trigger-chance", 0.05);
        int interval = config.getConfig().getInt("features.events.swarm-room.check-interval-seconds", 200);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getServer().getWorlds().forEach(world -> {
                    if (world.getEnvironment() != org.bukkit.World.Environment.NORMAL) return;
                    
                    world.getPlayers().forEach(player -> {
                        if (CaveChecker.isInCave(player.getLocation()) && random.nextDouble() < chance) {
                            triggerSwarmRoom(player.getLocation());
                        }
                    });
                });
            }
        }.runTaskTimer(plugin, 0, interval * 20L);
    }
    
    private void triggerSwarmRoom(Location center) {
        String eventId = "swarm_room_" + System.currentTimeMillis();
        int duration = config.getConfig().getInt("features.events.swarm-room.duration-seconds", 30);
        
        // Loud ambient cue
        if (config.getConfig().getBoolean("features.events.swarm-room.loud-ambient-cue", true)) {
            SoundManager.playSoundAtLocation(center, Sound.ENTITY_ZOMBIE_AMBIENT,
                org.bukkit.SoundCategory.HOSTILE, 2.0f, 0.8f);
        }
        
        eventManager.startEvent(eventId, EventManager.EventType.SWARM_ROOM, center, duration);
        
        // Notify nearby players
        center.getWorld().getPlayers().forEach(player -> {
            if (player.getLocation().distance(center) <= 50) {
                plugin.getIntegrationManager().sendEventNotification(player, "Swarm Room Event!");
            }
        });
        
        // Spawn swarm
        int zombieCount = config.getConfig().getInt("features.events.swarm-room.zombie-count", 15);
        int skeletonCount = config.getConfig().getInt("features.events.swarm-room.skeleton-count", 12);
        
        for (int i = 0; i < zombieCount; i++) {
            Location spawnLoc = findSafeSpawn(center, 10);
            if (spawnLoc != null) {
                center.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
            }
        }
        
        for (int i = 0; i < skeletonCount; i++) {
            Location spawnLoc = findSafeSpawn(center, 10);
            if (spawnLoc != null) {
                center.getWorld().spawnEntity(spawnLoc, EntityType.SKELETON);
            }
        }
    }
    
    private Location findSafeSpawn(Location center, int radius) {
        for (int i = 0; i < 20; i++) {
            Location test = center.clone().add(
                (random.nextDouble() - 0.5) * radius * 2,
                0,
                (random.nextDouble() - 0.5) * radius * 2
            );
            
            // Find ground
            while (test.getBlock().getType().isAir() && test.getY() > 0) {
                test.add(0, -1, 0);
            }
            
            if (test.getBlock().getType().isSolid() && 
                test.clone().add(0, 1, 0).getBlock().getType().isAir() &&
                test.clone().add(0, 2, 0).getBlock().getType().isAir()) {
                return test.clone().add(0, 1, 0);
            }
        }
        return null;
    }
    
    @org.bukkit.event.EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        // Silverfish Hive
        if (block.getType() == Material.INFESTED_STONE ||
            block.getType() == Material.INFESTED_COBBLESTONE ||
            block.getType() == Material.INFESTED_STONE_BRICKS ||
            block.getType() == Material.INFESTED_MOSSY_STONE_BRICKS ||
            block.getType() == Material.INFESTED_CRACKED_STONE_BRICKS ||
            block.getType() == Material.INFESTED_CHISELED_STONE_BRICKS) {
            
            if (!config.isFeatureEnabled("events.silverfish-hive")) return;
            if (!config.getConfig().getBoolean("features.events.silverfish-hive.trigger-on-break", true)) {
                return;
            }
            
            triggerSilverfishHive(event.getBlock().getLocation());
        }
    }
    
    private void triggerSilverfishHive(Location center) {
        int count = config.getConfig().getInt("features.events.silverfish-hive.infested-blocks", 10);
        
        // Spawn massive release
        for (int i = 0; i < count * 2; i++) {
            Location spawnLoc = center.clone().add(
                (random.nextDouble() - 0.5) * 3,
                0,
                (random.nextDouble() - 0.5) * 3
            );
            center.getWorld().spawnEntity(spawnLoc, EntityType.SILVERFISH);
        }
    }
    
    private void startCreeperTrapSystem() {
        // Creeper traps are triggered when player enters dead-end areas
        // This would require more complex detection, simplified for now
    }
    
    private void startBatFlockSystem() {
        double chance = config.getConfig().getDouble("features.events.bat-flock.trigger-chance", 0.03);
        int interval = config.getConfig().getInt("features.events.bat-flock.check-interval-seconds", 150);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getServer().getWorlds().forEach(world -> {
                    if (world.getEnvironment() != org.bukkit.World.Environment.NORMAL) return;
                    
                    world.getPlayers().forEach(player -> {
                        if (CaveChecker.isInCave(player.getLocation()) && random.nextDouble() < chance) {
                            triggerBatFlock(player.getLocation());
                        }
                    });
                });
            }
        }.runTaskTimer(plugin, 0, interval * 20L);
    }
    
    private void triggerBatFlock(Location center) {
        String eventId = "bat_flock_" + System.currentTimeMillis();
        int duration = config.getConfig().getInt("features.events.bat-flock.duration-seconds", 10);
        
        // Notify nearby players
        center.getWorld().getPlayers().forEach(player -> {
            if (player.getLocation().distance(center) <= 50) {
                plugin.getIntegrationManager().sendEventNotification(player, "Bat Flock Event!");
            }
        });
        int batCount = config.getConfig().getInt("features.events.bat-flock.bat-count", 20);
        
        eventManager.startEvent(eventId, EventManager.EventType.BAT_FLOCK, center, duration);
        
        // Spawn bat flock
        for (int i = 0; i < batCount; i++) {
            Location spawnLoc = center.clone().add(
                (random.nextDouble() - 0.5) * 10,
                random.nextDouble() * 5,
                (random.nextDouble() - 0.5) * 10
            );
            center.getWorld().spawnEntity(spawnLoc, EntityType.BAT);
        }
    }
    
    @org.bukkit.event.EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!CaveChecker.isInCave(event.getEntity().getLocation())) return;
        
        EventManager.ActiveEvent activeEvent = eventManager.getEventAt(event.getEntity().getLocation());
        if (activeEvent == null) return;
        
        double dropMultiplier = 1.0;
        
        if (activeEvent.getType() == EventManager.EventType.SWARM_ROOM) {
            dropMultiplier = config.getConfig().getDouble(
                "features.events.swarm-room.drop-multiplier", 1.8);
        }
        
        if (dropMultiplier > 1.0) {
            final double finalMultiplier = dropMultiplier;
            event.getDrops().forEach(drop -> {
                int bonus = (int)(drop.getAmount() * (finalMultiplier - 1.0));
                if (bonus > 0) {
                    drop.setAmount(drop.getAmount() + bonus);
                }
            });
        }
    }
    
    public EventManager getEventManager() {
        return eventManager;
    }
}

