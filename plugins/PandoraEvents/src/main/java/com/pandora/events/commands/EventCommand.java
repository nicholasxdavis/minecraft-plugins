package com.pandora.events.commands;

import com.pandora.events.PandoraEventsPlugin;
import com.pandora.events.listeners.FactionNameProtectionListener;
import com.pandora.events.managers.EventManager;
import com.pandora.events.util.PandoraMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventCommand implements CommandExecutor {
    
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhd])", Pattern.CASE_INSENSITIVE);
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "start":
                return handleStart(sender, args);
            case "tp":
            case "teleport":
                return handleTeleport(sender);
            case "auto":
                return handleAuto(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(PandoraMessage.header("=== PandoraEvents Commands ==="));
        sender.sendMessage(PandoraMessage.success("/basevent start [time] ") + PandoraMessage.text("- Start an event (optionally with delay)"));
        sender.sendMessage(PandoraMessage.text("  Examples: ") + PandoraMessage.highlight("/basevent start 5m") + 
            PandoraMessage.text(", ") + PandoraMessage.highlight("/basevent start 1h") + 
            PandoraMessage.text(", ") + PandoraMessage.highlight("/basevent start 30s"));
        sender.sendMessage(PandoraMessage.success("/basevent tp ") + PandoraMessage.text("- Teleport to active event (admin only)"));
        sender.sendMessage(PandoraMessage.success("/basevent auto [true/false] ") + PandoraMessage.text("- Enable/disable auto-events"));
    }
    
    private boolean handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pandoraevents.spawn")) {
            sender.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You don't have permission to use this command!")));
            return true;
        }
        
        // Require player to be online
        if (!(sender instanceof Player)) {
            sender.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("This command must be run by a player!")));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (PandoraEventsPlugin.getInstance().getEventManager().isEventActive()) {
            sender.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("An event is already active!")));
            return true;
        }
        
        // Get player's location
        Location spawnLocation = player.getLocation();
        
        long delayTicks = 0;
        
        // Parse time delay if provided
        if (args.length > 1) {
            String timeArg = args[1];
            delayTicks = parseTimeToTicks(timeArg);
            
            if (delayTicks < 0) {
                sender.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Invalid time format! Use: 30s, 5m, 1h, 4h, etc.")));
                return true;
            }
        }
        
        // Protect the event name first
        FactionNameProtectionListener.protectEventName();
        
        // Store location for delayed spawn (in case player moves)
        final Location finalSpawnLocation = spawnLocation.clone();
        
        if (delayTicks > 0) {
            // Schedule event
            long delaySeconds = delayTicks / 20;
            String timeStr = formatTime(delaySeconds);
            sender.sendMessage(PandoraMessage.formatWithPrefix(
                PandoraMessage.success("Event will start in ") + 
                PandoraMessage.highlight(timeStr) + 
                PandoraMessage.text(" at your location!")
            ));
            Bukkit.broadcastMessage(PandoraMessage.format(
                PandoraMessage.success("An event will start in ") + 
                PandoraMessage.highlight(timeStr) + 
                PandoraMessage.text(" at ") + 
                PandoraMessage.highlight(player.getName() + "'s location") + 
                PandoraMessage.text("!")
            ));
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.broadcastMessage(PandoraMessage.format(
                        PandoraMessage.highlight("The event is starting now!")
                    ));
                    PandoraEventsPlugin.getInstance().getEventSpawner().spawnEventBase(finalSpawnLocation);
                }
            }.runTaskLater(PandoraEventsPlugin.getInstance(), delayTicks);
        } else {
            // Start immediately
            sender.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.success("Spawning event base at your location...")));
            boolean success = PandoraEventsPlugin.getInstance().getEventSpawner().spawnEventBase(spawnLocation);
            
            if (success) {
                sender.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.success("Event base spawned successfully at your location!")));
            } else {
                sender.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Failed to spawn event base. Check console for details.")));
            }
        }
        
        return true;
    }
    
    private boolean handleTeleport(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("This command can only be used by players!")));
            return true;
        }
        
        if (!sender.hasPermission("pandoraevents.tp")) {
            sender.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You don't have permission to use this command!")));
            return true;
        }
        
        EventManager eventManager = PandoraEventsPlugin.getInstance().getEventManager();
        
        if (!eventManager.isEventActive()) {
            sender.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("No event is currently active!")));
            return true;
        }
        
        Location beaconLoc = eventManager.getEventBeaconLocation();
        if (beaconLoc == null) {
            sender.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Event beacon location not found!")));
            return true;
        }
        
        Player player = (Player) sender;
        player.teleport(beaconLoc);
        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.success("Teleported to event beacon!")));
        
        return true;
    }
    
    private boolean handleAuto(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pandoraevents.admin")) {
            sender.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You don't have permission to use this command!")));
            return true;
        }
        
        if (args.length < 2) {
            boolean enabled = PandoraEventsPlugin.getInstance().getAutoEventScheduler().isAutoEnabled();
            sender.sendMessage(PandoraMessage.formatWithPrefix(
                PandoraMessage.success("Auto-events are currently: ") + 
                PandoraMessage.highlight(enabled ? "enabled" : "disabled")
            ));
            sender.sendMessage(PandoraMessage.text("Use: /basevent auto true/false"));
            return true;
        }
        
        String value = args[1].toLowerCase();
        boolean enable;
        
        if (value.equals("true") || value.equals("on") || value.equals("enable")) {
            enable = true;
        } else if (value.equals("false") || value.equals("off") || value.equals("disable")) {
            enable = false;
        } else {
            sender.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Invalid value! Use: true/false")));
            return true;
        }
        
        PandoraEventsPlugin.getInstance().getAutoEventScheduler().setAutoEnabled(enable);
        sender.sendMessage(PandoraMessage.formatWithPrefix(
            PandoraMessage.success("Auto-events ") + 
            PandoraMessage.highlight(enable ? "enabled" : "disabled")
        ));
        
        return true;
    }
    
    /**
     * Parse time string to ticks (e.g., "5m" -> 6000 ticks)
     */
    private long parseTimeToTicks(String timeStr) {
        Matcher matcher = TIME_PATTERN.matcher(timeStr);
        
        if (!matcher.matches()) {
            return -1;
        }
        
        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2).toLowerCase();
        
        long seconds;
        switch (unit) {
            case "s":
                seconds = amount;
                break;
            case "m":
                seconds = amount * 60;
                break;
            case "h":
                seconds = amount * 3600;
                break;
            case "d":
                seconds = amount * 86400;
                break;
            default:
                return -1;
        }
        
        return seconds * 20; // Convert to ticks
    }
    
    /**
     * Format seconds to readable time string
     */
    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + " second" + (seconds != 1 ? "s" : "");
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return minutes + " minute" + (minutes != 1 ? "s" : "");
        } else {
            long hours = seconds / 3600;
            return hours + " hour" + (hours != 1 ? "s" : "");
        }
    }
}
