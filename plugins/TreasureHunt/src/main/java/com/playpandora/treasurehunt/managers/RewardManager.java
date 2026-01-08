package com.playpandora.treasurehunt.managers;

import com.playpandora.treasurehunt.TreasureHunt;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RewardManager {
    
    private final TreasureHunt plugin;
    private Economy economy;
    
    public RewardManager(TreasureHunt plugin) {
        this.plugin = plugin;
        setupEconomy();
    }
    
    private void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found! Money rewards disabled.");
            return;
        }
        
        org.bukkit.plugin.RegisteredServiceProvider<Economy> rsp = 
            plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
        }
    }
    
    public void giveTreasureReward(Player player) {
        double multiplier = plugin.getConfig().getDouble("reward-multiplier", 2.0);
        
        // Give money (x2)
        ConfigurationSection rewardsSection = plugin.getConfig().getConfigurationSection("rewards.treasure");
        if (rewardsSection != null) {
            double money = rewardsSection.getDouble("money", 5000.0) * multiplier;
            if (economy != null && money > 0) {
                economy.depositPlayer(player, money);
            }
            
            // Give items
            if (rewardsSection.contains("items")) {
                for (String itemString : rewardsSection.getStringList("items")) {
                    String[] parts = itemString.split(":");
                    if (parts.length == 2) {
                        try {
                            Material material = Material.valueOf(parts[0].toUpperCase());
                            int amount = Integer.parseInt(parts[1]);
                            // Apply multiplier to amount
                            amount = (int) (amount * multiplier);
                            ItemStack item = new ItemStack(material, amount);
                            player.getInventory().addItem(item);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Invalid item in config: " + itemString);
                        }
                    }
                }
            }
        }
        
        // Give levels
        if (plugin.getConfig().getBoolean("levels.enabled", true)) {
            int levels = plugin.getConfig().getInt("levels.treasure-claim", 50);
            plugin.getIntegrationManager().giveLevels(player, levels);
        }
        
        // Give XP
        if (plugin.getConfig().getBoolean("xp.enabled", true)) {
            double xp = plugin.getConfig().getDouble("xp.treasure-claim", 1000.0) * multiplier;
            plugin.getIntegrationManager().giveXP(player, xp);
        }
        
        // Send notification
        plugin.getIntegrationManager().sendTreasureFound(player, "Treasure");
        
        // Send message
        String message = plugin.getConfig().getString("messages.treasure-claimed", 
            "{prefix} &7You claimed the treasure! &6+%levels% &7levels and &6$%money%");
        int levels = plugin.getConfig().getInt("levels.treasure-claim", 50);
        double moneyReward = rewardsSection != null ? rewardsSection.getDouble("money", 5000.0) * multiplier : 0;
        message = message.replace("%levels%", String.valueOf(levels))
                        .replace("%money%", String.format("%.2f", moneyReward));
        player.sendMessage(plugin.formatMessage(message));
    }
    
    public void giveHintReward(Player player) {
        double multiplier = plugin.getConfig().getDouble("reward-multiplier", 2.0);
        
        // Give money (x2)
        ConfigurationSection rewardsSection = plugin.getConfig().getConfigurationSection("rewards.hint");
        if (rewardsSection != null) {
            double money = rewardsSection.getDouble("money", 500.0) * multiplier;
            if (economy != null && money > 0) {
                economy.depositPlayer(player, money);
            }
            
            // Give items
            if (rewardsSection.contains("items")) {
                for (String itemString : rewardsSection.getStringList("items")) {
                    String[] parts = itemString.split(":");
                    if (parts.length == 2) {
                        try {
                            Material material = Material.valueOf(parts[0].toUpperCase());
                            int amount = Integer.parseInt(parts[1]);
                            // Apply multiplier to amount
                            amount = (int) (amount * multiplier);
                            ItemStack item = new ItemStack(material, amount);
                            player.getInventory().addItem(item);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Invalid item in config: " + itemString);
                        }
                    }
                }
            }
        }
        
        // Give levels
        if (plugin.getConfig().getBoolean("levels.enabled", true)) {
            int levels = plugin.getConfig().getInt("levels.hint-claim", 10);
            plugin.getIntegrationManager().giveLevels(player, levels);
        }
        
        // Give XP
        if (plugin.getConfig().getBoolean("xp.enabled", true)) {
            double xp = plugin.getConfig().getDouble("xp.hint-claim", 200.0) * multiplier;
            plugin.getIntegrationManager().giveXP(player, xp);
        }
        
        // Get hints remaining
        int hintsRemaining = plugin.getTreasureManager().getHintsRemaining(player);
        
        // Send notification
        plugin.getIntegrationManager().sendHintClaimed(player, hintsRemaining);
        
        // Send message with rewards
        double moneyReward = rewardsSection != null ? rewardsSection.getDouble("money", 500.0) * multiplier : 0;
        double xpReward = plugin.getConfig().getBoolean("xp.enabled", true) ? 
            plugin.getConfig().getDouble("xp.hint-claim", 200.0) * multiplier : 0;
        int levelReward = plugin.getConfig().getBoolean("levels.enabled", true) ? 
            plugin.getConfig().getInt("levels.hint-claim", 10) : 0;
        
        String message = plugin.getConfig().getString("messages.hint-claimed", 
            "{prefix} &7You claimed a hint! &6+%xp% XP &7| &6$%money% &7| &6+%levels% levels &7| &6%hints% &7hints remaining.");
        message = message.replace("%hints%", String.valueOf(hintsRemaining))
                        .replace("%money%", String.format("%.2f", moneyReward))
                        .replace("%xp%", String.format("%.1f", xpReward))
                        .replace("%levels%", String.valueOf(levelReward));
        player.sendMessage(plugin.formatMessage(message));
    }
}




