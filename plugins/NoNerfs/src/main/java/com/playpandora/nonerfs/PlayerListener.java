package com.playpandora.nonerfs;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerListener implements Listener {
    
    private final NoNerfs plugin;
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    
    public PlayerListener(NoNerfs plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        
        // Apply speed boost using attribute modifier (+0.1 player speed + 0.2 walking = 0.3 total)
        applySpeedModifier(player, SPEED_MODIFIER_UUID, "nonerfs-speed", 0.3);
        
        // Note: Reach increase requires server-side configuration or NMS, skipping for simplicity
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();
        
        // Remove speed modifier
        removeModifier(player, Attribute.MOVEMENT_SPEED, SPEED_MODIFIER_UUID);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Player)) {
            return;
        }
        
        // Only slow down food loss, not food gain
        var player = (org.bukkit.entity.Player) event.getEntity();
        int oldLevel = player.getFoodLevel();
        int newLevel = event.getFoodLevel();
        
        // If food is decreasing (losing hunger)
        if (newLevel < oldLevel) {
            int loss = oldLevel - newLevel;
            // Reduce loss by 25% (multiply by 0.75)
            int reducedLoss = (int) Math.round(loss * 0.75);
            int finalLevel = oldLevel - reducedLoss;
            
            // Ensure we don't go below 0
            event.setFoodLevel(Math.max(0, finalLevel));
        }
    }
    
    private void applySpeedModifier(org.bukkit.entity.Player player, UUID uuid, String name, double value) {
        AttributeInstance attribute = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attribute != null) {
            // Remove existing modifier if present
            removeModifier(player, Attribute.MOVEMENT_SPEED, uuid);
            
            // Add new modifier
            AttributeModifier modifier = new AttributeModifier(
                uuid,
                name,
                value,
                AttributeModifier.Operation.ADD_SCALAR
            );
            attribute.addModifier(modifier);
        }
    }
    
    private void removeModifier(org.bukkit.entity.Player player, Attribute attribute, UUID uuid) {
        AttributeInstance attributeInstance = player.getAttribute(attribute);
        if (attributeInstance != null) {
            attributeInstance.getModifiers().stream()
                .filter(mod -> mod.getUniqueId().equals(uuid))
                .findFirst()
                .ifPresent(attributeInstance::removeModifier);
        }
    }
}
