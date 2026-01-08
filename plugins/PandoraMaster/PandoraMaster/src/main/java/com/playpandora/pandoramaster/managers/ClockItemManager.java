package com.playpandora.pandoramaster.managers;

import com.playpandora.pandoramaster.PandoraMaster;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ClockItemManager {
    
    private final PandoraMaster plugin;
    private static final String CLOCK_ITEM_KEY = "pandoramaster_clock";
    
    public ClockItemManager(PandoraMaster plugin) {
        this.plugin = plugin;
    }
    
    public ItemStack createClockItem() {
        ItemStack clock = new ItemStack(Material.CLOCK);
        ItemMeta meta = clock.getItemMeta();
        
        if (meta != null) {
            String name = plugin.getConfig().getString("items.clock.name", "&e&lHub Menu");
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', name));
            
            List<String> lore = new ArrayList<>();
            List<String> configLore = plugin.getConfig().getStringList("items.clock.lore");
            if (configLore.isEmpty()) {
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Right-click to open"));
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7the hub menu"));
            } else {
                for (String line : configLore) {
                    lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
                }
            }
            meta.setLore(lore);
            
            // Mark this item as the clock item using persistent data
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, CLOCK_ITEM_KEY);
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            
            clock.setItemMeta(meta);
        }
        
        return clock;
    }
    
    public boolean isClockItem(ItemStack item) {
        if (item == null || item.getType() != Material.CLOCK) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, CLOCK_ITEM_KEY);
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
    
    public void giveClockToPlayer(Player player) {
        // Check if player already has the clock item
        for (ItemStack item : player.getInventory().getContents()) {
            if (isClockItem(item)) {
                return; // Player already has the clock
            }
        }
        
        // Find an empty slot or hotbar slot
        int slot = player.getInventory().firstEmpty();
        if (slot == -1) {
            // No empty slot, try to give it anyway (will drop)
            player.getWorld().dropItemNaturally(player.getLocation(), createClockItem());
            player.sendMessage(plugin.formatMessage("messages.clock-given", 
                "&e&lPandora &8» &r&7You have received your hub menu item!"));
        } else {
            player.getInventory().setItem(slot, createClockItem());
            player.sendMessage(plugin.formatMessage("messages.clock-given", 
                "&e&lPandora &8» &r&7You have received your hub menu item!"));
        }
    }
    
    public void giveClockToAllOnline() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            giveClockToPlayer(player);
        }
    }
}



