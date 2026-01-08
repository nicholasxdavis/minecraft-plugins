package com.massivecraft.factions.cmd;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Standalone command handler for /top
 * Redirects to /f top money
 */
public class CmdStandaloneTop implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Simply execute /f top money
        // Handle /top base as well
        if (args.length > 0 && args[0].equalsIgnoreCase("base")) {
            // /top base -> /f top money (skip "base")
            String[] newArgs = new String[args.length];
            newArgs[0] = "top";
            newArgs[1] = "money";
            if (args.length > 1) {
                System.arraycopy(args, 1, newArgs, 2, args.length - 1);
            }
            return Bukkit.dispatchCommand(sender, "f " + String.join(" ", newArgs));
        } else {
            // /top -> /f top money
            String cmd = "f top money";
            if (args.length > 0) {
                cmd += " " + String.join(" ", args);
            }
            return Bukkit.dispatchCommand(sender, cmd);
        }
    }
}

