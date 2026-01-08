package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.util.PandoraMessage;
import com.massivecraft.factions.util.SaberGUI;
import com.massivecraft.factions.util.serializable.InventoryItem;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CmdBasePurchase extends FCommand {

    private static final double BASE_PURCHASE_COST = 50000.0; // $50,000
    private static final int REQUIRED_LEVEL = 5; // Level 5 required to create a base
    private static final ConcurrentHashMap<UUID, Boolean> pendingPurchases = new ConcurrentHashMap<>();

    public CmdBasePurchase() {
        super();
        this.getAliases().addAll(Arrays.asList("create"));

        this.setRequirements(new CommandRequirements.Builder(Permission.CREATE)
                .playerOnly()
                .build());
    }

    @Override
    public void perform(CommandContext context) {
        Player player = context.player;
        UUID playerId = player.getUniqueId();

        // Check if player already has a faction
        if (context.fPlayer.hasFaction()) {
            context.msg(TL.COMMAND_CREATE_MUSTLEAVE);
            return;
        }

        // Check level requirement
        com.massivecraft.factions.managers.LevelManager levelManager = FactionsPlugin.getInstance().getLevelManager();
        if (levelManager != null && levelManager.isLevelPluginAvailable()) {
            int playerLevel = levelManager.getLevel(player);
            if (playerLevel < REQUIRED_LEVEL) {
                player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You need to be level " + REQUIRED_LEVEL + " to create a base!")));
                player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("Your level: ") + 
                    PandoraMessage.highlight(String.valueOf(playerLevel)) + PandoraMessage.text(" | Required: ") + 
                    PandoraMessage.highlight("Level " + REQUIRED_LEVEL)));
                return;
            }
        }

        // Open purchase confirmation GUI
        openPurchaseGUI(player);
    }

    public void openPurchaseGUI(Player player) {
        // Get managers
        FactionsPlugin plugin = FactionsPlugin.getInstance();
        com.massivecraft.factions.managers.LevelManager levelManager = plugin.getLevelManager();
        com.massivecraft.factions.managers.EconomyManager economyManager = plugin.getEconomyManager();
        
        // Check level
        int playerLevel = 0;
        boolean hasRequiredLevel = true;
        if (levelManager != null && levelManager.isLevelPluginAvailable()) {
            playerLevel = levelManager.getLevel(player);
            hasRequiredLevel = levelManager.hasLevel(player, REQUIRED_LEVEL);
        }
        
        // Check balance
        double balance = 0.0;
        boolean canAfford = false;
        if (economyManager != null && economyManager.isEconomyAvailable()) {
            balance = economyManager.getBalance(player);
            canAfford = economyManager.hasEnough(player, BASE_PURCHASE_COST);
        }
        
        final boolean finalCanAfford = canAfford && hasRequiredLevel;
        final double finalBalance = balance;
        final int finalPlayerLevel = playerLevel;
        
        SaberGUI gui = new SaberGUI(player, org.bukkit.ChatColor.DARK_GRAY + "Purchase Base", 27) {
            @Override
            public void redraw() {
                // Fill background with gray glass panes
                ItemStack background = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta bgMeta = background.getItemMeta();
                bgMeta.setDisplayName(" ");
                background.setItemMeta(bgMeta);
                for (int i = 0; i < 27; i++) {
                    if (i != 11 && i != 13 && i != 15) {
                        this.setItem(i, background, () -> {});
                    }
                }

                // Check requirements
                boolean hasLevel = levelManager != null && levelManager.isLevelPluginAvailable() && levelManager.hasLevel(player, REQUIRED_LEVEL);
                boolean canPay = economyManager != null && economyManager.isEconomyAvailable() && economyManager.hasEnough(player, BASE_PURCHASE_COST);
                boolean canPurchase = hasLevel && canPay;

                // Confirm button (yellow wool or red if can't purchase)
                Material confirmMaterial = canPurchase ? Material.YELLOW_WOOL : Material.RED_WOOL;
                ItemStack confirmItem = new ItemStack(confirmMaterial);
                ItemMeta confirmMeta = confirmItem.getItemMeta();
                
                java.util.List<String> confirmLore = new java.util.ArrayList<>();
                confirmLore.add("");
                
                // Add level requirement info
                if (levelManager != null && levelManager.isLevelPluginAvailable()) {
                    confirmLore.add(ChatColor.GRAY + "Required Level: " + 
                        (hasLevel ? ChatColor.GREEN : ChatColor.RED) + "Level " + REQUIRED_LEVEL);
                    confirmLore.add(ChatColor.GRAY + "Your Level: " + 
                        (hasLevel ? ChatColor.GREEN : ChatColor.RED) + finalPlayerLevel);
                }
                
                confirmLore.add("");
                
                // Add cost info
                if (economyManager != null && economyManager.isEconomyAvailable()) {
                    confirmLore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + economyManager.format(BASE_PURCHASE_COST));
                    confirmLore.add(ChatColor.GRAY + "Your Balance: " + 
                        (canPay ? ChatColor.GOLD : ChatColor.RED) + economyManager.format(finalBalance));
                    
                    if (!canPay) {
                        double needed = BASE_PURCHASE_COST - finalBalance;
                        confirmLore.add(ChatColor.RED + "You need " + economyManager.format(needed) + " more!");
                    }
                }
                
                confirmLore.add("");
                
                // Status message
                if (!hasLevel) {
                    confirmMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "✗ Level Required");
                    confirmLore.add(ChatColor.RED + "You need Level " + REQUIRED_LEVEL + " to create a base!");
                } else if (!canPay) {
                    confirmMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "✗ Insufficient Funds");
                    confirmLore.add(ChatColor.RED + "You don't have enough money!");
                } else {
                    confirmMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "✓ Confirm Purchase");
                    confirmLore.add(ChatColor.YELLOW + "Click to purchase a base beacon!");
                }
                
                confirmLore.add("");
                confirmLore.add(ChatColor.GRAY + "You will receive a beacon item.");
                confirmLore.add(ChatColor.GRAY + "Place it in the wilderness to claim");
                confirmLore.add(ChatColor.GRAY + "a 100x100 plot and create your base.");
                
                confirmMeta.setLore(confirmLore);
                confirmItem.setItemMeta(confirmMeta);
                this.setItem(11, confirmItem, () -> {
                    if (canPurchase) {
                        handlePurchase(player);
                        this.close();
                    } else if (!hasLevel) {
                        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You need to be level " + REQUIRED_LEVEL + " to create a base!")));
                    } else {
                        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You don't have enough money to purchase a base!")));
                    }
                });

                // Cancel button (red wool)
                ItemStack cancelItem = new ItemStack(Material.RED_WOOL);
                ItemMeta cancelMeta = cancelItem.getItemMeta();
                cancelMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "✗ Cancel");
                cancelMeta.setLore(Arrays.asList(
                    "",
                    ChatColor.GRAY + "Click to cancel the purchase."
                ));
                cancelItem.setItemMeta(cancelMeta);
                this.setItem(15, cancelItem, () -> {
                    player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.warning("Purchase cancelled.")));
                    this.close();
                });

                // Info item (beacon)
                ItemStack infoItem = new ItemStack(Material.BEACON);
                ItemMeta infoMeta = infoItem.getItemMeta();
                
                java.util.List<String> infoLore = new java.util.ArrayList<>();
                infoLore.add("");
                infoLore.add(ChatColor.GRAY + "After purchasing, you will receive");
                infoLore.add(ChatColor.GRAY + "a beacon item. Place it in the");
                infoLore.add(ChatColor.GRAY + "wilderness to claim a 100x100 area");
                infoLore.add(ChatColor.GRAY + "and officially create your base.");
                infoLore.add("");
                
                // Add level requirement to info
                if (levelManager != null && levelManager.isLevelPluginAvailable()) {
                    infoLore.add(ChatColor.YELLOW + "Required Level: " + ChatColor.WHITE + REQUIRED_LEVEL);
                }
                
                infoLore.add(ChatColor.YELLOW + "Cost: " + ChatColor.WHITE + 
                    (economyManager != null && economyManager.isEconomyAvailable() ? 
                        economyManager.format(BASE_PURCHASE_COST) : "$" + String.format("%.2f", BASE_PURCHASE_COST)));
                infoLore.add("");
                infoLore.add(ChatColor.RED + "" + ChatColor.BOLD + "⚠ WARNING ⚠");
                infoLore.add(ChatColor.RED + "If your beacon is destroyed,");
                infoLore.add(ChatColor.RED + "your base will be disbanded!");
                
                infoMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Base Beacon");
                infoMeta.setLore(infoLore);
                infoItem.setItemMeta(infoMeta);
                this.setItem(13, infoItem, () -> {});
            }
        };

        gui.openGUI(FactionsPlugin.getInstance());
    }

    private void handlePurchase(Player player) {
        UUID playerId = player.getUniqueId();
        
        FactionsPlugin plugin = FactionsPlugin.getInstance();
        com.massivecraft.factions.managers.LevelManager levelManager = plugin.getLevelManager();
        com.massivecraft.factions.managers.EconomyManager economyManager = plugin.getEconomyManager();
        
        // Check level requirement again
        if (levelManager != null && levelManager.isLevelPluginAvailable()) {
            if (!levelManager.hasLevel(player, REQUIRED_LEVEL)) {
                int playerLevel = levelManager.getLevel(player);
                player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You need to be level " + REQUIRED_LEVEL + " to create a base!")));
                player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("Your level: ") + 
                    PandoraMessage.highlight(String.valueOf(playerLevel)) + PandoraMessage.text(" | Required: ") + 
                    PandoraMessage.highlight("Level " + REQUIRED_LEVEL)));
                return;
            }
        }
        
        // Check balance and withdraw money (same method as FarmShop/PetPlugin)
        if (economyManager != null && economyManager.isEconomyAvailable()) {
            if (!economyManager.hasEnough(player, BASE_PURCHASE_COST)) {
                double balance = economyManager.getBalance(player);
                double needed = BASE_PURCHASE_COST - balance;
                player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You don't have enough money!")));
                player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("You need ") + 
                    PandoraMessage.highlight(economyManager.format(needed)) + PandoraMessage.text(" more.")));
                return;
            }
            
            // Withdraw money from player (same as FarmShop)
            if (!economyManager.withdraw(player, BASE_PURCHASE_COST)) {
                player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Failed to process payment! Please try again.")));
                return;
            }
        } else {
            // Economy not available - allow free purchase
            com.massivecraft.factions.util.Logger.print("Economy is not available - allowing free base purchase for " + player.getName(), 
                com.massivecraft.factions.util.Logger.PrefixType.WARNING);
        }

        // Give beacon item with metadata for identification
        ItemStack beacon = new ItemStack(Material.BEACON);
        ItemMeta meta = beacon.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Base Beacon");
        meta.setLore(Arrays.asList(
            "",
            ChatColor.GRAY + "Place this in the wilderness to",
            ChatColor.GRAY + "claim a 100x100 area and create",
            ChatColor.GRAY + "your base.",
            "",
            ChatColor.RED + "" + ChatColor.BOLD + "⚠ WARNING ⚠",
            ChatColor.RED + "If destroyed, your base",
            ChatColor.RED + "will be disbanded!"
        ));
        beacon.setItemMeta(meta);
        
        // Store purchase status
        pendingPurchases.put(playerId, true);

        // Give item to player
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), beacon);
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.warning("Your inventory is full! The beacon was dropped at your location.")));
        } else {
            player.getInventory().addItem(beacon);
        }

        // Format cost message
        String costFormatted = "$" + String.format("%.2f", BASE_PURCHASE_COST);
        if (economyManager != null && economyManager.isEconomyAvailable()) {
            costFormatted = economyManager.format(BASE_PURCHASE_COST);
        }
        
        player.sendMessage("");
        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.success("✓ Purchase Successful!")));
        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("You have purchased a base beacon for ") + 
            PandoraMessage.highlight(costFormatted) + PandoraMessage.text("!")));
        player.sendMessage("");
        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.highlight("Place the beacon in the wilderness to claim your 100x100 plot and create your base.")));
        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Warning: If your beacon is destroyed, your base will be disbanded!")));
        player.sendMessage("");
    }

    public static boolean hasPendingPurchase(UUID playerId) {
        return pendingPurchases.getOrDefault(playerId, false);
    }

    public static void removePendingPurchase(UUID playerId) {
        pendingPurchases.remove(playerId);
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_CREATE_DESCRIPTION;
    }
}

