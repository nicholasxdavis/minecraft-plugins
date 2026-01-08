package com.massivecraft.factions.util;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.managers.BaseLevelManager;
import com.massivecraft.factions.managers.EconomyManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GUI for upgrading base level
 */
public class BaseLevelGUI extends SaberGUI {
    
    private final Faction faction;
    private final Player player;
    
    public BaseLevelGUI(Player player, Faction faction) {
        super(player, ChatColor.DARK_GRAY + "Base Level Upgrades", 45);
        this.faction = faction;
        this.player = player;
    }
    
    public static void openGUI(Player player, Faction faction) {
        if (faction == null || !faction.isNormal() || !faction.hasBeacon()) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You must have a base to access upgrades!")));
            return;
        }
        
        // Check if player is member
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        if (fPlayer == null || !faction.getFPlayers().contains(fPlayer)) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Only base members can access upgrades!")));
            return;
        }
        
        // Only leaders and mods can upgrade
        if (fPlayer.getRole() != com.massivecraft.factions.struct.Role.LEADER && 
            fPlayer.getRole() != com.massivecraft.factions.struct.Role.MODERATOR) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Only mods and owners can upgrade base level!")));
            return;
        }
        
        new BaseLevelGUI(player, faction).openGUI(FactionsPlugin.getInstance());
    }
    
    @Override
    public void redraw() {
        // Fill background
        ItemStack background = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = background.getItemMeta();
        bgMeta.setDisplayName(" ");
        background.setItemMeta(bgMeta);
        
        for (int i = 0; i < 45; i++) {
            this.setItem(i, background, () -> {});
        }
        
        // Get current base level
        int currentLevel = BaseLevelManager.getBaseLevel(faction);
        int maxLevel = BaseLevelManager.getMaxLevel();
        
        // Get player balance
        FactionsPlugin plugin = FactionsPlugin.getInstance();
        EconomyManager economyManager = plugin.getEconomyManager();
        double balance = 0.0;
        boolean canAfford = false;
        
        if (economyManager != null && economyManager.isEconomyAvailable()) {
            balance = economyManager.getBalance(player);
        }
        
        // Calculate current obsidian health
        int currentObsidianHP = BaseLevelManager.getEffectiveObsidianHealth(faction);
        int baseObsidianHP = com.massivecraft.factions.Conf.obsidianMaxHealth;
        
        // Display info item (slot 4 - top center)
        ItemStack infoItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Base Level System");
        
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add(ChatColor.GRAY + "Current Base Level: " + ChatColor.GREEN + currentLevel + "/" + maxLevel);
        infoLore.add(ChatColor.GRAY + "Obsidian Health: " + ChatColor.YELLOW + currentObsidianHP + " HP");
        infoLore.add(ChatColor.GRAY + "Base Health: " + ChatColor.GRAY + baseObsidianHP + " HP");
        if (currentLevel > 0) {
            double bonus = currentLevel * BaseLevelManager.getHealthBonusPerLevelPercent();
            infoLore.add(ChatColor.GRAY + "Health Bonus: " + ChatColor.GREEN + "+" + String.format("%.0f%%", bonus));
        }
        infoLore.add("");
        infoLore.add(ChatColor.YELLOW + "Each level increases obsidian");
        infoLore.add(ChatColor.YELLOW + "health by " + String.format("%.0f", BaseLevelManager.getHealthBonusPerLevelPercent()) + "%!");
        
        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);
        this.setItem(4, infoItem, () -> {});
        
        // Display each level (slots 19-23, bottom row)
        int[] levelSlots = {19, 20, 21, 22, 23};
        for (int level = 1; level <= maxLevel; level++) {
            int slot = levelSlots[level - 1];
            boolean isUnlocked = level <= currentLevel;
            boolean canUpgrade = level == currentLevel + 1;
            double cost = BaseLevelManager.getUpgradeCost(level - 1);
            
            ItemStack levelItem = new ItemStack(isUnlocked ? Material.EMERALD_BLOCK : 
                                              canUpgrade ? Material.GOLD_BLOCK : Material.REDSTONE_BLOCK);
            ItemMeta levelMeta = levelItem.getItemMeta();
            
            String levelName = isUnlocked ? 
                ChatColor.GREEN + "✓ Level " + level : 
                canUpgrade ? ChatColor.YELLOW + "Level " + level : 
                ChatColor.GRAY + "Level " + level;
            
            levelMeta.setDisplayName(levelName);
            
            List<String> levelLore = new ArrayList<>();
            levelLore.add("");
            
            if (isUnlocked) {
                levelLore.add(ChatColor.GREEN + "✓ UNLOCKED");
            } else if (canUpgrade) {
                levelLore.add(ChatColor.YELLOW + "Click to upgrade!");
            } else {
                levelLore.add(ChatColor.RED + "Locked - Upgrade previous levels first");
            }
            
            levelLore.add("");
            levelLore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + 
                (economyManager != null && economyManager.isEconomyAvailable() ? 
                    economyManager.format(cost) : "$" + String.format("%.2f", cost)));
            
            if (canUpgrade) {
                levelLore.add("");
                if (economyManager != null && economyManager.isEconomyAvailable()) {
                    levelLore.add(ChatColor.GRAY + "Your Balance: " + 
                        (balance >= cost ? ChatColor.GREEN : ChatColor.RED) + 
                        economyManager.format(balance));
                    
                    if (balance < cost) {
                        double needed = cost - balance;
                        levelLore.add(ChatColor.RED + "You need " + economyManager.format(needed) + " more!");
                    }
                }
            }
            
            levelLore.add("");
            double healthBonusPercent = BaseLevelManager.getHealthBonusPerLevelPercent();
            levelLore.add(ChatColor.GRAY + "Obsidian Health Bonus: " + ChatColor.GREEN + "+" + String.format("%.0f", level * healthBonusPercent) + "%");
            
            // Calculate HP for this specific level
            double multiplierForLevel = 1.0 + (level * (healthBonusPercent / 100.0));
            int futureHP = (int) Math.round(baseObsidianHP * multiplierForLevel);
            levelLore.add(ChatColor.GRAY + "New Obsidian Health: " + ChatColor.YELLOW + futureHP + " HP");
            
            levelMeta.setLore(levelLore);
            levelItem.setItemMeta(levelMeta);
            
            if (canUpgrade) {
                final int upgradeLevel = level;
                final double upgradeCost = cost;
                this.setItem(slot, levelItem, () -> {
                    handleUpgrade(upgradeLevel, upgradeCost);
                });
            } else {
                this.setItem(slot, levelItem, () -> {});
            }
        }
        
        // Back button (slot 36)
        ItemStack backItem = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back");
        backMeta.setLore(Arrays.asList("", ChatColor.GRAY + "Click to go back"));
        backItem.setItemMeta(backMeta);
        this.setItem(36, backItem, () -> {
            this.close();
            BeaconMenuGUI.openMenu(player, faction);
        });
    }
    
    private void handleUpgrade(int targetLevel, double cost) {
        FactionsPlugin plugin = FactionsPlugin.getInstance();
        EconomyManager economyManager = plugin.getEconomyManager();
        
        // Check balance
        if (economyManager == null || !economyManager.isEconomyAvailable()) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Economy is not available!")));
            return;
        }
        
        double balance = economyManager.getBalance(player);
        if (balance < cost) {
            double needed = cost - balance;
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You don't have enough money!")));
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("You need ") + 
                PandoraMessage.highlight(economyManager.format(needed)) + PandoraMessage.text(" more.")));
            return;
        }
        
        // Withdraw money
        if (!economyManager.withdraw(player, cost)) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Failed to process payment! Please try again.")));
            return;
        }
        
        // Upgrade base level
        if (BaseLevelManager.setBaseLevel(faction, targetLevel)) {
            player.sendMessage("");
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.success("✓ Base Level Upgraded!")));
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("Your base is now level ") + 
                PandoraMessage.highlight(String.valueOf(targetLevel)) + PandoraMessage.text("!")));
            
            int newHP = BaseLevelManager.getEffectiveObsidianHealth(faction);
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("Obsidian health is now: ") + 
                PandoraMessage.highlight(newHP + " HP")));
            player.sendMessage("");
            
            // Redraw GUI
            this.redraw();
        } else {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Failed to upgrade base level!")));
            // Refund money
            if (economyManager.isEconomyAvailable()) {
                try {
                    economyManager.getBalance(player); // This will update balance, refund via deposit
                    // Actually we need to deposit back
                    economyManager.deposit(player, cost);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to refund money after upgrade failure: " + e.getMessage());
                }
            }
        }
    }
}

