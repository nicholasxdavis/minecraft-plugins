package com.playpandora.essentialsgui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

public class EssentialsHook {
    
    private final EssentialsGUI plugin;
    private Object essentials;
    private boolean essentialsAvailable;
    
    public EssentialsHook(EssentialsGUI plugin) {
        this.plugin = plugin;
        setupEssentials();
    }
    
    private void setupEssentials() {
        // Try EssentialsX first (it's the modern version)
        Plugin essPlugin = Bukkit.getServer().getPluginManager().getPlugin("EssentialsX");
        if (essPlugin == null) {
            // Fallback to Essentials
            essPlugin = Bukkit.getServer().getPluginManager().getPlugin("Essentials");
        }
        if (essPlugin != null && essPlugin.isEnabled()) {
            try {
                // Try to get IEssentials interface
                Class<?> iEssentialsClass = Class.forName("net.ess3.api.IEssentials");
                if (iEssentialsClass.isInstance(essPlugin)) {
                    essentials = essPlugin;
                    essentialsAvailable = true;
                    plugin.getLogger().info("Successfully hooked into Essentials plugin (IEssentials)");
                } else {
                    // Try to get Essentials instance via getInstance() method
                    try {
                        Method getInstanceMethod = essPlugin.getClass().getMethod("getInstance");
                        Object essInstance = getInstanceMethod.invoke(null);
                        if (iEssentialsClass.isInstance(essInstance)) {
                            essentials = essInstance;
                            essentialsAvailable = true;
                            plugin.getLogger().info("Successfully hooked into Essentials plugin (via getInstance)");
                        } else {
                            // Use the plugin directly anyway
                            essentials = essPlugin;
                            essentialsAvailable = true;
                            plugin.getLogger().info("Successfully hooked into Essentials plugin (direct)");
                        }
                    } catch (Exception e) {
                        // Use the plugin directly
                        essentials = essPlugin;
                        essentialsAvailable = true;
                        plugin.getLogger().info("Successfully hooked into Essentials plugin (fallback)");
                    }
                }
            } catch (ClassNotFoundException e) {
                // Try without IEssentials interface
                essentials = essPlugin;
                essentialsAvailable = true;
                plugin.getLogger().info("Successfully hooked into Essentials plugin (no IEssentials interface)");
            }
        } else {
            plugin.getLogger().warning("Essentials plugin not found or not enabled");
            essentialsAvailable = false;
        }
    }
    
    /**
     * Re-check Essentials availability (useful if Essentials loads after this plugin)
     */
    public void recheckEssentials() {
        if (!essentialsAvailable) {
            setupEssentials();
        }
    }
    
    public boolean isEssentialsAvailable() {
        return essentialsAvailable && essentials != null;
    }
    
