package com.playpandora.nonerfs;

import org.bukkit.plugin.java.JavaPlugin;

public class NoNerfs extends JavaPlugin {
    
    private static NoNerfs instance;
    private EffectRemover effectRemover;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        // Start effect remover task
        effectRemover = new EffectRemover(this);
        effectRemover.start();
        
        getLogger().info("NoNerfs v" + getDescription().getVersion() + " has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Stop effect remover task
        if (effectRemover != null) {
            effectRemover.stop();
        }
        
        getLogger().info("NoNerfs has been disabled!");
    }
    
    public static NoNerfs getInstance() {
        return instance;
    }
}

