package com.sainttx.notes.command;

import com.sainttx.notes.NotesPlugin;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Main command handler for /note, /banknote, /notes, /moneynote
 * Handles both player creation and admin give commands
 */
public class NoteCommand implements CommandExecutor {

    private NotesPlugin plugin;

    public NoteCommand(NotesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Show help or create note if player
            if (sender instanceof Player) {
                sender.sendMessage(plugin.colorMessage("&7Usage: /" + label + " <amount>"));
                sender.sendMessage(plugin.colorMessage("&7Usage: /" + label + " give <player> <amount>"));
            } else {
                sender.sendMessage(plugin.colorMessage("&7Usage: /" + label + " give <player> <amount>"));
                sender.sendMessage(plugin.colorMessage("&7Usage: /" + label + " reload"));
            }
            return true;
        }

        // Handle reload command
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("banknotes.reload")) {
                sender.sendMessage(plugin.getMessage("messages.insufficient-permissions"));
            } else {
                plugin.reloadConfig();
                plugin.reload();
                sender.sendMessage(plugin.getMessage("messages.reloaded"));
            }
            return true;
        }

        // Handle give command (admin)
        if (args[0].equalsIgnoreCase("give") && args.length >= 3) {
            if (!sender.hasPermission("banknotes.give")) {
                sender.sendMessage(plugin.getMessage("messages.insufficient-permissions"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.getMessage("messages.target-not-found"));
                return true;
            }

            double amount;
            try {
                amount = Double.parseDouble(args[2]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(plugin.getMessage("messages.invalid-number"));
                return true;
            }

            if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
                sender.sendMessage(plugin.getMessage("messages.invalid-number"));
            } else {
                ItemStack banknote = plugin.createBanknote(sender.getName(), amount);
                target.getInventory().addItem(banknote);

                String senderName = sender instanceof ConsoleCommandSender ? plugin.getConfig().getString("settings.console-name") : sender.getName();
                target.sendMessage(plugin.getMessage("messages.note-received")
                        .replace("[money]", plugin.formatDouble(amount))
                        .replace("[player]", senderName));
                sender.sendMessage(plugin.getMessage("messages.note-given")
                        .replace("[money]", plugin.formatDouble(amount))
                        .replace("[player]", target.getName()));
            }
            return true;
        }

        // Handle create note command (player)
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("messages.only-players"));
            return true;
        }

        if (!sender.hasPermission("banknotes.withdraw")) {
            sender.sendMessage(plugin.getMessage("messages.insufficient-permissions"));
            return true;
        }

        Player player = (Player) sender;
        double amount;

        try {
            amount = args[0].equalsIgnoreCase("all")
                    ? plugin.getEconomy().getBalance(player) : Double.parseDouble(args[0]);
        } catch (NumberFormatException invalidNumber) {
            player.sendMessage(plugin.getMessage("messages.invalid-number"));
            return true;
        }

        double min = plugin.getConfig().getDouble("settings.minimum-withdraw-amount");
        double max = plugin.getConfig().getDouble("settings.maximum-withdraw-amount");

        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            player.sendMessage(plugin.getMessage("messages.invalid-number"));
        } else if (Double.compare(amount, min) < 0) {
            player.sendMessage(plugin.getMessage("messages.less-than-minimum")
                    .replace("[money]", plugin.formatDouble(min)));
        } else if (Double.compare(amount, max) > 0) {
            player.sendMessage(plugin.getMessage("messages.more-than-maximum")
                    .replace("[money]", plugin.formatDouble(max)));
        } else if (Double.compare(plugin.getEconomy().getBalance(player), amount) < 0) {
            player.sendMessage(plugin.getMessage("messages.insufficient-funds"));
        } else if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(plugin.getMessage("messages.inventory-full"));
        } else {
            ItemStack banknote = plugin.createBanknote(player.getName(), amount);
            EconomyResponse response = plugin.getEconomy().withdrawPlayer(player, amount);

            if (response == null || !response.transactionSuccess()) {
                player.sendMessage(plugin.colorMessage("&7There was an error processing your transaction"));
                plugin.getLogger().warning("Error processing player withdrawal " +
                        "(" + player.getName() + " for $" + plugin.formatDouble(amount) + ") " +
                        "[message: " + (response == null ? "null" : response.errorMessage) + "]");
                return true;
            }

            player.getInventory().addItem(banknote);
            player.sendMessage(plugin.getMessage("messages.note-created").replace("[money]", plugin.formatDouble(amount)));
        }

        return true;
    }
}

