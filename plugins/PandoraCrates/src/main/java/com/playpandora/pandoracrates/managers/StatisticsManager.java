package com.playpandora.pandoracrates.managers;

import com.playpandora.pandoracrates.PandoraCrates;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatisticsManager {
    
    private final PandoraCrates plugin;
    private FileConfiguration statsConfig;
    private File statsFile;
    
    public StatisticsManager(PandoraCrates plugin) {
        this.plugin = plugin;
        loadStats();
    }
    
    private void loadStats() {
        statsFile = new File(plugin.getDataFolder(), "statistics.yml");
        if (!statsFile.exists()) {
            try {
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create statistics.yml: " + e.getMessage());
            }
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
    }
    
    public void saveStats() {
        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save statistics.yml: " + e.getMessage());
        }
    }
    
    public void recordCrateOpen(UUID playerUUID, String crateId, String rarity) {
        String path = "players." + playerUUID.toString() + ".crates." + crateId;
        int opens = statsConfig.getInt(path + ".opens", 0);
        statsConfig.set(path + ".opens", opens + 1);
        
        String rarityPath = path + ".rarities." + rarity;
        int rarityCount = statsConfig.getInt(rarityPath, 0);
        statsConfig.set(rarityPath, rarityCount + 1);
        
        // Total opens
        int totalOpens = statsConfig.getInt("players." + playerUUID.toString() + ".total_opens", 0);
        statsConfig.set("players." + playerUUID.toString() + ".total_opens", totalOpens + 1);
        
        // Don't save immediately - let auto-save handle it
    }
    
    public int getCrateOpens(UUID playerUUID, String crateId) {
        return statsConfig.getInt("players." + playerUUID.toString() + ".crates." + crateId + ".opens", 0);
    }
    
    public int getTotalOpens(UUID playerUUID) {
        return statsConfig.getInt("players." + playerUUID.toString() + ".total_opens", 0);
    }
    
    public Map<String, Integer> getRarityStats(UUID playerUUID, String crateId) {
        Map<String, Integer> stats = new HashMap<>();
        String path = "players." + playerUUID.toString() + ".crates." + crateId + ".rarities";
        
        if (statsConfig.contains(path)) {
            for (String rarity : statsConfig.getConfigurationSection(path).getKeys(false)) {
                stats.put(rarity, statsConfig.getInt(path + "." + rarity, 0));
            }
        }
        
        return stats;
    }
}




