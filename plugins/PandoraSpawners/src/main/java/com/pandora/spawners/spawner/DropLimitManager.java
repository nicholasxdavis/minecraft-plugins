package com.pandora.spawners.spawner;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.Wither;
import org.bukkit.entity.Evoker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import com.pandora.spawners.PandoraSpawners;
import com.pandora.spawners.api.spawner.IGenerator;
import com.pandora.spawners.api.spawner.ISpawner;
import com.pandora.spawners.configuration.location.LocationRegistry;
import com.pandora.spawners.spawner.generator.GeneratorRegistry;
import com.pandora.spawners.spawner.type.SpawnerType;
import com.pandora.spawners.utility.DataManager;

/**
 * Manages drop limits for spawners per 24 hours
 * - Shulker Shells: 40 per spawner per 24h
 * - Nether Stars: 5 per spawner per 24h (from Wither)
 * - Totems: 1 per spawner per 24h (from Evoker)
 */
public class DropLimitManager implements Listener {
    
    private static final long DAY_MS = 24 * 60 * 60 * 1000L; // 24 hours in milliseconds
    
    // Drop limits per spawner per 24 hours
    private static final int SHULKER_SHELL_LIMIT = 40;
    private static final int NETHER_STAR_LIMIT = 5;
    private static final int TOTEM_LIMIT = 1;
    
    // Track drops per spawner location
    public static final Map<String, SpawnerDropTracker> dropTrackers = new HashMap<>();
    
    public static void initialize() {
        PandoraSpawners.instance().getServer().getPluginManager()
            .registerEvents(new DropLimitManager(), PandoraSpawners.instance());
        
        // Cleanup old trackers every hour
        PandoraSpawners.scheduler().runTimer(task -> {
            cleanupOldTrackers();
        }, 20 * 60 * 60, 20 * 60 * 60); // Every hour
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        
        // Check if entity was spawned by a spawner
        if (!DataManager.isSpawned(entity)) {
            return;
        }
        
        // Find the spawner that spawned this entity
        ISpawner spawner = findSpawnerForEntity(entity);
        if (spawner == null) {
            return;
        }
        
        Location spawnerLoc = spawner.block().getLocation();
        String spawnerKey = getSpawnerKey(spawnerLoc);
        SpawnerDropTracker tracker = dropTrackers.computeIfAbsent(spawnerKey, 
            k -> new SpawnerDropTracker());
        
        // Clean old data if needed
        tracker.cleanOldData();
        
        // Handle Shulker Shells
        if (entity instanceof Shulker) {
            int currentCount = tracker.getShulkerShells();
            int shellsToDrop = (int) event.getDrops().stream()
                .filter(item -> item.getType() == Material.SHULKER_SHELL)
                .mapToInt(ItemStack::getAmount)
                .sum();
            
            if (shellsToDrop > 0) {
                int allowed = Math.min(shellsToDrop, SHULKER_SHELL_LIMIT - currentCount);
                if (allowed <= 0) {
                    // Remove all shells
                    event.getDrops().removeIf(item -> item.getType() == Material.SHULKER_SHELL);
                } else if (allowed < shellsToDrop) {
                    // Remove excess shells
                    int toRemove = shellsToDrop - allowed;
                    java.util.Iterator<ItemStack> it = event.getDrops().iterator();
                    while (it.hasNext() && toRemove > 0) {
                        ItemStack item = it.next();
                        if (item.getType() == Material.SHULKER_SHELL) {
                            if (toRemove >= item.getAmount()) {
                                toRemove -= item.getAmount();
                                it.remove();
                            } else {
                                item.setAmount(item.getAmount() - toRemove);
                                toRemove = 0;
                            }
                        }
                    }
                    tracker.addShulkerShells(allowed);
                } else {
                    tracker.addShulkerShells(allowed);
                }
            }
        }
        
        // Handle Nether Stars (from Wither)
        if (entity instanceof Wither) {
            int currentCount = tracker.getNetherStars();
            int starsToDrop = (int) event.getDrops().stream()
                .filter(item -> item.getType() == Material.NETHER_STAR)
                .mapToInt(ItemStack::getAmount)
                .sum();
            
            if (starsToDrop > 0) {
                int allowed = Math.min(starsToDrop, NETHER_STAR_LIMIT - currentCount);
                if (allowed <= 0) {
                    event.getDrops().removeIf(item -> item.getType() == Material.NETHER_STAR);
                } else if (allowed < starsToDrop) {
                    int toRemove = starsToDrop - allowed;
                    java.util.Iterator<ItemStack> it = event.getDrops().iterator();
                    while (it.hasNext() && toRemove > 0) {
                        ItemStack item = it.next();
                        if (item.getType() == Material.NETHER_STAR) {
                            if (toRemove >= item.getAmount()) {
                                toRemove -= item.getAmount();
                                it.remove();
                            } else {
                                item.setAmount(item.getAmount() - toRemove);
                                toRemove = 0;
                            }
                        }
                    }
                    tracker.addNetherStars(allowed);
                } else {
                    tracker.addNetherStars(allowed);
                }
            }
        }
        
        // Handle Totems (from Evoker)
        if (entity instanceof Evoker) {
            int currentCount = tracker.getTotems();
            int totemsToDrop = (int) event.getDrops().stream()
                .filter(item -> item.getType() == Material.TOTEM_OF_UNDYING)
                .mapToInt(ItemStack::getAmount)
                .sum();
            
            if (totemsToDrop > 0) {
                int allowed = Math.min(totemsToDrop, TOTEM_LIMIT - currentCount);
                if (allowed <= 0) {
                    event.getDrops().removeIf(item -> item.getType() == Material.TOTEM_OF_UNDYING);
                } else if (allowed < totemsToDrop) {
                    int toRemove = totemsToDrop - allowed;
                    java.util.Iterator<ItemStack> it = event.getDrops().iterator();
                    while (it.hasNext() && toRemove > 0) {
                        ItemStack item = it.next();
                        if (item.getType() == Material.TOTEM_OF_UNDYING) {
                            if (toRemove >= item.getAmount()) {
                                toRemove -= item.getAmount();
                                it.remove();
                            } else {
                                item.setAmount(item.getAmount() - toRemove);
                                toRemove = 0;
                            }
                        }
                    }
                    tracker.addTotems(allowed);
                } else {
                    tracker.addTotems(allowed);
                }
            }
        }
    }
    
