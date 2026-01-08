package com.pandora.events.integration;

import com.pandora.events.PandoraEventsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Integration with Hook plugin for event notifications
 */
public class HookIntegration {
    
    private final PandoraEventsPlugin plugin;
    private Object hookAPI;
    private boolean hookAvailable;
    
    public HookIntegration(PandoraEventsPlugin plugin) {
        this.plugin = plugin;
        setupHook();
    }
    
    private void setupHook() {
        Plugin hookPlugin = Bukkit.getPluginManager().getPlugin("Hook");
        if (hookPlugin == null) {
            hookAvailable = false;
            plugin.getLogger().info("Hook plugin not found! Event notifications disabled.");
            return;
        }
        
        try {
            // Get Hook instance using getInstance() static method
            Method getInstanceMethod = hookPlugin.getClass().getMethod("getInstance");
            Object hookInstance = getInstanceMethod.invoke(null);
            
            if (hookInstance != null) {
                // Get API using getAPI() method
                Method getAPIMethod = hookInstance.getClass().getMethod("getAPI");
                hookAPI = getAPIMethod.invoke(hookInstance);
                
                if (hookAPI != null) {
                    hookAvailable = true;
                    plugin.getLogger().info("Successfully connected to Hook plugin! Event notifications enabled.");
                } else {
                    hookAvailable = false;
                    plugin.getLogger().warning("Hook API is null! Event notifications disabled.");
                }
            } else {
                hookAvailable = false;
                plugin.getLogger().warning("Hook instance is null! Event notifications disabled.");
            }
        } catch (Exception e) {
            hookAvailable = false;
            plugin.getLogger().warning("Error setting up Hook integration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean isHookAvailable() {
        return hookAvailable && hookAPI != null;
    }
    
    /**
     * Send event spawn notification via Hook
     */
    public void sendEventSpawn(Player player, String worldName, String coords) {
        if (!isHookAvailable()) {
            return;
        }
        
        try {
            String title = com.pandora.events.util.PandoraMessage.highlight("Event Spawned!");
            String subtitle = com.pandora.events.util.PandoraMessage.text("In " + worldName + " at " + coords);
            
            Method sendTitleMethod = hookAPI.getClass().getMethod("sendTitle", Player.class, String.class, String.class);
            sendTitleMethod.invoke(hookAPI, player, title, subtitle);
        } catch (Exception e) {
            plugin.getLogger().warning("Error sending event spawn notification via Hook: " + e.getMessage());
        }
    }
    
    /**
     * Send event win notification via Hook
     */
    public void sendEventWin(Player player, String message) {
        if (!isHookAvailable()) {
            return;
        }
        
        try {
            String title = com.pandora.events.util.PandoraMessage.highlight("Event Won!");
            String subtitle = com.pandora.events.util.PandoraMessage.text(message);
            
            Method sendTitleMethod = hookAPI.getClass().getMethod("sendTitle", Player.class, String.class, String.class);
            sendTitleMethod.invoke(hookAPI, player, title, subtitle);
        } catch (Exception e) {
            plugin.getLogger().warning("Error sending event win notification via Hook: " + e.getMessage());
        }
    }
    
    /**
     * Broadcast event notification to all players
     */
    public void broadcastEvent(String title, String subtitle) {
        if (!isHookAvailable()) {
            return;
        }
        
        try {
            Method broadcastTitleMethod = hookAPI.getClass().getMethod("broadcastTitle", String.class, String.class);
            broadcastTitleMethod.invoke(hookAPI, title, subtitle);
        } catch (Exception e) {
            plugin.getLogger().warning("Error broadcasting event notification via Hook: " + e.getMessage());
        }
    }
}

