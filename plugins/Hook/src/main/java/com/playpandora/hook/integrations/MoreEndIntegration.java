package com.playpandora.hook.integrations;

import com.playpandora.hook.Hook;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class MoreEndIntegration implements Listener {
    
    private final Hook plugin;
    
    public MoreEndIntegration(Hook plugin) {
        this.plugin = plugin;
    }
    
    public void hook() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler
    public void onPlayerEnterEnd(PlayerChangedWorldEvent event) {
        if (!plugin.getConfig().getBoolean("integrations.moreend.dimension-entry-notification", true)) {
            return;
        }
        
        World newWorld = event.getPlayer().getWorld();
        if (newWorld.getEnvironment() == World.Environment.THE_END) {
            plugin.getAPI().sendCustom(event.getPlayer(), "Welcome to the End!", 
                "The void calls...", 500, 3000, 1000);
        }
    }
    
    @EventHandler
    public void onRareMobKill(EntityDeathEvent event) {
        if (!plugin.getConfig().getBoolean("integrations.moreend.rare-mob-kill-notification", true)) {
            return;
        }
        
        if (!(event.getEntity().getKiller() instanceof Player)) {
            return;
        }
        
        Player killer = event.getEntity().getKiller();
        if (killer.getWorld().getEnvironment() != World.Environment.THE_END) {
            return;
        }
        
        org.bukkit.entity.EntityType type = event.getEntityType();
        if (type == org.bukkit.entity.EntityType.SHULKER || 
            type == org.bukkit.entity.EntityType.ENDER_DRAGON) {
            plugin.getAPI().sendCustom(killer, "Epic Kill!", 
                "You defeated a " + type.name().toLowerCase().replace("_", " "), 
                500, 3000, 1000);
        }
    }
}




