package com.playpandora.homebuffs.managers;

import com.playpandora.homebuffs.HomeBuffs;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class BuffManager {
    
    private final HomeBuffs plugin;
    private final Set<UUID> buffsEnabled = new HashSet<>();
    private final Map<UUID, Boolean> nearHomeStatus = new HashMap<>();
    private BukkitTask buffCheckTask;
    
    public BuffManager(HomeBuffs plugin) {
        this.plugin = plugin;
    }
    
    public void startBuffChecking() {
        int interval = plugin.getConfig().getInt("buffs.check-interval", 20);
        
        buffCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!buffsEnabled.contains(player.getUniqueId())) {
                        // Remove buffs if disabled
                        if (nearHomeStatus.getOrDefault(player.getUniqueId(), false)) {
                            removeBuffs(player);
                            nearHomeStatus.put(player.getUniqueId(), false);
                        }
                        continue;
                    }
                    
                    boolean isNearHome = isNearHome(player);
                    boolean wasNearHome = nearHomeStatus.getOrDefault(player.getUniqueId(), false);
                    
                    if (isNearHome && !wasNearHome) {
                        // Just entered home range
                        applyBuffs(player);
                        nearHomeStatus.put(player.getUniqueId(), true);
                    } else if (!isNearHome && wasNearHome) {
                        // Just left home range
                        removeBuffs(player);
                        nearHomeStatus.put(player.getUniqueId(), false);
                    } else if (isNearHome) {
                        // Still near home, refresh buffs
                        applyBuffs(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, interval);
    }
    
    public void stopBuffChecking() {
        if (buffCheckTask != null) {
            buffCheckTask.cancel();
        }
        
        // Remove all buffs from online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeBuffs(player);
        }
    }
    
    private boolean isNearHome(Player player) {
        EssentialsHook hook = plugin.getEssentialsHook();
        if (!hook.isEssentialsAvailable()) {
            return false;
        }
        
        List<String> homes = hook.getHomes(player);
        if (homes == null || homes.isEmpty()) {
            return false;
        }
        
        Location playerLoc = player.getLocation();
        double distanceSquared = plugin.getConfig().getDouble("buffs.distance", 50.0);
        distanceSquared = distanceSquared * distanceSquared; // Square it for distance comparison
        
        // Check all homes
        for (String homeName : homes) {
            Location homeLoc = hook.getHomeLocation(player, homeName);
            if (homeLoc != null && homeLoc.getWorld() != null && 
                homeLoc.getWorld().equals(playerLoc.getWorld())) {
                double distSq = playerLoc.distanceSquared(homeLoc);
                if (distSq <= distanceSquared) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private void applyBuffs(Player player) {
        // Health Boost (3 extra hearts = level 1 = 4 hearts, but we want 3 hearts, so we'll use level 1 which gives 4 hearts, or we can use custom health)
        if (plugin.getConfig().getBoolean("buffs.health-boost.enabled", true)) {
            int level = plugin.getConfig().getInt("buffs.health-boost.level", 1);
            player.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, Integer.MAX_VALUE, level - 1, true, false, false));
        }
        
        // Jump Boost
        if (plugin.getConfig().getBoolean("buffs.jump-boost.enabled", true)) {
            int level = plugin.getConfig().getInt("buffs.jump-boost.level", 1);
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, level - 1, true, false, false));
        }
        
        // Speed
        if (plugin.getConfig().getBoolean("buffs.speed.enabled", true)) {
            int level = plugin.getConfig().getInt("buffs.speed.level", 1);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, level - 1, true, false, false));
        }
        
        // Haste (mining speed)
        if (plugin.getConfig().getBoolean("buffs.haste.enabled", true)) {
            int level = plugin.getConfig().getInt("buffs.haste.level", 1);
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, level - 1, true, false, false));
        }
        
        // Regeneration
        if (plugin.getConfig().getBoolean("buffs.regeneration.enabled", true)) {
            int level = plugin.getConfig().getInt("buffs.regeneration.level", 1);
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, level - 1, true, false, false));
        }
    }
    
    private void removeBuffs(Player player) {
        player.removePotionEffect(PotionEffectType.HEALTH_BOOST);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.HASTE);
        player.removePotionEffect(PotionEffectType.REGENERATION);
    }
    
    public boolean areBuffsEnabled(UUID uuid) {
        return buffsEnabled.contains(uuid);
    }
    
    public void setBuffsEnabled(UUID uuid, boolean enabled) {
        if (enabled) {
            buffsEnabled.add(uuid);
        } else {
            buffsEnabled.remove(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                removeBuffs(player);
                nearHomeStatus.put(uuid, false);
            }
        }
    }
    
    public boolean isPlayerNearHome(Player player) {
        return nearHomeStatus.getOrDefault(player.getUniqueId(), false);
    }
}

