package com.playpandora.petplugin.models;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Pet {
    
    private final UUID ownerUUID;
    private final String petType;
    private final String generatedName;
    private String customName;
    private UUID entityUUID;
    private EntityType entityType;
    private double maxHealth;
    private double currentHealth;
    private Long deathTimestamp; // When the pet died (null if alive)
    
    public Pet(UUID ownerUUID, String petType, String generatedName) {
        this.ownerUUID = ownerUUID;
        this.petType = petType;
        this.generatedName = generatedName;
        this.customName = null;
        this.entityUUID = null;
        this.entityType = getEntityTypeForPetType(petType);
        this.maxHealth = 20.0;
        this.currentHealth = maxHealth;
        this.deathTimestamp = null;
    }
    
    public UUID getOwnerUUID() {
        return ownerUUID;
    }
    
    public String getPetType() {
        return petType;
    }
    
    public String getGeneratedName() {
        return generatedName;
    }
    
    public String getCustomName() {
        return customName;
    }
    
    public void setCustomName(String customName) {
        this.customName = customName;
    }
    
    public String getDisplayName() {
        return customName != null ? customName : generatedName;
    }
    
    public UUID getEntityUUID() {
        return entityUUID;
    }
    
    public void setEntityUUID(UUID entityUUID) {
        this.entityUUID = entityUUID;
    }
    
    public EntityType getEntityType() {
        return entityType;
    }
    
    public double getMaxHealth() {
        return maxHealth;
    }
    
    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
        this.currentHealth = Math.min(currentHealth, maxHealth);
    }
    
    public double getCurrentHealth() {
        return currentHealth;
    }
    
    public void setCurrentHealth(double currentHealth) {
        this.currentHealth = Math.max(0, Math.min(currentHealth, maxHealth));
    }
    
    public double getHealthPercentage() {
        return maxHealth > 0 ? currentHealth / maxHealth : 0;
    }
    
    public Long getDeathTimestamp() {
        return deathTimestamp;
    }
    
    public void setDeathTimestamp(Long deathTimestamp) {
        this.deathTimestamp = deathTimestamp;
    }
    
    public boolean isDead() {
        return deathTimestamp != null;
    }
    
    public void revive() {
        this.deathTimestamp = null;
        this.currentHealth = maxHealth;
    }
    
    private EntityType getEntityTypeForPetType(String petType) {
        return switch (petType.toLowerCase()) {
            case "horse" -> EntityType.HORSE;
            case "dog" -> EntityType.WOLF; // Wolves are used as dogs
            case "cat" -> EntityType.CAT;
            case "wolf" -> EntityType.WOLF;
            case "parrot" -> EntityType.PARROT;
            case "fox" -> EntityType.FOX;
            default -> EntityType.WOLF;
        };
    }
}

