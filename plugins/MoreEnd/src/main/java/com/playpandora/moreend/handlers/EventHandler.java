package com.playpandora.moreend.handlers;

import com.playpandora.moreend.MoreEnd;
import com.playpandora.moreend.utils.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class EventHandler implements Listener {
    private final MoreEnd plugin;
    private final ConfigManager config;
    private final EventManager eventManager;
    private final Map<UUID, Integer> chorusBreakCount = new HashMap<>();
    private final Random random = new Random();
    
    public EventHandler(MoreEnd plugin) {
        this.plugin = plugin;
        this.config = new ConfigManager(plugin);
        this.eventManager = new EventManager(plugin);
        
        startEventSystems();
    }
    
    private void startEventSystems() {
        if (!config.isFeatureEnabled("events")) return;
        
        if (config.isFeatureEnabled("events.end-storm")) {
            startEndStormSystem();
        }
        
        if (config.isFeatureEnabled("events.shulker-infestation")) {
            startShulkerInfestationSystem();
        }
        
        if (config.isFeatureEnabled("events.void-geysers")) {
            startVoidGeyserSystem();
        }
    }
    
    private void startEndStormSystem() {
        double chance = config.getConfig().getDouble("features.events.end-storm.trigger-chance", 0.03);
        int interval = config.getConfig().getInt("features.events.end-storm.check-interval-seconds", 400);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getServer().getWorlds().forEach(world -> {
                    if (world.getEnvironment() != org.bukkit.World.Environment.THE_END) return;
                    
                    world.getPlayers().forEach(player -> {
                        if (random.nextDouble() < chance) {
                            triggerEndStorm(player.getLocation());
                        }
                    });
                });
            }
        }.runTaskTimer(plugin, 0, interval * 20L);
    }
    
    private void triggerEndStorm(Location center) {
        String eventId = "end_storm_" + System.currentTimeMillis();
        int duration = config.getConfig().getInt("features.events.end-storm.duration-seconds", 120);
        
        eventManager.startEvent(eventId, EventManager.EventType.END_STORM, center, duration);
        
        // Notify nearby players
        center.getWorld().getPlayers().forEach(player -> {
            if (player.getLocation().distance(center) <= 50) {
                plugin.getIntegrationManager().sendEventNotification(player, "End Storm Event!");
            }
        });
        
        // Chorus pop randomly
        EventManager.ActiveEvent event = eventManager.getEventAt(center);
        if (event != null) {
            BukkitRunnable chorusTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!event.isActive()) {
                        cancel();
                        return;
                    }
                    
                    double popChance = config.getConfig().getDouble(
                        "features.events.end-storm.chorus-pop-chance", 0.1);
                    
                    if (random.nextDouble() < popChance) {
                        Location popLoc = center.clone().add(
                            (random.nextDouble() - 0.5) * 50,
                            0,
                            (random.nextDouble() - 0.5) * 50
                        );
                        
                        if (popLoc.getBlock().getType() == Material.CHORUS_FLOWER) {
                            popLoc.getBlock().breakNaturally();
                            SoundManager.playSoundAtLocation(popLoc, Sound.BLOCK_CHORUS_FLOWER_DEATH,
                                org.bukkit.SoundCategory.BLOCKS, 1.0f, 1.0f);
                        }
                    }
                }
            };
            chorusTask.runTaskTimer(plugin, 0, 20);
            event.addTask(chorusTask);
        }
        
        // Phantom waves
        if (config.getConfig().getBoolean("features.events.end-storm.phantom-waves", true)) {
            spawnPhantomWaves(center, event);
        }
    }
    
    private void spawnPhantomWaves(Location center, EventManager.ActiveEvent event) {
        BukkitRunnable waveTask = new BukkitRunnable() {
            int wave = 0;
            @Override
            public void run() {
                if (!event.isActive() || wave >= 5) {
                    cancel();
                    return;
                }
                
                for (int i = 0; i < 5; i++) {
                    Location spawnLoc = center.clone().add(
                        (random.nextDouble() - 0.5) * 30,
                        20 + random.nextDouble() * 10,
                        (random.nextDouble() - 0.5) * 30
                    );
                    center.getWorld().spawnEntity(spawnLoc, EntityType.PHANTOM);
                }
                
                wave++;
            }
        };
        waveTask.runTaskTimer(plugin, 0, 40);
        event.addTask(waveTask);
    }
    
    @org.bukkit.event.EventHandler
    public void onChorusBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.CHORUS_FLOWER &&
            event.getBlock().getType() != Material.CHORUS_PLANT) {
            return;
        }
        
        if (!DimensionChecker.isEnd(event.getBlock().getLocation())) return;
        if (!config.isFeatureEnabled("events.enderman-tide")) return;
        if (!config.getConfig().getBoolean("features.events.enderman-tide.trigger-on-chorus-break", true)) {
            return;
        }
        
        UUID playerId = event.getPlayer().getUniqueId();
        int count = chorusBreakCount.getOrDefault(playerId, 0) + 1;
        chorusBreakCount.put(playerId, count);
        
        int threshold = config.getConfig().getInt("features.events.enderman-tide.break-threshold", 10);
        
        if (count >= threshold) {
            triggerEndermanTide(event.getPlayer().getLocation());
            chorusBreakCount.put(playerId, 0);
        }
    }
    
    private void triggerEndermanTide(Location center) {
        int spawnCount = config.getConfig().getInt("features.events.enderman-tide.spawn-count", 25);
        boolean neutral = config.getConfig().getBoolean(
            "features.events.enderman-tide.neutral-until-provoked", true);
        
        for (int i = 0; i < spawnCount; i++) {
            Location spawnLoc = center.clone().add(
                (random.nextDouble() - 0.5) * 20,
                0,
                (random.nextDouble() - 0.5) * 20
            );
            
            Enderman enderman = (Enderman) center.getWorld().spawnEntity(spawnLoc, EntityType.ENDERMAN);
            // Endermen are naturally neutral, so this should work
        }
    }
    
    private void startShulkerInfestationSystem() {
        double chance = config.getConfig().getDouble("features.events.shulker-infestation.trigger-chance", 0.02);
        int interval = config.getConfig().getInt("features.events.shulker-infestation.check-interval-seconds", 600);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getServer().getWorlds().forEach(world -> {
                    if (world.getEnvironment() != org.bukkit.World.Environment.THE_END) return;
                    
                    world.getPlayers().forEach(player -> {
                        Location loc = player.getLocation();
                        // Check if in End City (rough check - Y level)
                        if (loc.getY() > 50 && random.nextDouble() < chance) {
                            triggerShulkerInfestation(player.getLocation());
                        }
                    });
                });
            }
        }.runTaskTimer(plugin, 0, interval * 20L);
    }
    
    private void triggerShulkerInfestation(Location center) {
        String eventId = "shulker_infestation_" + System.currentTimeMillis();
        int duration = config.getConfig().getInt("features.events.shulker-infestation.duration-seconds", 180);
        
        eventManager.startEvent(eventId, EventManager.EventType.SHULKER_INFESTATION, center, duration);
        
        // Notify nearby players
        center.getWorld().getPlayers().forEach(player -> {
            if (player.getLocation().distance(center) <= 50) {
                plugin.getIntegrationManager().sendEventNotification(player, "Shulker Infestation!");
            }
        });
        
        // Spawn extra Shulkers
        double multiplier = config.getConfig().getDouble("features.events.shulker-infestation.shulker-multiplier", 2.0);
        for (int i = 0; i < (int)(3 * multiplier); i++) {
            Location spawnLoc = center.clone().add(
                (random.nextDouble() - 0.5) * 20,
                random.nextDouble() * 10,
                (random.nextDouble() - 0.5) * 20
            );
            center.getWorld().spawnEntity(spawnLoc, EntityType.SHULKER);
        }
    }
    
    private void startVoidGeyserSystem() {
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getServer().getWorlds().forEach(world -> {
                    if (world.getEnvironment() != org.bukkit.World.Environment.THE_END) return;
                    
                    world.getPlayers().forEach(player -> {
                        Location loc = player.getLocation();
                        double voidY = 0;
                        double distance = Math.abs(loc.getY() - voidY);
                        double geyserDistance = config.getConfig().getDouble(
                            "features.events.void-geysers.distance-from-void", 3.0);
                        
                        if (distance <= geyserDistance) {
                            double strength = config.getConfig().getDouble(
                                "features.events.void-geysers.knockback-strength", 1.5);
                            
                            org.bukkit.util.Vector direction = loc.toVector().subtract(
                                new org.bukkit.util.Vector(loc.getX(), voidY, loc.getZ())).normalize();
                            direction.multiply(strength);
                            direction.setY(0.3);
                            
                            player.setVelocity(player.getVelocity().add(direction));
                        }
                    });
                });
            }
        }.runTaskTimer(plugin, 0, config.getConfig().getInt(
            "features.events.void-geysers.pulse-interval-ticks", 40));
    }
    
    @org.bukkit.event.EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!DimensionChecker.isEnd(event.getEntity().getLocation())) return;
        
        EventManager.ActiveEvent activeEvent = eventManager.getEventAt(event.getEntity().getLocation());
        if (activeEvent == null) return;
        
        double dropMultiplier = 1.0;
        
        switch (activeEvent.getType()) {
            case SHULKER_INFESTATION:
                if (event.getEntityType() == EntityType.SHULKER) {
                    dropMultiplier = config.getConfig().getDouble(
                        "features.events.shulker-infestation.shell-drop-multiplier", 2.0);
                }
                break;
            case END_STORM:
                dropMultiplier = config.getConfig().getDouble(
                    "features.events.end-storm.drop-multiplier", 1.5);
                break;
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

