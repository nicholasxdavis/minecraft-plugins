package com.playpandora.pandoracrates.models;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Crate {
    
    private final String id;
    private String name;
    private String displayName;
    private String keyName;
    private Material keyMaterial;
    private Material blockMaterial;
    private List<String> hologramLines;
    private final Map<String, RarityRewards> rewards;
    
    public Crate(String id) {
        this.id = id;
        this.rewards = new HashMap<>();
        this.hologramLines = new ArrayList<>();
    }
    
    public void loadFromConfig(ConfigurationSection config) {
        this.name = config.getString("name", id);
        this.displayName = config.getString("display-name", name);
        this.keyName = config.getString("key-name", name + " Key");
        
        String keyMaterialStr = config.getString("key-material", "TRIPWIRE_HOOK");
        try {
            this.keyMaterial = Material.valueOf(keyMaterialStr);
        } catch (IllegalArgumentException e) {
            this.keyMaterial = Material.TRIPWIRE_HOOK;
        }
        
        String blockMaterialStr = config.getString("block-material", "CHEST");
        try {
            this.blockMaterial = Material.valueOf(blockMaterialStr);
        } catch (IllegalArgumentException e) {
            this.blockMaterial = Material.CHEST;
        }
        
        this.hologramLines = config.getStringList("hologram");
        if (this.hologramLines.isEmpty()) {
            this.hologramLines.add(displayName);
            this.hologramLines.add("&7Right-click with a key to open");
            this.hologramLines.add("&7Left-click to preview rewards");
            this.hologramLines.add("&7Visit &e/crates &7to view all crates!");
        }
        
        // Load rewards
        ConfigurationSection rewardsSection = config.getConfigurationSection("rewards");
        if (rewardsSection != null) {
            for (String rarity : rewardsSection.getKeys(false)) {
                ConfigurationSection raritySection = rewardsSection.getConfigurationSection(rarity);
                if (raritySection != null) {
                    RarityRewards rarityRewards = new RarityRewards(rarity, raritySection);
                    rewards.put(rarity.toLowerCase(), rarityRewards);
                }
            }
        }
        
        // HARDCODE: Add spawners, creeper eggs, keys, and godsets to all crates
        // Ensure each crate has at least 28 rewards total
        int beforeCount = getTotalRewardCount();
        addHardcodedRewards();
        int afterHardcoded = getTotalRewardCount();
        ensureMinimumRewards(28);
        int finalCount = getTotalRewardCount();
        
        // Log reward counts
        System.out.println("[PandoraCrates] Crate '" + id + "' rewards loaded:");
        System.out.println("  Before hardcoded: " + beforeCount);
        System.out.println("  After hardcoded: " + afterHardcoded);
        System.out.println("  Final count: " + finalCount);
        for (Map.Entry<String, RarityRewards> entry : rewards.entrySet()) {
            System.out.println("    " + entry.getKey() + ": " + entry.getValue().getRewards().size() + " rewards");
        }
    }
    
    private int getTotalRewardCount() {
        int total = 0;
        for (RarityRewards rarityRewards : rewards.values()) {
            total += rarityRewards.getRewards().size();
        }
        return total;
    }
    
    private void ensureMinimumRewards(int minimum) {
        int totalRewards = getTotalRewardCount();
        
        if (totalRewards < minimum) {
            int needed = minimum - totalRewards;
            System.out.println("[PandoraCrates] Crate '" + id + "' needs " + needed + " more rewards to reach " + minimum);
            
            // Add remaining rewards to rare tier
            RarityRewards rareRewards = rewards.get("rare");
            if (rareRewards == null) {
                // Create rare tier if it doesn't exist
                rareRewards = new RarityRewards("rare", null);
                rewards.put("rare", rareRewards);
                System.out.println("[PandoraCrates] Created 'rare' tier for crate '" + id + "'");
            }
            
            // Add spawners to fill remaining slots
            String[] spawnerTypes = {"ZOMBIE", "SKELETON", "SPIDER", "CREEPER", "CAVE_SPIDER", "BLAZE", "ENDERMAN", "WITCH", "MAGMA_CUBE", "GHAST"};
            int addedSpawners = 0;
            for (int i = 0; i < needed && i < spawnerTypes.length; i++) {
                rareRewards.getRewards().add(createSpawnerReward(spawnerTypes[i], 1));
                addedSpawners++;
            }
            
            // If still need more, add creeper eggs
            int remaining = needed - addedSpawners;
            for (int i = 0; i < remaining; i++) {
                rareRewards.getRewards().add(createCreeperEggReward(1));
            }
            
            System.out.println("[PandoraCrates] Added " + addedSpawners + " spawners and " + remaining + " creeper eggs to crate '" + id + "'");
        } else {
            System.out.println("[PandoraCrates] Crate '" + id + "' already has " + totalRewards + " rewards (minimum: " + minimum + ")");
        }
    }
    
    private void addHardcodedRewards() {
        // Get all rarity tiers
        RarityRewards commonRewards = rewards.get("common");
        RarityRewards uncommonRewards = rewards.get("uncommon");
        RarityRewards rareRewards = rewards.get("rare");
        RarityRewards epicRewards = rewards.get("epic");
        RarityRewards legendaryRewards = rewards.get("legendary");
        
        int addedCount = 0;
        
        // COMMON TIER - Add creeper eggs
        if (commonRewards != null) {
            commonRewards.getRewards().add(createCreeperEggReward(1));
            commonRewards.getRewards().add(createCreeperEggReward(2));
            addedCount += 2;
        }
        
        // UNCOMMON TIER - Add creeper eggs and spawners
        if (uncommonRewards != null) {
            uncommonRewards.getRewards().add(createCreeperEggReward(2));
            uncommonRewards.getRewards().add(createCreeperEggReward(3));
            uncommonRewards.getRewards().add(createSpawnerReward("ZOMBIE", 1));
            uncommonRewards.getRewards().add(createSpawnerReward("SKELETON", 1));
            addedCount += 4;
        }
        
        // RARE TIER - Add spawners, creeper eggs, keys, and godsets
        if (rareRewards != null) {
            // Spawners
            rareRewards.getRewards().add(createSpawnerReward("ZOMBIE", 1));
            rareRewards.getRewards().add(createSpawnerReward("SKELETON", 1));
            rareRewards.getRewards().add(createSpawnerReward("SPIDER", 1));
            rareRewards.getRewards().add(createSpawnerReward("CREEPER", 1));
            rareRewards.getRewards().add(createSpawnerReward("CAVE_SPIDER", 1));
            
            // Creeper eggs
            rareRewards.getRewards().add(createCreeperEggReward(3));
            rareRewards.getRewards().add(createCreeperEggReward(5));
            
            // Keys
            rareRewards.getRewards().add(createCrateKeyReward(this.id, 1));
            rareRewards.getRewards().add(createCrateKeyReward("default", 1));
            
            // Godsets
            rareRewards.getRewards().add(createGodsetReward("iron"));
            addedCount += 10;
        }
        
        // EPIC TIER - Add spawners, keys, and godsets
        if (epicRewards != null) {
            // Spawners
            epicRewards.getRewards().add(createSpawnerReward("CREEPER", 1));
            epicRewards.getRewards().add(createSpawnerReward("SPIDER", 1));
            epicRewards.getRewards().add(createSpawnerReward("BLAZE", 1));
            epicRewards.getRewards().add(createSpawnerReward("ENDERMAN", 1));
            epicRewards.getRewards().add(createSpawnerReward("WITCH", 1));
            
            // Creeper eggs
            epicRewards.getRewards().add(createCreeperEggReward(5));
            epicRewards.getRewards().add(createCreeperEggReward(10));
            
            // Keys
            epicRewards.getRewards().add(createCrateKeyReward(this.id, 1));
            epicRewards.getRewards().add(createCrateKeyReward(this.id, 2));
            epicRewards.getRewards().add(createCrateKeyReward("default", 1));
            
            // Godsets
            epicRewards.getRewards().add(createGodsetReward("iron"));
            epicRewards.getRewards().add(createGodsetReward("diamond"));
            addedCount += 12;
        }
        
        // LEGENDARY TIER - Add spawners, keys, and godsets
        if (legendaryRewards != null) {
            // Spawners
            legendaryRewards.getRewards().add(createSpawnerReward("ENDERMAN", 1));
            legendaryRewards.getRewards().add(createSpawnerReward("BLAZE", 1));
            legendaryRewards.getRewards().add(createSpawnerReward("WITHER_SKELETON", 1));
            legendaryRewards.getRewards().add(createSpawnerReward("GHAST", 1));
            legendaryRewards.getRewards().add(createSpawnerReward("MAGMA_CUBE", 1));
            
            // Creeper eggs
            legendaryRewards.getRewards().add(createCreeperEggReward(10));
            legendaryRewards.getRewards().add(createCreeperEggReward(15));
            
            // Keys
            legendaryRewards.getRewards().add(createCrateKeyReward(this.id, 2));
            legendaryRewards.getRewards().add(createCrateKeyReward(this.id, 3));
            
            // Godsets
            legendaryRewards.getRewards().add(createGodsetReward("diamond"));
            legendaryRewards.getRewards().add(createGodsetReward("netherite"));
            addedCount += 12;
        }
        
        System.out.println("[PandoraCrates] Added " + addedCount + " hardcoded rewards to crate '" + id + "'");
    }
    
    private Reward createSpawnerReward(String spawnerType, int amount) {
        return new Reward(Reward.RewardType.SPAWNER, spawnerType, amount);
    }
    
    private Reward createCreeperEggReward(int amount) {
        return new Reward(Reward.RewardType.CREEPER_EGG, amount);
    }
    
    private Reward createCrateKeyReward(String crateId, int amount) {
        return new Reward(Reward.RewardType.CRATE_KEY, crateId, amount);
    }
    
    private Reward createGodsetReward(String tier) {
        return new Reward(Reward.RewardType.GODSET, tier, 1);
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getKeyName() {
        return keyName;
    }
    
    public Material getKeyMaterial() {
        return keyMaterial;
    }
    
    public Material getBlockMaterial() {
        return blockMaterial;
    }
    
    public List<String> getHologramLines() {
        return hologramLines;
    }
    
    public Map<String, RarityRewards> getRewards() {
        return rewards;
    }
    
    public RarityRewards getRarityRewards(String rarity) {
        return rewards.get(rarity.toLowerCase());
    }
}

