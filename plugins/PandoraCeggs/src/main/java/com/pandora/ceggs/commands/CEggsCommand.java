package com.pandora.ceggs.commands;

import com.pandora.ceggs.PandoraCeggs;
import com.pandora.ceggs.util.CEggsUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CEggsCommand implements CommandExecutor, TabCompleter {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pandoraceggs.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage("§ePandoraCeggs Commands:");
            sender.sendMessage("§7/ceggs give <player> <amount> §6- Give creeper eggs to a player");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("give")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /ceggs give <player> <amount>");
                return true;
            }
            
            Player target = PandoraCeggs.getInstance().getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: " + args[1]);
                return true;
            }
            
            int amount;
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) {
                    sender.sendMessage("§cAmount must be greater than 0!");
                    return true;
                }
                if (amount > 64) {
                    sender.sendMessage("§cAmount cannot exceed 64!");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid amount: " + args[2]);
                return true;
            }
            
            // Give the custom creeper eggs
            ItemStack eggs = CEggsUtil.createCEgg();
            eggs.setAmount(amount);
            
            // Add to inventory or drop if full
            if (target.getInventory().firstEmpty() != -1) {
                target.getInventory().addItem(eggs);
                sender.sendMessage("§aGave §6" + amount + " §acreeper egg(s) to §6" + target.getName());
                target.sendMessage("§aYou received §6" + amount + " §acreeper egg(s)!");
            } else {
                target.getWorld().dropItemNaturally(target.getLocation(), eggs);
                sender.sendMessage("§aGave §6" + amount + " §acreeper egg(s) to §6" + target.getName() + " §7(dropped, inventory full)");
                target.sendMessage("§aYou received §6" + amount + " §acreeper egg(s)! §7(dropped, inventory full)");
            }
            
            return true;
        }
        
        sender.sendMessage("§cUnknown subcommand. Use /ceggs for help.");
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("pandoraceggs.admin")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            return Arrays.asList("give");
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            // Return list of online players
            List<String> players = new ArrayList<>();
            for (Player player : PandoraCeggs.getInstance().getServer().getOnlinePlayers()) {
                players.add(player.getName());
            }
            return players;
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Arrays.asList("1", "16", "32", "64");
        }
        
        return new ArrayList<>();
    }
}

