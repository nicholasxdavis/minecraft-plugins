package com.playpandora.hook.integrations;

import com.playpandora.hook.Hook;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class MoreNetherIntegration implements Listener {
    
    private final Hook plugin;
    
    public MoreNetherIntegration(Hook plugin) {
        this.plugin = plugin;
    }
    
    public void hook() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler
    public void onPlayerEnterNether(PlayerChangedWorldEvent event) {
        if (!plugin.getConfig().getBoolean("integrations.morenether.dimension-entry-notification", true)) {
            return;
        }
        
        World newWorld = event.getPlayer().getWorld();
        if (newWorld.getEnvironment() == World.Environment.NETHER) {
            plugin.getAPI().sendCustom(event.getPlayer(), "Welcome to the Nether!", 
                "Beware the dangers that await...", 500, 3000, 1000);
        }
    }
    
    @EventHandler
    public void onRareMobKill(EntityDeathEvent event) {
        if (!plugin.getConfig().getBoolean("integrations.morenether.rare-mob-kill-notification", true)) {
            return;
        }
        
        if (!(event.getEntity().getKiller() instanceof Player)) {
            return;
        }
        
        Player killer = event.getEntity().getKiller();
        if (killer.getWorld().getEnvironment() != World.Environment.NETHER) {
            return;
        }
        
        org.bukkit.entity.EntityType type = event.getEntityType();
        if (type == org.bukkit.entity.EntityType.WITHER_SKELETON || 
            type == org.bukkit.entity.EntityType.GHAST ||
            type == org.bukkit.entity.EntityType.BLAZE) {
            plugin.getAPI().sendCustom(killer, "Rare Kill!", 
                "You defeated a " + type.name().toLowerCase().replace("_", " "), 
                500, 2500, 1000);
        }
    }
}




