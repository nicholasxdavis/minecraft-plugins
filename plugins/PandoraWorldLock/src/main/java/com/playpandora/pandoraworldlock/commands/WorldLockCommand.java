package com.playpandora.pandoraworldlock.commands;

import com.playpandora.pandoraworldlock.PandoraWorldLock;
import com.playpandora.pandoraworldlock.WorldLockManager;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class WorldLockCommand implements CommandExecutor, TabCompleter {
    
    private final PandoraWorldLock plugin;
    private final WorldLockManager worldLockManager;
    
    public WorldLockCommand(PandoraWorldLock plugin) {
        this.plugin = plugin;
        this.worldLockManager = plugin.getWorldLockManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pandoraworldlock.admin")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&c&lError &8» &7You don't have permission to use this command!"));
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "lock":
                return handleLock(sender, args);
            case "unlock":
                return handleUnlock(sender, args);
            case "list":
                return handleList(sender);
            case "info":
                return handleInfo(sender, args);
            case "reload":
                return handleReload(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private boolean handleLock(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&c&lUsage &8» &7/pwl lock <world> [time]"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&7Time format: &e1d2h30m &7(days, hours, minutes) or &e2025-02-20 17:00"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&7Leave time empty to lock forever."));
            return true;
        }
        
        String worldName = args[1];
        World world = findWorld(worldName);
        
        if (world == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&c&lError &8» &7World &e" + worldName + " &7not found!"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&7Available worlds: &e" + plugin.getServer().getWorlds().stream()
                    .map(World::getName)
                    .collect(Collectors.joining(", "))));
            return true;
        }
        
        LocalDateTime until = null;
        if (args.length >= 3) {
            String timeArg = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            Optional<LocalDateTime> parsed = worldLockManager.parseDuration(timeArg);
            if (!parsed.isPresent()) {
                parsed = worldLockManager.parseDateTime(timeArg);
            }
            if (parsed.isPresent()) {
                until = parsed.get();
            } else {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&c&lError &8» &7Invalid time format: &e" + timeArg));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&7Use: &e1d2h30m &7or &e2025-02-20 17:00"));
                return true;
            }
        }
        
        WorldLockManager.LockResult result = worldLockManager.lockWorld(world.getName(), until);
        if (result.success()) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&a&lSuccess &8» &7" + result.message()));
            
            // Teleport all players out of the world if it's now locked
            for (Player player : world.getPlayers()) {
                worldLockManager.teleportToSpawn(player);
                player.sendMessage(worldLockManager.getColoredLockedMessage(world));
            }
        } else {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&c&lError &8» &7" + result.message()));
        }
        
        return true;
    }
    
    private boolean handleUnlock(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&c&lUsage &8» &7/pwl unlock <world>"));
            return true;
        }
        
        String worldName = args[1];
        World world = findWorld(worldName);
        
        if (world == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&c&lError &8» &7World &e" + worldName + " &7not found!"));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&7Available worlds: &e" + plugin.getServer().getWorlds().stream()
                    .map(World::getName)
                    .collect(Collectors.joining(", "))));
            return true;
        }
        
        WorldLockManager.LockResult result = worldLockManager.unlockWorld(world.getName());
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&a&lSuccess &8» &7" + result.message()));
        
        return true;
    }
    
    private boolean handleList(CommandSender sender) {
        List<World> lockedWorlds = plugin.getServer().getWorlds().stream()
            .filter(worldLockManager::isWorldLocked)
            .collect(Collectors.toList());
        
        if (lockedWorlds.isEmpty()) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&e&lPandora WorldLock &8» &7No worlds are currently locked."));
            return true;
        }
        
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&e&lPandora WorldLock &8» &7Locked worlds:"));
        for (World world : lockedWorlds) {
            String unlockMsg = worldLockManager.getUnlockMessage(world);
            String status = unlockMsg.equals("indefinitely") ? 
                "&cIndefinite" : "&6" + unlockMsg;
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&8  • &e" + world.getName() + " &8(&7" + world.getEnvironment().name() + 
                "&8) &7- Unlocks: " + status));
        }
        
        return true;
    }
    
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&c&lUsage &8» &7/pwl info <world>"));
            return true;
        }
        
        String worldName = args[1];
        World world = findWorld(worldName);
        
        if (world == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&c&lError &8» &7World &e" + worldName + " &7not found!"));
            return true;
        }
        
        boolean locked = worldLockManager.isWorldLocked(world);
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&e&lPandora WorldLock &8» &7World Info:"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&8  • &7Name: &e" + world.getName()));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&8  • &7Environment: &e" + world.getEnvironment().name()));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&8  • &7Status: " + (locked ? "&cLocked" : "&aUnlocked")));
        
        if (locked) {
            String unlockMsg = worldLockManager.getUnlockMessage(world);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                "&8  • &7Unlocks: " + (unlockMsg.equals("indefinitely") ? 
                    "&cIndefinite" : "&6" + unlockMsg)));
        }
        
        return true;
    }
    
    private boolean handleReload(CommandSender sender) {
        plugin.reloadConfig();
        worldLockManager.loadLocks();
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&a&lSuccess &8» &7Configuration reloaded!"));
        return true;
    }
    
    private World findWorld(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        
        String lowerInput = input.toLowerCase();
        
        // Exact match first
        for (World world : plugin.getServer().getWorlds()) {
            if (world.getName().equalsIgnoreCase(input)) {
                return world;
            }
        }
        
        // Partial match
        for (World world : plugin.getServer().getWorlds()) {
            if (world.getName().toLowerCase().contains(lowerInput)) {
                return world;
            }
        }
        
        // Environment match (nether, end, normal)
        try {
            World.Environment env = World.Environment.valueOf(input.toUpperCase());
            for (World world : plugin.getServer().getWorlds()) {
                if (world.getEnvironment() == env) {
                    return world;
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Not an environment name
        }
        
        return null;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&e&lPandora WorldLock &8» &7Commands:"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&8  • &e/pwl lock <world> [time] &7- Lock a world"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&8  • &e/pwl unlock <world> &7- Unlock a world"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&8  • &e/pwl list &7- List all locked worlds"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&8  • &e/pwl info <world> &7- Get world lock info"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&8  • &e/pwl reload &7- Reload configuration"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&7Time format: &e1d2h30m &7or &e2025-02-20 17:00"));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("pandoraworldlock.admin")) {
            return new ArrayList<>();
        }
        
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("lock", "unlock", "list", "info", "reload"), completions);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("lock") || args[0].equalsIgnoreCase("unlock") || args[0].equalsIgnoreCase("info")) {
                List<String> worldNames = plugin.getServer().getWorlds().stream()
                    .map(World::getName)
                    .collect(Collectors.toList());
                StringUtil.copyPartialMatches(args[1], worldNames, completions);
            }
        }
        
        return completions;
    }
}