    public java.util.Collection<String> getWarps() {
        if (!isEssentialsAvailable()) {
            plugin.getLogger().warning("Essentials not available when trying to get warps");
            return new java.util.ArrayList<>();
        }
        try {
            // Try multiple methods to get warps
            Method getWarpsMethod = null;
            try {
                getWarpsMethod = essentials.getClass().getMethod("getWarps");
            } catch (NoSuchMethodException e) {
                // Try alternative method name
                try {
                    getWarpsMethod = essentials.getClass().getMethod("getWarpList");
                } catch (NoSuchMethodException e2) {
                    plugin.getLogger().warning("Could not find getWarps or getWarpList method");
                    return new java.util.ArrayList<>();
                }
            }
            
            Object warps = getWarpsMethod.invoke(essentials);
            if (warps == null) {
                plugin.getLogger().info("getWarps() returned null (no warps configured)");
                return new java.util.ArrayList<>();
            }
            
            // Try getList() method
            try {
                Method getListMethod = warps.getClass().getMethod("getList");
                Object result = getListMethod.invoke(warps);
                if (result != null) {
                    // Handle both Set and List return types
                    if (result instanceof java.util.Set) {
                        java.util.Set<String> warpSet = (java.util.Set<String>) result;
                        plugin.getLogger().info("Found " + warpSet.size() + " warps via getList()");
                        return warpSet;
                    } else if (result instanceof java.util.List) {
                        java.util.List<String> warpList = (java.util.List<String>) result;
                        plugin.getLogger().info("Found " + warpList.size() + " warps via getList()");
                        return new java.util.HashSet<>(warpList);
                    } else if (result instanceof java.util.Collection) {
                        java.util.Collection<String> warpCollection = (java.util.Collection<String>) result;
                        plugin.getLogger().info("Found " + warpCollection.size() + " warps via getList()");
                        return new java.util.HashSet<>(warpCollection);
                    }
                }
            } catch (NoSuchMethodException e) {
                // Try alternative: warps might be a Map
                try {
                    Method getMapMethod = warps.getClass().getMethod("getMap");
                    Object mapResult = getMapMethod.invoke(warps);
                    if (mapResult instanceof java.util.Map) {
                        java.util.Map<?, ?> warpMap = (java.util.Map<?, ?>) mapResult;
                        java.util.Set<String> warpSet = new java.util.HashSet<>();
                        for (Object key : warpMap.keySet()) {
                            if (key instanceof String) {
                                warpSet.add((String) key);
                            }
                        }
                        plugin.getLogger().info("Found " + warpSet.size() + " warps via getMap()");
                        return warpSet;
                    }
                } catch (Exception e2) {
                    // Continue to next attempt
                }
                
                // Try keySet() if it's a Map
                if (warps instanceof java.util.Map) {
                    java.util.Map<?, ?> warpMap = (java.util.Map<?, ?>) warps;
                    java.util.Set<String> warpSet = new java.util.HashSet<>();
                    for (Object key : warpMap.keySet()) {
                        if (key instanceof String) {
                            warpSet.add((String) key);
                        }
                    }
                    plugin.getLogger().info("Found " + warpSet.size() + " warps via Map.keySet()");
                    return warpSet;
                }
            }
            
            // If warps is already a Collection, return it directly
            if (warps instanceof java.util.Collection) {
                java.util.Collection<?> warpCollection = (java.util.Collection<?>) warps;
                java.util.Set<String> warpSet = new java.util.HashSet<>();
                for (Object warp : warpCollection) {
                    if (warp instanceof String) {
                        warpSet.add((String) warp);
                    }
                }
                plugin.getLogger().info("Found " + warpSet.size() + " warps via direct Collection");
                return warpSet;
            }
            
            plugin.getLogger().warning("Could not extract warp list from Essentials warps object (type: " + warps.getClass().getName() + ")");
            return new java.util.ArrayList<>();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get warps: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    public List<String> getHomes(Player player) {
        if (!isEssentialsAvailable()) {
            plugin.getLogger().warning("Essentials not available when trying to get homes for " + player.getName());
            return null;
        }
        try {
            // Try different method signatures for getUser
            Method getUserMethod = null;
            try {
                // Try with Player first (most common)
                getUserMethod = essentials.getClass().getMethod("getUser", org.bukkit.entity.Player.class);
            } catch (NoSuchMethodException e) {
                try {
                    // Try with OfflinePlayer
                    getUserMethod = essentials.getClass().getMethod("getUser", org.bukkit.OfflinePlayer.class);
                } catch (NoSuchMethodException e2) {
                    // Try with UUID
                    try {
                        getUserMethod = essentials.getClass().getMethod("getUser", java.util.UUID.class);
                    } catch (NoSuchMethodException e3) {
                        // Try with String (username)
                        try {
                            getUserMethod = essentials.getClass().getMethod("getUser", String.class);
                        } catch (NoSuchMethodException e4) {
                            plugin.getLogger().warning("Could not find getUser method in Essentials");
                            return null;
                        }
                    }
                }
            }
            
            Object user = null;
            if (getUserMethod != null) {
                Class<?> paramType = getUserMethod.getParameterTypes()[0];
                try {
                    if (paramType.isAssignableFrom(Player.class)) {
                        user = getUserMethod.invoke(essentials, player);
                    } else if (paramType.isAssignableFrom(org.bukkit.OfflinePlayer.class)) {
                        user = getUserMethod.invoke(essentials, (org.bukkit.OfflinePlayer) player);
                    } else if (paramType == java.util.UUID.class) {
                        user = getUserMethod.invoke(essentials, player.getUniqueId());
                    } else if (paramType == String.class) {
                        user = getUserMethod.invoke(essentials, player.getName());
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to invoke getUser: " + e.getMessage());
                    return null;
                }
            }
            
            if (user == null) {
                plugin.getLogger().warning("getUser returned null for player " + player.getName());
                return null;
            }
            
            // Try getHomes() method
            Method getHomesMethod = null;
            try {
                getHomesMethod = user.getClass().getMethod("getHomes");
            } catch (NoSuchMethodException e) {
                // Try alternative method names
                try {
                    getHomesMethod = user.getClass().getMethod("getHomeNames");
                } catch (NoSuchMethodException e2) {
                    try {
                        getHomesMethod = user.getClass().getMethod("getHomesList");
                    } catch (NoSuchMethodException e3) {
                        plugin.getLogger().warning("Could not find getHomes method in Essentials User");
                        return null;
                    }
                }
            }
            
            Object homesResult = getHomesMethod.invoke(user);
            if (homesResult == null) {
                plugin.getLogger().info("getHomes returned null for player " + player.getName() + " (player may have no homes)");
                return new java.util.ArrayList<>(); // Return empty list instead of null
            }
            
            if (homesResult instanceof List) {
                List<?> homesList = (List<?>) homesResult;
                List<String> homes = new java.util.ArrayList<>();
                for (Object home : homesList) {
                    if (home instanceof String) {
                        homes.add((String) home);
                    } else {
                        homes.add(String.valueOf(home));
                    }
                }
                plugin.getLogger().info("Found " + homes.size() + " homes for player " + player.getName() + ": " + homes);
                return homes;
            } else if (homesResult instanceof java.util.Collection) {
                java.util.Collection<?> homesCollection = (java.util.Collection<?>) homesResult;
                List<String> homes = new java.util.ArrayList<>();
                for (Object home : homesCollection) {
                    if (home instanceof String) {
                        homes.add((String) home);
                    } else {
                        homes.add(String.valueOf(home));
                    }
                }
                plugin.getLogger().info("Found " + homes.size() + " homes for player " + player.getName() + ": " + homes);
                return homes;
            } else if (homesResult instanceof java.util.Set) {
                java.util.Set<?> homesSet = (java.util.Set<?>) homesResult;
                List<String> homes = new java.util.ArrayList<>();
                for (Object home : homesSet) {
                    if (home instanceof String) {
                        homes.add((String) home);
                    } else {
                        homes.add(String.valueOf(home));
                    }
                }
                plugin.getLogger().info("Found " + homes.size() + " homes for player " + player.getName() + ": " + homes);
                return homes;
            }
            
            plugin.getLogger().warning("getHomes returned unexpected type: " + homesResult.getClass().getName() + " (value: " + homesResult + ")");
            return new java.util.ArrayList<>();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get homes for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>(); // Return empty list instead of null
        }
    }
    
    public void teleportToWarp(Player player, String warpName) {
        if (!isEssentialsAvailable()) {
            plugin.getLogger().warning("Essentials not available when trying to teleport to warp " + warpName);
            return;
        }
        // Use Essentials command to teleport (this ensures all Essentials features work)
        // Try essentials:warp first, then fallback to warp
        try {
            Bukkit.dispatchCommand(player, "essentials:warp " + warpName);
        } catch (Exception e) {
            // Fallback to regular warp command
            try {
                Bukkit.dispatchCommand(player, "warp " + warpName);
            } catch (Exception e2) {
                plugin.getLogger().warning("Failed to teleport player " + player.getName() + " to warp " + warpName + ": " + e2.getMessage());
            }
        }
    }
    
    public void teleportToHome(Player player, String homeName) {
        if (!isEssentialsAvailable()) {
            plugin.getLogger().warning("Essentials not available when trying to teleport to home " + homeName);
            return;
        }
        // Use Essentials command to teleport (this ensures all Essentials features work)
        // Try essentials:home first, then fallback to home
        try {
            Bukkit.dispatchCommand(player, "essentials:home " + homeName);
        } catch (Exception e) {
            // Fallback to regular home command
            try {
                Bukkit.dispatchCommand(player, "home " + homeName);
            } catch (Exception e2) {
                plugin.getLogger().warning("Failed to teleport player " + player.getName() + " to home " + homeName + ": " + e2.getMessage());
            }
        }
    }
    
    public Collection<String> getKits(Player player) {
        if (!isEssentialsAvailable()) {
            plugin.getLogger().warning("Essentials not available when trying to get kits for " + player.getName());
            return new java.util.ArrayList<>();
        }
        try {
            // Try to get KitHolder or similar
            Method getKitsMethod = null;
            try {
                // Try getKits() method
                getKitsMethod = essentials.getClass().getMethod("getKits");
            } catch (NoSuchMethodException e) {
                // Try alternative method name
                try {
                    getKitsMethod = essentials.getClass().getMethod("getKitList");
                } catch (NoSuchMethodException e2) {
                    plugin.getLogger().warning("Could not find getKits or getKitList method");
                    return new java.util.ArrayList<>();
                }
            }
            
            Object kits = getKitsMethod.invoke(essentials);
            if (kits == null) {
                plugin.getLogger().info("getKits() returned null (no kits configured)");
                return new java.util.ArrayList<>();
            }
            
            // Try getList() method
            try {
                Method getListMethod = kits.getClass().getMethod("getList");
                Object result = getListMethod.invoke(kits);
                if (result != null) {
                    // Handle both Set and List return types
                    if (result instanceof java.util.Set) {
                        java.util.Set<String> kitSet = (java.util.Set<String>) result;
                        // Filter kits player has permission for
                        return filterKitsByPermission(player, kitSet);
                    } else if (result instanceof java.util.List) {
                        java.util.List<String> kitList = (java.util.List<String>) result;
                        return filterKitsByPermission(player, new java.util.HashSet<>(kitList));
                    } else if (result instanceof java.util.Collection) {
                        java.util.Collection<String> kitCollection = (java.util.Collection<String>) result;
                        return filterKitsByPermission(player, new java.util.HashSet<>(kitCollection));
                    }
                }
            } catch (NoSuchMethodException e) {
                // Try alternative: kits might be a Map
                try {
                    Method getMapMethod = kits.getClass().getMethod("getMap");
                    Object mapResult = getMapMethod.invoke(kits);
                    if (mapResult instanceof java.util.Map) {
                        java.util.Map<?, ?> kitMap = (java.util.Map<?, ?>) mapResult;
                        java.util.Set<String> kitSet = new java.util.HashSet<>();
                        for (Object key : kitMap.keySet()) {
                            if (key instanceof String) {
                                kitSet.add((String) key);
                            }
                        }
                        return filterKitsByPermission(player, kitSet);
                    }
                } catch (Exception e2) {
                    // Continue to next attempt
                }
                
                // Try keySet() if it's a Map
                if (kits instanceof java.util.Map) {
                    java.util.Map<?, ?> kitMap = (java.util.Map<?, ?>) kits;
                    java.util.Set<String> kitSet = new java.util.HashSet<>();
                    for (Object key : kitMap.keySet()) {
                        if (key instanceof String) {
                            kitSet.add((String) key);
                        }
                    }
                    return filterKitsByPermission(player, kitSet);
                }
            }
            
            // If kits is already a Collection, return it directly
            if (kits instanceof java.util.Collection) {
                java.util.Collection<?> kitCollection = (java.util.Collection<?>) kits;
                java.util.Set<String> kitSet = new java.util.HashSet<>();
                for (Object kit : kitCollection) {
                    if (kit instanceof String) {
                        kitSet.add((String) kit);
                    }
                }
                return filterKitsByPermission(player, kitSet);
            }
            
            plugin.getLogger().warning("Could not extract kit list from Essentials kits object (type: " + kits.getClass().getName() + ")");
            return new java.util.ArrayList<>();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get kits: " + e.getMessage());
            e.printStackTrace();
            return new java.util.ArrayList<>();
        }
    }
    
    private Collection<String> filterKitsByPermission(Player player, java.util.Set<String> kits) {
        java.util.List<String> filtered = new java.util.ArrayList<>();
        for (String kitName : kits) {
            // Check if player has permission for this kit
            if (player.hasPermission("essentials.kits." + kitName.toLowerCase()) || 
                player.hasPermission("essentials.kits.*")) {
                filtered.add(kitName);
            }
        }
        return filtered;
    }
    
    public void giveKit(Player player, String kitName) {
        if (!isEssentialsAvailable()) {
            return;
        }
        // Use Essentials command to give kit (this ensures all Essentials features work)
        Bukkit.dispatchCommand(player, "kit " + kitName);
    }
}

