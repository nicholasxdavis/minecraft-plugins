package com.playpandora.hook.integrations;

import com.github.sirblobman.combatlogx.api.event.PlayerTagEvent;
import com.github.sirblobman.combatlogx.api.event.PlayerUntagEvent;
import com.playpandora.hook.Hook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class CombatLogXIntegration implements Listener {
    
    private final Hook plugin;
    private boolean hooked = false;
    
    public CombatLogXIntegration(Hook plugin) {
        this.plugin = plugin;
    }
    
    public void hook() {
        org.bukkit.plugin.Plugin combatLogX = Bukkit.getPluginManager().getPlugin("CombatLogX");
        
        if (combatLogX == null || !combatLogX.isEnabled()) {
            return;
        }
        
        // Register this as a listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
        hooked = true;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTag(PlayerTagEvent event) {
        if (!hooked) {
            return;
        }
        
        if (!plugin.getConfig().getBoolean("integrations.combatlogx.enabled", true)) {
            return;
        }
        
        Player player = event.getPlayer();
        Entity enemy = event.getEnemy();
        
        // Calculate time left
        long endTime = event.getEndTime();
        long currentTime = System.currentTimeMillis();
        long timeLeft = Math.max(0, (endTime - currentTime) / 1000); // Convert to seconds
        
        // Build subtitle with enemy name and time
        String subtitle = buildCombatSubtitle(enemy, timeLeft);
        
        // Send title notification
        // Title will be colored gold by NotificationManager
        plugin.getAPI().sendCustom(player, 
            "COMBAT!", 
            subtitle,
            500,  // fade in
            3000, // stay
            1000  // fade out
        );
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerUntag(PlayerUntagEvent event) {
        if (!hooked) {
            return;
        }
        
        if (!plugin.getConfig().getBoolean("integrations.combatlogx.enabled", true)) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Get untag reason for subtitle
        String reason = getUntagReasonMessage(event.getUntagReason());
        
        // Send title notification
        // Title will be colored gold by NotificationManager
        plugin.getAPI().sendCustom(player,
            "COMBAT ENDED",
            reason,
            500,  // fade in
            2500, // stay
            1000  // fade out
        );
    }
    
    private String buildCombatSubtitle(Entity enemy, long timeLeft) {
        StringBuilder subtitle = new StringBuilder();
        
        if (enemy != null) {
            String enemyName = enemy instanceof Player 
                ? ((Player) enemy).getName() 
                : enemy.getType().name();
            subtitle.append("Fighting: ").append(enemyName);
        } else {
            subtitle.append("In combat");
        }
        
        subtitle.append(" | Time: ").append(timeLeft).append("s");
        
        return subtitle.toString();
    }
    
    private String getUntagReasonMessage(com.github.sirblobman.combatlogx.api.object.UntagReason reason) {
        switch (reason.name()) {
            case "EXPIRE":
                return "Timer expired";
            case "ENEMY_DEATH":
                return "Enemy defeated";
            case "SELF_DEATH":
                return "You died";
            case "QUIT":
                return "You logged out";
            case "FORGIVE":
                return "Forgiven";
            case "COMMAND":
                return "Removed by command";
            default:
                return "Combat ended";
        }
    }
}

