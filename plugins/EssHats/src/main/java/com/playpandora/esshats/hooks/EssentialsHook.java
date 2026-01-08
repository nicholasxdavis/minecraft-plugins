package com.playpandora.esshats.hooks;

import com.playpandora.esshats.EssHats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class EssentialsHook {
    
    private final EssHats plugin;
    private Object essentials;
    private boolean essentialsAvailable;
    
    public EssentialsHook(EssHats plugin) {
        this.plugin = plugin;
        setupEssentials();
    }
    
    private void setupEssentials() {
        Plugin essPlugin = Bukkit.getServer().getPluginManager().getPlugin("EssentialsX");
        if (essPlugin == null) {
            essPlugin = Bukkit.getServer().getPluginManager().getPlugin("Essentials");
        }
        if (essPlugin != null && essPlugin.isEnabled()) {
            try {
                Class<?> iEssentialsClass = Class.forName("net.ess3.api.IEssentials");
                if (iEssentialsClass.isInstance(essPlugin)) {
                    essentials = essPlugin;
                    essentialsAvailable = true;
                    plugin.getLogger().info("Successfully hooked into EssentialsX (IEssentials)");
                } else {
                    try {
                        java.lang.reflect.Method getInstanceMethod = essPlugin.getClass().getMethod("getInstance");
                        Object essInstance = getInstanceMethod.invoke(null);
                        if (iEssentialsClass.isInstance(essInstance)) {
                            essentials = essInstance;
                            essentialsAvailable = true;
                            plugin.getLogger().info("Successfully hooked into EssentialsX (via getInstance)");
                        } else {
                            essentials = essPlugin;
                            essentialsAvailable = true;
                            plugin.getLogger().info("Successfully hooked into EssentialsX (direct)");
                        }
                    } catch (Exception e) {
                        essentials = essPlugin;
                        essentialsAvailable = true;
                        plugin.getLogger().info("Successfully hooked into EssentialsX (fallback)");
                    }
                }
            } catch (ClassNotFoundException e) {
                essentials = essPlugin;
                essentialsAvailable = true;
                plugin.getLogger().info("Successfully hooked into EssentialsX (no IEssentials interface)");
            }
        } else {
            plugin.getLogger().warning("EssentialsX plugin not found or not enabled");
            essentialsAvailable = false;
        }
    }
    
    public void recheckEssentials() {
        if (!essentialsAvailable) {
            setupEssentials();
        }
    }
    
    public boolean isEssentialsAvailable() {
        return essentialsAvailable && essentials != null;
    }
    
    /**
     * Sets a hat for the player by directly setting the helmet slot
     * This works the same way EssentialsX /hat command does
     */
    public void setHat(Player player, ItemStack hat) {
        // Directly set the helmet slot (same as EssentialsX /hat command)
        // Clone the hat item to avoid issues
        ItemStack hatClone = hat.clone();
        hatClone.setAmount(1);
        
        // Set the helmet
        player.getInventory().setHelmet(hatClone);
    }
}

