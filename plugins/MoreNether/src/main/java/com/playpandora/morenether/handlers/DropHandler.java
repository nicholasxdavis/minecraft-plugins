package com.playpandora.morenether.handlers;

import com.playpandora.morenether.MoreNether;
import com.playpandora.morenether.utils.ConfigManager;
import com.playpandora.morenether.utils.DimensionChecker;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class DropHandler implements Listener {
    private final MoreNether plugin;
    private final ConfigManager config;
    
    public DropHandler(MoreNether plugin) {
        this.plugin = plugin;
        this.config = new ConfigManager(plugin);
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!DimensionChecker.isNether(event.getEntity().getLocation())) {
            return;
        }
        
        // Give XP to killer if player
        if (event.getEntity().getKiller() != null) {
            plugin.getIntegrationManager().giveKillXP(event.getEntity().getKiller(), 
                (org.bukkit.entity.LivingEntity) event.getEntity());
            // Update quest progress
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
            case BLAZE:
                multiplier = config.getMultiplier("drop-rates", "blaze-rod-multiplier");
                targetMaterial = Material.BLAZE_ROD;
                break;
            case GHAST:
                multiplier = config.getMultiplier("drop-rates", "ghast-tear-multiplier");
                targetMaterial = Material.GHAST_TEAR;
                break;
            case WITHER_SKELETON:
                multiplier = config.getMultiplier("drop-rates", "wither-skull-multiplier");
                targetMaterial = Material.WITHER_SKELETON_SKULL;
                break;
            case PIGLIN:
            case PIGLIN_BRUTE:
                multiplier = config.getMultiplier("drop-rates", "gold-multiplier");
                targetMaterial = Material.GOLD_NUGGET;
                break;
            case MAGMA_CUBE:
                multiplier = config.getMultiplier("drop-rates", "magma-cream-multiplier");
                targetMaterial = Material.MAGMA_CREAM;
                break;
        }
        
        if (targetMaterial != null && multiplier > 1.0) {
            // Add bonus drops based on multiplier
            Collection<ItemStack> drops = event.getDrops();
            int baseCount = 0;
            
            // Count existing drops of this material
            for (ItemStack drop : drops) {
                if (drop.getType() == targetMaterial) {
                    baseCount += drop.getAmount();
                }
            }
            
            // Calculate bonus
            int bonus = (int) (baseCount * (multiplier - 1.0));
            if (bonus > 0) {
                drops.add(new ItemStack(targetMaterial, bonus));
            }
        }
        
        // Special handling for Hoglins (pork + leather)
        if (type == EntityType.HOGLIN) {
            double hoglinMultiplier = config.getMultiplier("drop-rates", "hoglin-drops-multiplier");
            if (hoglinMultiplier > 1.0) {
                for (ItemStack drop : event.getDrops()) {
                    if (drop.getType() == Material.PORKCHOP || drop.getType() == Material.LEATHER) {
                        int bonus = (int) (drop.getAmount() * (hoglinMultiplier - 1.0));
                        if (bonus > 0) {
                            drop.setAmount(drop.getAmount() + bonus);
                        }
                    }
                }
            }
        }
    }
}

