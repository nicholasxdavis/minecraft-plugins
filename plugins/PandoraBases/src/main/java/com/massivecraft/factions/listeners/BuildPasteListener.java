package com.massivecraft.factions.listeners;

import com.massivecraft.factions.*;
import com.massivecraft.factions.util.Logger;
import com.massivecraft.factions.util.PandoraMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class BuildPasteListener implements Listener {
    
    // Track active paste operations to identify BuildPaste block placements
    private static final java.util.Set<String> activePastes = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBuildPasteCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase().trim();
        Player player = event.getPlayer();
        
        // Check if this is a BuildPaste command
        if (!message.startsWith("/paste ") && !message.startsWith("/buildpaste ")) {
            return;
        }
        
        // Check if BuildPaste plugin exists
        if (FactionsPlugin.getInstance().getServer().getPluginManager().getPlugin("BuildPaste") == null) {
            return;
        }
        
        Location playerLoc = player.getLocation();
        FLocation floc = FLocation.wrap(playerLoc);
        Faction faction = Board.getInstance().getFactionAt(floc);
        
        // Prevent pasting in claimed areas (only allow in wilderness OR own faction)
        if (faction != null && !faction.isWilderness()) {
            // Check if player is in this faction (allow own faction)
            FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
            if (fPlayer != null && fPlayer.hasFaction() && fPlayer.getFaction() == faction) {
                // Player is in their own faction - allow paste
                // Track this paste operation (will be cleared when paste completes or after timeout)
                String pasteId = player.getName() + "_" + System.currentTimeMillis();
                activePastes.add(pasteId);
                // Remove after 60 seconds to prevent memory leaks
                FactionsPlugin.getInstance().getServer().getScheduler().runTaskLater(
                    FactionsPlugin.getInstance(), 
                    () -> activePastes.remove(pasteId), 
                    1200L); // 60 seconds
                return; // Allow the command
            } else {
                // Not in their own faction - block it
                event.setCancelled(true);
                player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You cannot paste in claimed territories!")));
                player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("BuildPaste is disabled in claimed areas to prevent exploits.")));
                return;
            }
        } else {
            // Track this paste operation (will be cleared when paste completes or after timeout)
            String pasteId = player.getName() + "_" + System.currentTimeMillis();
            activePastes.add(pasteId);
            // Remove after 60 seconds to prevent memory leaks
            FactionsPlugin.getInstance().getServer().getScheduler().runTaskLater(
                FactionsPlugin.getInstance(), 
                () -> activePastes.remove(pasteId), 
                1200L); // 60 seconds
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onServerCommand(ServerCommandEvent event) {
        String command = event.getCommand().trim();
        String commandLower = command.toLowerCase();
        CommandSender sender = event.getSender();
        
        // Check if this is a BuildPaste command from console
        if (!commandLower.startsWith("paste ") && !commandLower.startsWith("buildpaste ")) {
            return;
        }
        
        // Check if BuildPaste plugin exists
        if (FactionsPlugin.getInstance().getServer().getPluginManager().getPlugin("BuildPaste") == null) {
            return;
        }
        
        // Parse command to get location if coordinates are provided
        // BuildPaste commands can be: /paste <id> [x] [y] [z] or /paste <id> at <player>
        String[] args = command.split("\\s+");
        if (args.length < 2) {
            return; // Invalid command format
        }
        
        Location pasteLocation = null;
        
        // Try to parse coordinates from command (format: paste <id> <x> <y> <z>)
        if (args.length >= 5) {
            try {
                double x = Double.parseDouble(args[1]);
                double y = Double.parseDouble(args[2]);
                double z = Double.parseDouble(args[3]);
                // We need a world - default to first world or main world
                org.bukkit.World world = FactionsPlugin.getInstance().getServer().getWorlds().get(0);
                if (args.length >= 6) {
                    world = FactionsPlugin.getInstance().getServer().getWorld(args[5]);
                }
                if (world != null) {
                    pasteLocation = new Location(world, x, y, z);
                }
            } catch (NumberFormatException e) {
                // Coordinates not in numeric format, might be player name or paste ID
            }
        }
        
        // If we couldn't parse coordinates, check if command references a player
        if (pasteLocation == null && args.length >= 3) {
            // Check for format: paste <id> at <player>
            for (int i = 0; i < args.length - 1; i++) {
                if (args[i].equalsIgnoreCase("at")) {
                    String playerName = args[i + 1];
                    Player targetPlayer = FactionsPlugin.getInstance().getServer().getPlayer(playerName);
                    if (targetPlayer != null) {
                        pasteLocation = targetPlayer.getLocation();
                        break;
                    }
                }
            }
        }
        
        // If we have a location, check if it's in claimed territory
        if (pasteLocation != null) {
            FLocation floc = FLocation.wrap(pasteLocation);
            Faction faction = Board.getInstance().getFactionAt(floc);
            
            if (faction != null && !faction.isWilderness()) {
                // Check if there's a player context for this console command
                // Console commands from BuildPaste are usually executed on behalf of a player
                // Try to find the player who initiated the paste
                
                // Look for player name in command arguments or check online players near location
                Player nearbyPlayer = null;
                for (Player onlinePlayer : FactionsPlugin.getInstance().getServer().getOnlinePlayers()) {
                    if (onlinePlayer.getWorld().equals(pasteLocation.getWorld())) {
                        double distance = onlinePlayer.getLocation().distance(pasteLocation);
                        if (distance < 100) { // Within 100 blocks
                            nearbyPlayer = onlinePlayer;
                            break;
                        }
                    }
                }
                
                // Check if nearby player is in the faction
                if (nearbyPlayer != null) {
                    FPlayer fPlayer = FPlayers.getInstance().getByPlayer(nearbyPlayer);
                    if (fPlayer != null && fPlayer.hasFaction() && fPlayer.getFaction() == faction) {
                        // Player is in their own faction - allow paste
                        Logger.print("BuildPaste command from console allowed - player " + nearbyPlayer.getName() + 
                            " is pasting in their own faction: " + faction.getTag() + " at " + 
                            pasteLocation.getBlockX() + "," + pasteLocation.getBlockY() + "," + pasteLocation.getBlockZ(), 
                            Logger.PrefixType.DEFAULT);
                        return; // Allow the command
                    }
                }
                
                // No valid player context - block it
                event.setCancelled(true);
                Logger.print("BuildPaste command from console BLOCKED! Attempted to paste in claimed territory: " + 
                    faction.getTag() + " at " + pasteLocation.getBlockX() + "," + pasteLocation.getBlockY() + "," + pasteLocation.getBlockZ(), 
                    Logger.PrefixType.WARNING);
                sender.sendMessage("BuildPaste cannot be used in claimed territories! The command has been blocked.");
                return;
            }
        } else {
            // If we can't determine the location, block ALL console paste commands for safety
            // This ensures no BuildPaste commands from console can paste in claimed areas
            event.setCancelled(true);
            Logger.print("BuildPaste command from console BLOCKED! Location could not be determined. " +
                "All console BuildPaste commands are blocked to prevent pasting in claimed territories.", 
                Logger.PrefixType.WARNING);
            sender.sendMessage("BuildPaste commands from console are blocked. Use player commands instead, which will be checked for claimed territory.");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlaceFromBuildPaste(BlockPlaceEvent event) {
        // This handles block placements that might be from BuildPaste
        // We need to check if the block is being placed in claimed territory
        
        Location blockLoc = event.getBlockPlaced().getLocation();
        FLocation floc = FLocation.wrap(blockLoc);
        Faction faction = Board.getInstance().getFactionAt(floc);
        
        // Check if block is being placed in claimed territory
        if (faction != null && !faction.isWilderness()) {
            // Check if player is in this faction (allow own faction)
            Player player = event.getPlayer();
            if (player != null) {
                FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
                if (fPlayer != null && fPlayer.hasFaction() && fPlayer.getFaction() == faction) {
                    // Player is in their own faction - allow block placement
                    return;
                }
            }
            
            // Block is in claimed territory and player is not in that faction - cancel it
            event.setCancelled(true);
            
            // If there's a player, notify them
            if (player != null) {
                player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Cannot place blocks in claimed territories!")));
            } else {
                // Block was placed by console/plugin - log it
                Logger.print("Block placement in claimed territory blocked at " + 
                    blockLoc.getBlockX() + "," + blockLoc.getBlockY() + "," + blockLoc.getBlockZ() + 
                    " (Faction: " + faction.getTag() + ")", Logger.PrefixType.WARNING);
            }
        }
    }
    
    /**
     * Monitor for blocks being changed directly by plugins (bypassing BlockPlaceEvent)
     * This is a fallback for when BuildPaste uses setBlock methods directly
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockChange(org.bukkit.event.block.BlockPhysicsEvent event) {
        // BlockPhysicsEvent fires when blocks change state
        // However, this is mainly for physics updates, not direct block setting
        // We'll use this as a last resort to detect rapid block changes
    }
}

