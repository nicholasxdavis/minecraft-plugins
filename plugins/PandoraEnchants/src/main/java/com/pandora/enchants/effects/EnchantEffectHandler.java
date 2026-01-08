package com.pandora.enchants.effects;

import com.pandora.enchants.engine.PandoraEnchant;
import com.pandora.enchants.util.EnchantmentStorage;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import com.pandora.enchants.PandoraEnchants;
import com.pandora.enchants.util.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.List;

/**
 * Comprehensive enchantment effects handler
 * Fully compatible with Minecraft 1.21
 * ENHANCED: All enchants are 2-3x more powerful than vanilla with impressive visual effects
 */
public class EnchantEffectHandler implements Listener {
    
    private final Random random = new Random();
    private final Map<UUID, Long> lastThornsDamage = new HashMap<>();
    private final Map<UUID, Long> lastAutoFeed = new HashMap<>();
    private final Map<UUID, Set<Location>> veinMiningBlocks = new HashMap<>();
    private final Map<UUID, Set<Location>> magnetItems = new HashMap<>();
    
    // Ore types for vein miner
    private static final Set<Material> ORE_TYPES = new HashSet<>(Arrays.asList(
        Material.COAL_ORE, Material.COPPER_ORE, Material.IRON_ORE, Material.GOLD_ORE,
        Material.LAPIS_ORE, Material.REDSTONE_ORE, Material.DIAMOND_ORE, Material.EMERALD_ORE,
        Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE, Material.ANCIENT_DEBRIS,
        Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_COPPER_ORE, Material.DEEPSLATE_IRON_ORE,
        Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_LAPIS_ORE, Material.DEEPSLATE_REDSTONE_ORE,
        Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_EMERALD_ORE
    ));
    
    // Wood types for lumberjack
    private static final Set<Material> WOOD_TYPES = new HashSet<>(Arrays.asList(
        Material.OAK_LOG, Material.SPRUCE_LOG, Material.BIRCH_LOG, Material.JUNGLE_LOG,
        Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG,
        Material.OAK_WOOD, Material.SPRUCE_WOOD, Material.BIRCH_WOOD, Material.JUNGLE_WOOD,
        Material.ACACIA_WOOD, Material.DARK_OAK_WOOD, Material.MANGROVE_WOOD, Material.CHERRY_WOOD,
        Material.STRIPPED_OAK_LOG, Material.STRIPPED_SPRUCE_LOG, Material.STRIPPED_BIRCH_LOG,
        Material.STRIPPED_JUNGLE_LOG, Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_DARK_OAK_LOG,
        Material.STRIPPED_MANGROVE_LOG, Material.STRIPPED_CHERRY_WOOD, Material.STRIPPED_CHERRY_LOG
    ));
    
    // Farming materials for excavator
    private static final Set<Material> FARMING_MATERIALS = new HashSet<>(Arrays.asList(
        Material.SUGAR_CANE, Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
        Material.MELON, Material.PUMPKIN, Material.COCOA, Material.NETHER_WART, Material.SWEET_BERRY_BUSH,
        Material.CACTUS, Material.BAMBOO, Material.KELP, Material.KELP_PLANT, Material.SEA_PICKLE,
        Material.GLOW_BERRIES, Material.CAVE_VINES, Material.CAVE_VINES_PLANT, Material.TWISTING_VINES,
        Material.TWISTING_VINES_PLANT, Material.WEEPING_VINES, Material.WEEPING_VINES_PLANT,
        Material.BIG_DRIPLEAF, Material.BIG_DRIPLEAF_STEM, Material.SMALL_DRIPLEAF
    ));
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        
        // Check all armor pieces for passive effects
        checkArmorEffects(player);
        
        // Handle magnet enchant
        handleMagnet(player);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        ItemStack weapon = player.getInventory().getItemInMainHand();
        
        if (weapon == null || weapon.getType().isAir()) {
            return;
        }
        
        // SAFEGUARD: Prevent instant-kill effects on players (only allow on mobs)
        if (event.getEntity() instanceof Player) {
            // Check for instant-kill enchantments and skip them for players
            List<PandoraEnchant> enchants = EnchantmentStorage.getAllEnchants(weapon);
            for (PandoraEnchant enchant : enchants) {
                if (enchant == null) continue;
                String name = enchant.getNamespacedName().toLowerCase();
                // Skip instant-kill enchants for players (only work on mobs)
                if (name.contains("oneshot") || name.contains("one_shot") || 
                    name.contains("firstlife") || name.contains("first_life") ||
                    name.contains("instakill") || name.contains("instant_kill")) {
                    return; // Don't apply instant-kill effects to players
                }
            }
        }
        
        // BALANCE: Reduce damage from godset weapons when fighting 1 CE players
        if (event.getEntity() instanceof Player) {
            Player target = (Player) event.getEntity();
            boolean attackerHasGodset = com.pandora.enchants.util.GodSetManager.isGodsetItem(weapon);
            boolean targetHasGodset = false;
            
            // Check if target has any godset armor or weapon
            ItemStack[] targetArmor = target.getInventory().getArmorContents();
            for (ItemStack armor : targetArmor) {
                if (armor != null && com.pandora.enchants.util.GodSetManager.isGodsetItem(armor)) {
                    targetHasGodset = true;
                    break;
                }
            }
            ItemStack targetWeapon = target.getInventory().getItemInMainHand();
            if (targetWeapon != null && com.pandora.enchants.util.GodSetManager.isGodsetItem(targetWeapon)) {
                targetHasGodset = true;
            }
            
            // If attacker has godset weapon but target only has 1 CE (no godset), reduce damage
            if (attackerHasGodset && !targetHasGodset) {
                // Reduce damage by 30% to balance godset vs 1 CE fights
                double originalDamage = event.getDamage();
                event.setDamage(originalDamage * 0.7);
            }
        }
        
        // Check ALL enchants on the weapon
        List<PandoraEnchant> enchants = EnchantmentStorage.getAllEnchants(weapon);
        
        if (enchants.isEmpty()) {
            return;
        }
        
        // Check if this is a godset vs 1 CE fight (for balance reduction)
        boolean isGodsetVsOneCE = false;
        if (event.getEntity() instanceof Player) {
            Player target = (Player) event.getEntity();
            boolean attackerHasGodset = com.pandora.enchants.util.GodSetManager.isGodsetItem(weapon);
            boolean targetHasGodset = false;
            
            ItemStack[] targetArmor = target.getInventory().getArmorContents();
            for (ItemStack armor : targetArmor) {
                if (armor != null && com.pandora.enchants.util.GodSetManager.isGodsetItem(armor)) {
                    targetHasGodset = true;
                    break;
                }
            }
            ItemStack targetWeapon = target.getInventory().getItemInMainHand();
            if (targetWeapon != null && com.pandora.enchants.util.GodSetManager.isGodsetItem(targetWeapon)) {
                targetHasGodset = true;
            }
            
            isGodsetVsOneCE = attackerHasGodset && !targetHasGodset;
        }
        
