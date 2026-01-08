package com.playpandora.pandoraminer.managers;

import com.playpandora.pandoraminer.PandoraMiner;
import com.playpandora.pandoraminer.util.ColorUtil;
import com.playpandora.pandoraminer.util.InventoryUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages miner state for all players
 */
public class MinerManager {
    
    private final PandoraMiner plugin;
    private final Set<UUID> minerPlayers;
    private final Map<UUID, String> savedInventories; // UUID -> Base64 inventory data
    private File dataFile;
    private FileConfiguration dataConfig;
    
    public MinerManager(PandoraMiner plugin) {
        this.plugin = plugin;
        this.minerPlayers = new HashSet<>();
        this.savedInventories = new HashMap<>();
        
        // Load data file
        this.dataFile = new File(plugin.getDataFolder(), "saved_inventories.yml");
        if (!this.dataFile.exists()) {
            try {
                this.dataFile.getParentFile().mkdirs();
                this.dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create saved_inventories.yml: " + e.getMessage());
            }
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        // Load saved inventories from file
        loadSavedInventories();
        
        // Start periodic save task
        startAutoSave();
    }
    
    /**
     * Check if a player is in miner state
     */
    public boolean isMiner(Player player) {
        return minerPlayers.contains(player.getUniqueId());
    }
    
    /**
     * Toggle miner state for a player
     */
    public void toggleMiner(Player player) {
        if (isMiner(player)) {
            disableMiner(player);
        } else {
            enableMiner(player);
        }
    }
    
    /**
     * Enable miner state for a player
     */
    public void enableMiner(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Save current inventory
        saveInventory(player);
        
        // Clear inventory (temporarily) but preserve backpack
        ItemStack backpack = getBackpackItem(player);
        player.getInventory().clear();
        
        // Give miner kit
        giveMinerKit(player);
        
        // Restore backpack if exists
        if (backpack != null) {
            // Find first available slot
            for (int i = 0; i < 36; i++) {
                ItemStack slotItem = player.getInventory().getItem(i);
                if (slotItem == null || slotItem.getType() == Material.AIR || slotItem.getAmount() == 0) {
                    player.getInventory().setItem(i, backpack);
                    break;
                }
            }
        }
        
        // Apply potion effects
        applyMinerEffects(player);
        
        // Add to miner set
        minerPlayers.add(uuid);
        
        // Send message
        player.sendMessage(ColorUtil.format(ColorUtil.text("Miner state ") + ColorUtil.highlight("enabled") + ColorUtil.text("! Your inventory has been safely saved.")));
        
        // Send Hook notification
        com.playpandora.pandoraminer.integration.HookIntegration.sendMinerEnabled(player);
    }
    
    /**
     * Get backpack item from player's inventory
     */
    private ItemStack getBackpackItem(Player player) {
        ItemStack backpack = com.playpandora.pandoraminer.integration.MinepacksIntegration.getBackpackItem(player);
        return backpack != null ? backpack.clone() : null;
    }
    
    /**
     * Disable miner state for a player
     */
    public void disableMiner(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (!isMiner(player)) {
            return;
        }
        
        // Clear miner kit
        player.getInventory().clear();
        
        // Remove potion effects
        removeMinerEffects(player);
        
        // Restore saved inventory
        restoreInventory(player);
        
        // Remove from miner set
        minerPlayers.remove(uuid);
        
        // Send message
        player.sendMessage(ColorUtil.format(ColorUtil.text("Miner state ") + ColorUtil.highlight("disabled") + ColorUtil.text("! Your inventory has been restored.")));
        
        // Send Hook notification
        com.playpandora.pandoraminer.integration.HookIntegration.sendMinerDisabled(player);
    }
    
    /**
     * Save player's inventory
     */
    private void saveInventory(Player player) {
        UUID uuid = player.getUniqueId();
        
        try {
            ItemStack[] contents = player.getInventory().getContents();
            ItemStack[] armor = player.getInventory().getArmorContents();
            
            // Combine inventory and armor
            ItemStack[] fullInventory = new ItemStack[45]; // 36 inventory + 4 armor + 1 offhand
            System.arraycopy(contents, 0, fullInventory, 0, 36);
            System.arraycopy(armor, 0, fullInventory, 36, 4);
            fullInventory[40] = player.getInventory().getItemInOffHand();
            
            String inventoryData = InventoryUtil.inventoryToString(fullInventory);
            savedInventories.put(uuid, inventoryData);
            
            // Save to file immediately
            dataConfig.set("inventories." + uuid.toString(), inventoryData);
            dataConfig.save(dataFile);
            
            plugin.getLogger().fine("Saved inventory for " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save inventory for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Restore player's inventory
     */
    private void restoreInventory(Player player) {
        UUID uuid = player.getUniqueId();
        
        String inventoryData = savedInventories.get(uuid);
        if (inventoryData == null) {
            plugin.getLogger().warning("No saved inventory found for " + player.getName());
            return;
        }
        
        try {
            ItemStack[] fullInventory = InventoryUtil.stringToInventory(inventoryData);
            
            // Restore inventory
            ItemStack[] contents = new ItemStack[36];
            System.arraycopy(fullInventory, 0, contents, 0, 36);
            player.getInventory().setContents(contents);
            
            // Restore armor
            ItemStack[] armor = new ItemStack[4];
            System.arraycopy(fullInventory, 36, armor, 0, 4);
            player.getInventory().setArmorContents(armor);
            
            // Restore offhand
            if (fullInventory.length > 40 && fullInventory[40] != null) {
                player.getInventory().setItemInOffHand(fullInventory[40]);
            }
            
            // Remove from saved inventories
            savedInventories.remove(uuid);
            dataConfig.set("inventories." + uuid.toString(), null);
            dataConfig.save(dataFile);
            
            plugin.getLogger().fine("Restored inventory for " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to restore inventory for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Give miner kit to player
     */
    private void giveMinerKit(Player player) {
        // Gold helmet
        ItemStack helmet = new ItemStack(Material.GOLDEN_HELMET);
        ItemMeta helmetMeta = helmet.getItemMeta();
        helmetMeta.addEnchant(Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("protection")), 1, true);
        helmet.setItemMeta(helmetMeta);
        player.getInventory().setHelmet(helmet);
        
        // Gold chestplate
        ItemStack chestplate = new ItemStack(Material.GOLDEN_CHESTPLATE);
        ItemMeta chestplateMeta = chestplate.getItemMeta();
        chestplateMeta.addEnchant(Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("protection")), 1, true);
        chestplate.setItemMeta(chestplateMeta);
        player.getInventory().setChestplate(chestplate);
        
        // Gold leggings
        ItemStack leggings = new ItemStack(Material.GOLDEN_LEGGINGS);
        ItemMeta leggingsMeta = leggings.getItemMeta();
        leggingsMeta.addEnchant(Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("protection")), 1, true);
        leggings.setItemMeta(leggingsMeta);
        player.getInventory().setLeggings(leggings);
        
        // Gold boots
        ItemStack boots = new ItemStack(Material.GOLDEN_BOOTS);
        ItemMeta bootsMeta = boots.getItemMeta();
        bootsMeta.addEnchant(Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("protection")), 1, true);
        boots.setItemMeta(bootsMeta);
        player.getInventory().setBoots(boots);
        
        // Netherite pickaxe with Efficiency V
        ItemStack pickaxe = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta pickaxeMeta = pickaxe.getItemMeta();
        pickaxeMeta.addEnchant(Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("efficiency")), 5, true);
        pickaxeMeta.setUnbreakable(true);
        pickaxe.setItemMeta(pickaxeMeta);
        player.getInventory().setItemInMainHand(pickaxe);
    }
    
    /**
     * Apply potion effects to miner
     */
    private void applyMinerEffects(Player player) {
        // Haste II (equivalent to Efficiency V on pickaxe, but also affects other tools)
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, 1, false, false));
        
        // Invisibility
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
        
        // Speed II
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
        
        // Fire Resistance
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
    }
    
