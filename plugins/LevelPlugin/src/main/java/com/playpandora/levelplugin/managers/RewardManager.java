package com.playpandora.levelplugin.managers;

import com.playpandora.levelplugin.LevelPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

public class RewardManager {
    
    private final LevelPlugin plugin;
    
    public RewardManager(LevelPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void giveLevelReward(UUID uuid, int level) {
        if (!plugin.getConfig().getBoolean("rewards.enabled", true)) {
            return;
        }
        
        ConfigurationSection rewardsSection = plugin.getConfig().getConfigurationSection("rewards.levels");
        if (rewardsSection == null) {
            return;
        }
        
        String levelKey = String.valueOf(level);
        if (!rewardsSection.contains(levelKey)) {
            return;
        }
        
        ConfigurationSection levelReward = rewardsSection.getConfigurationSection(levelKey);
        if (levelReward == null) {
            return;
        }
        
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return;
        }
        
        // Check if already rewarded (prevent duplicate rewards on reload)
        // Use DataManager to check and save rewarded levels
        if (plugin.getDataManager().isLevelRewarded(uuid, level)) {
            return;
        }
        plugin.getDataManager().markLevelRewarded(uuid, level);
        
        // Give money reward
        if (levelReward.contains("money")) {
            double money = levelReward.getDouble("money", 0.0);
            if (money > 0 && plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
                try {
                    Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
                    var rsp = plugin.getServer().getServicesManager().getRegistration(economyClass);
                    if (rsp != null) {
                        Object economy = rsp.getProvider();
                        Method depositMethod = economy.getClass().getMethod("depositPlayer", 
                            org.bukkit.OfflinePlayer.class, double.class);
                        depositMethod.invoke(economy, player, money);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to give money reward: " + e.getMessage());
                }
            }
        }
        
        // Send message
        String message = levelReward.getString("message", "");
        if (!message.isEmpty()) {
            String prefix = "&e&lPandora &8» &r";
            message = message.replace("{prefix}", "");
            // Ensure message uses correct color scheme
            message = message.replace("&a", "&7").replace("&f", "&7").replace("&b", "&7");
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix + message));
        }
    }
}

