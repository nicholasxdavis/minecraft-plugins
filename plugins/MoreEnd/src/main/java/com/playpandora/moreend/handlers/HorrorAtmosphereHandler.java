package com.playpandora.moreend.handlers;

import com.playpandora.moreend.MoreEnd;
import com.playpandora.moreend.utils.AmbienceController;
import com.playpandora.moreend.utils.CameraShakeManager;
import com.playpandora.moreend.utils.ConfigManager;
import com.playpandora.moreend.utils.DimensionChecker;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HorrorAtmosphereHandler implements Listener {
    private final MoreEnd plugin;
    private final ConfigManager config;
    private final AmbienceController ambienceController;
    private final CameraShakeManager cameraShake;
    private final Map<UUID, Boolean> voidShakeActive = new HashMap<>();
    
    public HorrorAtmosphereHandler(MoreEnd plugin) {
        this.plugin = plugin;
        this.config = new ConfigManager(plugin);
        this.ambienceController = new AmbienceController(plugin);
        this.cameraShake = new CameraShakeManager(plugin);
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (!DimensionChecker.isEnd(player.getLocation())) {
            ambienceController.stopAmbience(player);
            stopVoidShake(player);
            return;
        }
        
        // Start End dread ambience
        if (config.isFeatureEnabled("horror-atmosphere.end-ambience")) {
            ambienceController.startAmbience(player, AmbienceController.AmbienceType.END_DREAD);
        }
        
        // Void pressure
        if (config.isFeatureEnabled("horror-atmosphere.void-pressure")) {
            handleVoidPressure(player);
        }
    }
    
    private void handleVoidPressure(Player player) {
        Location loc = player.getLocation();
        double voidY = 0; // Void is at Y=0 in End
        
        // Check distance from void edge
        double distanceFromVoid = Math.abs(loc.getY() - voidY);
        double shakeDistance = config.getConfig().getDouble(
            "features.horror-atmosphere.void-pressure.camera-shake-distance", 5.0);
        
        if (distanceFromVoid <= shakeDistance) {
            // Start camera shake
            if (!voidShakeActive.getOrDefault(player.getUniqueId(), false)) {
                float intensity = (float) config.getConfig().getDouble(
                    "features.horror-atmosphere.void-pressure.camera-shake-intensity", 0.1);
                cameraShake.startVoidShake(player, intensity);
                voidShakeActive.put(player.getUniqueId(), true);
            }
            
            // Random Enderman spawns near void
            if (config.getConfig().getBoolean(
                "features.horror-atmosphere.void-pressure.enderman-spawn-near-void", true)) {
                // Spawn Enderman logic (handled by spawn system)
            }
            
            // Random dragon scream
            if (Math.random() < config.getConfig().getDouble(
                "features.horror-atmosphere.void-pressure.dragon-scream-chance", 0.03)) {
                // Play dragon scream sound
                try {
                    loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_AMBIENT,
                        org.bukkit.SoundCategory.HOSTILE, 0.5f, 0.8f);
                } catch (Exception e) {
                    // Sound may not exist in this version
                }
            }
        } else {
            stopVoidShake(player);
        }
    }
    
    private void stopVoidShake(Player player) {
        if (voidShakeActive.remove(player.getUniqueId()) != null) {
            cameraShake.stopVoidShake(player);
        }
    }
    
    public void cleanup() {
        ambienceController.cleanup();
        cameraShake.cleanup();
        voidShakeActive.clear();
    }
}