        // Process each enchant
        for (PandoraEnchant enchant : enchants) {
            if (enchant == null) continue;
            
            int level = EnchantmentStorage.getEnchantLevel(weapon, enchant);
            
            if (level >= 1) {
                handleWeaponEffects(player, enchant, level, event, isGodsetVsOneCE);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityKill(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        
        if (killer == null) return;
        
        ItemStack weapon = killer.getInventory().getItemInMainHand();
        if (weapon == null || weapon.getType().isAir()) return;
        
        // Check ALL enchants on the weapon
        List<PandoraEnchant> enchants = EnchantmentStorage.getAllEnchants(weapon);
        for (PandoraEnchant enchant : enchants) {
            if (enchant == null) continue;
            int level = EnchantmentStorage.getEnchantLevel(weapon, enchant);
            if (level >= 1) {
                handleKillEffects(killer, enchant, level, entity);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        if (player == null) return;
        
        ItemStack tool = player.getInventory().getItemInMainHand();
        
        if (tool == null || tool.getType().isAir()) {
            return;
        }
        
        // Check ALL enchants on the tool
        List<PandoraEnchant> enchants = EnchantmentStorage.getAllEnchants(tool);
        
        if (enchants.isEmpty()) {
            return;
        }
        
        // Process each enchant
        for (PandoraEnchant enchant : enchants) {
            if (enchant == null) continue;
            
            int level = EnchantmentStorage.getEnchantLevel(tool, enchant);
            
            if (level >= 1) {
                handleToolEffects(player, enchant, level, event);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockDropItem(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || tool.getType().isAir()) return;
        
        // Check ALL enchants on the tool
        List<PandoraEnchant> enchants = EnchantmentStorage.getAllEnchants(tool);
        Block block = event.getBlock();
        
        for (PandoraEnchant enchant : enchants) {
            if (enchant == null) continue;
            int level = EnchantmentStorage.getEnchantLevel(tool, enchant);
            if (level < 1) continue;
            
            String name = enchant.getNamespacedName();
            
            // Handle autosmelt
            if (name.equals("autosmelt")) {
                handleAutoSmelt(event, block);
            }
            
            // Handle telekinesis
            if (name.equals("telekinesis")) {
                handleTelekinesis(event, player);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity entity = (LivingEntity) event.getEntity();
        
        if (!(entity instanceof Player)) return;
        Player player = (Player) entity;
        
        
        // Check armor for thorns and other defensive enchants
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack piece : armor) {
            if (piece == null || piece.getType().isAir()) continue;
            
            PandoraEnchant enchant = EnchantmentStorage.getEnchant(piece);
            if (enchant == null) continue;
            
            int level = EnchantmentStorage.getEnchantLevel(piece, enchant);
            if (level < 1) continue;
            
            handleDefensiveEffects(player, enchant, level, event);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player)) return;
        Player shooter = (Player) event.getEntity().getShooter();
        
        ItemStack bow = shooter.getInventory().getItemInMainHand();
        if (bow == null || (bow.getType() != Material.BOW && bow.getType() != Material.CROSSBOW)) {
            return;
        }
        
        PandoraEnchant enchant = EnchantmentStorage.getEnchant(bow);
        if (enchant == null) return;
        
        int level = EnchantmentStorage.getEnchantLevel(bow, enchant);
        if (level < 1) return;
        
        Location loc = event.getEntity().getLocation();
        String name = enchant.getNamespacedName();
        
        switch (name) {
            case "explosive":
                if (loc.getWorld() != null) {
                    // Balanced for 1 CE per item gameplay - significantly reduced explosion power
                    // Reduced from 0.8-2.0 to 0.3-0.7 to prevent OP damage
                    float power = 0.3f + (level * 0.2f); // Level 1: 0.5, Level 2: 0.7, Level 3: 0.9
                    loc.getWorld().createExplosion(loc, power, false, false);
                    shooter.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.0f);
                    if (loc.getWorld() != null) {
                        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 2, 0, 0, 0, 0);
                        loc.getWorld().spawnParticle(Particle.SMOKE, loc, 5, 0.5, 0.5, 0.5, 0.05);
                    }
                }
                break;
                
            case "poison_arrow":
                if (event.getHitEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getHitEntity();
                    // NO POTION EFFECTS - use damage over time instead
                    applyDamageOverTime(target, 1.0 + (level * 0.5), level * 3, "poison");
                    shooter.playSound(loc, Sound.ENTITY_SPIDER_AMBIENT, 0.75f, 0.8f);
                    if (loc.getWorld() != null) {
                        loc.getWorld().spawnParticle(Particle.ITEM_SLIME, loc.add(0, 1, 0), 6, 0.375, 0.375, 0.375, 0.0625);
                        loc.getWorld().spawnParticle(Particle.ENCHANT, loc, 4, 0.375, 0.375, 0.375, 0.0375);
                    }
                }
                break;
                
            case "homing":
                shooter.playSound(loc, Sound.ENTITY_ENDER_EYE_LAUNCH, 0.75f, 1.0f);
                if (loc.getWorld() != null) {
                    loc.getWorld().spawnParticle(Particle.PORTAL, loc, 13, 0.375, 0.375, 0.375, 0.125);
                    loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc, 8, 0.25, 0.25, 0.25, 0.0625);
                }
                break;
                
            case "multi_shot":
                shooter.playSound(loc, Sound.ENTITY_ARROW_SHOOT, 0.75f, 1.0f);
                if (loc.getWorld() != null) {
                    loc.getWorld().spawnParticle(Particle.CRIT, loc, 8, 0.375, 0.375, 0.375, 0.0625);
                }
                break;
                
            case "pierce":
                shooter.playSound(loc, Sound.ENTITY_ARROW_HIT, 0.625f, 0.9f);
                if (loc.getWorld() != null) {
                    loc.getWorld().spawnParticle(Particle.CRIT, loc, 6, 0.375, 0.375, 0.375, 0.0625);
                    loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 4, 0.25, 0.25, 0.25, 0.0375);
                }
                break;
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        ItemStack boots = player.getInventory().getBoots();
        
        if (boots == null || boots.getType().isAir()) return;
        
        PandoraEnchant enchant = EnchantmentStorage.getEnchant(boots);
        if (enchant == null || !enchant.getNamespacedName().equals("feather_fall")) return;
        
        int level = EnchantmentStorage.getEnchantLevel(boots, enchant);
        if (level < 1) return;
        
        double reduction = 0.5 + (level * 0.15); // Much stronger reduction
        event.setDamage(event.getDamage() * (1.0 - reduction));
        
        player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.5f, 1.14f);
        player.spawnParticle(Particle.CLOUD, player.getLocation(), 6, 0.375, 0.25, 0.375, 0.0375);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        lastThornsDamage.remove(uuid);
        lastAutoFeed.remove(uuid);
        veinMiningBlocks.remove(uuid);
        magnetItems.remove(uuid);
    }
    
    private void handleWeaponEffects(Player player, PandoraEnchant enchant, int level, EntityDamageByEntityEvent event, boolean isGodsetVsOneCE) {
        if (player == null || enchant == null || event == null) {
            return;
        }
        
        // SAFEGUARD: Prevent instant-kill effects on players (only allow on mobs)
        if (event.getEntity() instanceof Player) {
            String name = enchant.getNamespacedName().toLowerCase();
            // Skip instant-kill enchants for players (only work on mobs)
            if (name.contains("oneshot") || name.contains("one_shot") || 
                name.contains("firstlife") || name.contains("first_life") ||
                name.contains("instakill") || name.contains("instant_kill")) {
                return; // Don't apply instant-kill effects to players
            }
        }
        
        String name = enchant.getNamespacedName();
        Location loc = event.getEntity().getLocation();
        
        
        try {
        switch (name) {
            case "lifesteal": {
                    // Balanced for 1 CE per item - moderate healing
                    double heal = 0.5 + (level * 0.3); // Level 1: 0.8, Level 2: 1.1, Level 3: 1.4
                    double newHealth = Math.min(player.getHealth() + heal, player.getMaxHealth());
                    player.setHealth(newHealth);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.625f, 1.14f);
                    player.spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 3, 0.3125, 0.3125, 0.3125, 0);
                    player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1.5, 0), 1, 0.1875, 0.1875, 0.1875, 0);
                break;
            }
                
            case "lightning":
                    // Higher chance and multiple strikes
                    if (random.nextDouble() < (0.2 + (level * 0.1)) && loc.getWorld() != null) {
                    loc.getWorld().strikeLightningEffect(loc);
                        if (level >= 2) {
                            // Multiple strikes for higher levels
                            new BukkitRunnable() {
                                int strikes = 0;
                                @Override
                                public void run() {
                                    if (strikes >= level - 1) {
                                        cancel();
                                        return;
                                    }
                                    Location strikeLoc = loc.clone().add(
                                        (random.nextDouble() - 0.5) * 3,
                                        0,
                                        (random.nextDouble() - 0.5) * 3
                                    );
                                    loc.getWorld().strikeLightningEffect(strikeLoc);
                                    strikes++;
                                }
                            }.runTaskTimer(PandoraEnchants.getInstance(), 5L, 5L);
                        }
                        player.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.75f, 0.76f);
                        if (loc.getWorld() != null) {
                            loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 10, 0.375, 0.75, 0.375, 0.0625);
                        }
                }
                break;
                
            case "venom":
                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();
                        // NO POTION EFFECTS - use damage over time instead
                        double damagePerTick = 0.5 + (level * 0.3);
                        applyDamageOverTime(target, damagePerTick, level * 4, "venom");
                        player.playSound(loc, Sound.ENTITY_SPIDER_AMBIENT, 0.625f, 0.67f);
                        if (loc.getWorld() != null) {
                            loc.getWorld().spawnParticle(Particle.ITEM_SLIME, loc.add(0, 1, 0), 8, 0.4375, 0.4375, 0.4375, 0.0625);
                            loc.getWorld().spawnParticle(Particle.ENCHANT, loc, 5, 0.375, 0.375, 0.375, 0.0375);
                        }
                }
                break;
                
            case "wither":
                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();
                        // NO POTION EFFECTS - use damage over time instead
                        double damagePerTick = 0.8 + (level * 0.4);
                        applyDamageOverTime(target, damagePerTick, level * 5, "wither");
                        player.playSound(loc, Sound.ENTITY_WITHER_HURT, 0.625f, 0.9f);
                        if (loc.getWorld() != null) {
                            loc.getWorld().spawnParticle(Particle.SMOKE, loc.add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.0625);
                            loc.getWorld().spawnParticle(Particle.SOUL, loc, 6, 0.375, 0.375, 0.375, 0.0375);
                        }
                }
                break;
                
            case "critical":
                    // Balanced for 1 CE per item - moderate crit chance and damage
                    if (random.nextDouble() < (0.15 + (level * 0.05))) { // Level 1: 20%, Level 2: 25%, Level 3: 30%
                        double multiplier = 1.5 + (level * 0.3); // Level 1: 1.8x, Level 2: 2.1x, Level 3: 2.4x
                        // Reduce multiplier if godset vs 1 CE
                        if (isGodsetVsOneCE) {
                            multiplier = 1.0 + ((multiplier - 1.0) * 0.6); // Reduce bonus by 40%
                        }
                        event.setDamage(event.getDamage() * multiplier);
                        player.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.875f, 0.76f);
                        if (loc.getWorld() != null) {
                            loc.getWorld().spawnParticle(Particle.CRIT, loc.add(0, 1, 0), 10, 0.625, 0.625, 0.625, 0.125);
                            loc.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, loc.add(0, 1.5, 0), 8, 0.375, 0.375, 0.375, 0.0625);
                        }
                }
                break;
                
            case "execute":
                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();
                    double healthPercent = target.getHealth() / target.getMaxHealth();
                        if (healthPercent < 0.3) { // Trigger at lower health for balance
                            // Balanced for 1 CE per item - moderate execute bonus
                            double bonus = 1.3 + ((1.0 - healthPercent) * 0.8 * level); // Max ~1.5-1.9x at low health
                            // Reduce bonus if godset vs 1 CE
                            if (isGodsetVsOneCE) {
                                bonus = 1.0 + ((bonus - 1.0) * 0.6); // Reduce bonus by 40%
                            }
                        event.setDamage(event.getDamage() * bonus);
                            player.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.75f, 0.7f);
                            if (loc.getWorld() != null) {
                                loc.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, loc.add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                                loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 6, 0.375, 0.375, 0.375, 0.0625);
                            }
                    }
                }
                break;
                
            case "freeze":
                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();
                        target.setFreezeTicks(60 + (level * 30)); // Longer freeze
                        if (loc.getWorld() != null) {
                            loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 13, 0.625, 0.625, 0.625, 0.0625);
                            loc.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, loc, 8, 0.375, 0.375, 0.375, 0.0375);
                        }
                        player.playSound(loc, Sound.BLOCK_SNOW_BREAK, 0.625f, 0.8f);
                        player.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.2f);
                }
                break;
                
            case "execute_plus":
                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();
                    double healthPercent = target.getHealth() / target.getMaxHealth();
                        if (healthPercent < 0.35) { // Trigger at lower health for balance
                            // Balanced for 1 CE per item - stronger than execute but not OP
                            double bonus = 1.5 + ((1.0 - healthPercent) * 1.0 * level); // Max ~2.0-2.5x at low health
                            // Reduce bonus if godset vs 1 CE
                            if (isGodsetVsOneCE) {
                                bonus = 1.0 + ((bonus - 1.0) * 0.5); // Reduce bonus by 50% (stronger enchant, more reduction)
                            }
                        event.setDamage(event.getDamage() * bonus);
                            player.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.875f, 0.6f);
                            if (loc.getWorld() != null) {
                                loc.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, loc.add(0, 1, 0), 13, 0.625, 0.625, 0.625, 0.125);
                                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
                            }
                    }
                }
                break;
                
            case "bleed":
                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();
                        player.playSound(loc, Sound.ENTITY_PLAYER_HURT, 0.625f, 0.9f);
                        if (loc.getWorld() != null) {
                            loc.getWorld().spawnParticle(Particle.DUST, loc.add(0, 1, 0), 8, 0.375, 0.375, 0.375, 0.0625);
                            loc.getWorld().spawnParticle(Particle.DUST, loc, 5, 0.3125, 0.3125, 0.3125, 0.0375);
                        }
                        
                        // Balanced for 1 CE per item - moderate bleed damage
                    new BukkitRunnable() {
                        int ticks = 0;
                            final LivingEntity bleedTarget = target;
                        @Override
                        public void run() {
                                if (bleedTarget.isDead() || !bleedTarget.isValid() || ticks >= (level * 20)) { // Shorter duration
                                cancel();
                                return;
                            }
                                if (ticks % 20 == 0) {
                                    double damage = 0.5 + (level * 0.2); // Level 1: 0.7, Level 2: 0.9, Level 3: 1.1 per second
                                    bleedTarget.damage(damage);
                                    if (bleedTarget.getWorld() != null) {
                                        bleedTarget.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, 
                                            bleedTarget.getLocation().add(0, 1, 0), 4, 0.25, 0.25, 0.25, 0.0625);
                                        bleedTarget.getWorld().spawnParticle(Particle.DUST, 
                                            bleedTarget.getLocation().add(0, 0.5, 0), 3, 0.1875, 0.1875, 0.1875, 0.0375);
                                    }
                            }
                            ticks++;
                        }
                    }.runTaskTimer(PandoraEnchants.getInstance(), 0L, 1L);
                }
                break;
                
            case "rage":
                if (player.getHealth() < player.getMaxHealth()) {
                    double healthPercent = player.getHealth() / player.getMaxHealth();
                        // Balanced for 1 CE per item - moderate rage bonus
                        double damageMultiplier = 1.2 + ((1.0 - healthPercent) * 0.6 * level); // Max ~1.4-1.8x at low health
                        // Reduce multiplier if godset vs 1 CE
                        if (isGodsetVsOneCE) {
                            damageMultiplier = 1.0 + ((damageMultiplier - 1.0) * 0.6); // Reduce bonus by 40%
                        }
                    event.setDamage(event.getDamage() * damageMultiplier);
                        if (healthPercent < 0.5) {
                            player.playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 0.5f, 1.0f);
                            if (loc.getWorld() != null) {
                                loc.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, loc.add(0, 1, 0), 5, 0.375, 0.375, 0.375, 0.0625);
                                loc.getWorld().spawnParticle(Particle.FLAME, loc, 4, 0.25, 0.25, 0.25, 0.0375);
                            }
                    }
                }
                break;
                
            case "cleave":
                    // Balanced for 1 CE per item - moderate cleave chance and damage
                    if (random.nextDouble() < (0.25 + (level * 0.05)) && loc.getWorld() != null) { // Level 1: 30%, Level 2: 35%, Level 3: 40%
                        double radius = 2.0 + (level * 0.5); // Level 1: 2.5, Level 2: 3.0, Level 3: 3.5
                        int hitCount = 0;
                    for (Entity nearby : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                        if (nearby instanceof LivingEntity && nearby != event.getEntity() && nearby != player) {
                            LivingEntity nearbyEntity = (LivingEntity) nearby;
                                double damage = event.getDamage() * (0.3 + (level * 0.1)); // Level 1: 40%, Level 2: 50%, Level 3: 60%
                                // Reduce cleave damage if godset vs 1 CE
                                if (isGodsetVsOneCE && nearbyEntity instanceof Player) {
                                    damage *= 0.6; // Reduce by 40%
                                }
                            nearbyEntity.damage(damage, player);
                                nearbyEntity.getWorld().spawnParticle(Particle.SWEEP_ATTACK, 
                                    nearbyEntity.getLocation().add(0, 1, 0), 5, 0.375, 0.375, 0.375, 0.0625);
                                hitCount++;
                            }
                        }
                        if (hitCount > 0) {
                            player.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, 0.9f);
                            if (loc.getWorld() != null) {
                                loc.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 8, radius * 0.75, radius * 0.75, radius * 0.75, 0.0625);
                            }
                        }
                }
                break;
                
            case "disarm":
                    // Higher chance
                    if (random.nextDouble() < (0.15 + (level * 0.05)) && event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();
                    if (target instanceof Player) {
                        Player targetPlayer = (Player) target;
                        ItemStack mainHand = targetPlayer.getInventory().getItemInMainHand();
                        if (mainHand != null && !mainHand.getType().isAir()) {
                            targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), mainHand);
                            targetPlayer.getInventory().setItemInMainHand(null);
                                player.playSound(loc, Sound.ENTITY_ITEM_PICKUP, 0.75f, 0.3f);
                                if (loc.getWorld() != null) {
                                    loc.getWorld().spawnParticle(Particle.ITEM, loc, 10, 0.375, 0.375, 0.375, 0.1);
                                    loc.getWorld().spawnParticle(Particle.CRIT, loc, 6, 0.3125, 0.3125, 0.3125, 0.0625);
                                }
                        }
                    }
                }
                break;
                
            case "leech":
                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();
                        // Balanced for 1 CE per item - moderate health steal
                        double steal = 0.5 + (level * 0.2); // Level 1: 0.7, Level 2: 0.9, Level 3: 1.1
                    double newHealth = Math.min(player.getHealth() + steal, player.getMaxHealth());
                    player.setHealth(newHealth);
                    double newTargetHealth = Math.max(0, target.getHealth() - steal);
                    target.setHealth(newTargetHealth);
                        player.playSound(player.getLocation(), Sound.ENTITY_WITCH_DRINK, 0.625f, 1.0f);
                        if (loc.getWorld() != null) {
                            loc.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 4, 0.3125, 0.3125, 0.3125, 0.0625);
                            loc.getWorld().spawnParticle(Particle.SOUL, loc.add(0, 1, 0), 5, 0.3125, 0.3125, 0.3125, 0.0375);
                        }
                }
                break;
                
            case "vampire": {
                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();
                    double damage = event.getDamage();
                        // Balanced for 1 CE per item - moderate healing percentage
                        double healAmount = damage * (0.15 + (level * 0.05)); // Level 1: 20%, Level 2: 25%, Level 3: 30%
                        double newHealth = Math.min(player.getHealth() + healAmount, player.getMaxHealth());
                        player.setHealth(newHealth);
                        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.625f, 1.0f);
                        if (loc.getWorld() != null) {
                            loc.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 5, 0.375, 0.375, 0.375, 0.0625);
                            loc.getWorld().spawnParticle(Particle.SOUL, loc.add(0, 1, 0), 8, 0.375, 0.375, 0.375, 0.0625);
                            loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc, 6, 0.3125, 0.3125, 0.3125, 0.0375);
                        }
                }
                break;
            }
            }
        } catch (Exception e) {
            Logger.error("Error handling weapon effect " + name + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleKillEffects(Player killer, PandoraEnchant enchant, int level, LivingEntity killed) {
        String name = enchant.getNamespacedName();
        Location loc = killed.getLocation();
        
        switch (name) {
            case "soul_siphon":
                double heal = 4.0 + (level * 2.0); // Much stronger
                double newHealth = Math.min(killer.getHealth() + heal, killer.getMaxHealth());
                killer.setHealth(newHealth);
                // NO POTION EFFECTS - use visual effects only
                killer.playSound(loc, Sound.ENTITY_WITHER_AMBIENT, 0.75f, 0.6f);
                if (loc.getWorld() != null) {
                    loc.getWorld().spawnParticle(Particle.SOUL, loc, 15, 0.625, 0.625, 0.625, 0.125);
                    loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc, 10, 0.5, 0.5, 0.5, 0.0625);
                    loc.getWorld().spawnParticle(Particle.HEART, killer.getLocation().add(0, 1, 0), 6, 0.375, 0.375, 0.375, 0.0625);
                }
                break;
        }
    }
    
    private void handleDefensiveEffects(Player player, PandoraEnchant enchant, int level, EntityDamageEvent event) {
        String name = enchant.getNamespacedName();
        Location loc = player.getLocation();
        
        switch (name) {
            case "thorns":
                if (event instanceof EntityDamageByEntityEvent) {
                    EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
                    Entity damager = damageEvent.getDamager();
                    
                    // Prevent spam
                    UUID playerId = player.getUniqueId();
                    long now = System.currentTimeMillis();
                    if (lastThornsDamage.containsKey(playerId) && now - lastThornsDamage.get(playerId) < 500) {
                        return;
                    }
                    lastThornsDamage.put(playerId, now);
                    
                    if (damager instanceof LivingEntity && damager != player) {
                        LivingEntity attacker = (LivingEntity) damager;
                        // Balanced for 1 CE per item - moderate thorns damage
                        double thornsDamage = 0.8 + (level * 0.4); // Level 1: 1.2, Level 2: 1.6, Level 3: 2.0
                        attacker.damage(thornsDamage);
                        player.playSound(loc, Sound.ENTITY_PLAYER_HURT, 0.625f, 1.0f);
                        if (attacker.getWorld() != null) {
                            attacker.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, 
                                attacker.getLocation().add(0, 1, 0), 8, 0.375, 0.375, 0.375, 0.1);
                            attacker.getWorld().spawnParticle(Particle.CRIT, attacker.getLocation(), 5, 0.3125, 0.3125, 0.3125, 0.0625);
                        }
                    }
                }
                break;
                
            case "shield":
                // Balanced for 1 CE per item - moderate chance and reduction
                if (random.nextDouble() < (0.2 + (level * 0.05))) { // Level 1: 25%, Level 2: 30%, Level 3: 35%
                    double reduction = 0.25 + (level * 0.1); // Level 1: 35%, Level 2: 45%, Level 3: 55%
                    event.setDamage(event.getDamage() * (1.0 - reduction));
                    player.playSound(loc, Sound.ITEM_SHIELD_BLOCK, 0.625f, 0.8f);
                    if (loc.getWorld() != null) {
                        loc.getWorld().spawnParticle(Particle.ITEM, loc.add(0, 1, 0), 5, 0.375, 0.375, 0.375, 0.0625);
                        loc.getWorld().spawnParticle(Particle.ENCHANTED_HIT, loc, 6, 0.3125, 0.3125, 0.3125, 0.0375);
                    }
                }
                break;
                
            case "tank":
                // Balanced for 1 CE per item - moderate passive reduction
                double reduction = 0.1 + (level * 0.05); // Level 1: 15%, Level 2: 20%, Level 3: 25%
                event.setDamage(event.getDamage() * (1.0 - reduction));
                if (random.nextDouble() < 0.2) {
                    player.playSound(loc, Sound.BLOCK_ANVIL_LAND, 0.375f, 1.2f);
                    if (loc.getWorld() != null) {
                        loc.getWorld().spawnParticle(Particle.BLOCK, loc.add(0, 1, 0), 3, 0.25, 0.25, 0.25, 0.0375);
                    }
                }
                break;
                
            case "immortal":
                // Balanced for 1 CE per item - lower chance and less healing
                if (player.getHealth() <= 2.0 && random.nextDouble() < (0.3 + (level * 0.05))) { // Level 1: 35%, Level 2: 40%, Level 3: 45%
                    event.setCancelled(true);
                    double newHealth = Math.min(player.getHealth() + (1.5 + level * 0.5), player.getMaxHealth()); // Level 1: 2.0, Level 2: 2.5, Level 3: 3.0
                    player.setHealth(newHealth);
                    player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.75f, 0.5f);
                    if (loc.getWorld() != null) {
                        loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.add(0, 1, 0), 15, 0.625, 0.625, 0.625, 0.125);
                        loc.getWorld().spawnParticle(Particle.HEART, loc.add(0, 1.5, 0), 8, 0.375, 0.375, 0.375, 0.0625);
                    }
                }
                break;
        }
    }
    
    private void handleToolEffects(Player player, PandoraEnchant enchant, int level, BlockBreakEvent event) {
        String name = enchant.getNamespacedName();
        Block block = event.getBlock();
        Location loc = block.getLocation();
        
        
        switch (name) {
            case "vein_miner":
                if (ORE_TYPES.contains(block.getType()) && random.nextDouble() < (0.3 + (level * 0.1))) {
                    player.playSound(loc, Sound.BLOCK_STONE_BREAK, 0.5f, 0.9f);
                    if (loc.getWorld() != null) {
                        loc.getWorld().spawnParticle(Particle.BLOCK, loc.add(0.5, 0.5, 0.5), 5, 0.25, 0.25, 0.25, 0.0625);
                    }
                    breakConnectedOres(player, block, level);
                }
                break;
                
            case "autosmelt":
                player.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.375f, 0.8f);
                if (loc.getWorld() != null) {
                    loc.getWorld().spawnParticle(Particle.FLAME, loc.add(0.5, 0.5, 0.5), 3, 0.25, 0.25, 0.25, 0.0375);
                    loc.getWorld().spawnParticle(Particle.SMOKE, loc, 4, 0.1875, 0.1875, 0.1875, 0.0125);
                }
                break;
                
            case "experience":
                if (ORE_TYPES.contains(block.getType()) && random.nextDouble() < (0.4 * level)) {
                    int expAmount = 5 + random.nextInt(10); // Much more XP
                    player.giveExp(expAmount);
                    player.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.625f, 1.0f);
                    if (loc.getWorld() != null) {
                        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.add(0.5, 0.5, 0.5), 5, 0.25, 0.25, 0.25, 0.0625);
                        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 4, 0.1875, 0.1875, 0.1875, 0.0375);
                    }
                }
                break;
                
            case "lumberjack":
                if (WOOD_TYPES.contains(block.getType())) {
                    player.playSound(loc, Sound.BLOCK_WOOD_BREAK, 0.625f, 0.7f);
                    if (loc.getWorld() != null) {
                        loc.getWorld().spawnParticle(Particle.BLOCK, loc.add(0.5, 0.5, 0.5), 4, 0.25, 0.25, 0.25, 0.0375);
                    }
                    breakConnectedLogs(player, block);
                }
                break;
                
            case "magnet":
                // Handled in onPlayerMove
                break;
                
            case "telekinesis":
                // No particles/sounds on passive telekinesis
                break;
                
            case "tunnel":
                if (random.nextDouble() < (0.3 * level)) {
                    player.playSound(loc, Sound.BLOCK_STONE_BREAK, 0.375f, 1.0f);
                    BlockFace face = getBlockFaceFromYaw(player.getLocation().getYaw());
                    breakTunnel(player, block, face, level);
                }
                break;
                
            case "fortune_plus":
                // No particles/sounds on passive fortune
                break;
                
            case "jackhammer":
                if (random.nextDouble() < (0.4 * level)) {
                    player.playSound(loc, Sound.BLOCK_STONE_BREAK, 0.625f, 0.6f);
                    if (loc.getWorld() != null) {
                        loc.getWorld().spawnParticle(Particle.BLOCK, loc.add(0.5, 0.5, 0.5), 6, 0.375, 0.375, 0.375, 0.0625);
                        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 3, 0.1875, 0.1875, 0.1875, 0.0375);
                    }
                    breakArea(player, block, level);
                }
                break;
                
            case "excavator":
                // Only work on pickaxes
                ItemStack tool = player.getInventory().getItemInMainHand();
                if (tool == null || tool.getType().isAir()) {
                    break;
                }
                Material toolType = tool.getType();
                if (!toolType.name().contains("PICKAXE")) {
                    break;
                }
                // Always trigger for excavator (no random chance)
                player.playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 0.7f, 1.2f);
                player.playSound(loc, Sound.BLOCK_STONE_BREAK, 0.8f, 0.8f);
                if (loc.getWorld() != null) {
                    loc.getWorld().spawnParticle(Particle.EXPLOSION, loc.add(0.5, 0.5, 0.5), 8, 0.5, 0.5, 0.5, 0.1);
                    loc.getWorld().spawnParticle(Particle.BLOCK, loc.add(0.5, 0.5, 0.5), 10, 0.4, 0.4, 0.4, 0.08);
                    loc.getWorld().spawnParticle(Particle.CRIT, loc, 12, 0.3, 0.3, 0.3, 0.05);
                }
                breakExcavatorArea(player, block, level);
                break;
                
            case "efficiency_plus":
                // No particles/sounds on passive efficiency
                break;
            default:
                break;
        }
    }
    
    private void checkArmorEffects(Player player) {
        if (player == null || !player.isOnline()) return;
        
        ItemStack[] armor = player.getInventory().getArmorContents();
        
        for (ItemStack piece : armor) {
            if (piece == null || piece.getType().isAir()) continue;
            
            PandoraEnchant enchant = EnchantmentStorage.getEnchant(piece);
            if (enchant == null) continue;
            
            int level = EnchantmentStorage.getEnchantLevel(piece, enchant);
            if (level < 1) continue;
            
            handleArmorEffects(player, enchant, level);
        }
    }
    
    private void handleArmorEffects(Player player, PandoraEnchant enchant, int level) {
        if (player == null || enchant == null || !player.isOnline()) {
            return;
        }
        
        String name = enchant.getNamespacedName();
        Location loc = player.getLocation();
        
        
        try {
        switch (name) {
            case "regeneration":
                    if (player.getHealth() < player.getMaxHealth() && random.nextDouble() < (0.08 * level)) {
                        double heal = 0.5 + (level * 0.25); // Much stronger healing
                        double newHealth = Math.min(player.getHealth() + heal, player.getMaxHealth());
                        player.setHealth(newHealth);
                        // No particles/sounds on passive regen
                }
                break;
                
            case "night_vision":
                    // NO POTION EFFECTS - no particles/sounds on passive
                break;
                
            case "fire_resistance":
                    // NO POTION EFFECTS - just prevent fire damage in event handler
                break;
                
            case "absorption":
                    if (random.nextDouble() < (0.05 * level)) {
                        // Grant absorption hearts via visual only (no potion)
                        // No particles/sounds on passive
                }
                break;
                
            case "thorns":
                // Thorns handled in damage event
                break;
                
            case "reflex":
                    // NO POTION EFFECTS - no particles/sounds on passive
                break;
                
            case "gears":
                    // NO POTION EFFECTS - no particles/sounds on passive
                break;
                
            case "clarity":
                // Remove negative effects periodically
                    if (random.nextDouble() < 0.05) {
                        // NO POTION EFFECTS - no particles/sounds on passive
                }
                break;
                
            case "drunk":
                    // NO POTION EFFECTS - no particles/sounds on passive
                break;
                
            case "water_walker":
                    if (player.isInWater() || loc.getBlock().getType() == Material.WATER || 
                        loc.getBlock().getType() == Material.WATER_CAULDRON) {
                        // Keep player on water surface
                        if (player.getLocation().getY() % 1.0 < 0.1) {
                            Vector velocity = player.getVelocity();
                            if (velocity.getY() < 0) {
                                velocity.setY(0);
                                player.setVelocity(velocity);
                            }
                        }
                        // No particles/sounds on passive
                }
                break;
                
            case "auto_feed":
                    handleAutoFeed(player, level);
                break;
                
            case "feather_fall":
                // Reduces fall damage - handled in fall damage event
                break;
                default:
                break;
            }
        } catch (Exception e) {
            Logger.error("Error handling armor effect " + name + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleAutoFeed(Player player, int level) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        // Check cooldown (feed every 2-4 seconds based on level)
        if (lastAutoFeed.containsKey(uuid)) {
            long timeSinceLastFeed = now - lastAutoFeed.get(uuid);
            long cooldown = 4000 - (level * 500); // 4s base, -0.5s per level
            if (timeSinceLastFeed < cooldown) {
                return;
            }
        }
        
        // Check if player needs food
        int foodLevel = player.getFoodLevel();
        if (foodLevel >= 20) return;
        
        // Find food in inventory
        ItemStack[] contents = player.getInventory().getContents();
        for (ItemStack item : contents) {
            if (item != null && item.getType().isEdible()) {
                // Consume food
                int amount = item.getAmount();
                if (amount > 1) {
                    item.setAmount(amount - 1);
                } else {
                    player.getInventory().removeItem(item);
                }
                
                // Apply food effect
                Material foodType = item.getType();
                int foodValue = getFoodValue(foodType);
                int newFoodLevel = Math.min(foodLevel + foodValue, 20);
                player.setFoodLevel(newFoodLevel);
                
                // No visual feedback on passive auto-feed
                
                lastAutoFeed.put(uuid, now);
                return;
            }
        }
    }
    
    private int getFoodValue(Material food) {
        switch (food) {
            case BREAD: case COOKED_PORKCHOP: case COOKED_BEEF: case COOKED_CHICKEN:
            case COOKED_MUTTON: case COOKED_RABBIT: case BAKED_POTATO:
                return 5;
            case APPLE: case GOLDEN_CARROT: case COOKED_COD: case COOKED_SALMON:
                return 4;
            case CARROT: case POTATO: case BEETROOT:
                return 3;
            default:
                return 2;
        }
    }
    
    private void handleMagnet(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean hasMagnet = false;
        int level = 0;
        
        for (ItemStack piece : armor) {
            if (piece == null || piece.getType().isAir()) continue;
            PandoraEnchant enchant = EnchantmentStorage.getEnchant(piece);
            if (enchant != null && enchant.getNamespacedName().equals("magnet")) {
                hasMagnet = true;
                level = EnchantmentStorage.getEnchantLevel(piece, enchant);
                break;
            }
        }
        
        if (!hasMagnet || level < 1) return;
        
        double radius = 5.0 + (level * 2.0); // Much larger radius
        Location loc = player.getLocation();
        
        if (loc.getWorld() == null) return;
        
        int attracted = 0;
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (entity instanceof Item) {
                Item item = (Item) entity;
                if (item.getPickupDelay() <= 0) {
                    // Attract item to player
                    Location itemLoc = item.getLocation();
                    Vector direction = loc.toVector().subtract(itemLoc.toVector()).normalize();
                    double speed = 0.5 + (level * 0.1); // Faster attraction
                    item.setVelocity(direction.multiply(speed));
                    attracted++;
                }
            }
        }
        
        // No particles/sounds on passive magnet effect
    }
    
    private void breakConnectedOres(Player player, Block startBlock, int level) {
        UUID uuid = player.getUniqueId();
        Set<Location> processed = veinMiningBlocks.getOrDefault(uuid, new HashSet<>());
        Material oreType = startBlock.getType();
        
        if (!ORE_TYPES.contains(oreType)) return;
        
        int maxBlocks = 20 + (level * 10); // Much more blocks
        Queue<Block> queue = new LinkedList<>();
        queue.add(startBlock);
        processed.add(startBlock.getLocation());
        
        while (!queue.isEmpty() && processed.size() < maxBlocks) {
            Block current = queue.poll();
            if (current == null) continue;
            
            // Break block
            if (current.getType() == oreType && !current.getLocation().equals(startBlock.getLocation())) {
                current.breakNaturally(player.getInventory().getItemInMainHand());
                if (current.getWorld() != null && random.nextDouble() < 0.3) {
                    current.getWorld().spawnParticle(Particle.BLOCK, current.getLocation().add(0.5, 0.5, 0.5), 2, 0.15, 0.15, 0.15, 0.03);
                }
            }
            
            // Check adjacent blocks
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        Block adjacent = current.getRelative(x, y, z);
                        Location adjLoc = adjacent.getLocation();
                        
                        if (!processed.contains(adjLoc) && adjacent.getType() == oreType) {
                            processed.add(adjLoc);
                            queue.add(adjacent);
                        }
                    }
                }
            }
        }
        
        veinMiningBlocks.put(uuid, processed);
        new BukkitRunnable() {
            @Override
            public void run() {
                veinMiningBlocks.remove(uuid);
            }
        }.runTaskLater(PandoraEnchants.getInstance(), 20L);
    }
    
    private void breakConnectedLogs(Player player, Block startBlock) {
        Material logType = startBlock.getType();
        if (!WOOD_TYPES.contains(logType)) return;
        
        Set<Location> processed = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();
        queue.add(startBlock);
        processed.add(startBlock.getLocation());
        
        int maxBlocks = 100; // Much larger trees
        
        while (!queue.isEmpty() && processed.size() < maxBlocks) {
            Block current = queue.poll();
            if (current == null) continue;
            
            // Break block (except the one already broken)
            if (!current.getLocation().equals(startBlock.getLocation())) {
                current.breakNaturally(player.getInventory().getItemInMainHand());
                if (current.getWorld() != null && random.nextDouble() < 0.3) {
                    current.getWorld().spawnParticle(Particle.BLOCK, current.getLocation().add(0.5, 0.5, 0.5), 1, 0.15, 0.15, 0.15, 0.03);
                }
            }
            
            // Check adjacent blocks
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        Block adjacent = current.getRelative(x, y, z);
                        Location adjLoc = adjacent.getLocation();
                        
                        if (!processed.contains(adjLoc) && WOOD_TYPES.contains(adjacent.getType())) {
                            processed.add(adjLoc);
                            queue.add(adjacent);
                        }
                    }
                }
            }
        }
    }
    
    private void breakTunnel(Player player, Block startBlock, BlockFace face, int level) {
        int length = 5 + (level * 2); // Much longer tunnels
        ItemStack tool = player.getInventory().getItemInMainHand();
        
        for (int i = 1; i <= length; i++) {
            Block block = startBlock.getRelative(face, i);
            if (block.getType().isAir() || block.getType() == Material.BEDROCK) continue;
            block.breakNaturally(tool);
            if (block.getWorld() != null && random.nextDouble() < 0.3) {
                block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 1, 0.15, 0.15, 0.15, 0.03);
            }
        }
    }
    
    private void breakArea(Player player, Block center, int level) {
        int size = 2 + level; // 3x3 at level 1, 4x4 at level 2, etc.
        ItemStack tool = player.getInventory().getItemInMainHand();
        
        for (int x = -size; x <= size; x++) {
            for (int y = -size; y <= size; y++) {
                for (int z = -size; z <= size; z++) {
                    if (x == 0 && y == 0 && z == 0) continue; // Skip center (already broken)
                    Block block = center.getRelative(x, y, z);
                    if (block.getType().isAir() || block.getType() == Material.BEDROCK) continue;
                    block.breakNaturally(tool);
                    if (block.getWorld() != null && random.nextDouble() < 0.3) {
                        block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 2, 0.2, 0.2, 0.2, 0.05);
                    }
                }
            }
        }
    }
    
    private void breakExcavatorArea(Player player, Block center, int level) {
        // Determine area size based on level
        // Level 1: 3x3, Level 2: 4x4, Level 3: 5x5, Level 4: 6x6, Level 5: 8x8
        int minOffset, maxOffset;
        switch (level) {
            case 1:
                minOffset = -1;
                maxOffset = 1; // 3x3
                break;
            case 2:
                minOffset = -1;
                maxOffset = 2; // 4x4
                break;
            case 3:
                minOffset = -2;
                maxOffset = 2; // 5x5
                break;
            case 4:
                minOffset = -2;
                maxOffset = 3; // 6x6
                break;
            case 5:
                minOffset = -3;
                maxOffset = 4; // 8x8
                break;
            default:
                minOffset = -1;
                maxOffset = 1;
                break;
        }
        
        ItemStack tool = player.getInventory().getItemInMainHand();
        Material centerType = center.getType();
        boolean isFarmingMaterial = FARMING_MATERIALS.contains(centerType);
        
        for (int x = minOffset; x <= maxOffset; x++) {
            for (int y = minOffset; y <= maxOffset; y++) {
                for (int z = minOffset; z <= maxOffset; z++) {
                    if (x == 0 && y == 0 && z == 0) continue; // Skip center (already broken)
                    
                    Block block = center.getRelative(x, y, z);
                    Material blockType = block.getType();
                    
                    // Skip air and bedrock
                    if (blockType.isAir() || blockType == Material.BEDROCK) continue;
                    
                    // If it's a farming material, only break the same type
                    if (isFarmingMaterial) {
                        if (blockType == centerType) {
                            block.breakNaturally(tool);
                            if (block.getWorld() != null && random.nextDouble() < 0.4) {
                                block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 2, 0.2, 0.2, 0.2, 0.05);
                            }
                        }
                    } else {
                        // Not a farming material, break everything in the area
                        block.breakNaturally(tool);
                        if (block.getWorld() != null && random.nextDouble() < 0.3) {
                            block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 2, 0.2, 0.2, 0.2, 0.05);
                        }
                    }
                }
            }
        }
    }
    
    private void handleAutoSmelt(BlockDropItemEvent event, Block block) {
        Material blockType = block.getType();
        Material smeltedType = getSmeltedResult(blockType);
        
        if (smeltedType == null) return; // Not smeltable
        
        // Remove original drops
        event.getItems().clear();
        
        // Add smelted result
        ItemStack smelted = new ItemStack(smeltedType);
        HashMap<Integer, ItemStack> leftover = event.getPlayer().getInventory().addItem(smelted);
        
        // Drop any leftover items
        for (ItemStack left : leftover.values()) {
            block.getWorld().dropItemNaturally(block.getLocation(), left);
        }
        
        // Visual effects
        if (block.getWorld() != null) {
            block.getWorld().spawnParticle(Particle.FLAME, block.getLocation().add(0.5, 0.5, 0.5), 4, 0.25, 0.25, 0.25, 0.0375);
            block.getWorld().spawnParticle(Particle.SMOKE, block.getLocation(), 3, 0.1875, 0.1875, 0.1875, 0.0125);
        }
    }
    
    private Material getSmeltedResult(Material ore) {
        switch (ore) {
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
                return Material.IRON_INGOT;
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
            case NETHER_GOLD_ORE:
                return Material.GOLD_INGOT;
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
                return Material.COPPER_INGOT;
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
                return Material.COAL;
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
                return Material.DIAMOND;
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
                return Material.EMERALD;
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
                return Material.LAPIS_LAZULI;
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
                return Material.REDSTONE;
            case NETHER_QUARTZ_ORE:
                return Material.QUARTZ;
            case ANCIENT_DEBRIS:
                return Material.NETHERITE_SCRAP;
            case RAW_IRON:
                return Material.IRON_INGOT;
            case RAW_COPPER:
                return Material.COPPER_INGOT;
            case RAW_GOLD:
                return Material.GOLD_INGOT;
            default:
                return null;
        }
    }
    
    private void handleTelekinesis(BlockDropItemEvent event, Player player) {
        // Move all items directly to player inventory
        for (Item item : event.getItems()) {
            ItemStack stack = item.getItemStack();
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            
            // Drop any leftover items that don't fit
            for (ItemStack left : leftover.values()) {
                event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), left);
            }
            
            item.remove();
        }
        
        // Clear the drop list since we handled it manually
        event.getItems().clear();
        
        // Visual effects
        Location loc = event.getBlock().getLocation();
        player.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.375f, 1.8f);
        if (loc.getWorld() != null) {
            loc.getWorld().spawnParticle(Particle.PORTAL, loc.add(0.5, 0.5, 0.5), 6, 0.25, 0.25, 0.25, 0.0625);
        }
    }
    
    private BlockFace getBlockFaceFromYaw(float yaw) {
        yaw = (yaw % 360 + 360) % 360;
        if (yaw >= 315 || yaw < 45) return BlockFace.SOUTH;
        if (yaw >= 45 && yaw < 135) return BlockFace.WEST;
        if (yaw >= 135 && yaw < 225) return BlockFace.NORTH;
        return BlockFace.EAST;
    }
    
    /**
     * Apply damage over time without potion effects
     */
    private void applyDamageOverTime(LivingEntity target, double damagePerTick, int ticks, String type) {
        new BukkitRunnable() {
            int currentTick = 0;
            final LivingEntity entity = target;
            final String effectType = type;
            
            @Override
            public void run() {
                if (entity.isDead() || !entity.isValid() || currentTick >= ticks) {
                    cancel();
                    return;
                }
                
                if (currentTick % 20 == 0) {
                    entity.damage(damagePerTick);
                    Location loc = entity.getLocation();
                    
                    if (loc.getWorld() != null) {
                        switch (effectType) {
                            case "poison":
                                loc.getWorld().spawnParticle(Particle.ITEM_SLIME, loc.add(0, 1, 0), 4, 0.25, 0.25, 0.25, 0.0375);
                                break;
                            case "wither":
                                loc.getWorld().spawnParticle(Particle.SMOKE, loc.add(0, 1, 0), 5, 0.3125, 0.3125, 0.3125, 0.0375);
                                loc.getWorld().spawnParticle(Particle.SOUL, loc, 3, 0.25, 0.25, 0.25, 0.025);
                                break;
                            case "venom":
                                loc.getWorld().spawnParticle(Particle.ITEM_SLIME, loc.add(0, 1, 0), 4, 0.25, 0.25, 0.25, 0.0375);
                                loc.getWorld().spawnParticle(Particle.ENCHANT, loc, 3, 0.1875, 0.1875, 0.1875, 0.0125);
                                break;
                        }
                        loc.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, loc.add(0, 1, 0), 3, 0.25, 0.25, 0.25, 0.0625);
                    }
                }
                currentTick++;
            }
        }.runTaskTimer(PandoraEnchants.getInstance(), 0L, 1L);
    }
}
