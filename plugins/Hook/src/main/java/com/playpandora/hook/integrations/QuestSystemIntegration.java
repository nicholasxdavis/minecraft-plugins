package com.playpandora.hook.integrations;

import com.playpandora.hook.Hook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class QuestSystemIntegration implements Listener {
    
    private final Hook plugin;
    
    public QuestSystemIntegration(Hook plugin) {
        this.plugin = plugin;
    }
    
    public void hook() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Called when a quest is completed (from qab.sk)
     * This should be called from Skript or quest system
     */
    public void onQuestComplete(Player player, String questType, double reward, double xp) {
        if (!plugin.getConfig().getBoolean("integrations.quest-system.completion-notification", true)) {
            return;
        }
        
        String formattedReward = String.format("%.2f", reward);
        plugin.getAPI().sendCustom(player, "Quest Complete!", 
            questType + " quest finished! +$" + formattedReward + " & +" + (int)xp + " XP", 
            500, 4000, 1000);
    }
    
    /**
     * Called when quest progress reaches a milestone
     */
    public void onQuestProgress(Player player, int progress, int goal, String questType) {
        if (!plugin.getConfig().getBoolean("integrations.quest-system.progress-notification", true)) {
            return;
        }
        
        // Only show at milestones (25%, 50%, 75%, 100%)
        double percent = (double) progress / goal;
        if (percent >= 0.25 && percent < 0.3 || 
            percent >= 0.5 && percent < 0.55 ||
            percent >= 0.75 && percent < 0.8 ||
            percent >= 1.0) {
            
            int percentInt = (int)(percent * 100);
            plugin.getAPI().sendCustom(player, "Quest Progress", 
                questType + ": " + progress + "/" + goal + " (" + percentInt + "%)", 
                300, 2000, 800);
        }
    }
}




