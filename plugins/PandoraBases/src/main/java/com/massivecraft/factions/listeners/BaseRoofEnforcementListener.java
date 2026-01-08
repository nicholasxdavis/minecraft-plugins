package com.massivecraft.factions.listeners;

import com.massivecraft.factions.*;
import com.massivecraft.factions.util.BaseRoofValidator;
import com.massivecraft.factions.util.Logger;
import com.massivecraft.factions.util.PandoraMessage;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BaseRoofEnforcementListener implements Listener {

    private static final int CLAIM_RADIUS_CHUNKS = 4; // Same as beacon placement
    
    // Cache for violation warnings (faction ID -> last warning time)
    private static final Map<String, Long> violationWarningCache = new ConcurrentHashMap<>();
    private static final long WARNING_COOLDOWN_MS = 30000; // 30 seconds between warnings
    
    // Cache for validation results (faction ID -> last validation time)
    private static final Map<String, Long> validationCache = new ConcurrentHashMap<>();
    private static final long VALIDATION_CACHE_MS = 5000; // Cache validation for 5 seconds

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // Roof enforcement disabled - allow all block placement
        // Players can now build roofs freely
        return;
        /*
        if (!Conf.baseEnforceNoRoof || !Conf.baseBlockRoofPlacement) {
            return;
        }

        Block block = event.getBlockPlaced();
        Location loc = block.getLocation();
        FLocation floc = FLocation.wrap(loc);
        Faction faction = Board.getInstance().getFactionAt(floc);

        if (faction == null || !faction.isNormal() || !faction.hasBeacon()) {
            return; // Not a base or no beacon
        }

        // Check if this placement would violate roof rules
        if (isRoofViolation(faction, loc)) {
            Player player = event.getPlayer();
            
            // Check if player is member (warn but allow with grace period for members)
            FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
            boolean isMember = fPlayer != null && fPlayer.hasFaction() && fPlayer.getFaction().equals(faction);
            
            if (isMember && Conf.baseAllowMemberRoofPlacement) {
                // Member placing - warn but allow (will be auto-removed later)
                sendRoofWarning(player, faction, true);
            } else {
                // Non-member or member placement disabled - block it
                sendRoofWarning(player, faction, false);
                event.setCancelled(true);
            }
        }

        // Check beacon column if placing near beacon
        if (isNearBeacon(faction, loc)) {
            Location beaconLoc = faction.getBeaconLocation();
            if (beaconLoc != null && !BaseRoofValidator.validateBeaconSkyAccess(beaconLoc)) {
                Player player = event.getPlayer();
                FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
                boolean isMember = fPlayer != null && fPlayer.hasFaction() && fPlayer.getFaction().equals(faction);
                
                if (isMember && Conf.baseAllowMemberBeaconBlocking) {
                    // Warn member
                    player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.warning("⚠ Warning!")));
                    player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.warning("You're blocking your beacon's sky access!")));
                    player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("This may cause issues with your base.")));
                } else {
                    // Block it
                    player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("✗ Beacon Column Blocked!")));
                    player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You cannot place blocks that block the beacon's sky access!")));
                    player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("Beacons require a clear path to the sky.")));
                    event.setCancelled(true);
                }
            }
        }
        */
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        // Roof enforcement disabled - allow all block breaking
        return;
        /*
        if (!Conf.baseEnforceNoRoof) {
            return;
        }

        Block block = event.getBlock();
        Location loc = block.getLocation();
        FLocation floc = FLocation.wrap(loc);
        Faction faction = Board.getInstance().getFactionAt(floc);

        if (faction == null || !faction.isNormal() || !faction.hasBeacon()) {
            return; // Not a base
        }

        // If breaking a roof violation, validate the base after
        if (Conf.baseAutoRemoveRoofViolations) {
            // Schedule validation after break completes
            FactionsPlugin.getInstance().getServer().getScheduler().runTaskLater(
                FactionsPlugin.getInstance(), () -> validateAndFixBase(faction), 1L);
        }
        */
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        // Roof enforcement disabled - no validation on chunk load
        return;
        /*
        if (!Conf.baseEnforceNoRoof || !Conf.baseValidateOnChunkLoad) {
            return;
        }

        // Validate all bases in this chunk
        FLocation floc = FLocation.wrap(event.getChunk());
        Faction faction = Board.getInstance().getFactionAt(floc);

        if (faction != null && faction.isNormal() && faction.hasBeacon()) {
            // Schedule validation
            FactionsPlugin.getInstance().getServer().getScheduler().runTaskLater(
                FactionsPlugin.getInstance(), () -> validateAndFixBase(faction), 1L);
        }
        */
    }

    /**
     * Check if a location is a roof violation
     */
    private boolean isRoofViolation(Faction faction, Location loc) {
        if (!faction.hasBeacon()) {
            return false;
        }

        Location beaconLoc = faction.getBeaconLocation();
        if (beaconLoc == null) {
            return false;
        }

        int floorY = beaconLoc.getBlockY();
        int blockY = loc.getBlockY();

        // Roof area is above floor
        if (blockY <= floorY) {
            return false; // Floor or below, not roof
        }

        // Check if inside base boundaries
        FLocation floc = FLocation.wrap(loc);
        FLocation centerChunk = FLocation.wrap(beaconLoc);
        
        int minX = (centerChunk.getIntX() - CLAIM_RADIUS_CHUNKS) * 16 + 1;
        int maxX = (centerChunk.getIntX() + CLAIM_RADIUS_CHUNKS) * 16 - 1;
        int minZ = (centerChunk.getIntZ() - CLAIM_RADIUS_CHUNKS) * 16 + 1;
        int maxZ = (centerChunk.getIntZ() + CLAIM_RADIUS_CHUNKS) * 16 - 1;

        int blockX = loc.getBlockX();
        int blockZ = loc.getBlockZ();

        // Check if inside interior (not walls)
        if (blockX > minX && blockX < maxX && blockZ > minZ && blockZ < maxZ) {
            // This is inside the base interior, above floor = roof violation
            return true;
        }

        return false;
    }

    /**
     * Check if location is near beacon (in beacon column)
     */
    private boolean isNearBeacon(Faction faction, Location loc) {
        if (!faction.hasBeacon()) {
            return false;
        }

        Location beaconLoc = faction.getBeaconLocation();
        if (beaconLoc == null) {
            return false;
        }

        return loc.getBlockX() == beaconLoc.getBlockX() &&
               loc.getBlockZ() == beaconLoc.getBlockZ() &&
               loc.getBlockY() > beaconLoc.getBlockY();
    }

    /**
     * Send roof violation warning to player
     */
    private void sendRoofWarning(Player player, Faction faction, boolean isMember) {
        String factionId = faction.getId();
        long now = System.currentTimeMillis();
        Long lastWarning = violationWarningCache.get(factionId);
        
        // Throttle warnings
        if (lastWarning != null && (now - lastWarning) < WARNING_COOLDOWN_MS) {
            return; // Too soon for another warning
        }
        
        violationWarningCache.put(factionId, now);
        
        if (isMember) {
            player.sendMessage("");
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.warning("⚠ Roof Placement Warning")));
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.warning("You're placing a block in the roof area!")));
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("Bases cannot have roofs - this block will be")));
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("automatically removed if violations are enabled.")));
            player.sendMessage("");
        } else {
            player.sendMessage("");
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("✗ Roof Violation!")));
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Bases cannot have roofs!")));
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("All blocks above the floor must be air.")));
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("Only base members can modify the base structure.")));
            player.sendMessage("");
        }
    }

    /**
     * Validate base and fix violations if configured
     */
    private void validateAndFixBase(Faction faction) {
        if (!faction.hasBeacon()) {
            return;
        }

        Location beaconLoc = faction.getBeaconLocation();
        if (beaconLoc == null || beaconLoc.getWorld() == null) {
            return;
        }

        // Check validation cache
        String factionId = faction.getId();
        long now = System.currentTimeMillis();
        Long lastValidation = validationCache.get(factionId);
        if (lastValidation != null && (now - lastValidation) < VALIDATION_CACHE_MS) {
            return; // Recently validated, skip
        }
        validationCache.put(factionId, now);

        FLocation centerChunk = FLocation.wrap(beaconLoc);
        int floorY = beaconLoc.getBlockY();

        // Validate roof with detailed feedback
        List<Location> violations = BaseRoofValidator.validateRoof(
            faction, centerChunk, CLAIM_RADIUS_CHUNKS, floorY);

        if (!violations.isEmpty()) {
            int violationCount = violations.size();
            
            if (Conf.baseAutoRemoveRoofViolations) {
                // Remove violations in batches to avoid lag
                int removed = removeViolationsInBatches(violations, faction);
                
                if (removed > 0) {
                    if (Conf.logFactionCreate) {
                        Logger.print("Removed " + removed + " roof violations from base " + 
                            faction.getTag() + " (total: " + violationCount + ")", Logger.PrefixType.DEFAULT);
                    }

                    // Notify all members with detailed message
                    notifyMembers(faction, PandoraMessage.warning("⚠ Base Roof Violations Removed"), 
                        PandoraMessage.warning(removed + " roof violation blocks were automatically removed."),
                        PandoraMessage.text("Your base must remain open to the sky above the floor level."));
                }
            } else {
                // Just notify - don't remove
                notifyMembers(faction, PandoraMessage.error("✗ Base Roof Violations Detected"),
                    PandoraMessage.error("Your base has " + violationCount + " roof violations!"),
                    PandoraMessage.text("Bases cannot have roofs. Please remove blocks above the floor."),
                    PandoraMessage.text("Violations may cause your base to be invalid."));
            }
        }

        // Validate beacon sky access with detailed feedback
        boolean beaconValid = BaseRoofValidator.validateBeaconSkyAccess(beaconLoc);
        if (!beaconValid) {
            notifyMembers(faction, PandoraMessage.error("⚠ Beacon Sky Access Blocked!"),
                PandoraMessage.error("Your base beacon's sky access is blocked!"),
                PandoraMessage.text("Beacons require a clear vertical path to the sky."),
                PandoraMessage.text("Remove any blocks above your beacon."));
        }
    }

    /**
     * Remove violations in batches to avoid lag
     */
    private int removeViolationsInBatches(List<Location> violations, Faction faction) {
        int removed = 0;
        int batchSize = 100; // Remove 100 blocks per tick
        
        if (violations.size() <= batchSize) {
            // Small batch - remove all at once
            removed = BaseRoofValidator.removeRoofViolations(violations);
        } else {
            // Large batch - remove in chunks
            List<Location> batch = new ArrayList<>();
            for (Location loc : violations) {
                batch.add(loc);
                if (batch.size() >= batchSize) {
                    removed += BaseRoofValidator.removeRoofViolations(batch);
                    batch.clear();
                    
                    // Schedule next batch
                    FactionsPlugin.getInstance().getServer().getScheduler().runTaskLater(
                        FactionsPlugin.getInstance(), () -> {
                            if (!batch.isEmpty()) {
                                BaseRoofValidator.removeRoofViolations(batch);
                            }
                        }, 1L);
                }
            }
            // Remove remaining
            if (!batch.isEmpty()) {
                removed += BaseRoofValidator.removeRoofViolations(batch);
            }
        }
        
        return removed;
    }

    /**
     * Notify all online members of a base
     */
    private void notifyMembers(Faction faction, String... messages) {
        for (FPlayer member : faction.getFPlayers()) {
            if (member.isOnline() && member.getPlayer() != null) {
                Player player = member.getPlayer();
                player.sendMessage("");
                for (String msg : messages) {
                    player.sendMessage(PandoraMessage.formatWithPrefix(msg));
                }
                player.sendMessage("");
            }
        }
    }
}

