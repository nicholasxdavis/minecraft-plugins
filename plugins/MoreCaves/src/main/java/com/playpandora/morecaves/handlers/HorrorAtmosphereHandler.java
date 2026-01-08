package com.playpandora.morecaves.handlers;

import com.playpandora.morecaves.MoreCaves;
import com.playpandora.morecaves.utils.AmbienceController;
import com.playpandora.morecaves.utils.ConfigManager;
import com.playpandora.morecaves.utils.CaveChecker;
import com.playpandora.morecaves.utils.DimensionChecker;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class HorrorAtmosphereHandler implements Listener {
    private final MoreCaves plugin;
    private final ConfigManager config;
    private final AmbienceController ambienceController;
    
    public HorrorAtmosphereHandler(MoreCaves plugin) {
        this.plugin = plugin;
        this.config = new ConfigManager(plugin);
        this.ambienceController = new AmbienceController(plugin);
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (!DimensionChecker.isOverworld(player.getLocation())) {
            ambienceController.stopAmbience(player);
            return;
        }
        
        // Check if player is in a cave (Y level and light check)
        Location loc = player.getLocation();
        boolean isInCave = CaveChecker.isInCave(loc);
        
        if (isInCave && config.isFeatureEnabled("horror-atmosphere.cave-ambience")) {
            ambienceController.startAmbience(player, AmbienceController.AmbienceType.CAVE_HORROR);
        } else {
            ambienceController.stopAmbience(player);
        }
    }
    
    public void cleanup() {
        ambienceController.cleanup();
    }
}

