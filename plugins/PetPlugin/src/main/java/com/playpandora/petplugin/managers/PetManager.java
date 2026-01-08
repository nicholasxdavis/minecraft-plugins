package com.playpandora.petplugin.managers;

import com.playpandora.petplugin.PetPlugin;
import com.playpandora.petplugin.models.Pet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class PetManager {
    
    private final PetPlugin plugin;
    private final Map<UUID, Pet> activePets = new HashMap<>(); // Player UUID -> Active Pet
    private final Map<UUID, LivingEntity> petEntities = new HashMap<>(); // Pet UUID -> Entity
    private BukkitTask healthMonitorTask;
    
    public PetManager(PetPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void startHealthMonitoring() {
        healthMonitorTask = new BukkitRunnable() {
            @Override
            public void run() {
                monitorPetHealth();
                healPets();
            }
        }.runTaskTimer(plugin, 0L, 20L); // Every second
    }
    
    private void monitorPetHealth() {
        double threshold = plugin.getConfig().getDouble("pets.run-away-health-threshold", 0.2);
        
        Iterator<Map.Entry<UUID, Pet>> iterator = activePets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Pet> entry = iterator.next();
            UUID playerUUID = entry.getKey();
            Pet pet = entry.getValue();
            LivingEntity entity = petEntities.get(pet.getEntityUUID());
            
            if (entity == null || entity.isDead()) {
                handlePetDeath(playerUUID, pet);
                iterator.remove();
                continue;
            }
            
            // Update pet health in data model
            pet.setCurrentHealth(entity.getHealth());
            pet.setMaxHealth(entity.getMaxHealth());
            plugin.getDataManager().updatePetHealth(playerUUID, pet);
            
            double healthPercent = entity.getHealth() / entity.getMaxHealth();
            
            if (healthPercent <= threshold) {
                // Make pet run away
                makePetRunAway(playerUUID, pet, entity);
            }
        }
    }
    
    private void healPets() {
        double healRate = plugin.getConfig().getDouble("pets.auto-heal-rate", 0.5);
        
        for (Map.Entry<UUID, Pet> entry : activePets.entrySet()) {
            UUID playerUUID = entry.getKey();
            Pet pet = entry.getValue();
            LivingEntity entity = petEntities.get(pet.getEntityUUID());
            
            if (entity != null && !entity.isDead()) {
                double currentHealth = entity.getHealth();
                double maxHealth = entity.getMaxHealth();
                
                if (currentHealth < maxHealth) {
                    entity.setHealth(Math.min(maxHealth, currentHealth + healRate));
                }
                
                // Update pet health in data model
                pet.setCurrentHealth(entity.getHealth());
                pet.setMaxHealth(maxHealth);
                
                // Mark for save (will be saved on next auto-save)
                plugin.getDataManager().updatePetHealth(playerUUID, pet);
            }
        }
    }
    
    private void makePetRunAway(UUID playerUUID, Pet pet, LivingEntity entity) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) {
            return;
        }
        
        // Make pet run away from player
        Location playerLoc = player.getLocation();
        Location petLoc = entity.getLocation();
        
        // Calculate direction away from player
        double dx = petLoc.getX() - playerLoc.getX();
        double dz = petLoc.getZ() - playerLoc.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        if (distance > 0) {
            double speed = 1.5;
            Location targetLoc = petLoc.clone().add(
                (dx / distance) * 10,
                0,
                (dz / distance) * 10
            );
            
            if (entity instanceof org.bukkit.entity.Sittable sittable) {
                sittable.setSitting(false);
            }
            
            // Teleport pet away
            entity.teleport(targetLoc);
            
            // Send message to player
            String message = plugin.formatMessage("messages.pet-low-health", 
                "{prefix}Your pet is low on health and has run away!");
            player.sendMessage(message);
            
            // Despawn the pet
            despawnPet(playerUUID);
        }
    }
    
    private void handlePetDeath(UUID playerUUID, Pet pet) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            String message = plugin.formatMessage("messages.pet-died",
                "{prefix}Your pet has died! Use &6/pet revive " + pet.getDisplayName() + 
                " &7within 6 hours to revive it for &6$50,000&7.");
            player.sendMessage(message);
            
            // Remove effects
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
        
        // Mark pet as dead (not removed, so it can be revived)
        plugin.getDataManager().markPetAsDead(playerUUID, pet);
        
        // Remove entity
        if (pet.getEntityUUID() != null) {
            petEntities.remove(pet.getEntityUUID());
        }
    }
    
    public boolean spawnPet(Player player, Pet pet) {
        // Check if player already has a pet spawned
        if (activePets.containsKey(player.getUniqueId())) {
            String message = plugin.formatMessage("messages.pet-already-spawned",
                "{prefix}You already have a pet spawned. Despawn it first with &6/pet despawn&7.");
            player.sendMessage(message);
            return false;
        }
        
        Location spawnLoc = player.getLocation().add(2, 0, 2);
        LivingEntity entity = (LivingEntity) player.getWorld().spawnEntity(spawnLoc, pet.getEntityType());
        
        // Configure pet based on type
        configurePet(entity, pet.getPetType());
        
        // Set pet name
        String displayName = pet.getDisplayName();
        entity.setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&6" + displayName));
        entity.setCustomNameVisible(true);
        
        // Make pet follow player
        if (entity instanceof Tameable tameable) {
            tameable.setOwner(player);
            tameable.setTamed(true);
        }
        
        // Make sure pet doesn't sit
        if (entity instanceof org.bukkit.entity.Sittable sittable) {
            sittable.setSitting(false);
        }
        
        // Start following task for better pet behavior
        startFollowingTask(entity, player);
        
        // Store references
        pet.setEntityUUID(entity.getUniqueId());
        activePets.put(player.getUniqueId(), pet);
        petEntities.put(entity.getUniqueId(), entity);
        
        // Apply special abilities
        applyPetAbilities(entity, pet.getPetType(), player);
        
        String message = plugin.formatMessage("messages.pet-spawned",
            "{prefix}Your pet &6{pet_name} &7has been spawned!",
            "pet_name", displayName);
        player.sendMessage(message);
        
        return true;
    }
    
    private void configurePet(LivingEntity entity, String petType) {
        ConfigurationSection petConfig = plugin.getConfig().getConfigurationSection("pet-types." + petType);
        if (petConfig == null) {
            return;
        }
        
        // Set max health
        double maxHealth = petConfig.getDouble("max-health", 20.0);
        org.bukkit.attribute.AttributeInstance healthAttr = entity.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(maxHealth);
            entity.setHealth(maxHealth);
        }
        
        // Configure based on pet type
        switch (petType.toLowerCase()) {
            case "horse" -> {
                if (entity instanceof Horse horse) {
                    double speed = petConfig.getDouble("movement-speed", 0.35);
                    double jump = petConfig.getDouble("jump-strength", 1.0);
                    org.bukkit.attribute.AttributeInstance speedAttr = horse.getAttribute(Attribute.MOVEMENT_SPEED);
                    if (speedAttr != null) {
                        speedAttr.setBaseValue(speed);
                    }
                    horse.setJumpStrength(jump);
                    horse.setTamed(true);
                    // Add saddle so horse is ready to ride
                    org.bukkit.inventory.ItemStack saddle = new org.bukkit.inventory.ItemStack(org.bukkit.Material.SADDLE);
                    horse.getInventory().setSaddle(saddle);
                }
            }
            case "dog" -> {
                if (entity instanceof Wolf wolf) {
                    double attackDamage = petConfig.getDouble("attack-damage", 8.0);
                    double attackSpeed = petConfig.getDouble("attack-speed", 1.5);
                    org.bukkit.attribute.AttributeInstance attackDamageAttr = wolf.getAttribute(Attribute.ATTACK_DAMAGE);
                    if (attackDamageAttr != null) {
                        attackDamageAttr.setBaseValue(attackDamage);
                    }
                    org.bukkit.attribute.AttributeInstance attackSpeedAttr = wolf.getAttribute(Attribute.ATTACK_SPEED);
                    if (attackSpeedAttr != null) {
                        attackSpeedAttr.setBaseValue(attackSpeed);
                    }
                    double followRange = petConfig.getDouble("follow-range", 32.0);
                    org.bukkit.attribute.AttributeInstance followRangeAttr = wolf.getAttribute(Attribute.FOLLOW_RANGE);
                    if (followRangeAttr != null) {
                        followRangeAttr.setBaseValue(followRange);
                    }
                    wolf.setTamed(true);
                    wolf.setAngry(false);
                }
            }
            case "cat" -> {
                if (entity instanceof Cat cat) {
                    cat.setTamed(true);
                }
            }
            case "wolf" -> {
                if (entity instanceof Wolf wolf) {
                    double attackDamage = petConfig.getDouble("attack-damage", 6.0);
                    double attackSpeed = petConfig.getDouble("attack-speed", 1.3);
                    org.bukkit.attribute.AttributeInstance attackDamageAttr = wolf.getAttribute(Attribute.ATTACK_DAMAGE);
                    if (attackDamageAttr != null) {
                        attackDamageAttr.setBaseValue(attackDamage);
                    }
                    org.bukkit.attribute.AttributeInstance attackSpeedAttr = wolf.getAttribute(Attribute.ATTACK_SPEED);
                    if (attackSpeedAttr != null) {
                        attackSpeedAttr.setBaseValue(attackSpeed);
                    }
                    wolf.setTamed(true);
                }
            }
        }
        
        // Prevent damage if configured
        if (plugin.getConfig().getBoolean("pets.prevent-damage", true)) {
            entity.setInvulnerable(true);
        }
        
        // Pets are visible but unkillable (except by other real players)
    }
    
    private void applyPetAbilities(LivingEntity entity, String petType, Player player) {
        ConfigurationSection petConfig = plugin.getConfig().getConfigurationSection("pet-types." + petType);
        if (petConfig == null) {
            return;
        }
        
        switch (petType.toLowerCase()) {
            case "cat" -> {
                int speedLevel = petConfig.getInt("speed-boost-level", 1);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, speedLevel - 1, true, false));
                // Give 2 extra hearts (4 HP) when cat is spawned
                double currentMaxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(currentMaxHealth + 4.0);
                player.setHealth(Math.min(player.getHealth() + 4.0, player.getAttribute(Attribute.MAX_HEALTH).getValue()));
            }
            case "parrot" -> {
                int nightVisionLevel = petConfig.getInt("night-vision-level", 1);
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, nightVisionLevel - 1, true, false));
            }
            case "fox" -> {
                // Invisibility on sneak is handled in the player toggle sneak event
            }
        }
    }
    
    public boolean hasCatPet(UUID playerUUID) {
        Pet pet = activePets.get(playerUUID);
        return pet != null && pet.getPetType().equalsIgnoreCase("cat");
    }
    
    public int getCatExtraLives(UUID playerUUID) {
        Pet pet = activePets.get(playerUUID);
        if (pet != null && pet.getPetType().equalsIgnoreCase("cat")) {
            ConfigurationSection petConfig = plugin.getConfig().getConfigurationSection("pet-types.cat");
            if (petConfig != null) {
                return petConfig.getInt("extra-lives", 1);
            }
        }
        return 0;
    }
    
    public void despawnPet(UUID playerUUID) {
        Pet pet = activePets.remove(playerUUID);
        if (pet == null) {
            return;
        }
        
        LivingEntity entity = petEntities.remove(pet.getEntityUUID());
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }
        
        // Remove effects from player
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            // Remove cat speed boost
            player.removePotionEffect(PotionEffectType.SPEED);
            // Remove parrot night vision
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            // Remove fox invisibility
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            // Reset extra lives metadata (legacy, kept for compatibility)
            player.removeMetadata("petplugin_extra_lives", plugin);
            
            // Remove cat extra hearts if cat pet
            if (pet.getPetType().equalsIgnoreCase("cat")) {
                double currentMaxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
                double defaultMaxHealth = 20.0; // Default player max health
                if (currentMaxHealth > defaultMaxHealth) {
                    double newMaxHealth = Math.max(defaultMaxHealth, currentMaxHealth - 4.0);
                    player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(newMaxHealth);
                    // Adjust current health if needed
                    if (player.getHealth() > newMaxHealth) {
                        player.setHealth(newMaxHealth);
                    }
                }
            }
        }
    }
    
    public void despawnAllPets() {
        for (UUID playerUUID : new HashSet<>(activePets.keySet())) {
            despawnPet(playerUUID);
        }
    }
    
    public Pet getActivePet(UUID playerUUID) {
        return activePets.get(playerUUID);
    }
    
    public LivingEntity getPetEntity(UUID petUUID) {
        return petEntities.get(petUUID);
    }
    
    /**
     * Get the owner UUID of a pet entity
     * @param entityUUID The UUID of the pet entity
     * @return The owner's UUID, or null if not found
     */
    public UUID getPetOwner(UUID entityUUID) {
        for (Map.Entry<UUID, Pet> entry : activePets.entrySet()) {
            Pet pet = entry.getValue();
            if (pet.getEntityUUID() != null && pet.getEntityUUID().equals(entityUUID)) {
                return entry.getKey(); // Return player UUID (owner)
            }
        }
        return null;
    }
    
    public void teleportPetToPlayer(Player player) {
        Pet pet = activePets.get(player.getUniqueId());
        if (pet == null) {
            return;
        }
        
        LivingEntity entity = petEntities.get(pet.getEntityUUID());
        if (entity == null || entity.isDead()) {
            return;
        }
        
        double distance = entity.getLocation().distance(player.getLocation());
        double teleportDistance = plugin.getConfig().getDouble("pets.teleport-distance", 50.0);
        
        if (distance > teleportDistance) {
            Location teleportLoc = player.getLocation().add(2, 0, 2);
            entity.teleport(teleportLoc);
        }
    }
    
    private void startFollowingTask(LivingEntity entity, Player player) {
        // Task to ensure pet follows player
        new BukkitRunnable() {
            @Override
            public void run() {
                if (entity.isDead() || !entity.isValid()) {
                    cancel();
                    return;
                }
                
                if (player == null || !player.isOnline()) {
                    cancel();
                    return;
                }
                
                // Check if pet is too far
                double distance = entity.getLocation().distance(player.getLocation());
                if (distance > 30) {
                    // Make pet follow
                    if (entity instanceof Tameable tameable && tameable.isTamed()) {
                        if (entity instanceof org.bukkit.entity.Sittable sittable) {
                            sittable.setSitting(false);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 40L); // Check every 2 seconds
    }
}

