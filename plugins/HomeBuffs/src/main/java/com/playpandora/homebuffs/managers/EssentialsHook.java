package com.playpandora.homebuffs.managers;

import com.playpandora.homebuffs.HomeBuffs;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.List;

public class EssentialsHook {
    
    private final HomeBuffs plugin;
    private Object essentials;
    private boolean essentialsAvailable;
    
    public EssentialsHook(HomeBuffs plugin) {
        this.plugin = plugin;
        setupEssentials();
    }
    
    private void setupEssentials() {
        Plugin essPlugin = Bukkit.getServer().getPluginManager().getPlugin("Essentials");
        if (essPlugin != null) {
            try {
                // Check if it implements IEssentials
                Class<?> iEssentialsClass = Class.forName("net.ess3.api.IEssentials");
                if (iEssentialsClass.isInstance(essPlugin)) {
                    essentials = essPlugin;
                    essentialsAvailable = true;
                }
            } catch (ClassNotFoundException e) {
                essentialsAvailable = false;
            }
        } else {
            essentialsAvailable = false;
        }
    }
    
    public boolean isEssentialsAvailable() {
        return essentialsAvailable && essentials != null;
    }
    
    public List<String> getHomes(Player player) {
        if (!isEssentialsAvailable()) {
            return null;
        }
        try {
            // Try different method signatures for getUser
            Method getUserMethod = null;
            try {
                getUserMethod = essentials.getClass().getMethod("getUser", org.bukkit.entity.Player.class);
            } catch (NoSuchMethodException e) {
                try {
                    getUserMethod = essentials.getClass().getMethod("getUser", org.bukkit.OfflinePlayer.class);
                } catch (NoSuchMethodException e2) {
                    getUserMethod = essentials.getClass().getMethod("getUser", java.util.UUID.class);
                }
            }
            
            Object user = null;
            if (getUserMethod != null) {
                Class<?> paramType = getUserMethod.getParameterTypes()[0];
                if (paramType.isAssignableFrom(Player.class)) {
                    user = getUserMethod.invoke(essentials, player);
                } else if (paramType.isAssignableFrom(org.bukkit.OfflinePlayer.class)) {
                    user = getUserMethod.invoke(essentials, (org.bukkit.OfflinePlayer) player);
                } else if (paramType == java.util.UUID.class) {
                    user = getUserMethod.invoke(essentials, player.getUniqueId());
                }
            }
            
            if (user == null) {
                return null;
            }
            
            Method getHomesMethod = user.getClass().getMethod("getHomes");
            Object homesResult = getHomesMethod.invoke(user);
            if (homesResult instanceof List) {
                return (List<String>) homesResult;
            } else if (homesResult instanceof java.util.Collection) {
                return new java.util.ArrayList<>((java.util.Collection<String>) homesResult);
            }
            return null;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get homes: " + e.getMessage());
            return null;
        }
    }
    
    public Location getHomeLocation(Player player, String homeName) {
        if (!isEssentialsAvailable()) {
            return null;
        }
        try {
            // Try different method signatures for getUser
            Method getUserMethod = null;
            try {
                getUserMethod = essentials.getClass().getMethod("getUser", org.bukkit.entity.Player.class);
            } catch (NoSuchMethodException e) {
                try {
                    getUserMethod = essentials.getClass().getMethod("getUser", org.bukkit.OfflinePlayer.class);
                } catch (NoSuchMethodException e2) {
                    getUserMethod = essentials.getClass().getMethod("getUser", java.util.UUID.class);
                }
            }
            
            Object user = null;
            if (getUserMethod != null) {
                Class<?> paramType = getUserMethod.getParameterTypes()[0];
                if (paramType.isAssignableFrom(Player.class)) {
                    user = getUserMethod.invoke(essentials, player);
                } else if (paramType.isAssignableFrom(org.bukkit.OfflinePlayer.class)) {
                    user = getUserMethod.invoke(essentials, (org.bukkit.OfflinePlayer) player);
                } else if (paramType == java.util.UUID.class) {
                    user = getUserMethod.invoke(essentials, player.getUniqueId());
                }
            }
            
            if (user == null) {
                return null;
            }
            
            // Get home location
            Method getHomeMethod = user.getClass().getMethod("getHome", String.class);
            Object homeLocation = getHomeMethod.invoke(user, homeName);
            if (homeLocation instanceof Location) {
                return (Location) homeLocation;
            }
            return null;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get home location: " + e.getMessage());
            return null;
        }
    }
}


