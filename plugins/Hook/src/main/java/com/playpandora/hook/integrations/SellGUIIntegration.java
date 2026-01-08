package com.playpandora.hook.integrations;

import com.playpandora.hook.Hook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class SellGUIIntegration implements Listener {
    
    private final Hook plugin;
    
    public SellGUIIntegration(Hook plugin) {
        this.plugin = plugin;
    }
    
    public void hook() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Hook into SellGUI's sell events
        // This would ideally be done through SellGUI's SellManager
    }
    
    /**
     * Called by SellGUI when items are sold
     * This would be called from SellGUI's sell method
     */
    public void onItemsSold(Player player, double amount, int itemsSold) {
        if (!plugin.getConfig().getBoolean("integrations.sellgui.sell-notification", true)) {
            return;
        }
        
        plugin.getAPI().sendSell(player, amount, itemsSold);
    }
}








