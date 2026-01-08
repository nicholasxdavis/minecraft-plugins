package com.playpandora.pandorabanners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BannerManager {
    
    private final PandoraBanners plugin;
    private final List<ArmorStand> spawnedBanners;
    
    public BannerManager(PandoraBanners plugin) {
        this.plugin = plugin;
        this.spawnedBanners = new ArrayList<>();
    }
    
    private int bannerSpawnCount = 0;
    
    public void spawnBanner(Player player, String direction) {
        String[] bannerLines = plugin.getBannerParser().getBanner(direction);
        
        if (bannerLines == null || bannerLines.length == 0) {
            player.sendMessage("§e§lPandora §8» §cBanner for direction '" + direction + "' not found!");
            return;
        }
        
        Location playerLoc = player.getLocation();
        Location spawnLoc = playerLoc.clone();
        
        // Spawn directly at player's location (like holographic displays)
        // Multiple banners will be stacked vertically
        double verticalOffset = bannerSpawnCount * (bannerLines.length * 0.25 + 0.5); // Space between banners
        bannerSpawnCount++;
        
        // Spawn armor stands for each line, stacked vertically
        // Use slightly tighter spacing for better readability
        double lineHeight = 0.23; // Height between lines (slightly tighter)
        double startY = (bannerLines.length - 1) * lineHeight / 2;
        
        List<ArmorStand> bannerStands = new ArrayList<>();
        
        for (int i = 0; i < bannerLines.length; i++) {
            String line = bannerLines[i];
            if (line.trim().isEmpty()) continue;
            
            Location lineLoc = spawnLoc.clone();
            lineLoc.setY(lineLoc.getY() + verticalOffset + startY - (i * lineHeight));
            
            ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(lineLoc, EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setCollidable(false);
            stand.setCustomNameVisible(true);
            stand.setPersistent(true); // Never despawn
            stand.setRemoveWhenFarAway(false); // Don't remove when far away
            stand.setMarker(true);
            stand.setSmall(true);
            
            // Apply gradient colors to the text
            Component gradientText = applyGradient(line, TextColor.color(0xFFE066), TextColor.color(0xFF8C00));
            stand.customName(gradientText);
            
            // Make armor stand always face north (consistent display)
            lineLoc.setYaw(0);
            stand.teleport(lineLoc);
            
            bannerStands.add(stand);
            spawnedBanners.add(stand);
        }
    }
    
    public void resetSpawnCount() {
        bannerSpawnCount = 0;
    }
    
    public void clearBanners(Player player) {
        int removed = 0;
        // Remove all banners in the same world, regardless of distance
        for (ArmorStand stand : new ArrayList<>(spawnedBanners)) {
            if (stand.getWorld().equals(player.getWorld())) {
                stand.remove();
                spawnedBanners.remove(stand);
                removed++;
            }
        }
        player.sendMessage("§e§lPandora §8» §7Removed §6" + removed + " §7banner stands!");
    }
    
    public void clearAllBanners() {
        for (ArmorStand stand : spawnedBanners) {
            stand.remove();
        }
        spawnedBanners.clear();
    }
    
    /**
     * Applies a gradient effect to text using two colors
     */
    private Component applyGradient(String text, TextColor startColor, TextColor endColor) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        
        // Remove any existing color codes for clean gradient
        text = text.replaceAll("§[0-9a-fk-or]", "");
        
        Component result = Component.empty();
        int length = text.length();
        
        if (length == 0) {
            return result;
        }
        
        // Calculate color interpolation for each character
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            double ratio = length > 1 ? (double) i / (length - 1) : 0;
            
            // Interpolate between start and end colors
            int red = (int) (startColor.red() + (endColor.red() - startColor.red()) * ratio);
            int green = (int) (startColor.green() + (endColor.green() - startColor.green()) * ratio);
            int blue = (int) (startColor.blue() + (endColor.blue() - startColor.blue()) * ratio);
            
            TextColor charColor = TextColor.color(red, green, blue);
            result = result.append(Component.text(c).color(charColor).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        }
        
        return result;
    }
}

