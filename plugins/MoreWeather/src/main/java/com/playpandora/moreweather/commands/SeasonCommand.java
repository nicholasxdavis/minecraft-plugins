package com.playpandora.moreweather.commands;

import com.playpandora.moreweather.MoreWeather;
import com.playpandora.moreweather.models.Season;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SeasonCommand implements CommandExecutor {
    
    private final MoreWeather plugin;
    
    public SeasonCommand(MoreWeather plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("moreweather.admin")) {
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7You don't have permission to use this command!"));
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lSeason Commands:"));
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e/season current &7- Show current season"));
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e/season set <season> &7- Set season"));
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e/season next &7- Advance to next season"));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("current")) {
            Season current = plugin.getSeasonManager().getCurrentSeason();
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7Current season: &6" + current.name()));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 2) {
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7Usage: &6/season set <spring|summer|fall|winter>"));
                return true;
            }
            
            Season season;
            try {
                season = Season.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7Invalid season! Use: &6spring, summer, fall, or winter"));
                return true;
            }
            
            plugin.getSeasonManager().setSeason(season);
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7Season set to &6" + season.name()));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("next")) {
            plugin.getSeasonManager().advanceSeason();
            Season newSeason = plugin.getSeasonManager().getCurrentSeason();
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7Season advanced to &6" + newSeason.name()));
            return true;
        }
        
        return false;
    }
}








