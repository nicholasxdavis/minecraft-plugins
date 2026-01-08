package com.massivecraft.factions.util;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class BeaconMenuGUI extends SaberGUI {

    private final Faction faction;
    private final Player player;

    public BeaconMenuGUI(Player player, Faction faction) {
        super(player, ChatColor.DARK_GRAY + "Base Menu", 9);
        this.faction = faction;
        this.player = player;
    }

    public static void openMenu(Player player, Faction faction) {
        new BeaconMenuGUI(player, faction).openGUI(FactionsPlugin.getInstance());
    }

    @Override
    public void redraw() {
        // Fill background
        ItemStack background = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = background.getItemMeta();
        bgMeta.setDisplayName(" ");
        background.setItemMeta(bgMeta);
        for (int i = 0; i < 9; i++) {
            this.setItem(i, background, () -> {});
        }
        
        // Base Home option (slot 0 - first option)
        boolean hasHome = faction.hasHome();
        ItemStack homeItem = new ItemStack(hasHome ? Material.RED_BED : Material.BARRIER);
        ItemMeta homeMeta = homeItem.getItemMeta();
        homeMeta.setDisplayName(ChatColor.YELLOW + "Base Home");
        if (hasHome) {
            homeMeta.setLore(Arrays.asList(
                    "",
                    ChatColor.GRAY + "Teleport to your base home.",
                    "",
                    ChatColor.YELLOW + "Click to teleport!"
            ));
        } else {
            homeMeta.setLore(Arrays.asList(
                    "",
                    ChatColor.GRAY + "No home set for this base.",
                    ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/f sethome" + ChatColor.GRAY + " to set one.",
                    ""
            ));
        }
        homeItem.setItemMeta(homeMeta);
        this.setItem(0, homeItem, () -> {
            if (hasHome) {
                this.close();
                // Execute home teleportation directly using CommandContext
                // This prevents the command from being intercepted
                com.massivecraft.factions.FPlayer fPlayer = com.massivecraft.factions.FPlayers.getInstance().getByPlayer(player);
                if (fPlayer != null) {
                    com.massivecraft.factions.cmd.CommandContext context = new com.massivecraft.factions.cmd.CommandContext(player, new java.util.ArrayList<>(), "base");
                    context.fPlayer = fPlayer;
                    context.faction = faction;
                    com.massivecraft.factions.FactionsPlugin.getInstance().cmdBase.cmdHome.perform(context);
                }
            } else {
                player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("No home set! Use ") + 
                    PandoraMessage.highlight("/f sethome") + PandoraMessage.text(" to set one.")));
            }
        });

        // Base Shop option (slot 1)
        ItemStack shopItem = new ItemStack(Material.EMERALD);
        ItemMeta shopMeta = shopItem.getItemMeta();
        shopMeta.setDisplayName(ChatColor.GREEN + "Base Shop");
        shopMeta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "Purchase items that",
                ChatColor.GRAY + "increase your base value.",
                "",
                ChatColor.YELLOW + "Click to shop!"
        ));
        shopItem.setItemMeta(shopMeta);
        this.setItem(1, shopItem, () -> {
            this.close();
            BaseShopGUI.openShop(player, faction);
        });

        // Base Level Upgrades option (slot 2)
        int currentBaseLevel = com.massivecraft.factions.managers.BaseLevelManager.getBaseLevel(faction);
        int maxLevel = com.massivecraft.factions.managers.BaseLevelManager.getMaxLevel();
        int obsidianHP = com.massivecraft.factions.managers.BaseLevelManager.getEffectiveObsidianHealth(faction);
        
        ItemStack levelItem = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta levelMeta = levelItem.getItemMeta();
        levelMeta.setDisplayName(ChatColor.YELLOW + "Base Level: " + ChatColor.GOLD + currentBaseLevel + "/" + maxLevel);
        levelMeta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "Upgrade your base level to",
                ChatColor.GRAY + "increase obsidian health!",
                "",
                ChatColor.GRAY + "Current Level: " + ChatColor.GOLD + currentBaseLevel + "/" + maxLevel,
                ChatColor.GRAY + "Obsidian Health: " + ChatColor.GOLD + obsidianHP + " HP",
                "",
                ChatColor.YELLOW + "Click to upgrade!"
        ));
        levelItem.setItemMeta(levelMeta);
        this.setItem(2, levelItem, () -> {
            this.close();
            com.massivecraft.factions.util.BaseLevelGUI.openGUI(player, faction);
        });

        // Base Stats/Value display (slot 3)
        double baseValue = com.massivecraft.factions.util.BaseValueCalculator.getCachedBaseValue(faction);
        ItemStack statsItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta statsMeta = statsItem.getItemMeta();
        statsMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Base Stats");
        
        // Format the value nicely
        String valueFormatted;
        if (baseValue >= 1000000) {
            valueFormatted = String.format("%.2fM", baseValue / 1000000.0);
        } else if (baseValue >= 1000) {
            valueFormatted = String.format("%.2fK", baseValue / 1000.0);
        } else {
            valueFormatted = String.format("%.2f", baseValue);
        }
        
        statsMeta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "Base Value: " + ChatColor.GOLD + "$" + valueFormatted,
                ChatColor.GRAY + "Members: " + ChatColor.GOLD + faction.getFPlayers().size(),
                ChatColor.GRAY + "Base Level: " + ChatColor.GOLD + currentBaseLevel + "/" + maxLevel,
                ChatColor.GRAY + "Obsidian Health: " + ChatColor.GOLD + obsidianHP + " HP",
                "",
                ChatColor.YELLOW + "Click to refresh!"
        ));
        statsItem.setItemMeta(statsMeta);
        this.setItem(3, statsItem, () -> {
            // Refresh cache and redraw
            com.massivecraft.factions.util.BaseValueCalculator.invalidateCache(faction);
            this.redraw();
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.success("Base stats refreshed!")));
        });

        // Base Chest option (slot 4)
        ItemStack chestItem = new ItemStack(Material.CHEST);
        ItemMeta chestMeta = chestItem.getItemMeta();
        chestMeta.setDisplayName(ChatColor.YELLOW + "Base Chest");
        chestMeta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "Faction-wide storage chest.",
                ChatColor.GRAY + "All members can access this.",
                "",
                ChatColor.YELLOW + "Click to open!"
        ));
        chestItem.setItemMeta(chestMeta);
        this.setItem(4, chestItem, () -> {
            this.close();
            BaseChestManager.openChest(player, faction);
        });

        // General Shop option (slot 5)
        ItemStack generalShopItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta generalShopMeta = generalShopItem.getItemMeta();
        generalShopMeta.setDisplayName(ChatColor.YELLOW + "General Shop");
        generalShopMeta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "Access the general shop",
                ChatColor.GRAY + "to buy various items.",
                "",
                ChatColor.YELLOW + "Click to open!"
        ));
        generalShopItem.setItemMeta(generalShopMeta);
        this.setItem(5, generalShopItem, () -> {
            this.close();
            // Dispatch /buy command as the player
            org.bukkit.Bukkit.getScheduler().runTask(FactionsPlugin.getInstance(), () -> {
                player.performCommand("buy");
            });
        });
    }
}

