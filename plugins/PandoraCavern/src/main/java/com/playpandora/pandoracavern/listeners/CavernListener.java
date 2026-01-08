package com.playpandora.pandoracavern.listeners;

import com.playpandora.pandoracavern.PandoraCavern;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

public class CavernListener implements Listener {
    
    private final PandoraCavern plugin;
    private final Random random;
    
    public CavernListener(PandoraCavern plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        
        // Check if player is placing a special cavern ore
        if (plugin.getBlockManager().isCavernOre(item)) {
            Location location = event.getBlockPlaced().getLocation();
            
            // Register this location as a cavern block
            plugin.getBlockManager().placeCavernBlock(location);
            
            player.sendMessage(plugin.formatMessage("{prefix} &7Placed a &6cavern block &7at this location!"));
            player.sendMessage(plugin.formatMessage("{prefix} &7This block will respawn &67 seconds &7after being mined."));
            return;
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();
        
        // Check if this is a cavern block
        if (!plugin.getDataManager().isCavernBlock(location)) {
            return; // Not a cavern block, let normal behavior happen
        }
        
        // Check if player is using removal pickaxe
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (plugin.getBlockManager().isRemovalPickaxe(tool)) {
            // Permanently remove the block
            event.setCancelled(true);
            plugin.getBlockManager().removeCavernBlock(location);
            block.setType(Material.AIR);
            
            player.sendMessage(plugin.formatMessage("{prefix} &7Permanently removed cavern block at this location!"));
            return;
        }
        
        // Normal mining - give loot and mark for respawn
        event.setCancelled(true);
        block.setType(Material.AIR);
        
        // Give loot
        giveLoot(player, location);
        
        // Mark block as mined for respawn
        plugin.getRespawnManager().markBlockAsMined(location);
        
        player.sendMessage(plugin.formatMessage("{prefix} &7Mined cavern block! It will respawn in &67 seconds&7."));
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerHoldingPickaxe(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        
        // Remove any existing haste from pickaxe first
        if (player.hasPotionEffect(org.bukkit.potion.PotionEffectType.HASTE)) {
            org.bukkit.potion.PotionEffect haste = player.getPotionEffect(org.bukkit.potion.PotionEffectType.HASTE);
            if (haste != null && haste.getAmplifier() == 4) {
                player.removePotionEffect(org.bukkit.potion.PotionEffectType.HASTE);
            }
        }
        
        // Check old slot too
        ItemStack oldItem = player.getInventory().getItem(event.getPreviousSlot());
        if (oldItem != null && plugin.getBlockManager().isRemovalPickaxe(oldItem)) {
            if (player.hasPotionEffect(org.bukkit.potion.PotionEffectType.HASTE)) {
                org.bukkit.potion.PotionEffect haste = player.getPotionEffect(org.bukkit.potion.PotionEffectType.HASTE);
                if (haste != null && haste.getAmplifier() == 4) {
                    player.removePotionEffect(org.bukkit.potion.PotionEffectType.HASTE);
                }
            }
        }
        
        if (item != null && plugin.getBlockManager().isRemovalPickaxe(item)) {
            // Apply haste 5 effect (amplifier 4 = level 5)
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.HASTE,
                Integer.MAX_VALUE,
                4, // Haste 5 (level 4 = haste 5)
                true,
                false
            ));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        // Check if player is holding removal pickaxe on join
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && plugin.getBlockManager().isRemovalPickaxe(item)) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.HASTE,
                Integer.MAX_VALUE,
                4,
                true,
                false
            ));
        }
    }
    
    private void giveLoot(Player player, Location location) {
        List<String> lootList = plugin.getConfig().getStringList("loot");
        
        if (lootList.isEmpty()) {
            // Default loot if config is empty
            giveDefaultLoot(player, location);
            return;
        }
        
        // Pick random loot from config
        String lootEntry = lootList.get(random.nextInt(lootList.size()));
        String[] parts = lootEntry.split(":");
        
        if (parts.length != 2) {
            giveDefaultLoot(player, location);
            return;
        }
        
        try {
            Material material = Material.valueOf(parts[0].toUpperCase());
            int amount = Integer.parseInt(parts[1]);
            
            ItemStack loot = new ItemStack(material, amount);
            
            // Drop item at location
            location.getWorld().dropItemNaturally(location, loot);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid loot entry in config: " + lootEntry);
            giveDefaultLoot(player, location);
        }
    }
    
    private void giveDefaultLoot(Player player, Location location) {
        // Default food items
        Material[] defaultLoot = {
            Material.COOKED_BEEF,
            Material.GOLDEN_APPLE,
            Material.COOKED_PORKCHOP,
            Material.BAKED_POTATO
        };
        
        Material lootType = defaultLoot[random.nextInt(defaultLoot.length)];
        int amount = random.nextInt(3) + 1; // 1-3 items
        
        ItemStack loot = new ItemStack(lootType, amount);
        location.getWorld().dropItemNaturally(location, loot);
    }
}

