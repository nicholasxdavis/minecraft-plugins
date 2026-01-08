package com.playpandora.pandorabanners.commands;

import com.playpandora.pandorabanners.PandoraBanners;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AsciiCommand implements CommandExecutor, TabCompleter {
    
    private final PandoraBanners plugin;
    private final List<String> validDirections = Arrays.asList("north", "south", "east", "west", "end", "nether");
    
    public AsciiCommand(PandoraBanners plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§e§lPandora §8» §cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("pandorabanners.spawn")) {
            player.sendMessage("§e§lPandora §8» §cYou don't have permission to use this command!");
            return true;
        }
        
        if (args.length == 0) {
            player.sendMessage("§e§lPandora §8» §7Usage: §6/ascii spawn <direction>");
            player.sendMessage("§e§lPandora §8» §7Available directions: §6" + String.join("§7, §6", validDirections));
            player.sendMessage("§e§lPandora §8» §7Or: §6/ascii delete §7to remove all banners");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("spawn")) {
            if (args.length < 2) {
                player.sendMessage("§e§lPandora §8» §cPlease specify a direction!");
                player.sendMessage("§e§lPandora §8» §7Available: §6" + String.join("§7, §6", validDirections));
                player.sendMessage("§e§lPandora §8» §7You can spawn multiple: §6/ascii spawn north,end,nether");
                return true;
            }
            
            // Support multiple directions separated by commas
            String[] directions = args[1].toLowerCase().split(",");
            int spawned = 0;
            
            // Reset spawn count for new batch
            plugin.getBannerManager().resetSpawnCount();
            
            for (String direction : directions) {
                direction = direction.trim();
                
                if (!validDirections.contains(direction)) {
                    player.sendMessage("§e§lPandora §8» §cInvalid direction: §6" + direction);
                    continue;
                }
                
                if (!plugin.getBannerParser().hasBanner(direction)) {
                    player.sendMessage("§e§lPandora §8» §cBanner for direction '§6" + direction + "§c' not found in the banner file!");
                    continue;
                }
                
                plugin.getBannerManager().spawnBanner(player, direction);
                spawned++;
            }
            
            if (spawned == 0) {
                player.sendMessage("§e§lPandora §8» §cNo banners were spawned. Check your directions!");
            } else if (spawned > 1) {
                player.sendMessage("§e§lPandora §8» §7Successfully spawned §6" + spawned + " §7banners!");
            }
            
            return true;
        }
        
        if (args[0].equalsIgnoreCase("clear") || args[0].equalsIgnoreCase("delete")) {
            plugin.getBannerManager().clearBanners(player);
            return true;
        }
        
        player.sendMessage("§e§lPandora §8» §7Usage: §6/ascii spawn <direction>");
        player.sendMessage("§e§lPandora §8» §7Or: §6/ascii delete §7to remove all banners");
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            if ("spawn".startsWith(args[0].toLowerCase())) {
                completions.add("spawn");
            }
            if ("clear".startsWith(args[0].toLowerCase())) {
                completions.add("clear");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            for (String direction : validDirections) {
                if (direction.startsWith(args[1].toLowerCase())) {
                    completions.add(direction);
                }
            }
        }
        
        return completions;
    }
}

