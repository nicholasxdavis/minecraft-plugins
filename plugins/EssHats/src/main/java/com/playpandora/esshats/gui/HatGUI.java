package com.playpandora.esshats.gui;

import com.playpandora.esshats.EssHats;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HatGUI {
    
    private final EssHats plugin;
    
    // Hat definitions organized by tier
    private final Map<String, HatItem> sTierHats = new HashMap<>();
    private final Map<String, HatItem> aTierHats = new HashMap<>();
    private final Map<String, HatItem> bTierHats = new HashMap<>();
    private final Map<String, HatItem> cTierHats = new HashMap<>();
    private final Map<String, HatItem> dTierHats = new HashMap<>();
    
    public HatGUI(EssHats plugin) {
        this.plugin = plugin;
        initializeHats();
    }
    
    public void reinitialize() {
        sTierHats.clear();
        aTierHats.clear();
        bTierHats.clear();
        cTierHats.clear();
        dTierHats.clear();
        initializeHats();
    }
    
    private void initializeHats() {
        // S-Tier
        String sTierLabel = plugin.getConfigManager().getTierLabel("s");
        sTierHats.put("dragon_head", new HatItem(Material.DRAGON_HEAD, "Dragon Head", "S-Tier", sTierLabel, "Undisputed #1. Endgame, iconic, instant status."));
        sTierHats.put("wither_skeleton_skull", new HatItem(Material.WITHER_SKELETON_SKULL, "Wither Skeleton Skull", "S-Tier", sTierLabel, "Pain to farm, intimidating look."));
        sTierHats.put("piglin_head", new HatItem(Material.PIGLIN_HEAD, "Piglin Head", "S-Tier", sTierLabel, "Rare drop, newer, still uncommon."));
        sTierHats.put("player_head", new HatItem(Material.PLAYER_HEAD, "Player Head", "S-Tier", sTierLabel, "Infinite variants, custom textures = drip potential."));
        sTierHats.put("creeper_head", new HatItem(Material.CREEPER_HEAD, "Creeper Head", "S-Tier", sTierLabel, "Charged creeper requirement makes it legit."));
        
        // A-Tier
        String aTierLabel = plugin.getConfigManager().getTierLabel("a");
        aTierHats.put("zombie_head", new HatItem(Material.ZOMBIE_HEAD, "Zombie Head", "A-Tier", aTierLabel, "Still rare, classic mob flex."));
        aTierHats.put("skeleton_skull", new HatItem(Material.SKELETON_SKULL, "Skeleton Skull", "A-Tier", aTierLabel, "Cleaner look than zombie, harder than it seems."));
        aTierHats.put("end_crystal", new HatItem(Material.END_CRYSTAL, "End Crystal", "A-Tier", aTierLabel, "Tall silhouette, creepy factor."));
        aTierHats.put("dragon_egg", new HatItem(Material.DRAGON_EGG, "Dragon Egg", "A-Tier", aTierLabel, "Yes it works. Absolute troll/flex hat."));
        aTierHats.put("beacon", new HatItem(Material.BEACON, "Beacon", "A-Tier", aTierLabel, "Big glowing flex, screams \"rich\"."));
        
        // B-Tier
        String bTierLabel = plugin.getConfigManager().getTierLabel("b");
        bTierHats.put("jack_o_lantern", new HatItem(Material.JACK_O_LANTERN, "Jack o'Lantern", "B-Tier", bTierLabel, "OG PvP / Halloween classic."));
        bTierHats.put("carved_pumpkin", new HatItem(Material.CARVED_PUMPKIN, "Carved Pumpkin", "B-Tier", bTierLabel, "Simple, iconic, still looks good."));
        bTierHats.put("sea_lantern", new HatItem(Material.SEA_LANTERN, "Sea Lantern", "B-Tier", bTierLabel, "Clean glow, underrated."));
        bTierHats.put("conduit", new HatItem(Material.CONDUIT, "Conduit", "B-Tier", bTierLabel, "Small but flashy, niche flex."));
        bTierHats.put("shulker_box", new HatItem(Material.SHULKER_BOX, "Shulker Box", "B-Tier", bTierLabel, "Practical + colorful, modern look."));
        
        // C-Tier
        String cTierLabel = plugin.getConfigManager().getTierLabel("c");
        cTierHats.put("enchanting_table", new HatItem(Material.ENCHANTING_TABLE, "Enchanting Table", "C-Tier", cTierLabel, "Floating glyphs = instant attention."));
        cTierHats.put("anvil", new HatItem(Material.ANVIL, "Anvil", "C-Tier", cTierLabel, "Heavy meme energy."));
        cTierHats.put("barrel", new HatItem(Material.BARREL, "Barrel", "C-Tier", cTierLabel, "Surprisingly clean on some skins."));
        cTierHats.put("chest", new HatItem(Material.CHEST, "Chest", "C-Tier", cTierLabel, "Old-school, still works."));
        cTierHats.put("lectern", new HatItem(Material.LECTERN, "Lectern", "C-Tier", cTierLabel, "Book-on-head vibe."));
        
        // D-Tier
        String dTierLabel = plugin.getConfigManager().getTierLabel("d");
        dTierHats.put("furnace", new HatItem(Material.FURNACE, "Furnace", "D-Tier", dTierLabel, "Chunky but funny."));
        dTierHats.put("crafting_table", new HatItem(Material.CRAFTING_TABLE, "Crafting Table", "D-Tier", dTierLabel, "Classic noob-to-pro meme."));
        dTierHats.put("observer", new HatItem(Material.OBSERVER, "Observer", "D-Tier", dTierLabel, "Creepy red-dot stare."));
        dTierHats.put("note_block", new HatItem(Material.NOTE_BLOCK, "Note Block", "D-Tier", dTierLabel, "Music nerd energy."));
        dTierHats.put("tnt", new HatItem(Material.TNT, "TNT", "D-Tier", dTierLabel, "Universal \"don't stand near me\" message."));
    }
    
    public void openGUI(Player player) {
        // Calculate available hats based on permissions
        List<HatItem> availableHats = new ArrayList<>();
        
        boolean hasAll = player.hasPermission("esshats.all");
        boolean hasS = hasAll || player.hasPermission("esshats.tier.s");
        boolean hasA = hasAll || player.hasPermission("esshats.tier.a");
        boolean hasB = hasAll || player.hasPermission("esshats.tier.b");
        boolean hasC = hasAll || player.hasPermission("esshats.tier.c");
        boolean hasD = hasAll || player.hasPermission("esshats.tier.d");
        
        if (hasS) availableHats.addAll(sTierHats.values());
        if (hasA) availableHats.addAll(aTierHats.values());
        if (hasB) availableHats.addAll(bTierHats.values());
        if (hasC) availableHats.addAll(cTierHats.values());
        if (hasD) availableHats.addAll(dTierHats.values());
        
        if (availableHats.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-hat-permissions"));
            return;
        }
        
        // Get GUI size from config
        int size = plugin.getConfigManager().getGUISize();
        
        String title = plugin.getConfigManager().getGUITitle();
        Inventory inv = Bukkit.createInventory(null, size,
            org.bukkit.ChatColor.translateAlternateColorCodes('&', title));
        
        // Fill borders
        fillBorders(inv, size);
        
        // Add close button (bottom right)
        int closeSlot = size - 1;
        inv.setItem(closeSlot, createCloseButton());
        
        // Add hat items (start from slot 10, skip borders)
        int currentSlot = 10;
        int maxSlot = size - 10; // Leave last row for close button
        
        for (HatItem hat : availableHats) {
            // Find next valid slot (skip border columns 0 and 8)
            while (currentSlot < maxSlot && currentSlot != closeSlot) {
                int col = currentSlot % 9;
                if (col != 0 && col != 8) {
                    break;
                }
                currentSlot++;
            }
            
            if (currentSlot >= maxSlot || currentSlot == closeSlot) {
                break;
            }
            
            inv.setItem(currentSlot, createHatItem(hat));
            currentSlot++;
            
            // Skip right border when reaching end of row
            if ((currentSlot % 9) == 8) {
                currentSlot += 2; // Skip to next row, past border
            }
        }
        
        // Fill remaining empty slots
        fillEmptySlots(inv, size);
        
        // Play sound effect when opening GUI
        float[] soundParams = plugin.getConfigManager().getSoundGUIOpenParams();
        player.playSound(player.getLocation(), plugin.getConfigManager().getSoundGUIOpen(), 
            soundParams[0], soundParams[1]);
        
        player.openInventory(inv);
    }
    
    private ItemStack createHatItem(HatItem hat) {
        ItemStack item = new ItemStack(hat.getMaterial());
        ItemMeta meta = item.getItemMeta();
        
        if (meta == null) {
            return item;
        }
        
        // Set display name with configured color
        String displayName = plugin.getConfigManager().getHatNameColor() + hat.getDisplayName();
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName));
        
        // Build lore with configured formatting
        List<String> lore = new ArrayList<>();
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', hat.getTierLabel()));
        lore.add("");
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfigManager().getDescriptionColor() + hat.getDescription()));
        lore.add("");
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfigManager().getClickToEquipText()));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    public HatItem getHatByItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        
        Material material = item.getType();
        
        // Check all tiers
        for (HatItem hat : sTierHats.values()) {
            if (hat.getMaterial() == material) {
                return hat;
            }
        }
        for (HatItem hat : aTierHats.values()) {
            if (hat.getMaterial() == material) {
                return hat;
            }
        }
        for (HatItem hat : bTierHats.values()) {
            if (hat.getMaterial() == material) {
                return hat;
            }
        }
        for (HatItem hat : cTierHats.values()) {
            if (hat.getMaterial() == material) {
                return hat;
            }
        }
        for (HatItem hat : dTierHats.values()) {
            if (hat.getMaterial() == material) {
                return hat;
            }
        }
        
        return null;
    }
    
    private int calculateOptimalSize(int itemCount) {
        if (itemCount <= 9) return 9;
        if (itemCount <= 18) return 18;
        if (itemCount <= 27) return 27;
        if (itemCount <= 36) return 36;
        if (itemCount <= 45) return 45;
        return 54;
    }
    
    private void fillBorders(Inventory inv, int size) {
        ItemStack border = createBorder();
        int rows = size / 9;
        
        // Top row
        if (rows > 0) {
            for (int i = 0; i < 9; i++) {
                if (inv.getItem(i) == null) {
                    inv.setItem(i, border);
                }
            }
        }
        
        // Bottom row
        if (rows > 0) {
            int startBottom = (rows - 1) * 9;
            for (int i = startBottom; i < size; i++) {
                if (inv.getItem(i) == null) {
                    inv.setItem(i, border);
                }
            }
        }
        
        // Side columns
        if (rows > 0) {
            for (int row = 0; row < rows; row++) {
                int leftSlot = row * 9;
                int rightSlot = row * 9 + 8;
                if (inv.getItem(leftSlot) == null) {
                    inv.setItem(leftSlot, border);
                }
                if (inv.getItem(rightSlot) == null) {
                    inv.setItem(rightSlot, border);
                }
            }
        }
    }
    
    private void fillEmptySlots(Inventory inv, int size) {
        ItemStack border = createBorder();
        for (int i = 0; i < size; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, border);
            }
        }
    }
    
    private ItemStack createBorder() {
        ItemStack item = new ItemStack(plugin.getConfigManager().getBorderMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(plugin.getConfigManager().getCloseButtonMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getCloseButtonName()));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                plugin.getConfigManager().getCloseButtonLore()));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    // Inner class for hat items
    public static class HatItem {
        private final Material material;
        private final String displayName;
        private final String tier;
        private final String tierLabel;
        private final String description;
        
        public HatItem(Material material, String displayName, String tier, String tierLabel, String description) {
            this.material = material;
            this.displayName = displayName;
            this.tier = tier;
            this.tierLabel = tierLabel;
            this.description = description;
        }
        
        public Material getMaterial() {
            return material;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getTier() {
            return tier;
        }
        
        public String getTierLabel() {
            return tierLabel;
        }
        
        public String getDescription() {
            return description;
        }
    }
}

