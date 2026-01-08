package com.massivecraft.factions.util;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.data.FactionData;
import com.massivecraft.factions.data.helpers.FactionDataHelper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Beacon;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages beacon beam activation and color changes based on perks
 * Uses an invisible beacon above the real beacon for the beam
 */
public class BeaconBeamManager {

    // Track invisible beacon locations: factionId -> invisibleBeaconLocation
    private static final Map<String, Location> invisibleBeacons = new HashMap<>();
    
    // Recurring task to keep invisible beacons hidden
    private static BukkitRunnable invisibilityTask = null;

    // Glass colors for different perk combinations
    private static final Material DEFAULT_BEAM_COLOR = Material.WHITE_STAINED_GLASS; // White = default
    private static final Material EXTRA_HEARTS_COLOR = Material.RED_STAINED_GLASS; // Red for extra hearts
    private static final Material JUMP_BOOST_COLOR = Material.LIME_STAINED_GLASS; // Lime for jump boost
    private static final Material SPEED_BOOST_COLOR = Material.CYAN_STAINED_GLASS; // Cyan for speed boost
    private static final Material MULTIPLE_PERKS_COLOR = Material.PURPLE_STAINED_GLASS; // Purple for multiple perks

    /**
     * Start the recurring task to keep invisible beacons hidden
     */
    public static void startInvisibilityTask() {
        if (invisibilityTask != null) {
            return; // Already running
        }
        
        invisibilityTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Keep all invisible beacons hidden to all players
                for (Map.Entry<String, Location> entry : invisibleBeacons.entrySet()) {
                    Location invisibleBeaconLoc = entry.getValue();
                    if (invisibleBeaconLoc != null && invisibleBeaconLoc.getWorld() != null) {
                        Block block = invisibleBeaconLoc.getBlock();
                        if (block.getType() == Material.BEACON) {
                            // Send block change to all nearby players to make it appear as air
                            for (Player player : invisibleBeaconLoc.getWorld().getPlayers()) {
                                if (player.getLocation().distance(invisibleBeaconLoc) <= 64) {
                                    try {
                                        player.sendBlockChange(invisibleBeaconLoc, Material.AIR.createBlockData());
                                    } catch (Exception e) {
                                        // Ignore errors
                                    }
                                }
                            }
                        }
                    }
                }
            }
        };
        
        // Run every 2 seconds to keep beacons invisible
        invisibilityTask.runTaskTimer(FactionsPlugin.getInstance(), 20L, 40L);
    }

    /**
     * Stop the recurring invisibility task
     */
    public static void stopInvisibilityTask() {
        if (invisibilityTask != null) {
            invisibilityTask.cancel();
            invisibilityTask = null;
        }
    }

    /**
     * Enable beacon beam and set initial color
     * Uses the real beacon with glass block 1 block above it for the beam
     * Also creates an invisible beacon 1 block above for beam positioning
     */
    public static void enableBeam(Faction faction, Location realBeaconLoc) {
        if (faction == null || realBeaconLoc == null || !faction.hasBeacon()) {
            return;
        }

        // Start invisibility task if not already running
        startInvisibilityTask();

        // Remove existing invisible beacon if any
        removeBeam(faction, realBeaconLoc);

        // Use delayed task to ensure real beacon is placed
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Block realBeaconBlock = realBeaconLoc.getBlock();
                    if (realBeaconBlock.getType() != Material.BEACON) {
                        Logger.print("Real beacon block not found at " + realBeaconLoc, Logger.PrefixType.WARNING);
                        return;
                    }

                    // First, place glass block 1 block above REAL beacon to activate its beam
                    Location realGlassLoc = realBeaconLoc.clone().add(0, 1, 0);
                    Block realGlassBlock = realGlassLoc.getBlock();
                    if (realGlassBlock.getType() == Material.AIR || !isStainedGlass(realGlassBlock.getType())) {
                        realGlassBlock.setType(DEFAULT_BEAM_COLOR, false);
                        realGlassBlock.getState().update(true, true);
                    }
                    
                    // Update real beacon state to activate beam
                    BlockState realBeaconState = realBeaconBlock.getState();
                    if (realBeaconState instanceof Beacon) {
                        realBeaconState.update(true, true);
                    }

                    // Now create invisible beacon 1 block above the real beacon for beam positioning
                    Location invisibleBeaconLoc = realBeaconLoc.clone().add(0, 1, 0);
                    Block invisibleBeaconBlock = invisibleBeaconLoc.getBlock();
                    
                    // Only place if it's air
                    if (invisibleBeaconBlock.getType() == Material.AIR) {
                        // Place glass block FIRST at 2 blocks above real beacon (1 block above invisible beacon)
                        Location glassLoc = invisibleBeaconLoc.clone().add(0, 1, 0);
                        Block glassBlock = glassLoc.getBlock();
                        
                        if (glassBlock.getType() == Material.AIR || !isStainedGlass(glassBlock.getType())) {
                            glassBlock.setType(DEFAULT_BEAM_COLOR, false);
                            glassBlock.getState().update(true, true);
                        }
                        
                        // Now place invisible beacon
                        invisibleBeaconBlock.setType(Material.BEACON, false);
                        invisibleBeacons.put(faction.getId(), invisibleBeaconLoc);
                        
                        // Update beacon state to activate beam
                        BlockState state = invisibleBeaconBlock.getState();
                        if (state instanceof Beacon) {
                            state.update(true, true);
                        }
                        
                        // Remove glass from real beacon
                        if (realGlassBlock.getType() == DEFAULT_BEAM_COLOR) {
                            realGlassBlock.setType(Material.AIR, false);
                            realGlassBlock.getState().update(true, true);
                        }
                        
                        // Wait a moment for beam to render, then make invisible
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                makeBeaconInvisibleToPlayers(invisibleBeaconLoc);
                                updateBeamColor(faction, realBeaconLoc);
                            }
                        }.runTaskLater(FactionsPlugin.getInstance(), 20L);
                    } else if (invisibleBeaconBlock.getType() == Material.BEACON) {
                        // Invisible beacon already exists
                        invisibleBeacons.put(faction.getId(), invisibleBeaconLoc);
                        
                        // Ensure glass block is still there
                        Location glassLoc = invisibleBeaconLoc.clone().add(0, 1, 0);
                        Block glassBlock = glassLoc.getBlock();
                        if (glassBlock.getType() == Material.AIR || !isStainedGlass(glassBlock.getType())) {
                            glassBlock.setType(DEFAULT_BEAM_COLOR, false);
                            glassBlock.getState().update(true, true);
                        }
                        
                        // Update beacon state
                        BlockState state = invisibleBeaconBlock.getState();
                        if (state instanceof Beacon) {
                            state.update(true, true);
                        }
                        
                        makeBeaconInvisibleToPlayers(invisibleBeaconLoc);
                        updateBeamColor(faction, realBeaconLoc);
                    }
                } catch (Exception e) {
                    Logger.print("Error enabling beacon beam for " + faction.getTag() + ": " + e.getMessage(), Logger.PrefixType.WARNING);
                    e.printStackTrace();
                }
            }
        }.runTaskLater(FactionsPlugin.getInstance(), 20L); // 20 ticks delay to ensure everything is placed
    }


    /**
     * Update beacon beam color based on enabled perks
     * Updates the glass block above the invisible beacon
     */
    public static void updateBeamColor(Faction faction, Location realBeaconLoc) {
        if (faction == null || realBeaconLoc == null || !faction.hasBeacon()) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // Get the invisible beacon location
                    Location invisibleBeaconLoc = invisibleBeacons.get(faction.getId());
                    if (invisibleBeaconLoc == null) {
                        // Invisible beacon doesn't exist, create it
                        enableBeam(faction, realBeaconLoc);
                        return;
                    }

                    Block invisibleBeaconBlock = invisibleBeaconLoc.getBlock();
                    if (invisibleBeaconBlock.getType() != Material.BEACON) {
                        // Invisible beacon was removed, recreate it
                        enableBeam(faction, realBeaconLoc);
                        return;
                    }

                    // Get perk statuses
                    FactionDataHelper dataHelper = FactionsPlugin.getInstance().getFactionDataHelper();
                    if (dataHelper == null) return;

                    FactionData data = dataHelper.getOrLoadFactionData(faction);
                    if (data == null) return;

                    boolean extraHearts = getPerkStatus(data, "extraHearts");
                    boolean jumpBoost = getPerkStatus(data, "jumpBoost");
                    boolean speedBoost = getPerkStatus(data, "speedBoost");

                    // Determine beam color based on enabled perks
                    Material beamColor = DEFAULT_BEAM_COLOR;
                    
                    int enabledCount = 0;
                    if (extraHearts) enabledCount++;
                    if (jumpBoost) enabledCount++;
                    if (speedBoost) enabledCount++;

                    if (enabledCount == 0) {
                        beamColor = DEFAULT_BEAM_COLOR; // White = no perks
                    } else if (enabledCount >= 2) {
                        beamColor = MULTIPLE_PERKS_COLOR; // Purple = multiple perks
                    } else if (extraHearts) {
                        beamColor = EXTRA_HEARTS_COLOR; // Red = extra hearts
                    } else if (jumpBoost) {
                        beamColor = JUMP_BOOST_COLOR; // Lime = jump boost
                    } else if (speedBoost) {
                        beamColor = SPEED_BOOST_COLOR; // Cyan = speed boost
                    }

                    // Place/update glass block above invisible beacon (2 blocks above real beacon)
                    Location glassLoc = invisibleBeaconLoc.clone().add(0, 1, 0);
                    Block glassBlock = glassLoc.getBlock();
                    
                    // Always ensure glass block is placed, even if type matches
                    if (glassBlock.getType() != beamColor) {
                        glassBlock.setType(beamColor, false);
                    }
                    
                    // Force update to ensure beam color changes immediately
                    glassBlock.getState().update(true, true);
                    
                    // Also update the beacon block state to refresh the beam
                    BlockState beaconState = invisibleBeaconBlock.getState();
                    if (beaconState instanceof Beacon) {
                        beaconState.update(true, true);
                    }
                    
                    // Make invisible again after update
                    makeBeaconInvisibleToPlayers(invisibleBeaconLoc);
                } catch (Exception e) {
                    Logger.print("Error updating beacon beam color for " + faction.getTag() + ": " + e.getMessage(), Logger.PrefixType.WARNING);
                }
            }
        }.runTask(FactionsPlugin.getInstance());
    }

    /**
     * Remove beacon beam (remove invisible beacon and glass block)
     */
    public static void removeBeam(Faction faction, Location realBeaconLoc) {
        if (realBeaconLoc == null) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // Get invisible beacon location
                    Location invisibleBeaconLoc = invisibleBeacons.remove(faction.getId());
                    if (invisibleBeaconLoc != null) {
                        // Remove glass block above invisible beacon
                        Location glassLoc = invisibleBeaconLoc.clone().add(0, 1, 0);
                        Block glassBlock = glassLoc.getBlock();
                        
                        if (isStainedGlass(glassBlock.getType())) {
                            glassBlock.setType(Material.AIR, false);
                        }
                        
                        // Remove invisible beacon block
                        Block invisibleBeaconBlock = invisibleBeaconLoc.getBlock();
                        if (invisibleBeaconBlock.getType() == Material.BEACON) {
                            invisibleBeaconBlock.setType(Material.AIR, false);
                        }
                    } else {
                        // Fallback: try to remove glass block 1 block above real beacon
                        Location glassLoc = realBeaconLoc.clone().add(0, 1, 0);
                        Block glassBlock = glassLoc.getBlock();
                        
                        if (isStainedGlass(glassBlock.getType())) {
                            glassBlock.setType(Material.AIR, false);
                        }
                    }
                    
                    // Stop invisibility task if no more invisible beacons
                    if (invisibleBeacons.isEmpty()) {
                        stopInvisibilityTask();
                    }
                } catch (Exception e) {
                    Logger.print("Error removing beacon beam: " + e.getMessage(), Logger.PrefixType.WARNING);
                }
            }
        }.runTask(FactionsPlugin.getInstance());
    }

    /**
     * Check if a material is stained glass
     */
    private static boolean isStainedGlass(Material material) {
        if (material == null) return false;
        String name = material.name();
        return name.contains("STAINED_GLASS") && !name.contains("PANE");
    }

    /**
     * Get perk status from faction data
     */
    private static boolean getPerkStatus(FactionData data, String perkName) {
        Object statusObj = data.get("perk." + perkName);
        return statusObj != null && statusObj instanceof Boolean && (Boolean) statusObj;
    }

    /**
     * Make beacon block invisible to players using NMS
     * Sends block change packets to make it appear as air
     */
    private static void makeBeaconInvisibleToPlayers(Location beaconLoc) {
        try {
            // Use reflection to send block change packets to all nearby players
            // This makes the beacon appear as air to players while still functioning
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (player.getWorld().equals(beaconLoc.getWorld())) {
                    // Send block change to make it appear as air
                    player.sendBlockChange(beaconLoc, Material.AIR.createBlockData());
                }
            }
        } catch (Exception e) {
            Logger.print("Error making beacon invisible: " + e.getMessage(), Logger.PrefixType.WARNING);
        }
    }

    /**
     * Update invisible beacon visibility for a player (when they join or get near)
     */
    public static void updateInvisibleBeaconForPlayer(Player player) {
        // When a player joins or gets near, make sure invisible beacons appear as air to them
        for (Map.Entry<String, Location> entry : invisibleBeacons.entrySet()) {
            Location invisibleBeaconLoc = entry.getValue();
            if (invisibleBeaconLoc != null && player.getWorld().equals(invisibleBeaconLoc.getWorld())) {
                if (player.getLocation().distance(invisibleBeaconLoc) <= 64) {
                    try {
                        player.sendBlockChange(invisibleBeaconLoc, Material.AIR.createBlockData());
                    } catch (Exception e) {
                        // Ignore errors
                    }
                }
            }
        }
    }
    
    /**
     * Get all invisible beacon locations (for debugging)
     */
    public static Map<String, Location> getInvisibleBeacons() {
        return new HashMap<>(invisibleBeacons);
    }
}

