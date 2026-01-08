package com.playpandora.essentialsgui.gui;

import com.playpandora.essentialsgui.EssentialsGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RulesGUI {
    
    private final EssentialsGUI plugin;
    
    public RulesGUI(EssentialsGUI plugin) {
        this.plugin = plugin;
    }
    
    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54,
            org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8Rules"));
        
        // Fill borders
        fillBorders(inv);
        
        // Add close button (bottom right)
        int closeSlot = 53;
        inv.setItem(closeSlot, createCloseButton());
        
        // Add rules items
        List<RuleItem> rules = getRules();
        int slot = 10; // Start after first row and left border
        
        for (RuleItem rule : rules) {
            if (slot >= 44) break; // Stop before close button row
            
            ItemStack item = createRuleItem(rule);
            inv.setItem(slot, item);
            
            slot++;
            // Skip border columns
            if (slot % 9 == 8) {
                slot += 2; // Skip right border, move to next row
            }
        }
        
        // Fill remaining empty slots with borders
        fillEmptySlots(inv);
        
        player.openInventory(inv);
    }
    
    private List<RuleItem> getRules() {
        List<RuleItem> rules = new ArrayList<>();
        
        rules.add(new RuleItem(Material.RED_BED, "&e&lNo Bed Farm Killing",
            Arrays.asList("&7Killing players at their", "&7bed farms is not allowed.")));
        
        rules.add(new RuleItem(Material.ENDER_PEARL, "&e&lNo Teleport Trapping",
            Arrays.asList("&7Trapping players using", "&7teleport commands is", "&7not allowed.", "",
                "&6Note: &7Regular traps are", "&7allowed, but teleport", "&7traps are not.")));
        
        rules.add(new RuleItem(Material.EMERALD, "&e&lNo Scamming",
            Arrays.asList("&7Scamming other players", "&7is strictly prohibited.")));
        
        rules.add(new RuleItem(Material.IRON_SWORD, "&e&lNo Hacking",
            Arrays.asList("&7Using hacked clients or", "&7cheating modifications is", "&7not allowed.")));
        
        rules.add(new RuleItem(Material.SHIELD, "&e&lNo VPN (Unless Approved)",
            Arrays.asList("&7Using VPNs is not allowed", "&7unless you have received", "&7permission from staff.")));
        
        return rules;
    }
    
    private ItemStack createRuleItem(RuleItem rule) {
        ItemStack item = new ItemStack(rule.material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta == null) {
            return item;
        }
        
        // Set display name
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', rule.name));
        
        // Build lore
        List<String> lore = new ArrayList<>();
        for (String line : rule.lore) {
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private void fillBorders(Inventory inv) {
        ItemStack border = createBorder();
        int size = inv.getSize();
        int rows = size / 9;
        
        // Fill top row
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
        }
        
        // Fill bottom row
        int startBottom = (rows - 1) * 9;
        for (int i = startBottom; i < size; i++) {
            inv.setItem(i, border);
        }
        
        // Fill side columns
        for (int row = 0; row < rows; row++) {
            int leftSlot = row * 9;
            int rightSlot = row * 9 + 8;
            inv.setItem(leftSlot, border);
            inv.setItem(rightSlot, border);
        }
    }
    
    private void fillEmptySlots(Inventory inv) {
        ItemStack border = createBorder();
        int size = inv.getSize();
        for (int i = 0; i < size; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, border);
            }
        }
    }
    
    private ItemStack createBorder() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&c✖ Close"));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Click to close this menu"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private static class RuleItem {
        Material material;
        String name;
        List<String> lore;
        
        RuleItem(Material material, String name, List<String> lore) {
            this.material = material;
            this.name = name;
            this.lore = lore;
        }
    }
}


