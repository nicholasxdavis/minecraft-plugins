package com.playpandora.esshats.listeners;

import com.playpandora.esshats.EssHats;
import com.playpandora.esshats.gui.HatGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {
    
    private final EssHats plugin;
    
    public GUIListener(EssHats plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        String title = event.getView().getTitle();
        
        // Check if it's the hat GUI
        if (title.equals(plugin.getConfigManager().getGUITitle())) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) {
                return;
            }
            
            ItemStack clickedItem = event.getCurrentItem();
            
            // Check for close button
            if (clickedItem.getType() == plugin.getConfigManager().getCloseButtonMaterial()) {
                float[] soundParams = plugin.getConfigManager().getSoundButtonClickParams();
                player.playSound(player.getLocation(), plugin.getConfigManager().getSoundButtonClick(), 
                    soundParams[0], soundParams[1]);
                player.closeInventory();
                return;
            }
            
            // Check for border
            if (clickedItem.getType() == plugin.getConfigManager().getBorderMaterial()) {
                float[] soundParams = plugin.getConfigManager().getSoundButtonClickParams();
                player.playSound(player.getLocation(), plugin.getConfigManager().getSoundButtonClick(), 
                    soundParams[0] * 0.6f, soundParams[1] * 0.8f);
                return;
            }
            
            HatGUI hatGUI = plugin.getHatGUI();
            HatGUI.HatItem hat = hatGUI.getHatByItem(clickedItem);
            
            if (hat != null) {
                // Check permissions for this specific tier
                // Extract just the letter from "S-Tier", "A-Tier", etc.
                String tier = hat.getTier().substring(0, 1).toLowerCase();
                boolean hasPermission = player.hasPermission("esshats.all") || 
                                       player.hasPermission("esshats.tier." + tier);
                
                if (!hasPermission) {
                    player.sendMessage(plugin.getConfigManager().getMessage("no-tier-permission", "tier", hat.getTier()));
                    float[] soundParams = plugin.getConfigManager().getSoundPermissionDeniedParams();
                    player.playSound(player.getLocation(), plugin.getConfigManager().getSoundPermissionDenied(), 
                        soundParams[0], soundParams[1]);
                    return;
                }
                
                // Create the hat item
                ItemStack hatItem = new ItemStack(hat.getMaterial());
                
                // Set the hat using Essentials hook (which directly sets helmet slot)
                plugin.getEssentialsHook().setHat(player, hatItem);
                player.sendMessage(plugin.getConfigManager().getMessage("hat-equipped", "hat", hat.getDisplayName()));
                
                // Play sound effect
                float[] soundParams = plugin.getConfigManager().getSoundHatEquipParams();
                player.playSound(player.getLocation(), plugin.getConfigManager().getSoundHatEquip(), 
                    soundParams[0], soundParams[1]);
                
                player.closeInventory();
            }
        }
    }
}

