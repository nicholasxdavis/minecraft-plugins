package com.playpandora.pandorabanners;

import com.playpandora.pandorabanners.commands.AsciiCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class PandoraBanners extends JavaPlugin {
    
    private static PandoraBanners instance;
    private BannerParser bannerParser;
    private BannerManager bannerManager;
    private String bannerFilePath;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Get banner file path from config or use default
        saveDefaultConfig();
        bannerFilePath = getConfig().getString("banner-file-path", 
            "C:\\Users\\lifa2\\Downloads\\pandora_banners.md");
        
        // Initialize banner parser
        bannerParser = new BannerParser(bannerFilePath);
        
        if (!bannerParser.loadBanners()) {
            getLogger().warning("Failed to load banners from: " + bannerFilePath);
            getLogger().warning("Please check the file path in config.yml");
        } else {
            getLogger().info("Loaded " + bannerParser.getAllBanners().size() + " banners!");
            for (String direction : bannerParser.getAllBanners().keySet()) {
                getLogger().info("  - " + direction);
            }
        }
        
        // Initialize banner manager
        bannerManager = new BannerManager(this);
        
        // Register command
        AsciiCommand asciiCommand = new AsciiCommand(this);
        getCommand("ascii").setExecutor(asciiCommand);
        getCommand("ascii").setTabCompleter(asciiCommand);
        
        getLogger().info("PandoraBanners v" + getDescription().getVersion() + " has been enabled!");
    }
    
    @Override
    public void onDisable() {
        if (bannerManager != null) {
            bannerManager.clearAllBanners();
        }
        getLogger().info("PandoraBanners has been disabled!");
    }
    
    public static PandoraBanners getInstance() {
        return instance;
    }
    
    public BannerParser getBannerParser() {
        return bannerParser;
    }
    
    public BannerManager getBannerManager() {
        return bannerManager;
    }
    
    public String getBannerFilePath() {
        return bannerFilePath;
    }
}