    /**
     * Remove potion effects from miner
     */
    private void removeMinerEffects(Player player) {
        player.removePotionEffect(PotionEffectType.HASTE);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
    }
    
    /**
     * Load saved inventories from file
     */
    private void loadSavedInventories() {
        if (!dataConfig.contains("inventories")) {
            return;
        }
        
        for (String uuidString : dataConfig.getConfigurationSection("inventories").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                String inventoryData = dataConfig.getString("inventories." + uuidString);
                if (inventoryData != null) {
                    savedInventories.put(uuid, inventoryData);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in saved_inventories.yml: " + uuidString);
            }
        }
        
        plugin.getLogger().info("Loaded " + savedInventories.size() + " saved inventories");
    }
    
    /**
     * Start auto-save task
     */
    private void startAutoSave() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    dataConfig.save(dataFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to auto-save saved_inventories.yml: " + e.getMessage());
                }
            }
        }.runTaskTimer(plugin, 1200L, 1200L); // Save every minute
    }
    
    /**
     * Clear saved inventory for a player (used on quit)
     */
    public void clearSavedInventory(UUID uuid) {
        savedInventories.remove(uuid);
        minerPlayers.remove(uuid);
        
        // Remove from file
        if (dataConfig != null) {
            dataConfig.set("inventories." + uuid.toString(), null);
            try {
                dataConfig.save(dataFile);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to clear saved inventory for " + uuid + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Restore all inventories on disable (for players still in miner mode)
     */
    public void restoreAllInventories() {
        for (UUID uuid : new HashSet<>(minerPlayers)) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                disableMiner(player);
            }
        }
    }
}

