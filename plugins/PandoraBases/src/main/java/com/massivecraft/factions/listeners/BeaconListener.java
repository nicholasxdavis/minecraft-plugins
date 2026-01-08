package com.massivecraft.factions.listeners;

import com.massivecraft.factions.*;
import com.massivecraft.factions.cmd.CmdBasePurchase;
import com.massivecraft.factions.event.FactionCreateEvent;
import com.massivecraft.factions.event.FPlayerJoinEvent;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.util.Logger;
import com.massivecraft.factions.util.PandoraMessage;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class BeaconListener implements Listener {

    private static final int CLAIM_RADIUS_CHUNKS = 4; // 100x100 blocks = ~4 chunks radius (64 blocks per side, but we use 4 for safety)

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBeaconPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() != Material.BEACON) {
            return;
        }

        Player player = event.getPlayer();
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        ItemStack item = event.getItemInHand();

        // Check if this is a base beacon item (check by display name and lore)
        boolean isBaseBeacon = false;
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            // Check display name (with or without bold)
            if (meta.hasDisplayName()) {
                String displayName = ChatColor.stripColor(meta.getDisplayName());
                if (displayName.equals("Base Beacon") || displayName.equals("Base Beacon")) {
                    isBaseBeacon = true;
                }
            }
            // Also check lore as backup
            if (!isBaseBeacon && meta.hasLore() && meta.getLore() != null) {
                for (String lore : meta.getLore()) {
                    String cleanLore = ChatColor.stripColor(lore);
                    if (cleanLore.contains("Place this in the wilderness to") || 
                        cleanLore.contains("claim a 100x100 area")) {
                        isBaseBeacon = true;
                        break;
                    }
                }
            }
        }

        // Check if player has a pending purchase OR if this is a base beacon item
        if (isBaseBeacon || CmdBasePurchase.hasPendingPurchase(player.getUniqueId())) {
            // Allow the placement - don't cancel it
            // The handleBaseBeaconPlacement will only cancel if there's an error
            handleBaseBeaconPlacement(event, player, fPlayer, block.getLocation());
        }
    }

    private void handleBaseBeaconPlacement(BlockPlaceEvent event, Player player, FPlayer fPlayer, Location beaconLoc) {
        // Null checks
        if (player == null || fPlayer == null || beaconLoc == null) {
            event.setCancelled(true);
            return;
        }

        // Check if player already has a faction
        if (fPlayer.hasFaction()) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You already have a base!")));
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("You must leave your current base before creating a new one.")));
            event.setCancelled(true);
            return;
        }

        // Check world restrictions
        if (Conf.worldsNoFactionsPlugin.contains(beaconLoc.getWorld().getName())) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Bases cannot be created in this world!")));
            event.setCancelled(true);
            return;
        }

        // Check if location is in wilderness - MUST be wilderness to place base beacon
        FLocation floc = FLocation.wrap(beaconLoc);
        Faction atFaction = Board.getInstance().getFactionAt(floc);
        
        // Only allow placement in wilderness - cancel if null, not wilderness, or in any claimed territory
        if (atFaction == null || !atFaction.isWilderness()) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You can only place your base beacon in the wilderness!")));
            if (atFaction != null && !atFaction.isWilderness()) {
                player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("This location is claimed by: ") + 
                    PandoraMessage.highlight(atFaction.getTag())));
            }
            event.setCancelled(true);
            return;
        }

        // Check if there's enough space (100x100 area = ~4 chunks radius)
        String spaceCheck = canClaimAreaWithDetails(floc, CLAIM_RADIUS_CHUNKS);
        if (spaceCheck != null) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("There isn't enough wilderness space for a 100x100 claim here!")));
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text(spaceCheck)));
            event.setCancelled(true);
            return;
        }

        // Create the faction
        Faction faction = Factions.getInstance().createFaction();
        if (faction == null) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Failed to create base! Please try again.")));
            event.setCancelled(true);
            return;
        }

        // Set faction tag (use player name as default, but check if valid)
        String tag = generateBaseTag(player.getName());
        faction.setTag(tag);

        // Set beacon location
        faction.setBeaconLocation(beaconLoc);

        // Set home at beacon location
        faction.setHome(beaconLoc);

        // Remove pending purchase
        CmdBasePurchase.removePendingPurchase(player.getUniqueId());

        // Trigger faction creation event
        FactionCreateEvent createEvent = new FactionCreateEvent(player, tag);
        Bukkit.getServer().getPluginManager().callEvent(createEvent);
        if (createEvent.isCancelled()) {
            Factions.getInstance().removeFaction(faction.getId());
            event.setCancelled(true);
            return;
        }

        // Join player to faction
        FPlayerJoinEvent joinEvent = new FPlayerJoinEvent(fPlayer, faction, FPlayerJoinEvent.PlayerJoinReason.CREATE);
        Bukkit.getServer().getPluginManager().callEvent(joinEvent);
        fPlayer.setFaction(faction, false);
        fPlayer.setRole(Role.LEADER);

        // Claim 100x100 area (~4 chunks radius)
        claimArea(faction, floc, CLAIM_RADIUS_CHUNKS);

        // Store original beacon location for respawn if needed
        final Location originalBeaconLoc = beaconLoc.clone();
        final ItemStack beaconItem = player.getInventory().getItemInMainHand().clone();
        
        // Generate base structure (walls, floor, clear interior)
        // Floor should be 1 block below the beacon
        // IMPORTANT: Run structure generation asynchronously AFTER the beacon is placed
        // to ensure the beacon block exists before we try to skip it
        int floorY = beaconLoc.getBlockY() - 1;
        if (Conf.baseAutoGenerateWalls || Conf.baseAutoGenerateFloor || Conf.baseAutoClearInterior) {
            // Use a delayed task to ensure beacon is placed first
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    // Check if beacon still exists - if not, respawn it
                    Location currentBeaconLoc = faction.getBeaconLocation();
                    if (currentBeaconLoc == null || currentBeaconLoc.getBlock().getType() != Material.BEACON) {
                        // Beacon was deleted during generation - respawn it 1 block above original location
                        Location respawnLoc = originalBeaconLoc.clone().add(0, 1, 0);
                        if (respawnLoc.getBlock().getType() == Material.AIR) {
                            respawnLoc.getBlock().setType(Material.BEACON, false);
                            faction.setBeaconLocation(respawnLoc);
                            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.warning("Beacon was moved during generation. Respawned at " + 
                                respawnLoc.getBlockX() + ", " + respawnLoc.getBlockY() + ", " + respawnLoc.getBlockZ())));
                        } else {
                            // Can't respawn at that location, give beacon back to player
                            if (player.isOnline() && player.getInventory().firstEmpty() != -1) {
                                player.getInventory().addItem(beaconItem);
                                player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Cube generation failed! Beacon returned to inventory.")));
                            }
                            return;
                        }
                    }
                    
                    if (Conf.baseAutoGenerateWalls || Conf.baseAutoGenerateFloor) {
                        com.massivecraft.factions.util.BaseStructureGenerator.generateBaseStructure(
                            faction, floc, CLAIM_RADIUS_CHUNKS, floorY, faction.getBeaconLocation());
                    }
                    // IMPORTANT: Delay clearing until after structure generation completes
                    // This prevents clearing from interfering with structure generation
                    // Clearing will happen in batches and should complete quickly
                    if (Conf.baseAutoClearInterior) {
                        // Delay clearing by 5 seconds to let structure generation start
                        // This ensures walls/floor/roof are generated before clearing
                        FactionsPlugin.getInstance().getServer().getScheduler().runTaskLater(
                            FactionsPlugin.getInstance(), () -> {
                                // Only clear if structure generation has started
                                com.massivecraft.factions.util.BaseStructureGenerator.clearInteriorBlocks(
                                    faction, floc, CLAIM_RADIUS_CHUNKS, floorY, faction.getBeaconLocation());
                            }, 100L); // 5 seconds delay (100 ticks)
                    }
                    
                    // Verify cube was generated successfully after a delay - multiple checks
                    // First check after 5 seconds
                    new org.bukkit.scheduler.BukkitRunnable() {
                        @Override
                        public void run() {
                            // Check if beacon still exists and structure was generated
                            Location checkBeaconLoc = faction.getBeaconLocation();
                            if (checkBeaconLoc == null || checkBeaconLoc.getBlock().getType() != Material.BEACON) {
                                // Beacon doesn't exist - check if it was absorbed during generation
                                // Try to respawn 1 block above original location first
                                Location respawnAttempt = originalBeaconLoc.clone().add(0, 1, 0);
                                if (respawnAttempt.getBlock().getType() == Material.AIR) {
                                    respawnAttempt.getBlock().setType(Material.BEACON, false);
                                    faction.setBeaconLocation(respawnAttempt);
                                    if (player.isOnline()) {
                                        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.warning("Beacon was respawned 1 block above original location during generation.")));
                                    }
                                } else {
                                    // Can't respawn, give beacon back to inventory
                                    if (player.isOnline()) {
                                        // Check for empty slot
                                        if (player.getInventory().firstEmpty() != -1) {
                                            player.getInventory().addItem(beaconItem);
                                        } else {
                                            // Drop at player location if inventory full
                                            player.getWorld().dropItemNaturally(player.getLocation(), beaconItem);
                                        }
                                        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Cube generation failed! Beacon returned to inventory.")));
                                    }
                                }
                            } else {
                                // Beacon exists - verify structure was generated by checking a wall block
                                // Check after a longer delay to ensure structure generation completed
                                new org.bukkit.scheduler.BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        Location finalCheckLoc = faction.getBeaconLocation();
                                        if (finalCheckLoc == null || finalCheckLoc.getBlock().getType() != Material.BEACON) {
                                            // Final check - beacon still missing, give it back
                                            if (player.isOnline()) {
                                                if (player.getInventory().firstEmpty() != -1) {
                                                    player.getInventory().addItem(beaconItem);
                                                } else {
                                                    player.getWorld().dropItemNaturally(player.getLocation(), beaconItem);
                                                }
                                                player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Cube generation verification failed! Beacon returned to inventory.")));
                                            }
                                        }
                                    }
                                }.runTaskLater(FactionsPlugin.getInstance(), 200L); // Check after 10 more seconds
                            }
                        }
                    }.runTaskLater(FactionsPlugin.getInstance(), 100L); // Check after 5 seconds
                }
            }.runTaskLater(FactionsPlugin.getInstance(), 1L); // Run 1 tick after placement
        }

        // Set economy balance if enabled
        if (Conf.econEnabled) {
            Econ.setBalance(faction.getAccountId(), Conf.econFactionStartingBalance);
        }

        // Create nametag above beacon
        com.massivecraft.factions.util.BeaconNametagManager.createNametag(faction, beaconLoc);

        // Enable beacon beam
        com.massivecraft.factions.util.BeaconBeamManager.enableBeam(faction, beaconLoc);

        // Broadcast success
        player.sendMessage("");
        player.sendMessage(PandoraMessage.header("═══════════════════════════════"));
        player.sendMessage(PandoraMessage.header("  ✓ BASE CREATED SUCCESSFULLY!"));
        player.sendMessage(PandoraMessage.header("═══════════════════════════════"));
        player.sendMessage("");
        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("Base Name: ") + 
            PandoraMessage.highlight(faction.getTag())));
        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("Beacon Location: ") + 
            PandoraMessage.highlight(beaconLoc.getBlockX() + ", " + beaconLoc.getBlockY() + ", " + beaconLoc.getBlockZ())));
        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("Claimed Area: ") + 
            PandoraMessage.highlight("100x100 blocks")));
        player.sendMessage("");
        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.highlight("A 100x100 area has been claimed around your beacon.")));
        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("⚠ WARNING: If your beacon is destroyed, your base will be disbanded!")));
        player.sendMessage("");

        // Send private message to creator about renaming
        player.sendMessage("");
        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.highlight("💡 Tip: Name Your Base")));
        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("Use ") + 
            PandoraMessage.highlight("./f rename <name>") + PandoraMessage.text(" or ") + 
            PandoraMessage.highlight("./base rename <name>") + PandoraMessage.text(" to name your base!")));
        player.sendMessage("");

        // Broadcast to all players
        if (FactionsPlugin.getInstance().getConfig().getBoolean("faction-creation-broadcast", true)) {
            String broadcastMsg = PandoraMessage.formatWithPrefix(
                PandoraMessage.text(fPlayer.getName() + " has formed a base")
            );
            for (FPlayer follower : FPlayers.getInstance().getOnlinePlayers()) {
                follower.getPlayer().sendMessage(broadcastMsg);
            }
        }

        if (Conf.logFactionCreate) {
            Logger.print(fPlayer.getName() + " created base " + tag, Logger.PrefixType.DEFAULT);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBeaconBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.BEACON) {
            return;
        }

        Location loc = block.getLocation();
        FLocation floc = FLocation.wrap(loc);
        Faction faction = Board.getInstance().getFactionAt(floc);
        
        // Check if breaking the invisible beacon (1 block above real beacon)
        // If so, don't process - only process real beacon breaks
        if (faction != null && faction.hasBeacon()) {
            Location realBeaconLoc = faction.getBeaconLocation();
            if (realBeaconLoc != null && 
                realBeaconLoc.getWorld() != null &&
                loc.getWorld() != null &&
                realBeaconLoc.getWorld().getName().equals(loc.getWorld().getName()) &&
                realBeaconLoc.getBlockX() == loc.getBlockX() &&
                realBeaconLoc.getBlockY() + 1 == loc.getBlockY() &&
                realBeaconLoc.getBlockZ() == loc.getBlockZ()) {
                // This is the invisible beacon - cancel the break and don't process
                event.setCancelled(true);
                return;
            }
        }

        if (faction == null || !faction.isNormal()) {
            return;
        }

        // Check if this is the faction's beacon
        if (faction.hasBeacon()) {
            Location beaconLoc = faction.getBeaconLocation();
            if (beaconLoc != null && 
                beaconLoc.getWorld() != null &&
                loc.getWorld() != null &&
                beaconLoc.getWorld().getName().equals(loc.getWorld().getName()) &&
                beaconLoc.getBlockX() == loc.getBlockX() &&
                beaconLoc.getBlockY() == loc.getBlockY() &&
                beaconLoc.getBlockZ() == loc.getBlockZ()) {
                
                // Allow anyone to break the beacon - don't cancel the event
                // This is the base beacon - disband the faction
                Player player = event.getPlayer();
                
                // Schedule disbandment after block break completes
                Bukkit.getScheduler().runTask(FactionsPlugin.getInstance(), () -> {
                    // Notify all members
                    String factionTag = faction.getTag();
                    for (FPlayer member : faction.getFPlayers()) {
                        if (member.isOnline() && member.getPlayer() != null) {
                            member.getPlayer().sendMessage("");
                            member.getPlayer().sendMessage(PandoraMessage.error("═══════════════════════════════"));
                            member.getPlayer().sendMessage(PandoraMessage.error("  ✗ BASE BEACON DESTROYED!"));
                            member.getPlayer().sendMessage(PandoraMessage.error("═══════════════════════════════"));
                            member.getPlayer().sendMessage("");
                            member.getPlayer().sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Your base beacon has been destroyed!")));
                            member.getPlayer().sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Base ") + 
                                PandoraMessage.highlight(factionTag) + PandoraMessage.error(" has been disbanded.")));
                            member.getPlayer().sendMessage("");
                        }
                    }

                    // Remove nametag and beam before disbanding
                    com.massivecraft.factions.util.BeaconNametagManager.removeNametag(faction);
                    com.massivecraft.factions.util.BeaconBeamManager.removeBeam(faction, beaconLoc);

                    // Remove all base structure blocks (walls, floor, roof)
                    FLocation beaconFloc = FLocation.wrap(beaconLoc);
                    int floorY = beaconLoc.getBlockY() - 1;
                    com.massivecraft.factions.util.BaseStructureGenerator.removeBaseStructure(
                        faction, beaconFloc, CLAIM_RADIUS_CHUNKS, floorY);

                    // Disband the faction - ALWAYS disband, no matter what
                    try {
                        faction.disband(player != null ? player : null, com.massivecraft.factions.event.FactionDisbandEvent.PlayerDisbandReason.PLUGIN);
                    } catch (Exception e) {
                        // Force disband even if there's an error
                        Factions.getInstance().removeFaction(faction.getId());
                    }
                    
                    if (player != null && player.isOnline()) {
                        player.sendMessage("");
                        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You have destroyed the base beacon!")));
                        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Base ") + 
                            PandoraMessage.highlight(factionTag) + PandoraMessage.error(" has been disbanded.")));
                        player.sendMessage("");
                    }
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBeaconExplode(BlockExplodeEvent event) {
        // Check if any beacon blocks were destroyed in the explosion
        for (Block block : new ArrayList<>(event.blockList())) {
            if (block.getType() != Material.BEACON) {
                continue;
            }

            Location loc = block.getLocation();
            FLocation floc = FLocation.wrap(loc);
            Faction faction = Board.getInstance().getFactionAt(floc);
            
            if (faction == null || !faction.isNormal()) {
                continue;
            }

            // Check if this is the faction's beacon
            if (faction.hasBeacon()) {
                Location beaconLoc = faction.getBeaconLocation();
                if (beaconLoc != null && 
                    beaconLoc.getWorld() != null &&
                    loc.getWorld() != null &&
                    beaconLoc.getWorld().getName().equals(loc.getWorld().getName()) &&
                    beaconLoc.getBlockX() == loc.getBlockX() &&
                    beaconLoc.getBlockY() == loc.getBlockY() &&
                    beaconLoc.getBlockZ() == loc.getBlockZ()) {
                    
                    // This is the base beacon - disband the faction
                    // Schedule disbandment after explosion completes
                    Bukkit.getScheduler().runTask(FactionsPlugin.getInstance(), () -> {
                        // Notify all members
                        String factionTag = faction.getTag();
                        for (FPlayer member : faction.getFPlayers()) {
                            if (member.isOnline() && member.getPlayer() != null) {
                                member.getPlayer().sendMessage("");
                                member.getPlayer().sendMessage(PandoraMessage.error("═══════════════════════════════"));
                                member.getPlayer().sendMessage(PandoraMessage.error("  ✗ BASE BEACON DESTROYED!"));
                                member.getPlayer().sendMessage(PandoraMessage.error("═══════════════════════════════"));
                                member.getPlayer().sendMessage("");
                                member.getPlayer().sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Your base beacon has been destroyed!")));
                                member.getPlayer().sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Base ") + 
                                    PandoraMessage.highlight(factionTag) + PandoraMessage.error(" has been disbanded.")));
                                member.getPlayer().sendMessage("");
                            }
                        }

                        // Remove nametag and beam before disbanding
                        com.massivecraft.factions.util.BeaconNametagManager.removeNametag(faction);
                        com.massivecraft.factions.util.BeaconBeamManager.removeBeam(faction, beaconLoc);

                        // Remove all base structure blocks (walls, floor, roof)
                        FLocation beaconFloc = FLocation.wrap(beaconLoc);
                        int floorY = beaconLoc.getBlockY() - 1;
                        com.massivecraft.factions.util.BaseStructureGenerator.removeBaseStructure(
                            faction, beaconFloc, CLAIM_RADIUS_CHUNKS, floorY);

                        // Disband the faction - ALWAYS disband, no matter what
                        try {
                            faction.disband(null, com.massivecraft.factions.event.FactionDisbandEvent.PlayerDisbandReason.PLUGIN);
                        } catch (Exception e) {
                            // Force disband even if there's an error
                            Factions.getInstance().removeFaction(faction.getId());
                        }
                    });
                    
                    // Don't process other beacons if we found one
                    break;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBeaconEntityExplode(EntityExplodeEvent event) {
        // Check if any beacon blocks were destroyed in the explosion
        for (Block block : new ArrayList<>(event.blockList())) {
            if (block.getType() != Material.BEACON) {
                continue;
            }

            Location loc = block.getLocation();
            FLocation floc = FLocation.wrap(loc);
            Faction faction = Board.getInstance().getFactionAt(floc);
            
            if (faction == null || !faction.isNormal()) {
                continue;
            }

            // Check if this is the faction's beacon
            if (faction.hasBeacon()) {
                Location beaconLoc = faction.getBeaconLocation();
                if (beaconLoc != null && 
                    beaconLoc.getWorld() != null &&
                    loc.getWorld() != null &&
                    beaconLoc.getWorld().getName().equals(loc.getWorld().getName()) &&
                    beaconLoc.getBlockX() == loc.getBlockX() &&
                    beaconLoc.getBlockY() == loc.getBlockY() &&
                    beaconLoc.getBlockZ() == loc.getBlockZ()) {
                    
                    // This is the base beacon - disband the faction
                    // Schedule disbandment after explosion completes
                    Bukkit.getScheduler().runTask(FactionsPlugin.getInstance(), () -> {
                        // Notify all members
                        String factionTag = faction.getTag();
                        for (FPlayer member : faction.getFPlayers()) {
                            if (member.isOnline() && member.getPlayer() != null) {
                                member.getPlayer().sendMessage("");
                                member.getPlayer().sendMessage(PandoraMessage.error("═══════════════════════════════"));
                                member.getPlayer().sendMessage(PandoraMessage.error("  ✗ BASE BEACON DESTROYED!"));
                                member.getPlayer().sendMessage(PandoraMessage.error("═══════════════════════════════"));
                                member.getPlayer().sendMessage("");
                                member.getPlayer().sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Your base beacon has been destroyed!")));
                                member.getPlayer().sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Base ") + 
                                    PandoraMessage.highlight(factionTag) + PandoraMessage.error(" has been disbanded.")));
                                member.getPlayer().sendMessage("");
                            }
                        }

                        // Remove nametag and beam before disbanding
                        com.massivecraft.factions.util.BeaconNametagManager.removeNametag(faction);
                        com.massivecraft.factions.util.BeaconBeamManager.removeBeam(faction, beaconLoc);

                        // Remove all base structure blocks (walls, floor, roof)
                        FLocation beaconFloc = FLocation.wrap(beaconLoc);
                        int floorY = beaconLoc.getBlockY() - 1;
                        com.massivecraft.factions.util.BaseStructureGenerator.removeBaseStructure(
                            faction, beaconFloc, CLAIM_RADIUS_CHUNKS, floorY);

                        // Disband the faction - ALWAYS disband, no matter what
                        try {
                            faction.disband(null, com.massivecraft.factions.event.FactionDisbandEvent.PlayerDisbandReason.PLUGIN);
                        } catch (Exception e) {
                            // Force disband even if there's an error
                            Factions.getInstance().removeFaction(faction.getId());
                        }
                    });
                    
                    // Don't process other beacons if we found one
                    break;
                }
            }
        }
    }

    private String canClaimAreaWithDetails(FLocation center, int radiusChunks) {
        int conflictCount = 0;
        String conflictFaction = null;
        
        for (int x = -radiusChunks; x <= radiusChunks; x++) {
            for (int z = -radiusChunks; z <= radiusChunks; z++) {
                FLocation checkLoc = FLocation.wrap(center.getWorldName(), center.getIntX() + x, center.getIntZ() + z);
                Faction at = Board.getInstance().getFactionAt(checkLoc);
                if (at == null || !at.isWilderness()) {
                    conflictCount++;
                    if (conflictFaction == null && at != null) {
                        conflictFaction = at.getTag();
                    }
                }
            }
        }
        
        if (conflictCount > 0) {
            if (conflictFaction != null) {
                return "Conflicts with " + conflictFaction + "'s territory (" + conflictCount + " chunks).";
            }
            return "Not enough wilderness space (" + conflictCount + " chunks are claimed).";
        }
        return null; // Success
    }
    
    private boolean canClaimArea(FLocation center, int radiusChunks) {
        return canClaimAreaWithDetails(center, radiusChunks) == null;
    }
    
    private String generateBaseTag(String playerName) {
        // Clean player name
        String tag = playerName;
        
        // Remove invalid characters
        tag = tag.replaceAll("[^a-zA-Z0-9]", "");
        
        // Ensure minimum length
        if (tag.length() < Conf.factionTagLengthMin) {
            tag = tag + "Base";
        }
        
        // Ensure maximum length
        if (tag.length() > Conf.factionTagLengthMax) {
            tag = tag.substring(0, Conf.factionTagLengthMax);
        }
        
        // Check if tag is taken and add suffix if needed
        int attempts = 0;
        String originalTag = tag;
        while (Factions.getInstance().isTagTaken(tag) && attempts < 100) {
            int suffix = (int)(System.currentTimeMillis() % 10000) + attempts;
            tag = originalTag.substring(0, Math.min(originalTag.length(), Conf.factionTagLengthMax - 3)) + suffix;
            attempts++;
        }
        
        // Final validation
        if (tag.length() < Conf.factionTagLengthMin) {
            tag = "Base" + System.currentTimeMillis() % 1000;
        }
        
        return tag;
    }

    private void claimArea(Faction faction, FLocation center, int radiusChunks) {
        int claimed = 0;
        int failed = 0;
        
        for (int x = -radiusChunks; x <= radiusChunks; x++) {
            for (int z = -radiusChunks; z <= radiusChunks; z++) {
                try {
                    FLocation claimLoc = FLocation.wrap(center.getWorldName(), center.getIntX() + x, center.getIntZ() + z);
                    if (claimLoc != null) {
                        Board.getInstance().setFactionAt(faction, claimLoc);
                        claimed++;
                    } else {
                        failed++;
                    }
                } catch (Exception e) {
                    failed++;
                    if (Conf.logFactionCreate) {
                        Logger.print("Failed to claim chunk at " + (center.getIntX() + x) + "," + (center.getIntZ() + z) + 
                            " for base " + faction.getTag() + ": " + e.getMessage(), Logger.PrefixType.FAILED);
                    }
                }
            }
        }
        
        if (Conf.logFactionCreate) {
            Logger.print("Claimed " + claimed + " chunks" + (failed > 0 ? " (failed: " + failed + ")" : "") + 
                " for base " + faction.getTag(), Logger.PrefixType.DEFAULT);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBeaconInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BEACON) {
            return;
        }

        Player player = event.getPlayer();
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        
        if (fPlayer == null || !fPlayer.hasFaction()) {
            return;
        }

        Faction faction = fPlayer.getFaction();
        if (faction == null || !faction.isNormal() || !faction.hasBeacon()) {
            return;
        }

        Location beaconLoc = faction.getBeaconLocation();
        if (beaconLoc == null || 
            !beaconLoc.getWorld().getName().equals(block.getWorld().getName()) ||
            beaconLoc.getBlockX() != block.getX() ||
            beaconLoc.getBlockY() != block.getY() ||
            beaconLoc.getBlockZ() != block.getZ()) {
            return; // Not this faction's beacon
        }

        // Check if player has access (must be member)
        if (!faction.getFPlayers().contains(fPlayer)) {
            return;
        }

        // Only mods and owners can access beacon menu, not regular members
        if (fPlayer.getRole() != Role.LEADER && fPlayer.getRole() != Role.MODERATOR) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Only mods and owners can access the beacon menu!")));
            event.setCancelled(true);
            return;
        }

        // Open beacon menu
        event.setCancelled(true);
        com.massivecraft.factions.util.BeaconMenuGUI.openMenu(player, faction);
    }
}

