package com.massivecraft.factions.tasks;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.util.CombatTagManager;
import com.massivecraft.factions.util.PandoraMessage;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Periodic task to update combat tag countdown and cleanup expired tags
 */
public class CombatTagTask extends org.bukkit.scheduler.BukkitRunnable {

    @Override
    public void run() {
        if (!Conf.combatTagEnabled) {
            return;
        }

        CombatTagManager manager = CombatTagManager.getInstance();
        
        // Cleanup expired tags
        manager.cleanupExpiredTags();
        
        // Update action bar for tagged players
        if (Conf.combatTagShowActionBar) {
            for (Player player : FactionsPlugin.getInstance().getServer().getOnlinePlayers()) {
                if (manager.isTagged(player)) {
                    int remaining = manager.getRemainingSeconds(player);
                    
                    // Show countdown in action bar
                    String color = remaining > 10 ? ChatColor.GREEN.toString() : 
                                  remaining > 5 ? ChatColor.YELLOW.toString() : 
                                  ChatColor.RED.toString();
                    String message = PandoraMessage.error("⚔ COMBAT: ") + color + remaining + "s";
                    
                    try {
                        String parsedMessage = com.massivecraft.factions.zcore.util.TextUtil.parse(message);
                        net.kyori.adventure.text.Component component = 
                            net.kyori.adventure.text.Component.text(parsedMessage);
                        if (com.massivecraft.factions.zcore.util.TextUtil.AUDIENCES != null) {
                            com.massivecraft.factions.zcore.util.TextUtil.AUDIENCES.player(player)
                                    .sendActionBar(component);
                        }
                    } catch (Exception e) {
                        // Fallback to regular message if action bar fails
                        player.sendMessage(message);
                    }
                }
            }
        }
    }
}

