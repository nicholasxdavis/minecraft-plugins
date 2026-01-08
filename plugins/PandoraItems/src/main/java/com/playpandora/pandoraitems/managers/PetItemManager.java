package com.playpandora.pandoraitems.managers;

import com.playpandora.pandoraitems.PandoraItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PetItemManager {
    
    private final PandoraItems plugin;
    
    public PetItemManager(PandoraItems plugin) {
        this.plugin = plugin;
    }
    
    public ItemStack createPetItem(String petKey) {
        // Try to get pet from PetPlugin
        Object petPlugin = Bukkit.getPluginManager().getPlugin("PetPlugin");
        if (petPlugin != null) {
            try {
                // Get pet config from PetPlugin
                Class<?> petPluginClass = petPlugin.getClass();
                java.lang.reflect.Method getConfig = petPluginClass.getMethod("getConfig");
                org.bukkit.configuration.file.FileConfiguration config = (org.bukkit.configuration.file.FileConfiguration) getConfig.invoke(petPlugin);
                
                ConfigurationSection petConfig = config.getConfigurationSection("pet-types." + petKey);
                if (petConfig != null && petConfig.getBoolean("enabled", true)) {
                    String name = petConfig.getString("name", "Pet");
                    List<String> description = petConfig.getStringList("description");
                    String materialStr = petConfig.getString("material", "SADDLE");
                    int requiredLevel = petConfig.getInt("required-level", 0);
                    
                    Material material = Material.SADDLE;
                    try {
                        material = Material.valueOf(materialStr);
                    } catch (IllegalArgumentException e) {
                        material = Material.SADDLE;
                    }
                    
                    ItemStack item = new ItemStack(material);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
                        List<String> lore = new ArrayList<>();
                        for (String line : description) {
                            lore.add(ChatColor.translateAlternateColorCodes('&', line));
                        }
                        if (requiredLevel > 0) {
                            lore.add("");
                            lore.add(ChatColor.GRAY + "Required Level: " + ChatColor.GOLD + requiredLevel);
                        }
                        lore.add("");
                        lore.add(ChatColor.GRAY + "Right-click to claim this pet");
                        lore.add(ChatColor.GRAY + "PandoraItems:Pet:" + petKey);
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                    return item;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to create pet item from PetPlugin: " + e.getMessage());
            }
        }
        
        // Fallback: create basic item
        return createFallbackPetItem(petKey);
    }
    
    private ItemStack createFallbackPetItem(String petKey) {
        ItemStack item = new ItemStack(Material.SADDLE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6Pet: &e" + petKey));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Right-click to claim this pet");
            lore.add(ChatColor.GRAY + "PandoraItems:Pet:" + petKey);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    public boolean isValidPetKey(String key) {
        Object petPlugin = Bukkit.getPluginManager().getPlugin("PetPlugin");
        if (petPlugin != null) {
            try {
                Class<?> petPluginClass = petPlugin.getClass();
                java.lang.reflect.Method getConfig = petPluginClass.getMethod("getConfig");
                org.bukkit.configuration.file.FileConfiguration config = (org.bukkit.configuration.file.FileConfiguration) getConfig.invoke(petPlugin);
                
                ConfigurationSection petConfig = config.getConfigurationSection("pet-types." + key);
                return petConfig != null && petConfig.getBoolean("enabled", true);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
    
    public int getRequiredLevel(String petKey) {
        Object petPlugin = Bukkit.getPluginManager().getPlugin("PetPlugin");
        if (petPlugin != null) {
            try {
                Class<?> petPluginClass = petPlugin.getClass();
                java.lang.reflect.Method getConfig = petPluginClass.getMethod("getConfig");
                org.bukkit.configuration.file.FileConfiguration config = (org.bukkit.configuration.file.FileConfiguration) getConfig.invoke(petPlugin);
                
                ConfigurationSection petConfig = config.getConfigurationSection("pet-types." + petKey);
                if (petConfig != null) {
                    return petConfig.getInt("required-level", 0);
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        return 0;
    }
}




