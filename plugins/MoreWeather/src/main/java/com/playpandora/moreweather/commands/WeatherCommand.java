package com.playpandora.moreweather.commands;

import com.playpandora.moreweather.MoreWeather;
import com.playpandora.moreweather.models.WeatherType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WeatherCommand implements CommandExecutor {
    
    private final MoreWeather plugin;
    
    public WeatherCommand(MoreWeather plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("moreweather.admin")) {
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7You don't have permission to use this command!"));
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lMoreWeather Commands:"));
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e/weather set <type> [world] &7- Set weather type"));
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e/weather clear [world] &7- Clear weather"));
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e/weather status [world] &7- Show current weather"));
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e/weather list &7- List available weather types"));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 2) {
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7Usage: &6/weather set <type> [world]"));
                return true;
            }
            
            WeatherType type;
            try {
                type = WeatherType.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7Invalid weather type! Use &6/weather list"));
                return true;
            }
            
            World world = null;
            if (args.length >= 3) {
                world = Bukkit.getWorld(args[2]);
            } else if (sender instanceof Player) {
                world = ((Player) sender).getWorld();
            } else {
                world = Bukkit.getWorlds().get(0);
            }
            
            if (world == null) {
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7World not found!"));
                return true;
            }
            
            plugin.getWeatherManager().setWeather(world, type);
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7Weather set to &6" + type.name() + " &7in &6" + world.getName()));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("clear")) {
            World world = null;
            if (args.length >= 2) {
                world = Bukkit.getWorld(args[1]);
            } else if (sender instanceof Player) {
                world = ((Player) sender).getWorld();
            } else {
                world = Bukkit.getWorlds().get(0);
            }
            
            if (world == null) {
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7World not found!"));
                return true;
            }
            
            plugin.getWeatherManager().setWeather(world, WeatherType.CLEAR);
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7Weather cleared in &6" + world.getName()));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("status")) {
            World world = null;
            if (args.length >= 2) {
                world = Bukkit.getWorld(args[1]);
            } else if (sender instanceof Player) {
                world = ((Player) sender).getWorld();
            } else {
                world = Bukkit.getWorlds().get(0);
            }
            
            if (world == null) {
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7World not found!"));
                return true;
            }
            
            WeatherType current = plugin.getWeatherManager().getCurrentWeather(world);
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lPandora &8» &r&7Current weather in &6" + world.getName() + "&7: &6" + current.name()));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lAvailable weather types:"));
            for (WeatherType type : WeatherType.values()) {
                sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e- &7" + type.name()));
            }
            return true;
        }
        
        return false;
    }
}








