package com.playpandora.morenether.handlers;

import com.playpandora.morenether.MoreNether;
import com.playpandora.morenether.utils.AmbienceController;
import com.playpandora.morenether.utils.ConfigManager;
import com.playpandora.morenether.utils.DimensionChecker;
import com.playpandora.morenether.utils.LightLevelChecker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HorrorAtmosphereHandler implements Listener {
    private final MoreNether plugin;
    private final ConfigManager config;
    private final AmbienceController ambienceController;
    private final Map<UUID, BukkitRunnable> heatPressureTasks = new HashMap<>();
    
    public HorrorAtmosphereHandler(MoreNether plugin) {
        this.plugin = plugin;
        this.config = new ConfigManager(plugin);
        this.ambienceController = new AmbienceController(plugin);
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (!DimensionChecker.isNether(player.getLocation())) {
            // Stop ambience if player leaves Nether
            ambienceController.stopAmbience(player);
            stopHeatPressure(player);
            return;
        }
        
        // Start horror ambience if in low light
        if (config.isFeatureEnabled("horror-atmosphere.darkness-pressure")) {
            if (LightLevelChecker.isLowLight(player)) {
                ambienceController.startAmbience(player, AmbienceController.AmbienceType.NETHER_HORROR);
            } else {
                ambienceController.stopAmbience(player);
            }
        }
        
        // Soul Sand Valley horror
        if (config.isFeatureEnabled("horror-atmosphere.soul-sand-valley-horror")) {
            handleSoulSandValleyHorror(player);
        }
        
        // Heat pressure in Basalt Deltas
        if (config.isFeatureEnabled("horror-atmosphere.heat-pressure")) {
            handleHeatPressure(player);
        }
    }
    
    private void handleSoulSandValleyHorror(Player player) {
        String biome = player.getLocation().getBlock().getBiome().toString();
        if (!biome.contains("SOUL_SAND_VALLEY")) {
            return;
        }
        
        // Soul moaning ambience (random intervals)
        // This would be handled by the ambience controller
        
        // Silent Valley chance (no mobs, just wind)
        double silentChance = config.getConfig().getDouble(
            "features.horror-atmosphere.soul-sand-valley-horror.silent-valley-chance", 0.1);
        
        // Note: Actual mob removal would require more complex logic
        // This is a placeholder for the concept
    }
    
    private void handleHeatPressure(Player player) {
        String biome = player.getLocation().getBlock().getBiome().toString();
        if (!biome.contains("BASALT_DELTAS")) {
            stopHeatPressure(player);
            return;
        }
        
        // Start heat pressure task if not already running
        if (heatPressureTasks.containsKey(player.getUniqueId())) {
            return;
        }
        
        double hungerMultiplier = config.getConfig().getDouble(
            "features.horror-atmosphere.heat-pressure.basalt-deltas-hunger-multiplier", 1.3);
        int checkInterval = config.getConfig().getInt(
            "features.horror-atmosphere.heat-pressure.check-interval-ticks", 20);
        
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || 
                    !DimensionChecker.isNether(player.getLocation())) {
                    String currentBiome = player.getLocation().getBlock().getBiome().toString();
                    if (!currentBiome.contains("BASALT_DELTAS")) {
                        cancel();
                        heatPressureTasks.remove(player.getUniqueId());
                        return;
                    }
                }
                
                // Increase hunger depletion
                // Note: This requires careful implementation to avoid conflicts
                // May need to use Paper API or scheduled food level reduction
                if (player.getFoodLevel() > 0) {
                    // Reduce food level slightly more often
                    if (Math.random() < (hungerMultiplier - 1.0) * 0.1) {
                        int newLevel = Math.max(0, player.getFoodLevel() - 1);
                        player.setFoodLevel(newLevel);
                    }
                }
            }
        };
        
        task.runTaskTimer(plugin, 0, checkInterval);
        heatPressureTasks.put(player.getUniqueId(), task);
    }
    
    private void stopHeatPressure(Player player) {
        BukkitRunnable task = heatPressureTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }
    
    public void cleanup() {
        ambienceController.cleanup();
        heatPressureTasks.values().forEach(BukkitRunnable::cancel);
        heatPressureTasks.clear();
    }
}

