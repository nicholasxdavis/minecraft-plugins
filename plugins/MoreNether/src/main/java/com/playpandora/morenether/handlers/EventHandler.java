package com.playpandora.morenether.handlers;

import com.playpandora.morenether.MoreNether;
import com.playpandora.morenether.utils.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class EventHandler implements Listener {
    private final MoreNether plugin;
    private final ConfigManager config;
    private final EventManager eventManager;
    private final Random random = new Random();
    
    public EventHandler(MoreNether plugin) {
        this.plugin = plugin;
        this.config = new ConfigManager(plugin);
        this.eventManager = new EventManager(plugin);
        
        startEventSystems();
    }
    
    public EventManager getEventManager() {
        return eventManager;
    }
    
    private void startEventSystems() {
        if (!config.isFeatureEnabled("events")) return;
        
        // Blaze Surge
        if (config.isFeatureEnabled("events.blaze-surge")) {
            startBlazeSurgeSystem();
        }
        
        // Soul Patrol
        if (config.isFeatureEnabled("events.soul-patrol")) {
            startSoulPatrolSystem();
        }
        
        // Gold Rush
        if (config.isFeatureEnabled("events.gold-rush")) {
            startGoldRushSystem();
        }
        
        // Hoglin Hunt
        if (config.isFeatureEnabled("events.hoglin-hunt")) {
            startHoglinHuntSystem();
        }
    }
    
    private void startBlazeSurgeSystem() {
        double chance = config.getConfig().getDouble("features.events.blaze-surge.trigger-chance", 0.05);
        int interval = config.getConfig().getInt("features.events.blaze-surge.check-interval-seconds", 300);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getServer().getWorlds().forEach(world -> {
                    if (world.getEnvironment() != org.bukkit.World.Environment.NETHER) return;
                    
                    world.getPlayers().forEach(player -> {
                        if (random.nextDouble() < chance) {
                            triggerBlazeSurge(player.getLocation());
                        }
                    });
                });
            }
        }.runTaskTimer(plugin, 0, interval * 20L);
    }
    
    private void triggerBlazeSurge(Location center) {
        String eventId = "blaze_surge_" + System.currentTimeMillis();
        int duration = config.getConfig().getInt("features.events.blaze-surge.duration-seconds", 60);
        
        // Audio cue
        if (config.getConfig().getBoolean("features.events.blaze-surge.audio-cue", true)) {
            SoundManager.playSoundAtLocation(center, Sound.ENTITY_BLAZE_AMBIENT,
                org.bukkit.SoundCategory.HOSTILE, 1.0f, 0.8f);
        }
        
        // Start event
        eventManager.startEvent(eventId, EventManager.EventType.BLAZE_SURGE, center, duration);
        
        // Notify nearby players
        center.getWorld().getPlayers().forEach(player -> {
            if (player.getLocation().distance(center) <= 50) {
                plugin.getIntegrationManager().sendEventNotification(player, "Blaze Surge Event!");
            }
        });
        
        // Spawn extra Blazes
        double multiplier = config.getConfig().getDouble("features.events.blaze-surge.spawn-multiplier", 3.0);
        spawnBlazesInArea(center, (int)(5 * multiplier));
    }
    
    private void spawnBlazesInArea(Location center, int count) {
        for (int i = 0; i < count; i++) {
            Location spawnLoc = center.clone().add(
                (random.nextDouble() - 0.5) * 20,
                random.nextDouble() * 10,
                (random.nextDouble() - 0.5) * 20
            );
            
            // Find safe spawn location (in Nether Fortress)
            if (spawnLoc.getBlock().getType() == Material.NETHER_BRICKS ||
                spawnLoc.getBlock().getRelative(0, -1, 0).getType() == Material.NETHER_BRICKS ||
                spawnLoc.getBlock().getType().isAir()) {
                spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.BLAZE);
            }
        }
    }
    
    private void startSoulPatrolSystem() {
        double chance = config.getConfig().getDouble("features.events.soul-patrol.trigger-chance", 0.03);
        int interval = config.getConfig().getInt("features.events.soul-patrol.check-interval-seconds", 400);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getServer().getWorlds().forEach(world -> {
                    if (world.getEnvironment() != org.bukkit.World.Environment.NETHER) return;
                    
                    world.getPlayers().forEach(player -> {
                        String biome = player.getLocation().getBlock().getBiome().toString();
                        if (biome.contains("SOUL_SAND_VALLEY") && random.nextDouble() < chance) {
                            triggerSoulPatrol(player.getLocation());
                        }
                    });
                });
            }
        }.runTaskTimer(plugin, 0, interval * 20L);
    }
    
    private void triggerSoulPatrol(Location center) {
        String eventId = "soul_patrol_" + System.currentTimeMillis();
        int duration = config.getConfig().getInt("features.events.soul-patrol.duration-seconds", 90);
        int patrolSize = config.getConfig().getInt("features.events.soul-patrol.patrol-size", 8);
        
        eventManager.startEvent(eventId, EventManager.EventType.SOUL_PATROL, center, duration);
        
        // Notify nearby players
        center.getWorld().getPlayers().forEach(player -> {
            if (player.getLocation().distance(center) <= 50) {
                plugin.getIntegrationManager().sendEventNotification(player, "Soul Patrol Event!");
            }
        });
        
        // Spawn skeleton patrol
        for (int i = 0; i < patrolSize; i++) {
            Location spawnLoc = center.clone().add(
                (random.nextDouble() - 0.5) * 15,
                0,
                (random.nextDouble() - 0.5) * 15
            );
            center.getWorld().spawnEntity(spawnLoc, EntityType.SKELETON);
        }
    }
    
    private void startGoldRushSystem() {
        double chance = config.getConfig().getDouble("features.events.gold-rush.trigger-chance", 0.02);
        int interval = config.getConfig().getInt("features.events.gold-rush.check-interval-seconds", 600);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getServer().getWorlds().forEach(world -> {
                    if (world.getEnvironment() != org.bukkit.World.Environment.NETHER) return;
                    
                    world.getPlayers().forEach(player -> {
                        Location loc = player.getLocation();
                        if (loc.getY() > 30 && loc.getY() < 120 && random.nextDouble() < chance) {
                            triggerGoldRush(player.getLocation());
                        }
                    });
                });
            }
        }.runTaskTimer(plugin, 0, interval * 20L);
    }
    
    private void triggerGoldRush(Location center) {
        String eventId = "gold_rush_" + System.currentTimeMillis();
        int duration = config.getConfig().getInt("features.events.gold-rush.duration-seconds", 120);
        
        eventManager.startEvent(eventId, EventManager.EventType.GOLD_RUSH, center, duration);
        
        // Notify nearby players
        center.getWorld().getPlayers().forEach(player -> {
            if (player.getLocation().distance(center) <= 50) {
                plugin.getIntegrationManager().sendEventNotification(player, "Gold Rush Event!");
            }
        });
        
        // Spawn Piglins around area
        for (int i = 0; i < 5; i++) {
            Location spawnLoc = center.clone().add(
                (random.nextDouble() - 0.5) * 20,
                0,
                (random.nextDouble() - 0.5) * 20
            );
            center.getWorld().spawnEntity(spawnLoc, EntityType.PIGLIN);
        }
    }
    
    private void startHoglinHuntSystem() {
        double chance = config.getConfig().getDouble("features.events.hoglin-hunt.trigger-chance", 0.04);
        int interval = config.getConfig().getInt("features.events.hoglin-hunt.check-interval-seconds", 350);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getServer().getWorlds().forEach(world -> {
                    if (world.getEnvironment() != org.bukkit.World.Environment.NETHER) return;
                    
                    world.getPlayers().forEach(player -> {
                        String biome = player.getLocation().getBlock().getBiome().toString();
                        if (biome.contains("CRIMSON") && random.nextDouble() < chance) {
                            triggerHoglinHunt(player.getLocation());
                        }
                    });
                });
            }
        }.runTaskTimer(plugin, 0, interval * 20L);
    }
    
    private void triggerHoglinHunt(Location center) {
        String eventId = "hoglin_hunt_" + System.currentTimeMillis();
        int duration = config.getConfig().getInt("features.events.hoglin-hunt.duration-seconds", 60);
        int packSize = config.getConfig().getInt("features.events.hoglin-hunt.pack-size", 12);
        
        eventManager.startEvent(eventId, EventManager.EventType.HOGLIN_HUNT, center, duration);
        
        // Notify nearby players
        center.getWorld().getPlayers().forEach(player -> {
            if (player.getLocation().distance(center) <= 50) {
                plugin.getIntegrationManager().sendEventNotification(player, "Hoglin Hunt Event!");
            }
        });
        
        // Spawn Hoglin pack
        for (int i = 0; i < packSize; i++) {
            Location spawnLoc = center.clone().add(
                (random.nextDouble() - 0.5) * 15,
                0,
                (random.nextDouble() - 0.5) * 15
            );
            center.getWorld().spawnEntity(spawnLoc, EntityType.HOGLIN);
        }
    }
    
    @org.bukkit.event.EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!DimensionChecker.isNether(event.getLocation())) return;
        
        // Check if in active event
        EventManager.ActiveEvent activeEvent = eventManager.getEventAt(event.getLocation());
        if (activeEvent == null) return;
        
        switch (activeEvent.getType()) {
            case BLAZE_SURGE:
                if (event.getEntityType() == EntityType.BLAZE) {
                    // Allow extra spawns
                }
                break;
            case SOUL_PATROL:
                if (event.getEntityType() == EntityType.SKELETON) {
                    // Allow patrol spawns
                }
                break;
        }
    }
    
    @org.bukkit.event.EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!DimensionChecker.isNether(event.getEntity().getLocation())) return;
        
        EventManager.ActiveEvent activeEvent = eventManager.getEventAt(event.getEntity().getLocation());
        if (activeEvent == null) return;
        
        double dropMultiplier = 1.0;
        
        switch (activeEvent.getType()) {
            case BLAZE_SURGE:
                if (event.getEntityType() == EntityType.BLAZE) {
                    dropMultiplier = config.getConfig().getDouble(
                        "features.events.blaze-surge.rod-drop-multiplier", 1.5);
                }
                break;
            case SOUL_PATROL:
                if (event.getEntityType() == EntityType.SKELETON) {
                    dropMultiplier = config.getConfig().getDouble(
                        "features.events.soul-patrol.bone-drop-multiplier", 2.0);
                }
                break;
            case GOLD_RUSH:
                if (event.getEntity() instanceof Piglin || event.getEntity() instanceof PiglinBrute) {
                    dropMultiplier = config.getConfig().getDouble(
                        "features.events.gold-rush.gold-drop-multiplier", 3.0);
                }
                break;
            case HOGLIN_HUNT:
                if (event.getEntityType() == EntityType.HOGLIN) {
                    dropMultiplier = config.getConfig().getDouble(
                        "features.events.hoglin-hunt.drop-multiplier", 1.8);
                }
                break;
        }
        
        // Apply drop multiplier
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
}