    private ISpawner findSpawnerForEntity(Entity entity) {
        // Find nearest spawner within reasonable distance
        Location entityLoc = entity.getLocation();
        double minDist = Double.MAX_VALUE;
        ISpawner nearest = null;
        
        // Use list() method to get all generators in the entity's world
        java.util.List<IGenerator> generators = GeneratorRegistry.list(entityLoc.getWorld());
        
        for (IGenerator generator : generators) {
            if (generator == null || generator.block() == null) continue;
            
            Location spawnerLoc = generator.block().getLocation();
            if (!spawnerLoc.getWorld().equals(entityLoc.getWorld())) continue;
            
            double dist = spawnerLoc.distanceSquared(entityLoc);
            if (dist < minDist && dist < 64 * 64) { // Within 64 blocks
                SpawnerType type = generator.cache().type();
                if (type.entity() == entity.getType()) {
                    minDist = dist;
                    nearest = generator.spawner();
                }
            }
        }
        
        return nearest;
    }
    
    private String getSpawnerKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }
    
    private static void cleanupOldTrackers() {
        long now = System.currentTimeMillis();
        dropTrackers.entrySet().removeIf(entry -> {
            SpawnerDropTracker tracker = entry.getValue();
            tracker.cleanOldData();
            return tracker.isEmpty();
        });
    }
    
    public static class SpawnerDropTracker {
        private final Map<Long, Integer> shulkerShells = new HashMap<>(); // timestamp -> count
        private final Map<Long, Integer> netherStars = new HashMap<>();
        private final Map<Long, Integer> totems = new HashMap<>();
        
        public void cleanOldData() {
            long now = System.currentTimeMillis();
            long cutoff = now - DAY_MS;
            
            shulkerShells.entrySet().removeIf(e -> e.getKey() < cutoff);
            netherStars.entrySet().removeIf(e -> e.getKey() < cutoff);
            totems.entrySet().removeIf(e -> e.getKey() < cutoff);
        }
        
        public int getShulkerShells() {
            return shulkerShells.values().stream().mapToInt(Integer::intValue).sum();
        }
        
        public int getNetherStars() {
            return netherStars.values().stream().mapToInt(Integer::intValue).sum();
        }
        
        public int getTotems() {
            return totems.values().stream().mapToInt(Integer::intValue).sum();
        }
        
        public void addShulkerShells(int amount) {
            if (amount > 0) {
                long now = System.currentTimeMillis();
                shulkerShells.put(now, shulkerShells.getOrDefault(now, 0) + amount);
            }
        }
        
        public void addNetherStars(int amount) {
            if (amount > 0) {
                long now = System.currentTimeMillis();
                netherStars.put(now, netherStars.getOrDefault(now, 0) + amount);
            }
        }
        
        public void addTotems(int amount) {
            if (amount > 0) {
                long now = System.currentTimeMillis();
                totems.put(now, totems.getOrDefault(now, 0) + amount);
            }
        }
        
        public boolean isEmpty() {
            return shulkerShells.isEmpty() && netherStars.isEmpty() && totems.isEmpty();
        }
    }
}

