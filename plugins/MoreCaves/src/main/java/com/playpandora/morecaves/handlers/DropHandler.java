package com.playpandora.morecaves.handlers;

import com.playpandora.morecaves.MoreCaves;
import com.playpandora.morecaves.utils.*;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class DropHandler implements Listener {
    private final MoreCaves plugin;
    private final ConfigManager config;
    
    public DropHandler(MoreCaves plugin) {
        this.plugin = plugin;
        this.config = new ConfigManager(plugin);
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!CaveChecker.isInCave(event.getEntity().getLocation())) {
            return;
        }
        
        // Give XP to killer if player
        if (event.getEntity().getKiller() != null) {
            plugin.getIntegrationManager().giveKillXP(event.getEntity().getKiller(), 
                (org.bukkit.entity.LivingEntity) event.getEntity());
            plugin.getIntegrationManager().updateQuestProgress(event.getEntity().getKiller(), 
                event.getEntityType().toString());
        }
        
        if (!config.isFeatureEnabled("drop-rates")) {
            return;
        }
        
        EntityType type = event.getEntityType();
        double multiplier = 1.0;
        Material targetMaterial = null;
        
        switch (type) {
            case ZOMBIE:
                multiplier = config.getMultiplier("drop-rates", "zombie-iron-multiplier");
                targetMaterial = Material.IRON_INGOT;
                break;
            case SKELETON:
                multiplier = config.getMultiplier("drop-rates", "skeleton-arrow-bone-multiplier");
                // Handle both arrows and bones
                for (ItemStack drop : event.getDrops()) {
                    if (drop.getType() == Material.ARROW || drop.getType() == Material.BONE) {
                        int bonus = (int) (drop.getAmount() * (multiplier - 1.0));
                        if (bonus > 0) {
                            drop.setAmount(drop.getAmount() + bonus);
                        }
                    }
                }
                return;
            case CREEPER:
                multiplier = config.getMultiplier("drop-rates", "creeper-gunpowder-multiplier");
                targetMaterial = Material.GUNPOWDER;
                break;
            case SPIDER:
                multiplier = config.getMultiplier("drop-rates", "spider-string-multiplier");
                targetMaterial = Material.STRING;
                break;
        }
        
        if (targetMaterial != null && multiplier > 1.0) {
            Collection<ItemStack> drops = event.getDrops();
            int baseCount = 0;
            
            for (ItemStack drop : drops) {
                if (drop.getType() == targetMaterial) {
                    baseCount += drop.getAmount();
                }
            }
            
            int bonus = (int) (baseCount * (multiplier - 1.0));
            if (bonus > 0) {
                drops.add(new ItemStack(targetMaterial, bonus));
            }
        }
    }
}

