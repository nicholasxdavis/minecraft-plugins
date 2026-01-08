package com.massivecraft.factions.listeners;

import com.massivecraft.factions.*;
import com.massivecraft.factions.util.Logger;
import com.massivecraft.factions.util.PandoraMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class GenBucketListener implements Listener {

    private static final int CLAIM_RADIUS_CHUNKS = 4; // Same as base claim radius
    private static final Material WALL_MATERIAL = Material.OBSIDIAN;

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGenBucketPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }

        String displayName = org.bukkit.ChatColor.stripColor(meta.getDisplayName());
        if (!displayName.equals("Genbucket")) {
            return;
        }

        Player player = event.getPlayer();
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        
        if (fPlayer == null || !fPlayer.hasFaction()) {
            event.setCancelled(true);
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You must be in a base to use genbuckets!")));
            return;
        }

        Faction faction = fPlayer.getFaction();
        if (!faction.isNormal() || !faction.hasBeacon()) {
            event.setCancelled(true);
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You must have a base to use genbuckets!")));
            return;
        }

        Location placeLoc = event.getBlockPlaced().getLocation();
        Location beaconLoc = faction.getBeaconLocation();
        
        if (beaconLoc == null) {
            event.setCancelled(true);
            return;
        }

        // Check if placement is within base boundaries
        if (!isWithinBaseBoundaries(placeLoc, beaconLoc)) {
            event.setCancelled(true);
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Genbuckets can only be placed in your base area!")));
            return;
        }

        // Cancel the block placement (we don't want to place the bucket as a block)
        event.setCancelled(true);
        
        // Remove the item from hand
        ItemStack handItem = player.getItemInHand();
        if (handItem != null && handItem.getAmount() > 1) {
            handItem.setAmount(handItem.getAmount() - 1);
        } else {
            player.setItemInHand(null);
        }

        // Generate walls within base boundaries
        generateWalls(faction, placeLoc, beaconLoc, player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGenBucketInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }

        String displayName = org.bukkit.ChatColor.stripColor(meta.getDisplayName());
        if (!displayName.equals("Genbucket")) {
            return;
        }

        Player player = event.getPlayer();
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        
        if (fPlayer == null || !fPlayer.hasFaction()) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You must be in a base to use genbuckets!")));
            event.setCancelled(true);
            return;
        }

        Faction faction = fPlayer.getFaction();
        if (!faction.isNormal() || !faction.hasBeacon()) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You must have a base to use genbuckets!")));
            event.setCancelled(true);
            return;
        }

        Location interactLoc = event.getClickedBlock().getLocation();
        Location beaconLoc = faction.getBeaconLocation();
        
        if (beaconLoc == null) {
            return;
        }

        // Check if interaction is within base boundaries
        if (!isWithinBaseBoundaries(interactLoc, beaconLoc)) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Genbuckets can only be used in your base area!")));
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        
        // Remove the item from hand
        ItemStack handItem = player.getItemInHand();
        if (handItem != null && handItem.getAmount() > 1) {
            handItem.setAmount(handItem.getAmount() - 1);
        } else {
            player.setItemInHand(null);
        }

        // Generate walls within base boundaries
        generateWalls(faction, interactLoc, beaconLoc, player);
    }

    /**
     * Check if a location is within base boundaries
     */
    private boolean isWithinBaseBoundaries(Location loc, Location beaconLoc) {
        if (loc.getWorld() == null || beaconLoc.getWorld() == null) {
            return false;
        }

        if (!loc.getWorld().getName().equals(beaconLoc.getWorld().getName())) {
            return false;
        }

        FLocation beaconFloc = FLocation.wrap(beaconLoc);
        FLocation checkFloc = FLocation.wrap(loc);
        
        // Check if location is within the claimed chunks
        int radiusChunks = CLAIM_RADIUS_CHUNKS;
        int minChunkX = beaconFloc.getIntX() - radiusChunks;
        int maxChunkX = beaconFloc.getIntX() + radiusChunks;
        int minChunkZ = beaconFloc.getIntZ() - radiusChunks;
        int maxChunkZ = beaconFloc.getIntZ() + radiusChunks;

        int checkChunkX = checkFloc.getIntX();
        int checkChunkZ = checkFloc.getIntZ();

        return checkChunkX >= minChunkX && checkChunkX <= maxChunkX &&
               checkChunkZ >= minChunkZ && checkChunkZ <= maxChunkZ;
    }

    /**
     * Generate walls within base boundaries
     */
    private void generateWalls(Faction faction, Location startLoc, Location beaconLoc, Player player) {
        if (startLoc.getWorld() == null || beaconLoc.getWorld() == null) {
            return;
        }

        FLocation beaconFloc = FLocation.wrap(beaconLoc);
        int radiusChunks = CLAIM_RADIUS_CHUNKS;
        
        // Calculate base boundaries (same as structure generator)
        int minX = (beaconFloc.getIntX() - radiusChunks) * 16;
        int maxX = (beaconFloc.getIntX() + radiusChunks) * 16 + 15;
        int minZ = (beaconFloc.getIntZ() - radiusChunks) * 16;
        int maxZ = (beaconFloc.getIntZ() + radiusChunks) * 16 + 15;

        int floorY = beaconLoc.getBlockY() - 1;
        int maxHeight = floorY + 100; // Base height is 100 blocks

        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.success("Generating walls...")));

        // Generate walls in batches to avoid lag
        new BukkitRunnable() {
            private int blocksPlaced = 0;
            private final int BATCH_SIZE = 500;
            
            // Wall generation state
            private int wallX = minX;
            private int wallZ = minZ;
            private int wallY = floorY + 1;
            private boolean wallsDone = false;

            @Override
            public void run() {
                if (wallsDone) {
                    player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.success("Walls generated! Placed " + blocksPlaced + " blocks.")));
                    this.cancel();
                    return;
                }

                int processed = 0;

                // Generate walls: perimeter at minX, maxX, minZ, maxZ
                while (processed < BATCH_SIZE && !wallsDone) {
                    // Process all Y levels for current X/Z positions
                    for (int y = wallY; y < maxHeight && processed < BATCH_SIZE; y++) {
                        // North wall (minZ)
                        if (wallX >= minX && wallX <= maxX) {
                            Location loc = new Location(startLoc.getWorld(), wallX, y, minZ);
                            Block block = loc.getBlock();
                            if (block.getType() != WALL_MATERIAL) {
                                block.setType(WALL_MATERIAL, false);
                                blocksPlaced++;
                                processed++;
                            }
                        }
                        
                        // South wall (maxZ)
                        if (wallX >= minX && wallX <= maxX && processed < BATCH_SIZE) {
                            Location loc = new Location(startLoc.getWorld(), wallX, y, maxZ);
                            Block block = loc.getBlock();
                            if (block.getType() != WALL_MATERIAL) {
                                block.setType(WALL_MATERIAL, false);
                                blocksPlaced++;
                                processed++;
                            }
                        }
                        
                        // West wall (minX)
                        if (wallZ >= minZ && wallZ <= maxZ && processed < BATCH_SIZE) {
                            Location loc = new Location(startLoc.getWorld(), minX, y, wallZ);
                            Block block = loc.getBlock();
                            if (block.getType() != WALL_MATERIAL) {
                                block.setType(WALL_MATERIAL, false);
                                blocksPlaced++;
                                processed++;
                            }
                        }
                        
                        // East wall (maxX)
                        if (wallZ >= minZ && wallZ <= maxZ && processed < BATCH_SIZE) {
                            Location loc = new Location(startLoc.getWorld(), maxX, y, wallZ);
                            Block block = loc.getBlock();
                            if (block.getType() != WALL_MATERIAL) {
                                block.setType(WALL_MATERIAL, false);
                                blocksPlaced++;
                                processed++;
                            }
                        }
                    }
                    
                    // Move to next position
                    wallY = floorY + 1;
                    wallX++;
                    if (wallX > maxX) {
                        wallX = minX;
                        wallZ++;
                        if (wallZ > maxZ) {
                            wallsDone = true;
                            break;
                        }
                    }
                }
            }
        }.runTaskTimer(FactionsPlugin.getInstance(), 1L, 1L);
    }
}




