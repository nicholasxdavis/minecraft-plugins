package com.pandora.ceggs;

import com.pandora.ceggs.commands.CEggsCommand;
import com.pandora.ceggs.listeners.CEggsListener;
import org.bukkit.plugin.java.JavaPlugin;

public class PandoraCeggs extends JavaPlugin {
    
    private static PandoraCeggs instance;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Register command
        getCommand("ceggs").setExecutor(new CEggsCommand());
        getCommand("ceggs").setTabCompleter(new CEggsCommand());
        
        // Register listener
        getServer().getPluginManager().registerEvents(new CEggsListener(), this);
        
        getLogger().info("PandoraCeggs has been enabled!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("PandoraCeggs has been disabled!");
    }
    
    public static PandoraCeggs getInstance() {
        return instance;
    }
}

