package com.playpandora.buygui.managers;

import com.playpandora.buygui.BuyGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class ShopManager {
    
    private final BuyGUI plugin;
    private Object pandoraSpawnersAPI;
    
    public ShopManager(BuyGUI plugin) {
        this.plugin = plugin;
        checkPandoraSpawners();
    }
    
    private void checkPandoraSpawners() {
        if (Bukkit.getPluginManager().getPlugin("PandoraSpawners") == null) {
            plugin.getLogger().info("PandoraSpawners not found. Spawner shop integration disabled.");
            return;
        }
        
        try {
            Class<?> pandoraSpawnersClass = Class.forName("com.pandora.spawners.PandoraSpawners");
            Method getInstanceMethod = pandoraSpawnersClass.getMethod("instance");
            Object pandoraSpawners = getInstanceMethod.invoke(null);
            
            // Try to get API if available
            try {
                Method getAPIMethod = pandoraSpawnersClass.getMethod("getAPI");
                pandoraSpawnersAPI = getAPIMethod.invoke(pandoraSpawners);
            } catch (Exception e) {
                // API method might not exist, that's okay
            }
            
            plugin.getLogger().info("PandoraSpawners integration enabled!");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to integrate with PandoraSpawners: " + e.getMessage());
        }
    }
    
    public boolean openPetShop(Player player) {
        if (Bukkit.getPluginManager().getPlugin("PetPlugin") == null) {
            return false;
        }
        
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pet");
            // Actually, we need to get the plugin instance and open the shop
            org.bukkit.plugin.Plugin petPlugin = Bukkit.getPluginManager().getPlugin("PetPlugin");
            if (petPlugin != null) {
                try {
                    Method getInstanceMethod = petPlugin.getClass().getMethod("getInstance");
                    Object petPluginInstance = getInstanceMethod.invoke(null);
                    Method getShopGUIMethod = petPluginInstance.getClass().getMethod("getShopGUI");
                    Object shopGUI = getShopGUIMethod.invoke(petPluginInstance);
                    Method openShopMethod = shopGUI.getClass().getMethod("openShop", Player.class);
                    openShopMethod.invoke(shopGUI, player);
                    return true;
                } catch (Exception e) {
                    // Fallback to command
                    player.performCommand("pet");
                    return true;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to open pet shop: " + e.getMessage());
        }
        
        return false;
    }
    
    public boolean openCannonShop(Player player) {
        if (Bukkit.getPluginManager().getPlugin("CannonShop") == null) {
            return false;
        }
        
        try {
            org.bukkit.plugin.Plugin cannonShopPlugin = Bukkit.getPluginManager().getPlugin("CannonShop");
            if (cannonShopPlugin != null) {
                try {
                    Method getInstanceMethod = cannonShopPlugin.getClass().getMethod("getInstance");
                    Object cannonShopInstance = getInstanceMethod.invoke(null);
                    Method getCannonManagerMethod = cannonShopInstance.getClass().getMethod("getCannonManager");
                    Object cannonManager = getCannonManagerMethod.invoke(cannonShopInstance);
                    
                    // Get GUI and open it
                    Class<?> cannonShopClass = Class.forName("com.playpandora.cannonshop.CannonShop");
                    // Try to get shop GUI
                    org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("CannonShop");
                    if (plugin != null) {
                        // Use command as fallback
                        player.performCommand("cannonshop");
                        return true;
                    }
                } catch (Exception e) {
                    player.performCommand("cannonshop");
                    return true;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to open cannon shop: " + e.getMessage());
        }
        
        return false;
    }
    
    public boolean openFarmShop(Player player) {
        if (Bukkit.getPluginManager().getPlugin("FarmShop") == null) {
            return false;
        }
        
        try {
            player.performCommand("farmshop");
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to open farm shop: " + e.getMessage());
        }
        
        return false;
    }
    
    public boolean openPerkShop(Player player) {
        if (Bukkit.getPluginManager().getPlugin("PerkShop") == null) {
            return false;
        }
        
        try {
            org.bukkit.plugin.Plugin perkShopPlugin = Bukkit.getPluginManager().getPlugin("PerkShop");
            if (perkShopPlugin != null) {
                try {
                    Class<?> perkShopClass = Class.forName("com.playpandora.perkshop.PerkShop");
                    Method getInstanceMethod = perkShopClass.getMethod("getInstance");
                    Object perkShopInstance = getInstanceMethod.invoke(null);
                    Method getShopGUIMethod = perkShopClass.getMethod("getShopGUI");
                    Object shopGUI = getShopGUIMethod.invoke(perkShopInstance);
                    Method openShopMethod = shopGUI.getClass().getMethod("openShop", Player.class);
                    openShopMethod.invoke(shopGUI, player);
                    return true;
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to open perk shop via reflection: " + e.getMessage());
                    // Fallback to command
                    player.performCommand("shop");
                    return true;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to open perk shop: " + e.getMessage());
        }
        
        return false;
    }
    
    public Object getPandoraSpawnersAPI() {
        return pandoraSpawnersAPI;
    }
}

