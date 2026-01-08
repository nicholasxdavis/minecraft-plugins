package com.playpandora.treasurehunt.managers;

import com.playpandora.treasurehunt.TreasureHunt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

public class HologramManager {
    
    private final TreasureHunt plugin;
    private final Map<Location, ArmorStand> holograms = new HashMap<>();
    
    public HologramManager(TreasureHunt plugin) {
        this.plugin = plugin;
    }
    
    public void createHologram(Location location, String text) {
        if (!plugin.getConfig().getBoolean("hologram.enabled", true)) {
            return;
        }
        
        // Remove existing hologram if any
        removeHologram(location);
        
        // Spawn location is 2 blocks above the chest
        Location hologramLoc = location.clone().add(0.5, 2.0, 0.5);
        
        // Spawn armor stand
        ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(hologramLoc, EntityType.ARMOR_STAND);
        
        // Configure armor stand
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setCanPickupItems(false);
        armorStand.setCustomNameVisible(true);
        armorStand.setInvulnerable(true);
        armorStand.setCollidable(false);
        armorStand.setMarker(true); // Makes it not interactable and smaller hitbox
        armorStand.setSmall(true); // Makes it smaller
        
        // Set custom name with color codes using Adventure API
        Component nameComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        armorStand.customName(nameComponent);
        armorStand.setCustomNameVisible(true);
        
        // Store reference
        holograms.put(location, armorStand);
    }
    
    public void removeHologram(Location location) {
        ArmorStand armorStand = holograms.remove(location);
        if (armorStand != null && !armorStand.isDead()) {
            armorStand.remove();
        }
    }
    
    public void updateHologram(Location location, String text) {
        ArmorStand armorStand = holograms.get(location);
        if (armorStand != null && !armorStand.isDead()) {
            Component nameComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(text);
            armorStand.customName(nameComponent);
        } else {
            // Recreate if it doesn't exist
            createHologram(location, text);
        }
    }
    
    public void removeAllHolograms() {
        for (ArmorStand armorStand : holograms.values()) {
            if (armorStand != null && !armorStand.isDead()) {
                armorStand.remove();
            }
        }
        holograms.clear();
    }
    
    public boolean hasHologram(Location location) {
        return holograms.containsKey(location);
    }
}

