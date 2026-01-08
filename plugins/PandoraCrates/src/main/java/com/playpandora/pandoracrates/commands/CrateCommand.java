package com.playpandora.pandoracrates.commands;

import com.playpandora.pandoracrates.PandoraCrates;
import com.playpandora.pandoracrates.models.Crate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class CrateCommand implements CommandExecutor {
    
    private final PandoraCrates plugin;
    
    public CrateCommand(PandoraCrates plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "give":
                // Check if next arg is "crate" or "key"
                if (args.length >= 2) {
                    String nextArg = args[1].toLowerCase();
                    if (nextArg.equals("crate")) {
                        return handleGiveCrate(sender, args);
                    } else if (nextArg.equals("key")) {
                        return handleGiveKey(sender, args);
                    }
                }
                // Fallback to old behavior (key give)
                return handleGiveKey(sender, args);
            case "key":
                // Check if next arg is "all"
                if (args.length >= 2 && args[1].equalsIgnoreCase("all")) {
                    return handleGiveKeyAll(sender, args);
                }
                return handleGiveKey(sender, args);
            case "crate":
                return handleGiveCrate(sender, args);
            case "preview":
                return handlePreview(sender, args);
            case "setholo":
            case "set":
                return handleSetCrate(sender, args);
            case "reload":
                return handleReload(sender);
            case "stats":
            case "statistics":
                return handleStats(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }
    
    private boolean handleGiveKey(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pandoracrates.key.give")) {
            sender.sendMessage(plugin.formatMessage("no-permission", 
                "{prefix} &cYou don't have permission to use this command!"));
            return true;
        }
        
        // Support both: /crate key give <player> <crate> [amount] and /crate give key <player> <crate> [amount]
        int playerIndex = 1;
        int crateIndex = 2;
        int amountIndex = 3;
        
        if (args.length >= 2 && args[1].equalsIgnoreCase("give")) {
            playerIndex = 2;
            crateIndex = 3;
            amountIndex = 4;
        }
        
        if (args.length < crateIndex + 1) {
            sender.sendMessage(plugin.formatMessage("invalid-crate",
                "{prefix} &cUsage: /pcrates key give <player> <crate> [amount]"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[playerIndex]);
        if (target == null) {
            sender.sendMessage(plugin.formatMessage("invalid-player",
                "{prefix} &cPlayer not found: &6{player}&7", "player", args[playerIndex]));
            return true;
        }
        
        String crateId = args[crateIndex].toLowerCase();
        Crate crate = plugin.getCrateManager().getCrate(crateId);
        if (crate == null) {
            sender.sendMessage(plugin.formatMessage("invalid-crate",
                "{prefix} &cInvalid crate: &6{crate}&7", "crate", crateId));
            return true;
        }
        
        int amount = 1;
        if (args.length >= amountIndex + 1) {
            try {
                amount = Integer.parseInt(args[amountIndex]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.formatMessage("invalid-crate",
                    "{prefix} &cInvalid amount: &6{amount}&7", "amount", args[amountIndex]));
                return true;
            }
        }
        
        org.bukkit.inventory.ItemStack key = plugin.getKeyManager().createKey(crateId, amount);
        if (key != null) {
            HashMap<Integer, org.bukkit.inventory.ItemStack> overflow = target.getInventory().addItem(key);
            if (!overflow.isEmpty()) {
                for (org.bukkit.inventory.ItemStack overflowItem : overflow.values()) {
                    target.getWorld().dropItemNaturally(target.getLocation(), overflowItem);
                }
            }
            
            sender.sendMessage(plugin.formatMessage("key-given",
                "{prefix} &7Gave &6{amount} &6{key} &7to &6{player}&7!",
                "amount", String.valueOf(amount),
                "key", crate.getKeyName(),
                "player", target.getName()));
            
            target.sendMessage(plugin.formatMessage("key-received",
                "{prefix} &7You received &6{amount} &6{key}&7!",
                "amount", String.valueOf(amount),
                "key", crate.getKeyName()));
        }
        
        return true;
    }
    
    private boolean handleGiveKeyAll(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pandoracrates.key.give")) {
            sender.sendMessage(plugin.formatMessage("no-permission", 
                "{prefix} &cYou don't have permission to use this command!"));
            return true;
        }
        
        // Format: /crate key all <crate> [amount]
        if (args.length < 3) {
            sender.sendMessage(plugin.formatMessage("invalid-crate",
                "{prefix} &cUsage: /pcrates key all <crate> [amount]"));
            return true;
        }
        
        String crateId = args[2].toLowerCase();
        Crate crate = plugin.getCrateManager().getCrate(crateId);
        if (crate == null) {
            sender.sendMessage(plugin.formatMessage("invalid-crate",
                "{prefix} &cInvalid crate: &6{crate}&7", "crate", crateId));
            return true;
        }
        
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.formatMessage("invalid-crate",
                    "{prefix} &cInvalid amount: &6{amount}&7", "amount", args[3]));
                return true;
            }
        }
        
        // Get all online players
        java.util.Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        if (onlinePlayers.isEmpty()) {
            sender.sendMessage(plugin.formatMessage("invalid-player",
                "{prefix} &cNo players online!"));
            return true;
        }
        
        int givenCount = 0;
        for (Player target : onlinePlayers) {
            org.bukkit.inventory.ItemStack key = plugin.getKeyManager().createKey(crateId, amount);
            if (key != null) {
                HashMap<Integer, org.bukkit.inventory.ItemStack> overflow = target.getInventory().addItem(key);
                if (!overflow.isEmpty()) {
                    for (org.bukkit.inventory.ItemStack overflowItem : overflow.values()) {
                        target.getWorld().dropItemNaturally(target.getLocation(), overflowItem);
                    }
                }
                
                target.sendMessage(plugin.formatMessage("key-received",
                    "{prefix} &7You received &6{amount} &6{key}&7!",
                    "amount", String.valueOf(amount),
                    "key", crate.getKeyName()));
                givenCount++;
            }
        }
        
        sender.sendMessage(plugin.formatRawMessage(
            "{prefix} &7Gave &6{amount} &6{key} &7to &6{count} &7player(s)!",
            "amount", String.valueOf(amount),
            "key", crate.getKeyName(),
            "count", String.valueOf(givenCount)));
        
        return true;
    }
    
    private boolean handleGiveCrate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pandoracrates.admin")) {
            sender.sendMessage(plugin.formatMessage("no-permission", 
                "{prefix} &cYou don't have permission to use this command!"));
            return true;
        }
        
        // Support both: /crate crate give <player> <crate> and /crate give crate <player> <crate>
        int playerIndex = 1;
        int crateIndex = 2;
        
        if (args.length >= 2 && args[1].equalsIgnoreCase("give")) {
            playerIndex = 2;
            crateIndex = 3;
        }
        
        if (args.length < crateIndex + 1) {
            sender.sendMessage(plugin.formatMessage("invalid-crate",
                "{prefix} &cUsage: /pcrates crate give <player> <crate>"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[playerIndex]);
        if (target == null) {
            sender.sendMessage(plugin.formatMessage("invalid-player",
                "{prefix} &cPlayer not found: &6{player}&7", "player", args[playerIndex]));
            return true;
        }
        
        String crateId = args[crateIndex].toLowerCase();
        Crate crate = plugin.getCrateManager().getCrate(crateId);
        if (crate == null) {
            sender.sendMessage(plugin.formatMessage("invalid-crate",
                "{prefix} &cInvalid crate: &6{crate}&7", "crate", crateId));
            return true;
        }
        
        org.bukkit.inventory.ItemStack crateItem = plugin.getCrateManager().createCrateItem(crateId);
        if (crateItem != null) {
            HashMap<Integer, org.bukkit.inventory.ItemStack> overflow = target.getInventory().addItem(crateItem);
            if (!overflow.isEmpty()) {
                for (org.bukkit.inventory.ItemStack overflowItem : overflow.values()) {
                    target.getWorld().dropItemNaturally(target.getLocation(), overflowItem);
                }
            }
            
            sender.sendMessage(plugin.formatRawMessage(
                "{prefix} &7Gave &6{crate} &7to &6{player}&7!",
                "crate", crate.getDisplayName(),
                "player", target.getName()));
            
            target.sendMessage(plugin.formatRawMessage(
                "{prefix} &7You received &6{crate}&7!",
                "crate", crate.getDisplayName()));
        }
        
        return true;
    }
    
    private boolean handlePreview(CommandSender sender, String[] args) {
        try {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.formatMessage("no-permission",
                    "{prefix} &cThis command can only be used by players!"));
                return true;
            }
            
            if (!sender.hasPermission("pandoracrates.preview")) {
                sender.sendMessage(plugin.formatMessage("no-permission",
                    "{prefix} &cYou don't have permission to use this command!"));
                return true;
            }
            
            if (args == null || args.length < 2) {
                sender.sendMessage(plugin.formatMessage("invalid-crate",
                    "{prefix} &cUsage: /pcrates preview <crate>"));
                return true;
            }
            
            String crateId = (args[1] != null && !args[1].isEmpty()) ? args[1].toLowerCase() : "";
            if (crateId.isEmpty()) {
                sender.sendMessage(plugin.formatMessage("invalid-crate",
                    "{prefix} &cUsage: /pcrates preview <crate>"));
                return true;
            }
            
            Crate crate = plugin.getCrateManager().getCrate(crateId);
            if (crate == null) {
                sender.sendMessage(plugin.formatMessage("invalid-crate",
                    "{prefix} &cInvalid crate: &6{crate}&7", "crate", crateId));
                return true;
            }
            
            sender.sendMessage(plugin.formatMessage("crate-preview",
                "{prefix} &7Previewing &6{crate} &7rewards...", "crate", crate.getDisplayName()));
            
            // Open preview GUI
            plugin.getPreviewGUI().openPreview((Player) sender, crate);
            
            return true;
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            plugin.getLogger().severe("Error in preview command: " + errorMsg);
            e.printStackTrace();
            sender.sendMessage(plugin.formatMessage("invalid-crate",
                "{prefix} &cAn error occurred while opening preview. Please try again."));
            return true;
        }
    }
    
    private boolean handleSetCrate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.formatMessage("no-permission",
                "{prefix} &cThis command can only be used by players!"));
            return true;
        }
        
        if (!sender.hasPermission("pandoracrates.setholo")) {
            sender.sendMessage(plugin.formatMessage("no-permission",
                "{prefix} &cYou don't have permission to use this command!"));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(plugin.formatMessage("invalid-crate",
                "{prefix} &cUsage: /pcrates set <crate>"));
            return true;
        }
        
        String crateId = args[1].toLowerCase();
        Crate crate = plugin.getCrateManager().getCrate(crateId);
        if (crate == null) {
            sender.sendMessage(plugin.formatMessage("invalid-crate",
                "{prefix} &cInvalid crate: &6{crate}&7", "crate", crateId));
            return true;
        }
        
        Player player = (Player) sender;
        Location location = player.getTargetBlock(null, 10).getLocation();
        
        plugin.getCrateManager().setCrateLocation(location, crateId);
            sender.sendMessage(plugin.formatRawMessage(
                "{prefix} &7Set crate &6{crate} &7at your location!", "crate", crate.getDisplayName()));
        
        return true;
    }
    
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("pandoracrates.admin")) {
            sender.sendMessage(plugin.formatMessage("no-permission",
                "{prefix} &cYou don't have permission to use this command!"));
            return true;
        }
        
        plugin.reloadConfig();
        plugin.getCrateManager().loadCrates();
        sender.sendMessage(plugin.formatRawMessage("{prefix} &7Configuration reloaded!"));
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.formatRawMessage("&6&lPandora Crates &7- Commands:"));
        sender.sendMessage(plugin.formatRawMessage("&7/pcrates crate give <player> <crate> &7- Give crate block"));
        sender.sendMessage(plugin.formatRawMessage("&7/pcrates key give <player> <crate> [amount] &7- Give keys"));
        sender.sendMessage(plugin.formatRawMessage("&7/pcrates key all <crate> [amount] &7- Give keys to all players"));
        sender.sendMessage(plugin.formatRawMessage("&7/pcrates preview <crate> &7- Preview crate rewards"));
        sender.sendMessage(plugin.formatRawMessage("&7/pcrates set <crate> &7- Set crate at location"));
        sender.sendMessage(plugin.formatRawMessage("&7/pcrates stats [player] &7- View statistics"));
        sender.sendMessage(plugin.formatRawMessage("&7/pcrates reload &7- Reload configuration"));
        sender.sendMessage(plugin.formatRawMessage("&7/crates &7- Visit the crates area"));
    }
    
    private boolean handleStats(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pandoracrates.preview")) {
            sender.sendMessage(plugin.formatMessage("no-permission",
                "{prefix} &cYou don't have permission to use this command!"));
            return true;
        }
        
        Player target = null;
        if (args.length >= 2) {
            if (!sender.hasPermission("pandoracrates.admin")) {
                sender.sendMessage(plugin.formatMessage("no-permission",
                    "{prefix} &cYou don't have permission to view other players' stats!"));
                return true;
            }
            target = Bukkit.getPlayer(args[1]);
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(plugin.formatMessage("invalid-player",
                "{prefix} &cYou must specify a player!"));
            return true;
        }
        
        if (target == null) {
            sender.sendMessage(plugin.formatMessage("invalid-player",
                "{prefix} &cPlayer not found!"));
            return true;
        }
        
        int totalOpens = plugin.getStatisticsManager().getTotalOpens(target.getUniqueId());
        sender.sendMessage(plugin.formatRawMessage("&e&l" + target.getName() + "'s Crate Statistics"));
        sender.sendMessage(plugin.formatRawMessage("&7Total Opens: &6" + totalOpens));
        
        for (String crateId : plugin.getCrateManager().getAllCrates().keySet()) {
            int opens = plugin.getStatisticsManager().getCrateOpens(target.getUniqueId(), crateId);
            if (opens > 0) {
                sender.sendMessage(plugin.formatRawMessage("&7" + crateId + ": &6" + opens + " opens"));
            }
        }
        
        return true;
    }
}

