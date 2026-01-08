package com.playpandora.petplugin.commands;

import com.playpandora.petplugin.PetPlugin;
import com.playpandora.petplugin.models.Pet;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class PetCommand implements CommandExecutor {
    
    private final PetPlugin plugin;
    
    public PetCommand(PetPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.formatMessage("messages.players-only",
                "{prefix}This command can only be used by players!"));
            return true;
        }
        
        if (args.length == 0) {
            // Open shop GUI
            plugin.getShopGUI().openShop(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "rename" -> {
                if (args.length < 3) {
                    player.sendMessage(plugin.formatMessage("messages.usage-rename",
                        "{prefix}Usage: &6/pet rename <generatedname> <newname>"));
                    return true;
                }
                
                String generatedName = args[1];
                StringBuilder newNameBuilder = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    if (i > 2) newNameBuilder.append(" ");
                    newNameBuilder.append(args[i]);
                }
                String newName = newNameBuilder.toString();
                
                renamePet(player, generatedName, newName);
            }
            case "spawn" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.formatMessage("messages.usage-spawn",
                        "{prefix}Usage: &6/pet spawn <pettype>"));
                    return true;
                }
                
                String petType = args[1];
                spawnPet(player, petType);
            }
            case "despawn" -> {
                plugin.getPetManager().despawnPet(player.getUniqueId());
                String message = plugin.formatMessage("messages.pet-despawned",
                    "{prefix}Your pet has been despawned.");
                player.sendMessage(message);
            }
            case "list" -> {
                listPets(player);
            }
            case "release" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.formatMessage("messages.usage-release",
                        "{prefix}Usage: &6/pet release <petname>"));
                    return true;
                }
                
                StringBuilder petNameBuilder = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (i > 1) petNameBuilder.append(" ");
                    petNameBuilder.append(args[i]);
                }
                String petName = petNameBuilder.toString();
                
                releasePet(player, petName);
            }
            case "revive" -> {
                if (args.length < 2) {
                    player.sendMessage(plugin.formatMessage("messages.usage-revive",
                        "{prefix}Usage: &6/pet revive <petname>"));
                    return true;
                }
                
                StringBuilder petNameBuilder = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (i > 1) petNameBuilder.append(" ");
                    petNameBuilder.append(args[i]);
                }
                String petName = petNameBuilder.toString();
                
                revivePet(player, petName);
            }
            default -> {
                player.sendMessage(plugin.formatMessage("messages.unknown-subcommand",
                    "{prefix}Unknown subcommand. Use: &6/pet [rename|spawn|despawn|list|release|revive]"));
            }
        }
        
        return true;
    }
    
    private void renamePet(Player player, String generatedName, String newName) {
        Pet pet = plugin.getDataManager().getPetByGeneratedName(player.getUniqueId(), generatedName);
        
        if (pet == null) {
            String message = plugin.formatMessage("messages.pet-not-found",
                "{prefix}Pet not found. Use &6/pet list &7to see your pets.");
            player.sendMessage(message);
            return;
        }
        
        pet.setCustomName(newName);
        plugin.getDataManager().updatePet(player.getUniqueId(), pet);
        
        // Update entity name if spawned
        if (pet.getEntityUUID() != null) {
            var entity = plugin.getPetManager().getPetEntity(pet.getEntityUUID());
            if (entity != null) {
                entity.setCustomName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&6" + newName));
            }
        }
        
        String message = plugin.formatMessage("messages.pet-renamed",
            "{prefix}Your pet has been renamed to &6{new_name}&7!",
            "new_name", newName);
        player.sendMessage(message);
    }
    
    private void spawnPet(Player player, String petType) {
        List<Pet> pets = plugin.getDataManager().getPlayerPets(player.getUniqueId());
        Pet pet = pets.stream()
                .filter(p -> p.getPetType().equalsIgnoreCase(petType))
                .findFirst()
                .orElse(null);
        
        if (pet == null) {
            player.sendMessage(plugin.formatMessage("messages.pet-type-not-owned",
                "{prefix}You don't own a &6" + petType + " &7pet. Use &6/pet &7to buy one!"));
            return;
        }
        
        plugin.getPetManager().spawnPet(player, pet);
    }
    
    private void listPets(Player player) {
        List<Pet> pets = plugin.getDataManager().getPlayerPets(player.getUniqueId());
        List<Pet> deadPets = plugin.getDataManager().getDeadPets(player.getUniqueId());
        
        if (pets.isEmpty() && deadPets.isEmpty()) {
            String message = plugin.formatMessage("messages.no-pets-owned",
                "{prefix}You don't own any pets yet. Use &6/pet &7to buy one!");
            player.sendMessage(message);
            return;
        }
        
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&6&lYour Pets:"));
        for (Pet pet : pets) {
            String status = plugin.getPetManager().getActivePet(player.getUniqueId()) == pet ? "&e[SPAWNED]" : "&7[Not Spawned]";
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "  " + status + " &7" + pet.getPetType() + " - &6" + pet.getDisplayName() +
                " &7(" + pet.getGeneratedName() + ")"));
        }
        
        if (!deadPets.isEmpty()) {
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7&lDead Pets (Can be revived):"));
            for (Pet pet : deadPets) {
                long deathTime = pet.getDeathTimestamp();
                long currentTime = System.currentTimeMillis();
                long timeSinceDeath = currentTime - deathTime;
                long hoursRemaining = (21600000 - timeSinceDeath) / 3600000; // 6 hours = 21600000 ms
                long minutesRemaining = ((21600000 - timeSinceDeath) % 3600000) / 60000;
                
                if (timeSinceDeath < 21600000) {
                    player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        "  &c[DEAD] &7" + pet.getPetType() + " - &6" + pet.getDisplayName() +
                        " &7(&c" + hoursRemaining + "h " + minutesRemaining + "m &7remaining)"));
                } else {
                    player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                        "  &c[DEAD] &7" + pet.getPetType() + " - &6" + pet.getDisplayName() +
                        " &7(&cToo late to revive&7)"));
                }
            }
        }
    }
    
    private void releasePet(Player player, String petName) {
        // Check alive pets first
        Pet pet = plugin.getDataManager().getPetByGeneratedName(player.getUniqueId(), petName);
        if (pet == null) {
            // Check by display name
            List<Pet> pets = plugin.getDataManager().getPlayerPets(player.getUniqueId());
            pet = pets.stream()
                    .filter(p -> p.getDisplayName().equalsIgnoreCase(petName))
                    .findFirst()
                    .orElse(null);
        }
        
        if (pet == null) {
            String message = plugin.formatMessage("messages.pet-not-found",
                "{prefix}Pet not found. Use &6/pet list &7to see your pets.");
            player.sendMessage(message);
            return;
        }
        
        // Despawn if active
        if (plugin.getPetManager().getActivePet(player.getUniqueId()) == pet) {
            plugin.getPetManager().despawnPet(player.getUniqueId());
        }
        
        // Remove pet
        plugin.getDataManager().removePet(player.getUniqueId(), pet);
        
        String message = plugin.formatMessage("messages.pet-released",
            "{prefix}You released &6{pet_name}&7. The pet is gone forever.",
            "pet_name", pet.getDisplayName());
        player.sendMessage(message);
    }
    
    private void revivePet(Player player, String petName) {
        Pet pet = plugin.getDataManager().getDeadPetByName(player.getUniqueId(), petName);
        
        if (pet == null) {
            String message = plugin.formatMessage("messages.pet-not-found",
                "{prefix}Pet not found. Use &6/pet list &7to see your dead pets.");
            player.sendMessage(message);
            return;
        }
        
        // Check if within 6 hours
        long deathTime = pet.getDeathTimestamp();
        long currentTime = System.currentTimeMillis();
        long timeSinceDeath = currentTime - deathTime;
        long sixHours = 21600000; // 6 hours in milliseconds
        
        if (timeSinceDeath > sixHours) {
            String message = plugin.formatMessage("messages.revive-too-late",
                "{prefix}Too late! You can only revive pets within 6 hours of death.");
            player.sendMessage(message);
            return;
        }
        
        // Check balance
        double reviveCost = plugin.getConfig().getDouble("revive.cost", 50000.0);
        if (plugin.getEconomy() != null) {
            double balance = plugin.getPlayerBalance(player);
            if (balance < reviveCost) {
                double needed = reviveCost - balance;
                String message = plugin.formatMessage("messages.insufficient-funds",
                    "{prefix}Insufficient funds! You need &6${amount} &7more.",
                    "amount", String.format("%.2f", needed));
                player.sendMessage(message);
                return;
            }
            
            // Withdraw money
            plugin.withdrawPlayer(player, reviveCost);
        }
        
        // Revive pet
        plugin.getDataManager().revivePet(player.getUniqueId(), pet);
        
        String message = plugin.formatMessage("messages.pet-revived",
            "{prefix}You revived &6{pet_name} &7for &6${cost}&7!",
            "pet_name", pet.getDisplayName(),
            "cost", String.format("%.2f", reviveCost));
        player.sendMessage(message);
    }
}

