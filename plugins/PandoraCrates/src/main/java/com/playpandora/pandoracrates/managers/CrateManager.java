package com.playpandora.pandoracrates.managers;

import com.playpandora.pandoracrates.PandoraCrates;
import com.playpandora.pandoracrates.models.Crate;
import com.playpandora.pandoracrates.models.RarityRewards;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class CrateManager {
    
    private final PandoraCrates plugin;
    private final Map<String, Crate> crates;
    private final Map<Location, String> crateLocations; // Location -> Crate ID
    private final Map<Location, Location> hologramBlocks; // Crate location -> Hologram block location
    private LocationDataManager locationDataManager;
    
    public CrateManager(PandoraCrates plugin) {
        this.plugin = plugin;
        this.crates = new HashMap<>();
        this.crateLocations = new HashMap<>();
        this.hologramBlocks = new HashMap<>();
        // Use the plugin's LocationDataManager instance
        this.locationDataManager = plugin.getLocationDataManager();
    }
    
    public void loadCrates() {
        crates.clear();
        ConfigurationSection cratesSection = plugin.getConfig().getConfigurationSection("crates");
        if (cratesSection == null) {
            plugin.getLogger().warning("No crates section found in config!");
            return;
        }
        
        for (String crateId : cratesSection.getKeys(false)) {
            ConfigurationSection crateConfig = cratesSection.getConfigurationSection(crateId);
            if (crateConfig != null) {
                Crate crate = new Crate(crateId);
                crate.loadFromConfig(crateConfig);
                crates.put(crateId.toLowerCase(), crate);
                
                // Count total rewards
                int totalRewards = 0;
                for (Map.Entry<String, RarityRewards> entry : crate.getRewards().entrySet()) {
                    totalRewards += entry.getValue().getRewards().size();
                }
                plugin.getLogger().info("Loaded crate: " + crateId + " with " + totalRewards + " total rewards");
            }
        }
        
        plugin.getLogger().info("Loaded " + crates.size() + " crates!");
        
        // Load saved crate locations
        loadCrateLocations();
    }
    
    public void loadCrateLocations() {
        Map<Location, String> savedLocations = locationDataManager.loadCrateLocations();
        
        for (Map.Entry<Location, String> entry : savedLocations.entrySet()) {
            Location location = entry.getKey();
            String crateId = entry.getValue();
            
            Crate crate = getCrate(crateId);
            if (crate == null) {
                plugin.getLogger().warning("Crate '" + crateId + "' not found for location at " + 
                    location.getWorld().getName() + " " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
                continue;
            }
            
            // Restore crate location
            restoreCrateLocation(location, crateId);
        }
    }
    
    private void restoreCrateLocation(Location location, String crateId) {
        Crate crate = getCrate(crateId);
        if (crate == null) {
            return;
        }
        
        Block block = location.getBlock();
        // Set block type if it's air or different material
        if (block.getType() == Material.AIR || block.getType() != crate.getBlockMaterial()) {
            block.setType(crate.getBlockMaterial());
        }
        
        // Use block location (no pitch/yaw) for storage
        Location blockLoc = new Location(location.getWorld(), 
            location.getBlockX(), location.getBlockY(), location.getBlockZ());
        
        // Set crate location
        crateLocations.put(blockLoc, crateId.toLowerCase());
        
        // Create invisible barrier block above crate for hologram (1 block up)
        Location holoBlockLoc = blockLoc.clone().add(0, 1, 0);
        Block holoBlock = holoBlockLoc.getBlock();
        
        // Only place if air (don't replace existing blocks)
        if (holoBlock.getType() == Material.AIR) {
            holoBlock.setType(Material.BARRIER);
        }
        
        // Store hologram block location
        hologramBlocks.put(blockLoc, holoBlockLoc);
        
        // Create hologram on the invisible block after a short delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getHologramManager().createHologram(holoBlockLoc, crate);
        }, 10L); // Slightly longer delay to ensure world is fully loaded
    }
    
    public Crate getCrate(String id) {
        return crates.get(id.toLowerCase());
    }
    
    public Map<String, Crate> getAllCrates() {
        return new HashMap<>(crates);
    }
    
    public boolean isCrateBlock(Block block) {
        return getCrateAtLocation(block.getLocation()) != null;
    }
    
    public Crate getCrateAtLocation(Location location) {
        // Use block location for comparison (ignore pitch/yaw)
        Location blockLoc = new Location(location.getWorld(), 
            location.getBlockX(), location.getBlockY(), location.getBlockZ());
        
        for (Map.Entry<Location, String> entry : crateLocations.entrySet()) {
            Location crateLoc = entry.getKey();
            if (crateLoc.getWorld().equals(blockLoc.getWorld()) &&
                crateLoc.getBlockX() == blockLoc.getBlockX() &&
                crateLoc.getBlockY() == blockLoc.getBlockY() &&
                crateLoc.getBlockZ() == blockLoc.getBlockZ()) {
                return getCrate(entry.getValue());
            }
        }
        return null;
    }
    
    public void setCrateLocation(Location location, String crateId) {
        Crate crate = getCrate(crateId);
        if (crate != null) {
            Block block = location.getBlock();
            // Set block type if it's air or different material
            if (block.getType() == Material.AIR || block.getType() != crate.getBlockMaterial()) {
                block.setType(crate.getBlockMaterial());
            }
            
            // Use block location (no pitch/yaw) for storage
            Location blockLoc = new Location(location.getWorld(), 
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
            
            // Remove any existing crate at this location first
            removeCrateLocation(blockLoc);
            
            // Set new crate location
            crateLocations.put(blockLoc, crateId.toLowerCase());
            
            // Save to file
            locationDataManager.saveCrateLocation(blockLoc, crateId);
            
            // Create invisible barrier block above crate for hologram (1 block up)
            // Barrier blocks are completely invisible and don't interfere with interactions
            Location holoBlockLoc = blockLoc.clone().add(0, 1, 0);
            Block holoBlock = holoBlockLoc.getBlock();
            
            // Only place if air (don't replace existing blocks)
            if (holoBlock.getType() == Material.AIR) {
                // Use barrier block for true invisibility (completely invisible, no hitbox issues)
                holoBlock.setType(Material.BARRIER);
            }
            
            // Store hologram block location
            hologramBlocks.put(blockLoc, holoBlockLoc);
            
            // Create hologram on the invisible block after a short delay
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getHologramManager().createHologram(holoBlockLoc, crate);
            }, 2L);
        }
    }
    
    public ItemStack createCrateItem(String crateId) {
        return plugin.getCrateItemManager().createCrateItem(crateId);
    }
    
    public void removeCrateLocation(Location location) {
        // Use block location for comparison (ignore pitch/yaw)
        Location blockLoc = new Location(location.getWorld(), 
            location.getBlockX(), location.getBlockY(), location.getBlockZ());
        
        // Find and remove matching location
        Location toRemove = null;
        for (Location loc : crateLocations.keySet()) {
            if (loc.getWorld().equals(blockLoc.getWorld()) &&
                loc.getBlockX() == blockLoc.getBlockX() &&
                loc.getBlockY() == blockLoc.getBlockY() &&
                loc.getBlockZ() == blockLoc.getBlockZ()) {
                toRemove = loc;
                break;
            }
        }
        
        if (toRemove != null) {
            crateLocations.remove(toRemove);
            
            // Remove from file
            locationDataManager.removeCrateLocation(toRemove);
            
            // Remove hologram block if it exists
            Location holoBlockLoc = hologramBlocks.remove(toRemove);
            if (holoBlockLoc != null) {
                Block holoBlock = holoBlockLoc.getBlock();
                // Only remove if it's a barrier or glass pane (our hologram block)
                if (holoBlock.getType() == Material.BARRIER || holoBlock.getType() == Material.GLASS_PANE) {
                    holoBlock.setType(Material.AIR);
                }
                plugin.getHologramManager().removeHologram(holoBlockLoc);
            } else {
                // Fallback: try to remove hologram at crate location
                plugin.getHologramManager().removeHologram(blockLoc);
            }
        }
    }
    
    public Map<Location, String> getCrateLocations() {
        return new HashMap<>(crateLocations);
    }
}

