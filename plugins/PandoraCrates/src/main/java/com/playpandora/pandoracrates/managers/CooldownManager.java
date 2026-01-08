package com.playpandora.pandoracrates.managers;

import com.playpandora.pandoracrates.PandoraCrates;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CooldownManager {
    
    private final PandoraCrates plugin;
    private final Map<UUID, Long> cooldowns;
    private File cooldownFile;
    private FileConfiguration cooldownConfig;
    
    public CooldownManager(PandoraCrates plugin) {
        this.plugin = plugin;
        this.cooldowns = new HashMap<>();
        setupCooldownFile();
        loadCooldowns();
        
        // Clean up cooldowns every second
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            cooldowns.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
        }, 0L, 20L);
    }
    
    private void setupCooldownFile() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        cooldownFile = new File(plugin.getDataFolder(), "cooldowns.yml");
        
        if (!cooldownFile.exists()) {
            try {
                cooldownFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create cooldowns.yml: " + e.getMessage());
            }
        }
        
        cooldownConfig = YamlConfiguration.loadConfiguration(cooldownFile);
    }
    
    private void loadCooldowns() {
        if (cooldownConfig.getConfigurationSection("cooldowns") == null) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        Set<String> keys = cooldownConfig.getConfigurationSection("cooldowns").getKeys(false);
        
        for (String uuidStr : keys) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                long cooldownEnd = cooldownConfig.getLong("cooldowns." + uuidStr, 0);
                
                // Only load cooldowns that haven't expired yet
                if (cooldownEnd > currentTime) {
                    cooldowns.put(uuid, cooldownEnd);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in cooldowns.yml: " + uuidStr);
            }
        }
        
        plugin.getLogger().info("Loaded " + cooldowns.size() + " active cooldown(s)");
    }
    
    public void saveCooldowns() {
        try {
            // Clear existing cooldowns section
            cooldownConfig.set("cooldowns", null);
            
            // Save only active cooldowns
            long currentTime = System.currentTimeMillis();
            for (Map.Entry<UUID, Long> entry : cooldowns.entrySet()) {
                if (entry.getValue() > currentTime) {
                    cooldownConfig.set("cooldowns." + entry.getKey().toString(), entry.getValue());
                }
            }
            
            cooldownConfig.save(cooldownFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save cooldowns.yml: " + e.getMessage());
        }
    }
    
    public void setCooldown(UUID uuid, long milliseconds) {
        cooldowns.put(uuid, System.currentTimeMillis() + milliseconds);
    }
    
    public boolean isOnCooldown(UUID uuid) {
        Long cooldownEnd = cooldowns.get(uuid);
        return cooldownEnd != null && cooldownEnd > System.currentTimeMillis();
    }
    
    public long getRemainingCooldown(UUID uuid) {
        Long cooldownEnd = cooldowns.get(uuid);
        if (cooldownEnd == null) {
            return 0;
        }
        return Math.max(0, cooldownEnd - System.currentTimeMillis());
    }
    
    public void removeCooldown(UUID uuid) {
        cooldowns.remove(uuid);
    }
}




