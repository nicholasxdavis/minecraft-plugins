package com.playpandora.pandoracrates.managers;

import com.playpandora.pandoracrates.PandoraCrates;
import com.playpandora.pandoracrates.models.Crate;
import com.playpandora.pandoracrates.models.RarityRewards;
import com.playpandora.pandoracrates.models.Reward;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class RewardManager {
    
    private final PandoraCrates plugin;
    private final Random random;
    
    public RewardManager(PandoraCrates plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }
    
    public Reward selectReward(Crate crate) {
        // Select rarity based on chances
        String rarity = selectRarity(crate);
        if (rarity == null) {
            return null;
        }
        
        RarityRewards rarityRewards = crate.getRarityRewards(rarity);
        if (rarityRewards == null || rarityRewards.getRewards().isEmpty()) {
            return null;
        }
        
        // Select random reward from rarity pool
        List<Reward> rewards = rarityRewards.getRewards();
        return rewards.get(random.nextInt(rewards.size()));
    }
    
    private String selectRarity(Crate crate) {
        Map<String, RarityRewards> rewards = crate.getRewards();
        if (rewards.isEmpty()) {
            return null;
        }
        
        // Calculate total chance
        int totalChance = 0;
        for (RarityRewards rarityRewards : rewards.values()) {
            totalChance += rarityRewards.getChance();
        }
        
        if (totalChance == 0) {
            return null;
        }
        
        // Select random number
        int roll = random.nextInt(totalChance);
        int current = 0;
        
        // Find which rarity was rolled
        for (Map.Entry<String, RarityRewards> entry : rewards.entrySet()) {
            current += entry.getValue().getChance();
            if (roll < current) {
                return entry.getKey();
            }
        }
        
        // Fallback to first rarity
        return rewards.keySet().iterator().next();
    }
    
    private Crate currentCrate; // Store crate context for broadcasts
    
    public void giveReward(Player player, Reward reward, String rarity, Crate crate) {
        this.currentCrate = crate;
        giveReward(player, reward, rarity);
    }
    
    public void giveReward(Player player, Reward reward, String rarity) {
        switch (reward.getType()) {
            case ITEM:
                giveItemReward(player, reward);
                break;
            case MONEY:
                // Use banknotes plugin for money rewards
                giveBanknoteReward(player, reward);
                break;
            case LEVEL:
                // Level rewards removed - skip
                break;
            case COMMAND:
                executeCommandReward(player, reward);
                break;
            case PANDORA_ITEM_CANNON:
            case PANDORA_ITEM_FARM:
            case PANDORA_ITEM_PERK:
            case PANDORA_ITEM_PET:
            case PANDORA_ITEM_KIT:
                givePandoraItemReward(player, reward);
                break;
            case PERK:
                givePerkReward(player, reward);
                break;
            case PET:
                givePetReward(player, reward);
                break;
            case KIT:
                giveKitReward(player, reward);
                break;
            case BANKNOTE:
                giveBanknoteReward(player, reward);
                break;
            case SPAWNER:
                giveSpawnerReward(player, reward);
                break;
            case GODSET:
                giveGodsetReward(player, reward);
                break;
            case CREEPER_EGG:
                giveCreeperEggReward(player, reward);
                break;
            case ENCHANTED_GEAR:
                giveEnchantedGearReward(player, reward);
                break;
            case CRATE_KEY:
                giveCrateKeyReward(player, reward);
                break;
        }
        
        // Broadcast if rare+
        if (shouldBroadcast(rarity)) {
            broadcastReward(player, reward, rarity);
        }
    }
    
    private void giveItemReward(Player player, Reward reward) {
        ItemStack item = new ItemStack(reward.getMaterial(), reward.getAmount());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (reward.getName() != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', reward.getName()));
            }
            // Add lore to items
            List<String> lore = new ArrayList<>();
            if (reward.getLore() != null && !reward.getLore().isEmpty()) {
                for (String line : reward.getLore()) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
            }
            // Add default lore if none exists
            if (lore.isEmpty()) {
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Crate Reward"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7Type: &eItem"));
            }
            meta.setLore(lore);
            if (reward.getEnchantments() != null && !reward.getEnchantments().isEmpty()) {
                for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : reward.getEnchantments().entrySet()) {
                    Enchantment enchant = entry.getKey();
                    int level = entry.getValue();
                    // Apply enchantments (force allow unsafe enchantments)
                    if (enchant != null && level > 0) {
                        try {
                            // Use unsafe enchantment to allow any enchantment on any item
                            meta.addEnchant(enchant, level, true);
                        } catch (Exception e) {
                            // Try without unsafe flag if that fails
                            try {
                                if (enchant.canEnchantItem(item)) {
                                    meta.addEnchant(enchant, level, false);
                                }
                            } catch (Exception e2) {
                                plugin.getLogger().warning("Could not apply enchantment " + enchant.getKey() + " to " + item.getType() + ": " + e2.getMessage());
                            }
                        }
                    }
                }
            }
            item.setItemMeta(meta);
        }
        
        // Give item to player (like ExcellentCrates)
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        if (!overflow.isEmpty()) {
            // Drop overflow items at player location
            for (ItemStack overflowItem : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), overflowItem);
            }
            player.sendMessage(plugin.formatRawMessage(
                "{prefix} &eYour inventory was full! Some items were dropped on the ground."));
        }
        
        // Log reward given
        plugin.getLogger().info("Gave item reward to " + player.getName() + ": " + 
            (reward.getName() != null ? reward.getName() : reward.getMaterial().name()));
    }
    
    private void giveMoneyReward(Player player, Reward reward) {
        // Use banknotes plugin for money rewards instead of direct economy
        plugin.getIntegrationManager().giveBanknote(player, reward.getMoneyAmount());
    }
    
    private void giveLevelReward(Player player, Reward reward) {
        plugin.getIntegrationManager().giveLevels(player, reward.getAmount());
    }
    
    private void executeCommandReward(Player player, Reward reward) {
        if (reward.getCommand() != null && !reward.getCommand().isEmpty()) {
            String command = reward.getCommand()
                .replace("{player}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString());
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
        }
    }
    
    private void givePandoraItemReward(Player player, Reward reward) {
        if (reward.getItemKey() == null || reward.getItemKey().isEmpty()) {
            return;
        }
        
        String type = "";
        switch (reward.getType()) {
            case PANDORA_ITEM_CANNON:
                type = "cannon";
                break;
            case PANDORA_ITEM_FARM:
                type = "farm";
                break;
            case PANDORA_ITEM_PERK:
                type = "perk";
                break;
            case PANDORA_ITEM_PET:
                type = "pet";
                break;
            case PANDORA_ITEM_KIT:
                type = "kit";
                break;
        }
        
        plugin.getIntegrationManager().givePandoraItem(player, type, reward.getItemKey());
    }
    
    private void givePerkReward(Player player, Reward reward) {
        if (reward.getItemKey() != null && !reward.getItemKey().isEmpty()) {
            plugin.getIntegrationManager().givePerk(player, reward.getItemKey());
        }
    }
    
    private void givePetReward(Player player, Reward reward) {
        if (reward.getItemKey() != null && !reward.getItemKey().isEmpty()) {
            plugin.getIntegrationManager().givePet(player, reward.getItemKey());
        }
    }
    
    private void giveKitReward(Player player, Reward reward) {
        if (reward.getItemKey() != null && !reward.getItemKey().isEmpty()) {
            plugin.getIntegrationManager().giveKit(player, reward.getItemKey());
        }
    }
    
    private void giveBanknoteReward(Player player, Reward reward) {
        plugin.getIntegrationManager().giveBanknote(player, reward.getMoneyAmount());
    }
    
    private void giveSpawnerReward(Player player, Reward reward) {
        if (reward.getItemKey() == null || reward.getItemKey().isEmpty()) {
            plugin.getLogger().warning("Spawner reward missing item-key (spawner type)");
            return;
        }
        plugin.getIntegrationManager().giveSpawner(player, reward.getItemKey(), reward.getAmount());
    }
    
    private void giveGodsetReward(Player player, Reward reward) {
        if (reward.getItemKey() == null || reward.getItemKey().isEmpty()) {
            plugin.getLogger().warning("Godset reward missing item-key (tier: iron, diamond, or netherite)");
            return;
        }
        plugin.getIntegrationManager().giveGodset(player, reward.getItemKey());
    }
    
    private void giveCreeperEggReward(Player player, Reward reward) {
        plugin.getIntegrationManager().giveCreeperEgg(player, reward.getAmount());
    }
    
    private void giveEnchantedGearReward(Player player, Reward reward) {
        if (reward.getItemKey() == null || reward.getItemKey().isEmpty()) {
            plugin.getLogger().warning("Enchanted gear reward missing gear-type");
            return;
        }
        plugin.getIntegrationManager().giveEnchantedGear(player, reward.getItemKey(), reward.getEnchantments());
    }
    
    private void giveCrateKeyReward(Player player, Reward reward) {
        if (reward.getItemKey() == null || reward.getItemKey().isEmpty()) {
            plugin.getLogger().warning("Crate key reward missing item-key (crate-id)");
            return;
        }
        plugin.getKeyManager().giveKey(player, reward.getItemKey(), reward.getAmount());
    }
    
    private boolean shouldBroadcast(String rarity) {
        ConfigurationSection broadcastSection = plugin.getConfig().getConfigurationSection("broadcast");
        if (broadcastSection == null || !broadcastSection.getBoolean("enabled", true)) {
            return false;
        }
        
        List<String> rarities = broadcastSection.getStringList("rarities");
        return rarities.contains(rarity.toLowerCase());
    }
    
    private void broadcastReward(Player player, Reward reward, String rarity) {
        String message = plugin.getConfig().getString("broadcast.message", 
            "{prefix} &6{player} &7opened a &6{crate} &7and won &6{reward}&7!");
        
        String rewardName = getRewardDisplayName(reward);
        String rarityColor = getRarityColorCode(rarity);
        
        // Get crate name from context
        String crateName = currentCrate != null ? currentCrate.getDisplayName() : "crate";
        
        message = plugin.formatRawMessage(message, 
            "player", player.getName(),
            "reward", rarityColor + rewardName,
            "rarity", rarity.toUpperCase(),
            "crate", crateName);
        
        // Send to all players
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            p.sendMessage(message);
        }
    }
    
    private String getRarityColorCode(String rarity) {
        // Use only &7, &e, &6 colors
        switch (rarity.toLowerCase()) {
            case "common": return "&7";
            case "uncommon": return "&7";
            case "rare": return "&e";
            case "epic": return "&e";
            case "legendary": return "&6";
            default: return "&7";
        }
    }
    
    public String getRewardDisplayName(Reward reward) {
        switch (reward.getType()) {
            case ITEM:
                return reward.getName() != null ? reward.getName() : reward.getMaterial().name();
            case MONEY:
                // MONEY rewards now use banknotes
                return "$" + reward.getMoneyAmount() + " Banknote";
            case BANKNOTE:
                return "$" + reward.getMoneyAmount() + " Banknote";
            case LEVEL:
                return reward.getAmount() + " Levels";
            case PANDORA_ITEM_CANNON:
            case PANDORA_ITEM_FARM:
            case PANDORA_ITEM_PERK:
            case PANDORA_ITEM_PET:
            case PANDORA_ITEM_KIT:
                return reward.getItemKey();
            case PERK:
                return reward.getItemKey() + " Perk";
            case PET:
                return reward.getItemKey() + " Pet";
            case KIT:
                return reward.getItemKey() + " Kit";
            case SPAWNER:
                return reward.getItemKey() + " Spawner";
            case GODSET:
                return reward.getItemKey().toUpperCase() + " Godset";
            case CREEPER_EGG:
                return reward.getAmount() + " Creeper Egg(s)";
            case ENCHANTED_GEAR:
                return reward.getItemKey().replace("_", " ").toUpperCase();
            case CRATE_KEY:
                Crate keyCrate = plugin.getCrateManager().getCrate(reward.getItemKey());
                if (keyCrate != null) {
                    return reward.getAmount() + "x " + keyCrate.getDisplayName() + " Key";
                }
                return reward.getAmount() + "x " + reward.getItemKey() + " Key";
            default:
                return "Reward";
        }
    }
}

