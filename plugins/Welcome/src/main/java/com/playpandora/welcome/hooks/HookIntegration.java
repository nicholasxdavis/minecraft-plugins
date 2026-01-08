package com.playpandora.welcome.hooks;

import com.playpandora.welcome.Welcome;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class HookIntegration {
    
    private final Welcome plugin;
    private Object hookAPI;
    private boolean hookAvailable;
    private Method sendWelcomeMethod;
    
    public HookIntegration(Welcome plugin) {
        this.plugin = plugin;
        initializeHook();
    }
    
    private void initializeHook() {
        if (!plugin.getConfig().getBoolean("hook.enabled", true)) {
            hookAvailable = false;
            return;
        }
        
        org.bukkit.plugin.Plugin hookPlugin = Bukkit.getPluginManager().getPlugin("Hook");
        
        if (hookPlugin == null) {
            hookAvailable = false;
            plugin.getLogger().info("Hook plugin not found. Welcome titles disabled.");
            return;
        }
        
        try {
            // Use reflection to get the API
            Method getAPIMethod = hookPlugin.getClass().getMethod("getAPI");
            hookAPI = getAPIMethod.invoke(hookPlugin);
            
            if (hookAPI == null) {
                hookAvailable = false;
                return;
            }
            
            // Get the sendWelcome method
            sendWelcomeMethod = hookAPI.getClass().getMethod("sendWelcome", Player.class, boolean.class);
            hookAvailable = true;
            
            plugin.getLogger().info("Successfully hooked into Hook plugin!");
        } catch (Exception e) {
            hookAvailable = false;
            plugin.getLogger().info("Failed to hook into Hook plugin: " + e.getMessage());
        }
    }
    
    public boolean isHookAvailable() {
        return hookAvailable;
    }
    
    public void sendWelcome(Player player, boolean isFirstJoin) {
        if (!hookAvailable || hookAPI == null || sendWelcomeMethod == null) {
            return;
        }
        
        try {
            sendWelcomeMethod.invoke(hookAPI, player, isFirstJoin);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send welcome via Hook: " + e.getMessage());
        }
    }
}

