package com.playpandora.pandoracrates.managers;

import com.playpandora.pandoracrates.PandoraCrates;
import com.playpandora.pandoracrates.models.Crate;
import com.playpandora.pandoracrates.models.Reward;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AnimationManager {
    
    private final PandoraCrates plugin;
    private final Map<UUID, BukkitTask> activeAnimations;
    
    public AnimationManager(PandoraCrates plugin) {
        this.plugin = plugin;
        this.activeAnimations = new HashMap<>();
    }
    
    public void playOpeningAnimation(Player player, Location crateLocation, Crate crate) {
        if (activeAnimations.containsKey(player.getUniqueId())) {
            player.sendMessage(plugin.formatMessage("crate-opened", 
                "{prefix} &cYou are already opening a crate!",
                "crate", crate.getDisplayName()));
            return;
        }
        
        // Check cooldown
        if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId())) {
            long remaining = plugin.getCooldownManager().getRemainingCooldown(player.getUniqueId());
            player.sendMessage(plugin.formatRawMessage(
                "{prefix} &cPlease wait &6" + (remaining / 1000) + "s &cbefore opening another crate!",
                "crate", crate.getDisplayName()));
            return;
        }
        
        // Set cooldown
        plugin.getCooldownManager().setCooldown(player.getUniqueId(), 
            plugin.getConfig().getLong("cooldown.seconds", 3) * 1000);
        
        // Play opening sound and particles
        playOpeningEffects(crateLocation);
        
        // Play spin sound
        if (plugin.getConfig().getBoolean("sounds.spin.enabled", true)) {
            String soundType = plugin.getConfig().getString("sounds.spin.sound", "block.note_block.xyophone");
            float volume = (float) plugin.getConfig().getDouble("sounds.spin.volume", 0.7);
            float pitch = (float) plugin.getConfig().getDouble("sounds.spin.pitch", 1.0);
            playSoundAtLocation(crateLocation, soundType, volume, pitch);
        }
        
        // Generate reward pool
        List<Reward> rewardPool = generateRewardPool(crate);
        if (rewardPool.isEmpty()) {
            player.sendMessage(plugin.formatRawMessage(
                "{prefix} &cThis crate has no rewards configured!",
                "crate", crate.getDisplayName()));
            return;
        }
        
        // Select final reward first
        Reward finalReward = plugin.getRewardManager().selectReward(crate);
        String rarity = determineRarity(crate, finalReward);
        
        // Open spinner GUI
        plugin.getSpinnerGUI().openSpinner(player, crate, rewardPool, () -> {
            // Animation finished callback
            activeAnimations.remove(player.getUniqueId());
            
            // Record statistics
            plugin.getStatisticsManager().recordCrateOpen(player.getUniqueId(), crate.getId(), rarity);
            
            // Play reward effects
            playRewardEffects(crateLocation, rarity);
            
            // Give reward
            plugin.getRewardManager().giveReward(player, finalReward, rarity, crate);
            
            // Send title
            sendRewardTitle(player, finalReward, rarity);
            
            // Send chat message
            String rewardName = plugin.getRewardManager().getRewardDisplayName(finalReward);
            player.sendMessage(plugin.formatMessage("reward-received", 
                "{prefix} &7You received: &6{reward}&7!", 
                "reward", rewardName,
                "crate", crate.getDisplayName()));
            
            // Finish spinner GUI
            plugin.getSpinnerGUI().finishSpinner(player, finalReward);
        });
        
        // Start particle/sound effects task
        BukkitTask effectsTask = new BukkitRunnable() {
            private int tick = 0;
            private final int rollDuration = plugin.getConfig().getInt("animation.roll-duration-ticks", 60);
            
            @Override
            public void run() {
                tick++;
                
                // Play rolling effects
                if (tick % plugin.getConfig().getInt("animation.particle-interval", 2) == 0) {
                    playRollingParticles(crateLocation);
                }
                
                if (tick % plugin.getConfig().getInt("animation.sound-interval", 5) == 0) {
                    playRollingSound(crateLocation);
                }
                
                // End effects
                if (tick >= rollDuration) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        activeAnimations.put(player.getUniqueId(), effectsTask);
    }
    
    private List<Reward> generateRewardPool(Crate crate) {
        List<Reward> pool = new ArrayList<>();
        for (com.playpandora.pandoracrates.models.RarityRewards rarityRewards : crate.getRewards().values()) {
            List<Reward> rewards = rarityRewards.getRewards();
            if (rewards != null && !rewards.isEmpty()) {
                pool.addAll(rewards);
            }
        }
        
        // Safety check - if pool is empty, return empty list
        if (pool.isEmpty()) {
            plugin.getLogger().warning("Crate " + crate.getId() + " has no rewards configured!");
            return new ArrayList<>();
        }
        
        // Add some duplicates to make it look more realistic (but prevent infinite loop)
        int targetSize = 20;
        if (pool.size() > 0 && pool.size() < targetSize) {
            List<Reward> originalPool = new ArrayList<>(pool);
            int originalSize = pool.size();
            int iterations = 0;
            int maxIterations = 100; // Safety limit
            
            while (pool.size() < targetSize && iterations < maxIterations) {
                int needed = targetSize - pool.size();
                int toAdd = Math.min(needed, originalPool.size());
                if (toAdd > 0) {
                    pool.addAll(originalPool.subList(0, toAdd));
                } else {
                    break; // Can't add more
                }
                
                // Safety check - if size didn't change, break
                if (pool.size() == originalSize) {
                    break;
                }
                originalSize = pool.size();
                iterations++;
            }
        }
        
        Collections.shuffle(pool);
        return pool;
    }
    
    private void playOpeningEffects(Location location) {
        Location effectLoc = location.clone().add(0.5, 1.0, 0.5);
        
        if (plugin.getConfig().getBoolean("effects.on-open.particles.enabled", true)) {
            String particleType = plugin.getConfig().getString("effects.on-open.particles.type", "PORTAL");
            int count = plugin.getConfig().getInt("effects.on-open.particles.count", 30);
            
            // Spawn particles in a circle
            for (int i = 0; i < count; i++) {
                double angle = (2 * Math.PI * i) / count;
                double x = Math.cos(angle) * 0.5;
                double z = Math.sin(angle) * 0.5;
                
                try {
                    Particle particle = Particle.valueOf(particleType);
                    location.getWorld().spawnParticle(particle, 
                        effectLoc.clone().add(x, 0, z), 1, 0, 0, 0, 0.05);
                } catch (IllegalArgumentException e) {
                    location.getWorld().spawnParticle(Particle.PORTAL, 
                        effectLoc.clone().add(x, 0, z), 1, 0, 0, 0, 0.05);
                }
            }
        }
        
        if (plugin.getConfig().getBoolean("effects.on-open.sound.enabled", true)) {
            String soundType = plugin.getConfig().getString("effects.on-open.sound.type", "entity.player.levelup");
            float volume = (float) plugin.getConfig().getDouble("effects.on-open.sound.volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble("effects.on-open.sound.pitch", 1.2);
            
            try {
                location.getWorld().playSound(location, soundType, SoundCategory.PLAYERS, volume, pitch);
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    private void playRollingParticles(Location location) {
        if (plugin.getConfig().getBoolean("effects.during-roll.particles.enabled", true)) {
            String particleType = plugin.getConfig().getString("effects.during-roll.particles.type", "CRIT");
            int count = plugin.getConfig().getInt("effects.during-roll.particles.count", 5);
            
            try {
                Particle particle = Particle.valueOf(particleType);
                location.getWorld().spawnParticle(particle, location.add(0, 1, 0), count, 0.3, 0.5, 0.3, 0.05);
            } catch (IllegalArgumentException e) {
                location.getWorld().spawnParticle(Particle.CRIT, location.add(0, 1, 0), count, 0.3, 0.5, 0.3, 0.05);
            }
        }
    }
    
    private void playRollingSound(Location location) {
        if (plugin.getConfig().getBoolean("effects.during-roll.sound.enabled", true)) {
            String soundType = plugin.getConfig().getString("effects.during-roll.sound.type", "ui.button.click");
            float volume = (float) plugin.getConfig().getDouble("effects.during-roll.sound.volume", 0.5);
            float pitch = (float) plugin.getConfig().getDouble("effects.during-roll.sound.pitch", 1.0);
            
            playSoundAtLocation(location, soundType, volume, pitch);
        }
    }
    
    private void playSoundAtLocation(Location location, String soundName, float volume, float pitch) {
        try {
            // Try as enum first
            try {
                Sound soundEnum = Sound.valueOf(soundName.toUpperCase());
                location.getWorld().playSound(location, soundEnum, SoundCategory.PLAYERS, volume, pitch);
            } catch (IllegalArgumentException e) {
                // Try as string sound name (for newer versions)
                location.getWorld().playSound(location, soundName, SoundCategory.PLAYERS, volume, pitch);
            }
        } catch (Exception e) {
            // Ignore sound errors
        }
    }
    
    private String determineRarity(Crate crate, Reward reward) {
        for (Map.Entry<String, com.playpandora.pandoracrates.models.RarityRewards> entry : crate.getRewards().entrySet()) {
            if (entry.getValue().getRewards().contains(reward)) {
                return entry.getKey();
            }
        }
        return "common";
    }
    
    private void sendRewardTitle(Player player, Reward reward, String rarity) {
        // Hypixel-style title formatting with fancy symbols
        String title = "";
        String subtitle = "";
        
        if (rarity != null) {
            String rarityColor = plugin.getConfig().getString("rarities." + rarity.toLowerCase() + ".color", "YELLOW");
            ChatColor color = ChatColor.valueOf(rarityColor);
            
            // Create fancy title with rarity (Hypixel-style)
            if (rarity.equalsIgnoreCase("legendary")) {
                title = ChatColor.GOLD + "✬ " + ChatColor.YELLOW + ChatColor.BOLD + rarity.toUpperCase() + 
                    ChatColor.GOLD + " REWARD " + ChatColor.GOLD + "✬";
            } else if (rarity.equalsIgnoreCase("epic")) {
                title = ChatColor.LIGHT_PURPLE + "✦ " + color + ChatColor.BOLD + rarity.toUpperCase() + 
                    ChatColor.LIGHT_PURPLE + " REWARD " + ChatColor.LIGHT_PURPLE + "✦";
            } else if (rarity.equalsIgnoreCase("rare")) {
                title = ChatColor.AQUA + "◆ " + color + ChatColor.BOLD + rarity.toUpperCase() + 
                    ChatColor.AQUA + " REWARD " + ChatColor.AQUA + "◆";
            } else {
                title = color + "★ " + ChatColor.BOLD + rarity.toUpperCase() + 
                    color + " REWARD " + color + "★";
            }
        } else {
            title = ChatColor.GOLD + "★ REWARD ★";
        }
        
        // Format subtitle with reward name
        String rewardName = plugin.getRewardManager().getRewardDisplayName(reward);
        subtitle = ChatColor.YELLOW + rewardName;
        
        // Send title with proper timing (Hypixel-style: longer fade in, shorter stay)
        player.sendTitle(title, subtitle, 20, 60, 20);
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
    
    
    private void playRewardEffects(Location location, String rarity) {
        // Play win sound
        if (plugin.getConfig().getBoolean("sounds.win.enabled", true)) {
            String soundType = plugin.getConfig().getString("sounds.win.sound", "entity.player.levelup");
            float volume = (float) plugin.getConfig().getDouble("sounds.win.volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble("sounds.win.pitch", 1.2);
            playSoundAtLocation(location, soundType, volume, pitch);
        }
        
        Location effectLoc = location.clone().add(0.5, 1.0, 0.5);
        
        // Enhanced particle effects based on rarity (Hypixel-style)
        if (rarity != null) {
            try {
                if (rarity.equalsIgnoreCase("legendary")) {
                    // Legendary: Gold particles and fireworks
                    try {
                        location.getWorld().spawnParticle(Particle.FIREWORK, effectLoc, 50, 0.5, 0.5, 0.5, 0.2);
                    } catch (Exception e) {
                        location.getWorld().spawnParticle(Particle.CRIT, effectLoc, 50, 0.5, 0.5, 0.5, 0.2);
                    }
                    try {
                        Particle totemParticle = Particle.valueOf("TOTEM");
                        location.getWorld().spawnParticle(totemParticle, effectLoc, 30, 0.4, 0.4, 0.4, 0.1);
                    } catch (Exception e) {
                        location.getWorld().spawnParticle(Particle.ENCHANT, effectLoc, 30, 0.4, 0.4, 0.4, 0.1);
                    }
                    // Spawn multiple fireworks
                    for (int i = 0; i < 3; i++) {
                        Firework firework = location.getWorld().spawn(effectLoc.clone().add(
                            (Math.random() - 0.5) * 0.5, 0, (Math.random() - 0.5) * 0.5), Firework.class);
                        FireworkMeta meta = firework.getFireworkMeta();
                        meta.setPower(2);
                        firework.setFireworkMeta(meta);
                        firework.detonate();
                    }
                } else if (rarity.equalsIgnoreCase("epic")) {
                    // Epic: Purple particles
                    location.getWorld().spawnParticle(Particle.PORTAL, effectLoc, 40, 0.5, 0.5, 0.5, 0.15);
                    try {
                        Particle enchantTableParticle = Particle.valueOf("ENCHANTMENT_TABLE");
                        location.getWorld().spawnParticle(enchantTableParticle, effectLoc, 25, 0.4, 0.4, 0.4, 0.1);
                    } catch (Exception e) {
                        location.getWorld().spawnParticle(Particle.ENCHANT, effectLoc, 25, 0.4, 0.4, 0.4, 0.1);
                    }
                    Firework firework = location.getWorld().spawn(effectLoc, Firework.class);
                    FireworkMeta meta = firework.getFireworkMeta();
                    meta.setPower(1);
                    firework.setFireworkMeta(meta);
                    firework.detonate();
                } else if (rarity.equalsIgnoreCase("rare")) {
                    // Rare: Blue particles
                    try {
                        Particle villagerHappyParticle = Particle.valueOf("VILLAGER_HAPPY");
                        location.getWorld().spawnParticle(villagerHappyParticle, effectLoc, 30, 0.4, 0.4, 0.4, 0.1);
                    } catch (Exception e) {
                        location.getWorld().spawnParticle(Particle.HEART, effectLoc, 30, 0.4, 0.4, 0.4, 0.1);
                    }
                    location.getWorld().spawnParticle(Particle.ENCHANT, effectLoc, 20, 0.3, 0.3, 0.3, 0.05);
                } else {
                    // Common/Uncommon: Simple particles
                    try {
                        Particle villagerHappyParticle = Particle.valueOf("VILLAGER_HAPPY");
                        location.getWorld().spawnParticle(villagerHappyParticle, effectLoc, 15, 0.3, 0.3, 0.3, 0.05);
                    } catch (Exception e) {
                        location.getWorld().spawnParticle(Particle.HEART, effectLoc, 15, 0.3, 0.3, 0.3, 0.05);
                    }
                }
            } catch (Exception e) {
                // Ignore particle errors
            }
        }
        
        // Firework (legacy support)
        if (plugin.getConfig().getBoolean("effects.on-reward.fireworks.enabled", true)) {
            spawnFirework(effectLoc, rarity);
        }
        
        // Particles - burst effect (legacy support)
        if (plugin.getConfig().getBoolean("effects.on-reward.particles.enabled", true)) {
            String particleType = plugin.getConfig().getString("effects.on-reward.particles.type", "TOTEM");
            int count = plugin.getConfig().getInt("effects.on-reward.particles.count", 50);
            
            // Spawn particles in expanding circle
            for (int i = 0; i < count; i++) {
                double angle = (2 * Math.PI * i) / count;
                double radius = 0.3 + (Math.random() * 0.7);
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = Math.random() * 1.5;
                
                try {
                    Particle particle = Particle.valueOf(particleType);
                    location.getWorld().spawnParticle(particle, 
                        effectLoc.clone().add(x, y, z), 1, 0, 0, 0, 0.05);
                } catch (IllegalArgumentException e) {
                    // Use ENCHANTMENT_TABLE as fallback if TOTEM doesn't exist
                    Particle particleToUse = null;
                    try {
                        particleToUse = Particle.valueOf("TOTEM");
                    } catch (IllegalArgumentException ex) {
                        try {
                            particleToUse = Particle.valueOf("ENCHANTMENT_TABLE");
                        } catch (IllegalArgumentException ex2) {
                            particleToUse = Particle.HEART; // Always available fallback
                        }
                    }
                    if (particleToUse != null) {
                        location.getWorld().spawnParticle(particleToUse, 
                            effectLoc.clone().add(x, y, z), 1, 0, 0, 0, 0.05);
                    }
                }
            }
        }
        
        // Sound - play win sound
        if (plugin.getConfig().getBoolean("effects.on-reward.sound.enabled", true)) {
            String soundType = plugin.getConfig().getString("effects.on-reward.sound.type", "entity.firework_rocket.blast");
            float volume = (float) plugin.getConfig().getDouble("effects.on-reward.sound.volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble("effects.on-reward.sound.pitch", 1.0);
            
            playSoundAtLocation(location, soundType, volume, pitch);
        }
        
        // Also check for win sound specifically
        if (plugin.getConfig().getBoolean("sounds.win.enabled", true)) {
            String soundType = plugin.getConfig().getString("sounds.win.sound", "entity.player.levelup");
            float volume = (float) plugin.getConfig().getDouble("sounds.win.volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble("sounds.win.pitch", 1.2);
            
            playSoundAtLocation(location, soundType, volume, pitch);
        }
    }
    
    private void spawnFirework(Location location, String rarity) {
        org.bukkit.configuration.ConfigurationSection fireworkSection = plugin.getConfig().getConfigurationSection("fireworks." + rarity);
        if (fireworkSection == null) {
            return;
        }
        
        String colorStr = fireworkSection.getString("color", "WHITE");
        FireworkEffect.Type type = FireworkEffect.Type.BURST;
        
        try {
            FireworkEffect.Builder builder = FireworkEffect.builder();
            builder.with(type);
            
            // Parse color
            try {
                org.bukkit.Color color = getColorFromString(colorStr);
                builder.withColor(color);
                builder.withFade(color);
            } catch (Exception e) {
                builder.withColor(org.bukkit.Color.WHITE);
            }
            
            Firework firework = location.getWorld().spawn(location, Firework.class);
            FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(builder.build());
            meta.setPower(1);
            firework.setFireworkMeta(meta);
            
            // Detonate immediately
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (firework != null && !firework.isDead()) {
                        firework.detonate();
                    }
                }
            }.runTaskLater(plugin, 2L);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to spawn firework: " + e.getMessage());
        }
    }
    
    private org.bukkit.Color getColorFromString(String colorStr) {
        switch (colorStr.toUpperCase()) {
            case "WHITE": return org.bukkit.Color.WHITE;
            case "GREEN": return org.bukkit.Color.LIME;
            case "BLUE": return org.bukkit.Color.AQUA;
            case "PURPLE": return org.bukkit.Color.PURPLE;
            case "YELLOW": case "GOLD": return org.bukkit.Color.YELLOW;
            default: return org.bukkit.Color.WHITE;
        }
    }
    
    public boolean isPlayerAnimating(UUID uuid) {
        return activeAnimations.containsKey(uuid);
    }
    
    public void cancelAnimation(UUID uuid) {
        BukkitTask task = activeAnimations.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }
    
}

