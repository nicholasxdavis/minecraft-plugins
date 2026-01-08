package com.playpandora.pandoracrates.listeners;

import com.playpandora.pandoracrates.PandoraCrates;
import com.playpandora.pandoracrates.models.Crate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class CrateListener implements Listener {
    
    private final PandoraCrates plugin;
    
    public CrateListener(PandoraCrates plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        try {
            // Don't interfere with block placement - let BlockPlaceEvent handle it
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getItem() != null) {
                // Check if player is holding a crate item to place
                if (plugin.getCrateItemManager().isCrateItem(event.getItem())) {
                    // Allow placement - don't interfere
                    return;
                }
            }
            
            // Check if clicked block is a crate
            Crate crate = null;
            org.bukkit.block.Block block = event.getClickedBlock();
            
            if (block != null) {
                crate = plugin.getCrateManager().getCrateAtLocation(block.getLocation());
            }
            
            // Don't check for crate items in hand - only check placed blocks
            // This prevents interference with crate placement
            if (crate == null) {
                return;
            }
        
        // Deny item use and block interaction (like ExcellentCrates)
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        if (block != null) {
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        }
        
        // Check if player is already animating
        if (plugin.getAnimationManager().isPlayerAnimating(event.getPlayer().getUniqueId())) {
            event.getPlayer().sendMessage(plugin.formatMessage("crate-opened",
                "{prefix} &cYou are already opening a crate!",
                "crate", crate.getDisplayName()));
            return;
        }
        
        // Support both LEFT_CLICK_BLOCK and LEFT_CLICK_AIR (like ExcellentCrates)
        boolean isLeftClick = event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR;
        boolean isRightClick = event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR;
        
        // Left click = preview (punch/hit to preview)
        if (isLeftClick) {
            // Cancel the event to prevent block breaking
            event.setCancelled(true);
            // Show preview GUI
            plugin.getPreviewGUI().openPreview(event.getPlayer(), crate);
            // Play preview sound
            playSound(event.getPlayer(), "preview", "ui.button.click", 0.5f, 1.0f);
            return;
        }
        
        // Right click = open crate
        if (isRightClick) {
            // Check if player has a key
            ItemStack item = event.getItem();
            if (item == null || !plugin.getKeyManager().isKeyForCrate(item, crate.getId())) {
                // Try main hand if item is null
                if (item == null) {
                    item = event.getPlayer().getInventory().getItemInMainHand();
                }
                if (item == null || !plugin.getKeyManager().isKeyForCrate(item, crate.getId())) {
                    event.getPlayer().sendMessage(plugin.formatMessage("no-key",
                        "{prefix} &cYou don't have a key for this crate!",
                        "crate", crate.getDisplayName()));
                    // Play error sound
                    playSound(event.getPlayer(), "error", "entity.villager.no", 0.5f, 1.0f);
                    return;
                }
            }
            
            // Remove key
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                // Safely handle hand removal
                try {
                    org.bukkit.inventory.EquipmentSlot hand = event.getHand();
                    if (hand == null || hand == org.bukkit.inventory.EquipmentSlot.HAND) {
                        event.getPlayer().getInventory().setItemInMainHand(null);
                    } else {
                        event.getPlayer().getInventory().setItemInOffHand(null);
                    }
                } catch (Exception e) {
                    // Fallback: remove from main hand
                    event.getPlayer().getInventory().setItemInMainHand(null);
                }
            }
            
            // Play enter sound
            playSound(event.getPlayer(), "enter", "entity.player.levelup", 0.7f, 1.2f);
            
            // Start animation
            org.bukkit.Location crateLocation = block != null ? block.getLocation() : event.getPlayer().getLocation();
            plugin.getAnimationManager().playOpeningAnimation(
                event.getPlayer(),
                crateLocation,
                crate
            );
            
            // Message will be sent after animation completes
        }
        } catch (Exception e) {
            // Log error but don't crash the server
            plugin.getLogger().warning("Error in PlayerInteractEvent: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }
    
    private void playSound(org.bukkit.entity.Player player, String soundType, String defaultSound, float defaultVolume, float defaultPitch) {
        String soundPath = "sounds." + soundType + ".sound";
        String volumePath = "sounds." + soundType + ".volume";
        String pitchPath = "sounds." + soundType + ".pitch";
        
        String sound = plugin.getConfig().getString(soundPath, defaultSound);
        float volume = (float) plugin.getConfig().getDouble(volumePath, defaultVolume);
        float pitch = (float) plugin.getConfig().getDouble(pitchPath, defaultPitch);
        
        try {
            org.bukkit.Sound soundEnum = org.bukkit.Sound.valueOf(sound.toUpperCase());
            player.playSound(player.getLocation(), soundEnum, org.bukkit.SoundCategory.PLAYERS, volume, pitch);
        } catch (IllegalArgumentException e) {
            // Try as string sound name (for newer versions)
            try {
                player.getWorld().playSound(player.getLocation(), sound, org.bukkit.SoundCategory.PLAYERS, volume, pitch);
            } catch (Exception ex) {
                // Fallback to default
                try {
                    org.bukkit.Sound defaultSoundEnum = org.bukkit.Sound.valueOf(defaultSound.toUpperCase());
                    player.playSound(player.getLocation(), defaultSoundEnum, org.bukkit.SoundCategory.PLAYERS, defaultVolume, defaultPitch);
                } catch (Exception ex2) {
                    player.getWorld().playSound(player.getLocation(), defaultSound, org.bukkit.SoundCategory.PLAYERS, defaultVolume, defaultPitch);
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        
        // Prevent keys from being placed
        if (plugin.getKeyManager().isKey(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.formatMessage("no-permission",
                "{prefix} &cKeys cannot be placed! Use them to open crates."));
            playSound(event.getPlayer(), "error", "entity.villager.no", 0.5f, 1.0f);
            return;
        }
        
        // Check if this is a crate item
        String crateId = plugin.getCrateItemManager().getCrateType(item);
        if (crateId == null) {
            return;
        }
        
        // Allow placement - don't cancel
        // Set the crate location and create hologram
        plugin.getCrateManager().setCrateLocation(event.getBlockPlaced().getLocation(), crateId);
        
        // Send success message
        event.getPlayer().sendMessage(plugin.formatRawMessage(
            "{prefix} &7Placed &6{crate} &7at this location!",
            "crate", plugin.getCrateManager().getCrate(crateId).getDisplayName()));
        
        // Play placement sound
        playSound(event.getPlayer(), "place", "block.chest.open", 0.7f, 1.0f);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // Handle clicks on hologram entities (armor stands used by DecentHolograms)
        Entity entity = event.getRightClicked();
        if (!(entity instanceof ArmorStand)) {
            return;
        }
        
        // Check if this armor stand is near a crate block
        org.bukkit.Location entityLoc = entity.getLocation();
        org.bukkit.Location blockLoc = entityLoc.getBlock().getLocation();
        
        // Check surrounding blocks for crate (hologram might be slightly offset)
        Crate crate = null;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                org.bukkit.Location checkLoc = blockLoc.clone().add(x, 0, z);
                Crate foundCrate = plugin.getCrateManager().getCrateAtLocation(checkLoc);
                if (foundCrate != null) {
                    crate = foundCrate;
                    break;
                }
            }
            if (crate != null) break;
        }
        
        if (crate == null) {
            return;
        }
        
        event.setCancelled(true);
        
        Player player = event.getPlayer();
        
        // Check if player is already animating
        if (plugin.getAnimationManager().isPlayerAnimating(player.getUniqueId())) {
            player.sendMessage(plugin.formatMessage("crate-opened",
                "{prefix} &cYou are already opening a crate!",
                "crate", crate.getDisplayName()));
            return;
        }
        
        // Left click = preview, Right click = open (for entity interactions)
        // For entity clicks, we'll treat it as preview (left click equivalent)
        // Show preview GUI
        plugin.getPreviewGUI().openPreview(player, crate);
        // Play preview sound
        playSound(player, "preview", "ui.button.click", 0.5f, 1.0f);
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        org.bukkit.block.Block block = event.getBlock();
        
        // Check if broken block is a barrier block above a crate (hologram block)
        if (block.getType() == Material.BARRIER) {
            // Check if there's a crate below this barrier block
            Location crateLoc = block.getLocation().clone().add(0, -1, 0);
            Crate crate = plugin.getCrateManager().getCrateAtLocation(crateLoc);
            if (crate != null) {
                // Prevent breaking the hologram barrier block unless admin
                if (!event.getPlayer().hasPermission("pandoracrates.admin") && 
                    !event.getPlayer().hasPermission("pandoracrates.break")) {
                    event.setCancelled(true);
                    return;
                }
                // If admin, allow breaking - it will be cleaned up by removeCrateLocation
            }
        }
        
        // Check if broken block is a crate
        Crate crate = plugin.getCrateManager().getCrateAtLocation(block.getLocation());
        if (crate == null) {
            return;
        }
        
        // Check if player has permission to break crates
        if (!event.getPlayer().hasPermission("pandoracrates.admin") && 
            !event.getPlayer().hasPermission("pandoracrates.break")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.formatMessage("no-permission",
                "{prefix} &cYou don't have permission to break crates!"));
            return;
        }
        
        // Remove crate location (this will also remove the barrier block above)
        plugin.getCrateManager().removeCrateLocation(block.getLocation());
        
        // Send message
        event.getPlayer().sendMessage(plugin.formatRawMessage(
            "{prefix} &7Removed &6{crate} &7from this location!",
            "crate", crate.getDisplayName()));
        
        // Play break sound
        playSound(event.getPlayer(), "break", "block.chest.close", 0.7f, 1.0f);
    }
}

