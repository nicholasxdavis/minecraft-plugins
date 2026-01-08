package com.playpandora.hook.integrations;

import com.playpandora.hook.Hook;
import org.bukkit.entity.Player;

public class FarmShopIntegration {
    
    private final Hook plugin;
    
    public FarmShopIntegration(Hook plugin) {
        this.plugin = plugin;
    }
    
    public void hook() {
        // Hook into FarmShop events
    }
    
    /**
     * Called when a farm is purchased
     */
    public void onFarmPurchase(Player player, String farmType) {
        if (!plugin.getConfig().getBoolean("integrations.farmshop.purchase-notification", true)) {
            return;
        }
        
        plugin.getAPI().sendCustom(player, "Farm Purchased!", 
            "You bought: " + farmType, 500, 3000, 1000);
    }
}








