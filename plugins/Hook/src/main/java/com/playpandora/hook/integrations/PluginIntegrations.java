package com.playpandora.hook.integrations;

import com.playpandora.hook.Hook;
import org.bukkit.Bukkit;

public class PluginIntegrations {
    
    private final Hook plugin;
    private LevelPluginIntegration levelPlugin;
    private PerkShopIntegration perkShop;
    private SellGUIIntegration sellGUI;
    private MoreWeatherIntegration moreWeather;
    private BaseSystemIntegration baseSystem;
    private PetPluginIntegration petPlugin;
    private FarmShopIntegration farmShop;
    private TreasureHuntIntegration treasureHunt;
    private MoreNetherIntegration moreNether;
    private MoreEndIntegration moreEnd;
    private MoreCavesIntegration moreCaves;
    private QuestSystemIntegration questSystem;
    private EssentialsGUIIntegration essentialsGUI;
    private BountyIntegration bounty;
    private CombatLogXIntegration combatLogX;
    private PandoraMinerIntegration pandoraMiner;
    
    public PluginIntegrations(Hook plugin) {
        this.plugin = plugin;
    }
    
    public void initializeAll() {
        // LevelPlugin integration
        if (Bukkit.getPluginManager().getPlugin("LevelPlugin") != null) {
            levelPlugin = new LevelPluginIntegration(plugin);
            levelPlugin.hook();
            plugin.getLogger().info("LevelPlugin integration enabled!");
        }
        
        // PerkShop integration
        if (Bukkit.getPluginManager().getPlugin("PerkShop") != null) {
            perkShop = new PerkShopIntegration(plugin);
            perkShop.hook();
            plugin.getLogger().info("PerkShop integration enabled!");
        }
        
        // SellGUI integration
        if (Bukkit.getPluginManager().getPlugin("SellGUI") != null) {
            sellGUI = new SellGUIIntegration(plugin);
            sellGUI.hook();
            plugin.getLogger().info("SellGUI integration enabled!");
        }
        
        // MoreWeather integration
        if (Bukkit.getPluginManager().getPlugin("MoreWeather") != null) {
            moreWeather = new MoreWeatherIntegration(plugin);
            moreWeather.hook();
            plugin.getLogger().info("MoreWeather integration enabled!");
        }
        
        // BaseSystem integration
        if (Bukkit.getPluginManager().getPlugin("BaseSystem") != null) {
            baseSystem = new BaseSystemIntegration(plugin);
            baseSystem.hook();
            plugin.getLogger().info("BaseSystem integration enabled!");
        }
        
        // PetPlugin integration
        if (Bukkit.getPluginManager().getPlugin("PetPlugin") != null) {
            petPlugin = new PetPluginIntegration(plugin);
            petPlugin.hook();
            plugin.getLogger().info("PetPlugin integration enabled!");
        }
        
        // FarmShop integration
        if (Bukkit.getPluginManager().getPlugin("FarmShop") != null) {
            farmShop = new FarmShopIntegration(plugin);
            farmShop.hook();
            plugin.getLogger().info("FarmShop integration enabled!");
        }
        
        // TreasureHunt integration
        if (Bukkit.getPluginManager().getPlugin("TreasureHunt") != null) {
            treasureHunt = new TreasureHuntIntegration(plugin);
            treasureHunt.hook();
            plugin.getLogger().info("TreasureHunt integration enabled!");
        }
        
        // MoreNether integration
        if (Bukkit.getPluginManager().getPlugin("MoreNether") != null) {
            moreNether = new MoreNetherIntegration(plugin);
            moreNether.hook();
            plugin.getLogger().info("MoreNether integration enabled!");
        }
        
        // MoreEnd integration
        if (Bukkit.getPluginManager().getPlugin("MoreEnd") != null) {
            moreEnd = new MoreEndIntegration(plugin);
            moreEnd.hook();
            plugin.getLogger().info("MoreEnd integration enabled!");
        }
        
        // MoreCaves integration
        if (Bukkit.getPluginManager().getPlugin("MoreCaves") != null) {
            moreCaves = new MoreCavesIntegration(plugin);
            moreCaves.hook();
            plugin.getLogger().info("MoreCaves integration enabled!");
        }
        
        // Quest System integration (qab.sk)
        if (Bukkit.getPluginManager().getPlugin("Skript") != null) {
            questSystem = new QuestSystemIntegration(plugin);
            questSystem.hook();
            plugin.getLogger().info("Quest System integration enabled!");
        }
        
        // EssentialsGUI integration
        if (Bukkit.getPluginManager().getPlugin("EssentialsGUI") != null) {
            essentialsGUI = new EssentialsGUIIntegration(plugin);
            essentialsGUI.hook();
            plugin.getLogger().info("EssentialsGUI integration enabled!");
        }
        
        // Bounty system integration (qab.sk)
        if (Bukkit.getPluginManager().getPlugin("Skript") != null) {
            bounty = new BountyIntegration(plugin);
            bounty.hook();
            plugin.getLogger().info("Bounty system integration enabled!");
        }
        
        // CombatLogX integration
        if (Bukkit.getPluginManager().getPlugin("CombatLogX") != null) {
            combatLogX = new CombatLogXIntegration(plugin);
            combatLogX.hook();
            plugin.getLogger().info("CombatLogX integration enabled!");
        }
        
        // PandoraMiner integration
        if (Bukkit.getPluginManager().getPlugin("PandoraMiner") != null) {
            pandoraMiner = new PandoraMinerIntegration(plugin);
            pandoraMiner.hook();
            plugin.getLogger().info("PandoraMiner integration enabled!");
        }
    }
    
    public void shutdownAll() {
        // Cleanup if needed
    }
    
    public PandoraMinerIntegration getPandoraMinerIntegration() {
        return pandoraMiner;
    }
}




