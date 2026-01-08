package com.playpandora.welcome.managers;

import com.playpandora.welcome.Welcome;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class TipsManager {
    
    private final Welcome plugin;
    private final Set<UUID> tipsDisabled;
    private final Map<UUID, BukkitTask> activeTasks;
    
    public TipsManager(Welcome plugin) {
        this.plugin = plugin;
        this.tipsDisabled = new HashSet<>();
        this.activeTasks = new HashMap<>();
        
        // Load disabled tips from config
        loadDisabledTips();
    }
    
    public boolean toggleTips(Player player) {
        UUID uuid = player.getUniqueId();
        
        if (tipsDisabled.contains(uuid)) {
            tipsDisabled.remove(uuid);
            // Start tips task if not already running
            if (!activeTasks.containsKey(uuid)) {
                startTipsTask(player);
            }
            saveDisabledTips();
            return true;
        } else {
            tipsDisabled.add(uuid);
            // Cancel tips task
            BukkitTask task = activeTasks.remove(uuid);
            if (task != null) {
                task.cancel();
            }
            saveDisabledTips();
            return false;
        }
    }
    
    public boolean areTipsEnabled(Player player) {
        return !tipsDisabled.contains(player.getUniqueId());
    }
    
    public void startTipsTask(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Don't start if tips are disabled for this player
        if (tipsDisabled.contains(uuid)) {
            return;
        }
        
        // Cancel existing task if any
        BukkitTask existingTask = activeTasks.get(uuid);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Don't start if tips are disabled globally
        if (!plugin.getConfig().getBoolean("tips.enabled", true)) {
            return;
        }
        
        List<String> tips = plugin.getConfig().getStringList("tips.tip-messages");
        if (tips.isEmpty()) {
            return;
        }
        
        int delay = plugin.getConfig().getInt("tips.tip-delay", 120) * 20; // Convert to ticks
        
        // Create a random tip selector
        Random random = new Random();
        
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                // Get player from server (handles reconnects)
                Player onlinePlayer = plugin.getServer().getPlayer(uuid);
                
                // Check if player is online and tips are still enabled
                if (onlinePlayer == null || !onlinePlayer.isOnline() || tipsDisabled.contains(uuid)) {
                    activeTasks.remove(uuid);
                    return;
                }
                
                // Send a random tip
                String tip = tips.get(random.nextInt(tips.size()));
                onlinePlayer.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', tip));
            }
        }, delay, delay); // Start after delay, repeat every delay
        
        activeTasks.put(uuid, task);
    }
    
    public void cancelPlayerTask(java.util.UUID uuid) {
        BukkitTask task = activeTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }
    
    public void cancelAllTasks() {
        for (BukkitTask task : activeTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        activeTasks.clear();
    }
    
    private void loadDisabledTips() {
        List<String> disabled = plugin.getConfig().getStringList("tips.disabled-players");
        for (String uuidString : disabled) {
            try {
                tipsDisabled.add(UUID.fromString(uuidString));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in disabled-players: " + uuidString);
            }
        }
    }
    
    private void saveDisabledTips() {
        List<String> disabled = new ArrayList<>();
        for (UUID uuid : tipsDisabled) {
            disabled.add(uuid.toString());
        }
        plugin.getConfig().set("tips.disabled-players", disabled);
        plugin.saveConfig();
    }
}

