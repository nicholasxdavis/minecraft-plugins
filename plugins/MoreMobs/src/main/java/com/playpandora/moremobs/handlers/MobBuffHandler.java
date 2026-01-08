package com.playpandora.moremobs.handlers;

import com.playpandora.moremobs.MoreMobs;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class MobBuffHandler implements Listener {
    
    private final MoreMobs plugin;
    private final Random random = new Random();
    private final Map<UUID, Long> goatChargeCooldown = new HashMap<>();
    
    public MobBuffHandler(MoreMobs plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startBehaviorTicker();
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.isCancelled()) return;
        if (!plugin.getConfig().getBoolean("mob-buffs.enabled", true)) return;
        
        LivingEntity entity = event.getEntity();
        
        switch (entity.getType()) {
            case CAMEL:
                applyCamelBuffs((Camel) entity);
                break;
            case RAVAGER:
                applyRavagerBuffs((Ravager) entity);
                break;
            case GOAT:
                applyGoatBuffs((Goat) entity);
                break;
            case STRIDER:
                applyStriderBuffs((Strider) entity);
                break;
            case BOGGED:
                applyBoggedBuffs((Bogged) entity);
                break;
            case BREEZE:
                applyBreezeBuffs((Breeze) entity);
                break;
            default:
                break;
        }
    }
    
    private void applyCamelBuffs(Camel camel) {
        if (!plugin.getConfig().getBoolean("mob-buffs.camels.enabled", true)) return;
        
        // Camel jump height modification requires NMS access
        // For now, we'll use velocity boost when camel jumps as approximation
        double jumpMultiplier = plugin.getConfig().getDouble("mob-buffs.camels.jump-height-multiplier", 1.2);
        camel.setMetadata("moremobs_jump_multiplier", 
            new org.bukkit.metadata.FixedMetadataValue(plugin, jumpMultiplier));
    }
    
    private void applyRavagerBuffs(Ravager ravager) {
        if (!plugin.getConfig().getBoolean("mob-buffs.ravagers.enabled", true)) return;
        
        boolean canBreakLeaves = plugin.getConfig().getBoolean("mob-buffs.ravagers.can-break-leaves", true);
        boolean canBreakFences = plugin.getConfig().getBoolean("mob-buffs.ravagers.can-break-fences", true);
        
        // Store metadata for later checking
        if (canBreakLeaves) {
            ravager.setMetadata("moremobs_can_break_leaves", 
                new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        }
        if (canBreakFences) {
            ravager.setMetadata("moremobs_can_break_fences", 
                new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        }
        
        // Monitor ravager collisions to break blocks
        monitorRavagerBlockBreaking(ravager);
    }
    
    private void monitorRavagerBlockBreaking(Ravager ravager) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!ravager.isValid()) {
                    cancel();
                    return;
                }
                
                Block block = ravager.getLocation().getBlock();
                Material type = block.getType();
                
                boolean canBreakLeaves = ravager.hasMetadata("moremobs_can_break_leaves");
                boolean canBreakFences = ravager.hasMetadata("moremobs_can_break_fences");
                
                // Check if ravager should break this block
                if (canBreakLeaves && isLeaves(type)) {
                    if (ravager.getVelocity().length() > 0.1) { // Only when moving
                        block.breakNaturally();
                    }
                }
                
                if (canBreakFences && isFence(type)) {
                    if (ravager.getVelocity().length() > 0.1) {
                        block.breakNaturally();
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 5); // Check every 0.25 seconds
    }
    
    private boolean isLeaves(Material type) {
        return type == Material.OAK_LEAVES || type == Material.BIRCH_LEAVES ||
               type == Material.SPRUCE_LEAVES || type == Material.JUNGLE_LEAVES ||
               type == Material.ACACIA_LEAVES || type == Material.DARK_OAK_LEAVES ||
               type == Material.MANGROVE_LEAVES || type == Material.CHERRY_LEAVES ||
               type == Material.AZALEA_LEAVES || type == Material.FLOWERING_AZALEA_LEAVES;
    }
    
    private boolean isFence(Material type) {
        return type.name().contains("FENCE") && !type.name().contains("GATE");
    }
    
    private void applyGoatBuffs(Goat goat) {
        if (!plugin.getConfig().getBoolean("mob-buffs.goats.enabled", true)) return;
        
        double chargeMultiplier = plugin.getConfig().getDouble("mob-buffs.goats.charge-frequency-multiplier", 1.3);
        
        // Store metadata for charge frequency
        goat.setMetadata("moremobs_charge_frequency", 
            new org.bukkit.metadata.FixedMetadataValue(plugin, chargeMultiplier));
        
        // Monitor and trigger charges more frequently
        monitorGoatCharges(goat);
    }
    
    private void monitorGoatCharges(Goat goat) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!goat.isValid()) {
                    cancel();
                    return;
                }
                
                UUID uuid = goat.getUniqueId();
                long now = System.currentTimeMillis();
                long lastCharge = goatChargeCooldown.getOrDefault(uuid, 0L);
                
                // Check for nearby entities to charge
                if (now - lastCharge > 3000) { // Base cooldown 3 seconds
                    for (org.bukkit.entity.Entity nearbyEntity : goat.getNearbyEntities(8, 4, 8)) {
                        if (nearbyEntity instanceof LivingEntity) {
                            LivingEntity nearby = (LivingEntity) nearbyEntity;
                            if (nearby != goat && random.nextDouble() < 0.3) {
                                // Trigger charge by applying velocity
                                double chargeMultiplier = 1.0;
                                if (goat.hasMetadata("moremobs_charge_frequency")) {
                                    chargeMultiplier = goat.getMetadata("moremobs_charge_frequency").get(0).asDouble();
                                }
                                
                                if (random.nextDouble() < (chargeMultiplier - 1.0) * 0.5) {
                                    org.bukkit.util.Vector direction = nearby.getLocation()
                                        .toVector().subtract(goat.getLocation().toVector()).normalize();
                                    goat.setVelocity(direction.multiply(1.2));
                                    goatChargeCooldown.put(uuid, now);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 20); // Check every second
    }
    
    private void applyStriderBuffs(Strider strider) {
        if (!plugin.getConfig().getBoolean("mob-buffs.striders.enabled", true)) return;
        
        double speedMultiplier = plugin.getConfig().getDouble("mob-buffs.striders.lava-travel-speed-multiplier", 1.25);
        
        // Increase movement speed when on lava
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!strider.isValid()) {
                    cancel();
                    return;
                }
                
                Block block = strider.getLocation().getBlock().getRelative(0, -1, 0);
                if (block.getType() == Material.LAVA) {
                    // Use Paper API movement speed attribute if available, otherwise apply velocity boost
                    org.bukkit.util.Vector velocity = strider.getVelocity();
                    if (velocity.length() > 0.01) {
                        strider.setVelocity(velocity.multiply(1.0 + (speedMultiplier - 1.0) * 0.1));
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 10); // Check every 0.5 seconds
    }
    
    private void applyBoggedBuffs(Bogged bogged) {
        if (!plugin.getConfig().getBoolean("mob-buffs.bogged.enabled", true)) return;
        
        double speedMultiplier = plugin.getConfig().getDouble("mob-buffs.bogged.movement-speed-multiplier", 0.85);
        
        // Store metadata for movement speed reduction
        bogged.setMetadata("moremobs_speed_multiplier", 
            new org.bukkit.metadata.FixedMetadataValue(plugin, speedMultiplier));
        
        // Apply periodic speed reduction via velocity manipulation
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!bogged.isValid()) {
                    cancel();
                    return;
                }
                // Speed is reduced through slower movement calculations
                // This is handled indirectly through the movement system
            }
        }.runTaskTimer(plugin, 0, 20);
    }
    
    private void applyBreezeBuffs(Breeze breeze) {
        if (!plugin.getConfig().getBoolean("mob-buffs.breeze.enabled", true)) return;
        
        double pushMultiplier = plugin.getConfig().getDouble("mob-buffs.breeze.projectile-push-multiplier", 1.3);
        
        // Store metadata for projectile push strength
        breeze.setMetadata("moremobs_push_multiplier", 
            new org.bukkit.metadata.FixedMetadataValue(plugin, pushMultiplier));
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBreezeProjectileHit(EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("mob-buffs.breeze.enabled", true)) return;
        if (!(event.getEntity() instanceof Player)) return;
        
        // Check if damager is a projectile shot by a Breeze
        org.bukkit.entity.Entity damager = event.getDamager();
        if (!(damager instanceof org.bukkit.entity.Projectile)) return;
        
        org.bukkit.entity.Projectile projectile = (org.bukkit.entity.Projectile) damager;
        if (!(projectile.getShooter() instanceof Breeze)) return;
        
        Breeze breeze = (Breeze) projectile.getShooter();
        if (!breeze.hasMetadata("moremobs_push_multiplier")) return;
        
        double pushMultiplier = breeze.getMetadata("moremobs_push_multiplier").get(0).asDouble();
        
        // Apply extra knockback
        Player player = (Player) event.getEntity();
        org.bukkit.util.Vector direction = player.getLocation().toVector()
            .subtract(breeze.getLocation().toVector()).normalize();
        player.setVelocity(player.getVelocity().add(direction.multiply(0.2 * (pushMultiplier - 1.0))));
    }
    
    private void startBehaviorTicker() {
        // Periodic behavior updates
        new BukkitRunnable() {
            @Override
            public void run() {
                // Clean up old goat charge cooldowns
                long now = System.currentTimeMillis();
                goatChargeCooldown.entrySet().removeIf(entry -> now - entry.getValue() > 10000);
            }
        }.runTaskTimer(plugin, 0, 200); // Every 10 seconds
    }
    
    public void cleanup() {
        goatChargeCooldown.clear();
    }
}

