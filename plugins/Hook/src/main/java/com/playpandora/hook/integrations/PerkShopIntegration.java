package com.playpandora.hook.integrations;

import com.playpandora.hook.Hook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class PerkShopIntegration implements Listener {
    
    private final Hook plugin;
    
    public PerkShopIntegration(Hook plugin) {
        this.plugin = plugin;
    }
    
    public void hook() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Also hook into PerkShop's purchase manager if it exposes events
        // For now, we'll monitor inventory clicks in the perk shop GUI
    }
    
    @EventHandler
    public void onPerkPurchase(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Check if it's the perk shop GUI
        if (title != null && title.contains("Perk Shop")) {
            // This is a simplified check - actual implementation would need
            // to hook into PerkShop's PurchaseManager directly
            // For now, we can check if the player successfully purchased something
            // by monitoring their inventory or economy balance changes
        }
    }
    
    /**
     * Called by PerkShop when a purchase is made
     * This would be called from PerkShop's PurchaseManager
     */
    public void onPerkPurchased(Player player, String perkName) {
        if (!plugin.getConfig().getBoolean("integrations.perkshop.purchase-notification", true)) {
            return;
        }
        
        plugin.getAPI().sendPerkPurchase(player, perkName);
    }
}








