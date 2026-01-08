package com.playpandora.pandoracavern;

import com.playpandora.pandoracavern.commands.CavernCommand;
import com.playpandora.pandoracavern.listeners.CavernListener;
import com.playpandora.pandoracavern.managers.BlockManager;
import com.playpandora.pandoracavern.managers.DataManager;
import com.playpandora.pandoracavern.managers.RespawnManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class PandoraCavern extends JavaPlugin {
    
    private static PandoraCavern instance;
    private DataManager dataManager;
    private BlockManager blockManager;
    private RespawnManager respawnManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize managers
        dataManager = new DataManager(this);
        blockManager = new BlockManager(this);
        respawnManager = new RespawnManager(this);
        
        // Load saved block locations
        dataManager.loadBlocks();
        
        // Start respawn task
        respawnManager.startRespawnTask();
        
        // Register command
        if (getCommand("cavern") != null) {
            getCommand("cavern").setExecutor(new CavernCommand(this));
        } else {
            getLogger().warning("Command 'cavern' not found in plugin.yml!");
        }
        
        // Register listeners
        CavernListener listener = new CavernListener(this);
        getServer().getPluginManager().registerEvents(listener, this);
        
        // Start task to check for players holding removal pickaxe
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                ItemStack item = player.getInventory().getItemInMainHand();
                boolean hasPickaxe = item != null && blockManager.isRemovalPickaxe(item);
                
                if (hasPickaxe) {
                    // Apply haste 5 if not already applied
                    if (!player.hasPotionEffect(org.bukkit.potion.PotionEffectType.HASTE) ||
                        (player.getPotionEffect(org.bukkit.potion.PotionEffectType.HASTE) != null &&
                         player.getPotionEffect(org.bukkit.potion.PotionEffectType.HASTE).getAmplifier() != 4)) {
                        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.HASTE,
                            100, // 5 seconds
                            4, // Haste 5
                            true,
                            false
                        ));
                    }
                } else {
                    // Remove haste if not holding pickaxe
                    if (player.hasPotionEffect(org.bukkit.potion.PotionEffectType.HASTE)) {
                        org.bukkit.potion.PotionEffect haste = player.getPotionEffect(org.bukkit.potion.PotionEffectType.HASTE);
                        if (haste != null && haste.getAmplifier() == 4) {
                            player.removePotionEffect(org.bukkit.potion.PotionEffectType.HASTE);
                        }
                    }
                }
            }
        }, 20L, 20L); // Check every second
        
        getLogger().info("PandoraCavern v" + getDescription().getVersion() + " has been enabled!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Saving all data before shutdown...");
        
        // Save all block locations
        if (dataManager != null) {
            dataManager.saveBlocks();
        }
        
        // Stop respawn task
        if (respawnManager != null) {
            respawnManager.stopRespawnTask();
        }
        
        getLogger().info("All data saved! PandoraCavern has been disabled!");
    }
    
    public static PandoraCavern getInstance() {
        return instance;
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    public BlockManager getBlockManager() {
        return blockManager;
    }
    
    public RespawnManager getRespawnManager() {
        return respawnManager;
    }
    
    public String formatMessage(String message) {
        String prefix = getConfig().getString("visuals.prefix", "&e&lPandora &8» &r");
        message = message.replace("{prefix}", prefix);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public String formatMessage(String message, String... replacements) {
        String prefix = getConfig().getString("visuals.prefix", "&e&lPandora &8» &r");
        message = message.replace("{prefix}", prefix);
        
        // Replace all placeholders
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String placeholder = "{" + replacements[i] + "}";
                String value = replacements[i + 1];
                message = message.replace(placeholder, value);
            }
        }
        
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
}

