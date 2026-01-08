package com.playpandora.pandoraitems.listeners;

import com.playpandora.pandoraitems.PandoraItems;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ItemUseListener implements Listener {
    
    private final PandoraItems plugin;
    
    public ItemUseListener(PandoraItems plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return;
        }
        
        List<String> lore = meta.getLore();
        if (lore == null) {
            return;
        }
        
        // Check if this is a PandoraItems item
        String itemIdentifier = null;
        for (String line : lore) {
            String stripped = ChatColor.stripColor(line);
            if (stripped.startsWith("PandoraItems:")) {
                itemIdentifier = stripped;
                break;
            }
        }
        
        if (itemIdentifier == null) {
            return;
        }
        
        // Parse identifier: PandoraItems:Type:Key
        String[] parts = itemIdentifier.split(":");
        if (parts.length < 3) {
            return;
        }
        
        String itemType = parts[1];
        String itemKey = parts[2];
        
        // Check level requirement
        int requiredLevel = 0;
        switch (itemType) {
            case "Cannon":
                requiredLevel = plugin.getCannonItemManager().getRequiredLevel(itemKey);
                break;
            case "Farm":
                requiredLevel = plugin.getFarmItemManager().getRequiredLevel(itemKey);
                break;
            case "Kit":
                requiredLevel = plugin.getKitItemManager().getRequiredLevel(itemKey);
                break;
            case "Perk":
                requiredLevel = plugin.getPerkItemManager().getRequiredLevel(itemKey);
                break;
            case "Pet":
                requiredLevel = plugin.getPetItemManager().getRequiredLevel(itemKey);
                break;
        }
        
        if (requiredLevel > 0) {
            com.playpandora.pandoraitems.LevelAPI levelAPI = plugin.getLevelAPI();
            if (levelAPI != null) {
                int playerLevel = levelAPI.getLevel(player);
                if (playerLevel < requiredLevel) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.formatMessage("level-required",
                        "{prefix} &cYou need to be level &6{level} &cto use this item! You are level &6{current}&7",
                        "level", String.valueOf(requiredLevel), "current", String.valueOf(playerLevel)));
                    return;
                }
            } else {
                // If LevelAPI is not available, allow use but log warning
                plugin.getLogger().warning("LevelAPI not available, but item requires level " + requiredLevel);
            }
        }
        
        // Handle item use based on type
        Location loc = player.getLocation();
        
        switch (itemType) {
            case "Cannon":
                // Let CannonShop handle it completely, just add effects after a delay
                // Use a delayed task to play effects after the cannon is placed
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    playClaimEffects(loc);
                }, 5L); // 5 ticks delay to let cannon placement happen first
                break;
                
            case "Farm":
                // Let FarmShop handle it completely, just add effects after a delay
                // Use a delayed task to play effects after the farm is placed
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    playClaimEffects(loc);
                }, 5L); // 5 ticks delay to let farm placement happen first
                break;
                
            case "Kit":
                event.setCancelled(true); // Cancel to handle ourselves
                handleKitClaim(player, itemKey);
                playClaimEffects(loc);
                break;
                
            case "Perk":
                event.setCancelled(true); // Cancel to handle ourselves
                handlePerkClaim(player, itemKey);
                playClaimEffects(loc);
                break;
                
            case "Pet":
                event.setCancelled(true); // Cancel to handle ourselves
                handlePetClaim(player, itemKey);
                playClaimEffects(loc);
                break;
        }
    }
    
    private void handleKitClaim(Player player, String kitKey) {
        // Validate kit key
        if (!plugin.getKitItemManager().isValidKitKey(kitKey)) {
            player.sendMessage(plugin.formatMessage("invalid-item",
                "{prefix} &cInvalid kit key: &6{key}&7", "key", kitKey));
            return;
        }
        
        // Try to integrate with EssentialsX
        org.bukkit.plugin.Plugin essentials = plugin.getServer().getPluginManager().getPlugin("Essentials");
        if (essentials != null && essentials.isEnabled()) {
            // Execute EssentialsX kit command
            // Use console sender to bypass permission checks (EssentialsX will handle its own permissions)
            String command = "kit " + kitKey + " " + player.getName();
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
            
            player.sendMessage(plugin.formatMessage("item-used",
                "{prefix} &7You claimed &6{item}&7!", "item", "Kit: " + kitKey));
        } else {
            // Fallback: try to execute as player (EssentialsX might not be loaded)
            // This will work if EssentialsX is loaded but plugin reference failed
            try {
                plugin.getServer().dispatchCommand(player, "kit " + kitKey);
                player.sendMessage(plugin.formatMessage("item-used",
                    "{prefix} &7You claimed &6{item}&7!", "item", "Kit: " + kitKey));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to execute kit command: " + e.getMessage());
                player.sendMessage(plugin.formatMessage("item-used",
                    "{prefix} &cFailed to claim kit! EssentialsX may not be installed.", "item", "Kit: " + kitKey));
                return; // Don't remove item if command failed
            }
        }
        
        // Remove item from inventory
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }
    
    private void handlePerkClaim(Player player, String perkKey) {
        // Try to give perk through PerkShop
        org.bukkit.plugin.Plugin perkShop = plugin.getServer().getPluginManager().getPlugin("PerkShop");
        if (perkShop != null) {
            try {
                // Use reflection to add perk to player
                Class<?> perkShopClass = perkShop.getClass();
                java.lang.reflect.Method getDataManager = perkShopClass.getMethod("getDataManager");
                Object dataManager = getDataManager.invoke(perkShop);
                
                java.lang.reflect.Method addPerk = dataManager.getClass().getMethod("addPerk", java.util.UUID.class, String.class);
                addPerk.invoke(dataManager, player.getUniqueId(), perkKey);
                
                player.sendMessage(plugin.formatMessage("item-used",
                    "{prefix} &7You claimed &6{item}&7!", "item", "Perk: " + perkKey));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to claim perk through PerkShop: " + e.getMessage());
                player.sendMessage(plugin.formatMessage("item-used",
                    "{prefix} &7You used &6{item}&7!", "item", "Perk: " + perkKey));
            }
        } else {
            player.sendMessage(plugin.formatMessage("item-used",
                "{prefix} &7You used &6{item}&7!", "item", "Perk: " + perkKey));
        }
        
        // Remove item from inventory
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }
    
    private void handlePetClaim(Player player, String petKey) {
        // Try to give pet through PetPlugin
        org.bukkit.plugin.Plugin petPlugin = plugin.getServer().getPluginManager().getPlugin("PetPlugin");
        if (petPlugin != null) {
            try {
                // Use reflection to purchase pet
                Class<?> petPluginClass = petPlugin.getClass();
                java.lang.reflect.Method getPurchaseManager = petPluginClass.getMethod("getPurchaseManager");
                Object purchaseManager = getPurchaseManager.invoke(petPlugin);
                
                // Try to purchase pet (this will handle the logic)
                java.lang.reflect.Method purchasePet = purchaseManager.getClass().getMethod("purchasePet", Player.class, String.class);
                boolean success = (Boolean) purchasePet.invoke(purchaseManager, player, petKey);
                
                if (success) {
                    player.sendMessage(plugin.formatMessage("item-used",
                        "{prefix} &7You claimed &6{item}&7!", "item", "Pet: " + petKey));
                } else {
                    player.sendMessage(plugin.formatMessage("item-used",
                        "{prefix} &7You used &6{item}&7!", "item", "Pet: " + petKey));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to claim pet through PetPlugin: " + e.getMessage());
                player.sendMessage(plugin.formatMessage("item-used",
                    "{prefix} &7You used &6{item}&7!", "item", "Pet: " + petKey));
            }
        } else {
            player.sendMessage(plugin.formatMessage("item-used",
                "{prefix} &7You used &6{item}&7!", "item", "Pet: " + petKey));
        }
        
        // Remove item from inventory
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }
    
    private void playClaimEffects(Location location) {
        if (plugin.getConfig().getBoolean("effects.on-claim.particles.enabled", true)) {
            String particleType = plugin.getConfig().getString("effects.on-claim.particles.type", "TOTEM");
            int count = plugin.getConfig().getInt("effects.on-claim.particles.count", 30);
            double offsetX = plugin.getConfig().getDouble("effects.on-claim.particles.offset-x", 0.5);
            double offsetY = plugin.getConfig().getDouble("effects.on-claim.particles.offset-y", 1.0);
            double offsetZ = plugin.getConfig().getDouble("effects.on-claim.particles.offset-z", 0.5);
            
            Location effectLoc = location.clone().add(0, 1, 0);
            Particle particle = null;
            try {
                particle = Particle.valueOf(particleType);
            } catch (IllegalArgumentException e) {
                // Try common particle types as fallback
                try {
                    particle = Particle.valueOf("TOTEM");
                } catch (IllegalArgumentException ex) {
                    try {
                        particle = Particle.valueOf("VILLAGER_HAPPY");
                    } catch (IllegalArgumentException ex2) {
                        try {
                            particle = Particle.valueOf("ENCHANTMENT_TABLE");
                        } catch (IllegalArgumentException ex3) {
                            particle = Particle.HEART; // Always available
                        }
                    }
                }
            }
            
            if (particle != null) {
                location.getWorld().spawnParticle(particle, effectLoc, count, offsetX, offsetY, offsetZ, 0.1);
            }
        }
        
        if (plugin.getConfig().getBoolean("effects.on-claim.sound.enabled", true)) {
            String soundType = plugin.getConfig().getString("effects.on-claim.sound.type", "ENTITY_PLAYER_LEVELUP");
            float volume = (float) plugin.getConfig().getDouble("effects.on-claim.sound.volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble("effects.on-claim.sound.pitch", 1.2);
            
            // Try using string-based sound first (works in newer versions)
            try {
                String soundName = soundType.toLowerCase().replace("_", ".");
                location.getWorld().playSound(location, soundName, org.bukkit.SoundCategory.PLAYERS, volume, pitch);
            } catch (Exception e) {
                // Fallback: try old Sound enum method
                try {
                    Sound sound = Sound.valueOf(soundType);
                    location.getWorld().playSound(location, sound, org.bukkit.SoundCategory.PLAYERS, volume, pitch);
                } catch (Exception ex) {
                    // Use default sound
                    try {
                        location.getWorld().playSound(location, "entity.player.levelup", org.bukkit.SoundCategory.PLAYERS, volume, pitch);
                    } catch (Exception ex2) {
                        plugin.getLogger().warning("Could not play sound effect: " + ex2.getMessage());
                    }
                }
            }
        }
    }
}

