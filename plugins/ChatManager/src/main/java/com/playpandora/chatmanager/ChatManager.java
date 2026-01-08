package com.playpandora.chatmanager;

import com.playpandora.chatmanager.commands.ClearChatCommand;
import com.playpandora.chatmanager.listeners.ChatListener;
import org.bukkit.plugin.java.JavaPlugin;

public class ChatManager extends JavaPlugin {
    
    private static ChatManager instance;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Save default config
            saveDefaultConfig();
            
            // Validate configuration
            validateConfig();
            
            // Register chat listener
            ChatListener chatListener = new ChatListener(this);
            getServer().getPluginManager().registerEvents(chatListener, this);
            
            // Register commands with null checks
            if (getCommand("clearchat") != null) {
                getCommand("clearchat").setExecutor(new ClearChatCommand(this));
            } else {
                getLogger().warning("Command 'clearchat' not found in plugin.yml!");
            }
            
            // Check for PlaceholderAPI
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                getLogger().info("PlaceholderAPI found! Placeholder support enabled.");
            } else {
                getLogger().warning("PlaceholderAPI not found! Placeholder support disabled.");
            }
            
            // Check for Essentials
            if (getServer().getPluginManager().getPlugin("Essentials") != null) {
                getLogger().info("Essentials found! Essentials placeholder support enabled.");
            }
            
            getLogger().info("ChatManager v" + getDescription().getVersion() + " has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable ChatManager! " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    private void validateConfig() {
        if (!getConfig().contains("format")) {
            getLogger().warning("Missing 'format' in config.yml, using default format.");
        }
        if (!getConfig().contains("anti-spam.delay-ms")) {
            getLogger().warning("Missing 'anti-spam.delay-ms' in config.yml, using default: 1000ms");
        }
    }
    
    @Override
    public void onDisable() {
        try {
            getLogger().info("ChatManager has been disabled!");
        } catch (Exception e) {
            getLogger().severe("Error during shutdown: " + e.getMessage());
        }
    }
    
    public static ChatManager getInstance() {
        return instance;
    }
}


