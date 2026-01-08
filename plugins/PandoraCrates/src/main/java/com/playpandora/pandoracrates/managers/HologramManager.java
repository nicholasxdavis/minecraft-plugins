package com.playpandora.pandoracrates.managers;

import com.playpandora.pandoracrates.PandoraCrates;
import com.playpandora.pandoracrates.models.Crate;
import org.bukkit.Location;
import org.bukkit.ChatColor;

import java.util.HashMap;
import java.util.Map;

public class HologramManager {
    
    private final PandoraCrates plugin;
    private final Map<Location, Object> holograms; // Store hologram objects
    
    public HologramManager(PandoraCrates plugin) {
        this.plugin = plugin;
        this.holograms = new HashMap<>();
    }
    
    public void initializeHolograms() {
        // Holograms will be created when crates are placed
        // This method can be used to reload holograms if needed
    }
    
    public void createHologram(Location location, Crate crate) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("DecentHolograms")) {
            plugin.getLogger().warning("DecentHolograms not found! Holograms will not work.");
            return;
        }
        
        removeHologram(location);
        
        // Run async to avoid blocking
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                // Position hologram on the invisible block above the crate
                // The location passed is already the hologram block location (1 block above crate)
                org.bukkit.block.Block block = location.getBlock();
                double blockHeight = block.getBoundingBox().getHeight();
                
                // Calculate base Y offset: half block height + proper offset (like ExcellentCrates)
                // This ensures holograms are properly centered above blocks
                double baseYOffset = 0.3; // Base offset above the barrier block
                double yOffset = (blockHeight / 2.0) + baseYOffset;
                
                // Get hologram lines
                java.util.List<String> hologramLines = crate.getHologramLines();
                if (hologramLines.isEmpty()) {
                    return;
                }
                
                // Line spacing for proper hologram display (0.25 blocks per line like ExcellentCrates)
                double lineGap = 0.25;
                
                // Create hologram with proper line spacing
                // DecentHolograms handles line spacing automatically, but we position the first line correctly
                Location firstLineLocation = location.clone().add(0.5, yOffset, 0.5);
                
                // Translate color codes
                java.util.List<String> lines = new java.util.ArrayList<>();
                for (String line : hologramLines) {
                    if (line != null && !line.trim().isEmpty()) {
                        lines.add(ChatColor.translateAlternateColorCodes('&', line));
                    }
                }
                
                if (lines.isEmpty()) {
                    return;
                }
                
                String hologramId = "pandoracrates_" + location.getBlockX() + "_" + 
                    location.getBlockY() + "_" + location.getBlockZ();
                
                // Use reflection to access DecentHolograms API
                Class<?> dhapiClass = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
                java.lang.reflect.Method createMethod = dhapiClass.getMethod("createHologram", 
                    String.class, Location.class, java.util.List.class);
                Object hologram = createMethod.invoke(null, hologramId, firstLineLocation, lines);
                holograms.put(location, hologram);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to create hologram: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    public void removeHologram(Location location) {
        Object hologram = holograms.remove(location);
        if (hologram != null && plugin.getServer().getPluginManager().isPluginEnabled("DecentHolograms")) {
            try {
                java.lang.reflect.Method deleteMethod = hologram.getClass().getMethod("delete");
                deleteMethod.invoke(hologram);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to remove hologram: " + e.getMessage());
            }
        }
    }
    
    public void removeAllHolograms() {
        for (Location location : new java.util.HashSet<>(holograms.keySet())) {
            removeHologram(location);
        }
    }
    
    public void updateHologram(Location location, Crate crate) {
        removeHologram(location);
        createHologram(location, crate);
    }
}

