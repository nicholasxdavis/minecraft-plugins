package com.playpandora.craftaccess.listeners;

import com.playpandora.craftaccess.CraftAccess;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceListener implements Listener {
    
    private final CraftAccess plugin;
    
    public BlockPlaceListener(CraftAccess plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Material blockType = event.getBlock().getType();
        String blockName = blockType.name();
        
        // Check if this is a block we track
        String craftBlock = plugin.getConfig().getString("unlock-blocks.craft", "CRAFTING_TABLE");
        String enchantBlock = plugin.getConfig().getString("unlock-blocks.enchant", "ENCHANTING_TABLE");
        String anvilBlock = plugin.getConfig().getString("unlock-blocks.anvil", "ANVIL");
        
        if (blockName.equals(craftBlock)) {
            plugin.getDataManager().unlockBlock(event.getPlayer().getUniqueId(), "craft");
            event.getPlayer().sendMessage("§e§lPandora §7You've unlocked remote access to crafting tables! Use §6/craft §7to open one from anywhere.");
        } else if (blockName.equals(enchantBlock)) {
            plugin.getDataManager().unlockBlock(event.getPlayer().getUniqueId(), "enchant");
            event.getPlayer().sendMessage("§e§lPandora §7You've unlocked remote access to enchantment tables! Use §6/enchant §7to open one from anywhere.");
        } else if (blockName.equals(anvilBlock)) {
            plugin.getDataManager().unlockBlock(event.getPlayer().getUniqueId(), "anvil");
            event.getPlayer().sendMessage("§e§lPandora §7You've unlocked remote access to anvils! Use §6/anvil §7to open one from anywhere.");
        }
    }
}

