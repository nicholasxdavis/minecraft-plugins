package com.massivecraft.factions.util;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.managers.BaseShopManager;
import com.massivecraft.factions.managers.BaseShopManager.ShopItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BaseShopGUI extends SaberGUI {

    private final Faction faction;
    private final Player player;

    public BaseShopGUI(Player player, Faction faction) {
        super(player, ChatColor.translateAlternateColorCodes('&', BaseShopManager.getShopTitle()), 54);
        this.faction = faction;
        this.player = player;
    }

    public static void openShop(Player player, Faction faction) {
        if (faction == null || !faction.isNormal() || !faction.hasBeacon()) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You must have a base to access the shop!")));
            return;
        }

        // Check if player is member
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        if (fPlayer == null || !faction.getFPlayers().contains(fPlayer)) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Only base members can access the shop!")));
            return;
        }

        new BaseShopGUI(player, faction).openGUI(FactionsPlugin.getInstance());
    }

    @Override
    public void redraw() {
        // Fill background
        ItemStack background = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = background.getItemMeta();
        bgMeta.setDisplayName(" ");
        background.setItemMeta(bgMeta);
        
        // Fill all slots with background first
        for (int i = 0; i < 54; i++) {
            this.setItem(i, background, () -> {});
        }

        // Get all shop items
        Map<String, ShopItem> items = BaseShopManager.getAllShopItems();
        List<ShopItem> itemList = new ArrayList<>(items.values());
        
        // Calculate player balance
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        double balance = 0.0;
        if (Econ.shouldBeUsed() && fPlayer != null) {
            try {
                balance = Econ.getBalance(fPlayer.getAccountId());
            } catch (Exception e) {
                balance = 0.0;
            }
        }

        // Place items starting at slot 10 (skip first row and first column)
        int slot = 10;
        for (ShopItem shopItem : itemList) {
            if (slot >= 44) break; // Stop before last row
            
            // Skip border slots (columns 0, 8, 9, 17, etc.)
            int col = slot % 9;
            if (col == 0 || col == 8) {
                slot++;
                continue;
            }
            
            ItemStack item = createShopItem(shopItem, balance);
            this.setItem(slot, item, () -> {
                handlePurchase(shopItem);
            });
            
            slot++;
        }

        // Back button in bottom right
        ItemStack backItem = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.RED + "Back");
        backMeta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "Return to base menu"
        ));
        backItem.setItemMeta(backMeta);
        this.setItem(49, backItem, () -> {
            this.close();
            BeaconMenuGUI.openMenu(player, faction);
        });
    }

    private ItemStack createShopItem(ShopItem shopItem, double balance) {
        ItemStack item = shopItem.createItemStack().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        List<String> lore = new ArrayList<>(meta.getLore());
        
        // Update lore with current balance info
        boolean canAfford = balance >= shopItem.getPrice();
        
        // Add purchase status at the end
        lore.add("");
        if (canAfford) {
            lore.add(ChatColor.GREEN + "✓ You can afford this");
            lore.add(ChatColor.YELLOW + "Click to purchase!");
        } else {
            lore.add(ChatColor.RED + "✗ Insufficient funds");
            double needed = shopItem.getPrice() - balance;
            lore.add(ChatColor.GRAY + "You need: " + ChatColor.RED + formatMoney(needed) + ChatColor.GRAY + " more");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }

    private void handlePurchase(ShopItem shopItem) {
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        if (fPlayer == null) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Error: Could not find player data!")));
            return;
        }

        if (!Econ.shouldBeUsed()) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Economy is not enabled!")));
            return;
        }

        // Check balance
        double balance = Econ.getBalance(fPlayer.getAccountId());
        if (balance < shopItem.getPrice()) {
            double needed = shopItem.getPrice() - balance;
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You don't have enough money!")));
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("You need ") + 
                PandoraMessage.highlight(formatMoney(needed)) + PandoraMessage.text(" more.")));
            return;
        }

        // Check inventory space
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Your inventory is full!")));
            return;
        }

        // Charge player
        boolean success = Econ.modifyMoney(fPlayer, -shopItem.getPrice(), 
            "to purchase " + shopItem.getDisplayName(), 
            "for purchasing " + shopItem.getDisplayName());
        
        if (!success) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Failed to process payment!")));
            return;
        }

        // Give item
        ItemStack item = shopItem.createItemStack();
        player.getInventory().addItem(item);

        // Send success message
        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.success("Purchased ") + 
            PandoraMessage.highlight(shopItem.getDisplayName()) + PandoraMessage.text(" for ") + 
            PandoraMessage.highlight(formatMoney(shopItem.getPrice())) + PandoraMessage.text("!")));

        // Refresh GUI to update balance
        this.redraw();
    }

    private String formatMoney(double amount) {
        if (amount >= 1000000) {
            return String.format("$%.2fM", amount / 1000000.0);
        } else if (amount >= 1000) {
            return String.format("$%.2fK", amount / 1000.0);
        } else {
            return String.format("$%.2f", amount);
        }
    }
}
