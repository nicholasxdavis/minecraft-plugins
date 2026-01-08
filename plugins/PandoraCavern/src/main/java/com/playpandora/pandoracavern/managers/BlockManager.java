package com.playpandora.pandoracavern.managers;

import com.playpandora.pandoracavern.PandoraCavern;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class BlockManager {
    
    private final PandoraCavern plugin;
    private static final String CAVERN_ORE_KEY = "PANDORA_CAVERN_ORE";
    private static final String REMOVAL_PICKAXE_KEY = "PANDORA_CAVERN_REMOVAL_PICKAXE";
    
    public BlockManager(PandoraCavern plugin) {
        this.plugin = plugin;
    }
    
    public ItemStack createCavernOre(int amount) {
        // Use ANCIENT_DEBRIS as it's the actual netherite ore block in Minecraft
        ItemStack ore = new ItemStack(Material.ANCIENT_DEBRIS, amount);
        ItemMeta meta = ore.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lSpecial Netherite Ore"));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Place this block to create"));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7a respawning cavern block!"));
            meta.setLore(lore);
            
            // Add custom NBT data to identify this as a special ore
            meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, CAVERN_ORE_KEY),
                org.bukkit.persistence.PersistentDataType.STRING,
                "true"
            );
            
            ore.setItemMeta(meta);
        }
        return ore;
    }
    
    public boolean isCavernOre(ItemStack item) {
        if (item == null || item.getType() != Material.ANCIENT_DEBRIS || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        return meta.getPersistentDataContainer().has(
            new org.bukkit.NamespacedKey(plugin, CAVERN_ORE_KEY),
            org.bukkit.persistence.PersistentDataType.STRING
        );
    }
    
    public ItemStack createRemovalPickaxe() {
        ItemStack pickaxe = new ItemStack(Material.NETHERITE_PICKAXE, 1);
        ItemMeta meta = pickaxe.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lCavern Removal Pickaxe"));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Use this pickaxe to permanently"));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7remove cavern blocks!"));
            meta.setLore(lore);
            
            // Add custom NBT data
            meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, REMOVAL_PICKAXE_KEY),
                org.bukkit.persistence.PersistentDataType.STRING,
                "true"
            );
            
            // Add enchantments
            meta.addEnchant(org.bukkit.enchantments.Enchantment.EFFICIENCY, 5, true);
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 3, true);
            
            pickaxe.setItemMeta(meta);
        }
        
        // Apply haste effect will be done via potion effect when holding
        return pickaxe;
    }
    
    public boolean isRemovalPickaxe(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_PICKAXE || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        return meta.getPersistentDataContainer().has(
            new org.bukkit.NamespacedKey(plugin, REMOVAL_PICKAXE_KEY),
            org.bukkit.persistence.PersistentDataType.STRING
        );
    }
    
    public void placeCavernBlock(Location location) {
        Block block = location.getBlock();
        block.setType(Material.ANCIENT_DEBRIS);
        plugin.getDataManager().addBlockLocation(location);
    }
    
    public void removeCavernBlock(Location location) {
        Block block = location.getBlock();
        block.setType(Material.AIR);
        plugin.getDataManager().removeBlockLocation(location);
    }
    
    public void respawnCavernBlock(Location location) {
        Block block = location.getBlock();
        if (block.getType() != Material.ANCIENT_DEBRIS) {
            block.setType(Material.ANCIENT_DEBRIS);
        }
    }
}

