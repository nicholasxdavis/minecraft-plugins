package com.playpandora.pandoraonevsone.commands;

import com.playpandora.pandoraonevsone.PandoraOnevsOne;
import com.playpandora.pandoraonevsone.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command executor for /onevsone
 */
public class OneVsOneCommand implements CommandExecutor, TabCompleter {
    
    private final PandoraOnevsOne plugin;
    
    public OneVsOneCommand(PandoraOnevsOne plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ColorUtil.format(ColorUtil.text("Usage: /onevsone [set arena|challenge <player>|accept|leave]")));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "setarena":
            case "set":
                if (!sender.hasPermission("pandoraonevsone.setarena")) {
                    sender.sendMessage(ColorUtil.error("You don't have permission to use this command!"));
                    return true;
                }
                
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.error("This command can only be used by players!"));
                    return true;
                }
                
                Player admin = (Player) sender;
                plugin.getOneVsOneManager().setArenaLocation(admin.getLocation());
                admin.sendMessage(ColorUtil.format(ColorUtil.text("Arena location set!")));
                return true;
                
            case "challenge":
            case "chall":
            case "duel":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.error("This command can only be used by players!"));
                    return true;
                }
                
                Player challenger = (Player) sender;
                
                if (!challenger.hasPermission("pandoraonevsone.use")) {
                    challenger.sendMessage(ColorUtil.error("You don't have permission to use this command!"));
                    return true;
                }
                
                if (args.length < 2) {
                    challenger.sendMessage(ColorUtil.error("Usage: /onevsone challenge <player>"));
                    return true;
                }
                
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    challenger.sendMessage(ColorUtil.error("Player not found or not online!"));
                    return true;
                }
                
                if (target.equals(challenger)) {
                    challenger.sendMessage(ColorUtil.error("You cannot challenge yourself!"));
                    return true;
                }
                
                // Allow friends to challenge each other - no blocking
                // The 1v1 system is designed to allow friends to practice PvP
                plugin.getOneVsOneManager().challengePlayer(challenger, target);
                return true;
                
            case "accept":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.error("This command can only be used by players!"));
                    return true;
                }
                
                Player player = (Player) sender;
                
                if (!player.hasPermission("pandoraonevsone.use")) {
                    player.sendMessage(ColorUtil.error("You don't have permission to use this command!"));
                    return true;
                }
                
                plugin.getOneVsOneManager().acceptChallenge(player);
                return true;
                
            case "leave":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.error("This command can only be used by players!"));
                    return true;
                }
                
                Player leaver = (Player) sender;
                
                if (!plugin.getOneVsOneManager().isInDuel(leaver)) {
                    leaver.sendMessage(ColorUtil.error("You are not in a duel!"));
                    return true;
                }
                
                plugin.getOneVsOneManager().leaveDuel(leaver);
                leaver.sendMessage(ColorUtil.format(ColorUtil.text("You left the duel.")));
                return true;
                
            default:
                sender.sendMessage(ColorUtil.format(ColorUtil.text("Usage: /onevsone [set arena|challenge <player>|accept|leave]")));
                return true;
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = Arrays.asList("setarena", "challenge", "accept", "leave");
            return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("challenge") || args[0].equalsIgnoreCase("chall") || args[0].equalsIgnoreCase("duel"))) {
            // Return online players
            return plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}


