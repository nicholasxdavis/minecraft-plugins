package com.massivecraft.factions.listeners;

import com.massivecraft.factions.util.BaseChestManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class BaseChestListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();

        if (BaseChestManager.isBaseChest(inventory)) {
            BaseChestManager.saveChest(player, inventory);
        }
    }
}





