package com.playpandora.moreend.handlers;

import com.playpandora.moreend.MoreEnd;
import com.playpandora.moreend.utils.ConfigManager;
import com.playpandora.moreend.utils.DimensionChecker;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class DropHandler implements Listener {
    private final MoreEnd plugin;
    private final ConfigManager config;
    
    public DropHandler(MoreEnd plugin) {
        this.plugin = plugin;
        this.config = new ConfigManager(plugin);
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!DimensionChecker.isEnd(event.getEntity().getLocation())) {
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
            case ENDERMAN:
                multiplier = config.getMultiplier("drop-rates", "enderman-pearl-multiplier");
                targetMaterial = Material.ENDER_PEARL;
                break;
            case SHULKER:
                multiplier = config.getMultiplier("drop-rates", "shulker-shell-multiplier");
                targetMaterial = Material.SHULKER_SHELL;
                break;
            case PHANTOM:
                multiplier = config.getMultiplier("drop-rates", "phantom-membrane-multiplier");
                targetMaterial = Material.PHANTOM_MEMBRANE;
                break;
            case ENDERMITE:
                // Special handling - chance to drop pearl
                double pearlChance = config.getConfig().getDouble("features.drop-rates.endermite-pearl-chance", 0.15);
                if (Math.random() < pearlChance) {
                    event.getDrops().add(new ItemStack(Material.ENDER_PEARL, 1));
                }
                return;
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

