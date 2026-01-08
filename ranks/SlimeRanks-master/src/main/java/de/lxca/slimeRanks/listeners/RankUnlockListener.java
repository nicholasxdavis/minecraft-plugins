package de.lxca.slimeRanks.listeners;

import de.lxca.slimeRanks.Main;
import de.lxca.slimeRanks.objects.Rank;
import de.lxca.slimeRanks.objects.RankManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Listener to auto-unlock perks and pets when players get certain ranks
 * Also handles monthly key distribution
 */
public class RankUnlockListener {
    
    private static final Map<UUID, Long> lastKeyDistribution = new HashMap<>();
    private static final long MONTH_IN_MILLIS = 30L * 24L * 60L * 60L * 1000L; // 30 days
    private static final Set<UUID> processingPlayers = new HashSet<>(); // Prevent duplicate processing
    
    public static void initialize() {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            luckPerms.getEventBus().subscribe(Main.getInstance(), UserDataRecalculateEvent.class, RankUnlockListener::onUserDataRecalculate);
            Main.getLogger(RankUnlockListener.class).info("Rank unlock listener initialized successfully");
        } catch (Exception e) {
            Main.getLogger(RankUnlockListener.class).warn("Failed to initialize rank unlock listener: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void onUserDataRecalculate(UserDataRecalculateEvent event) {
        User user = event.getUser();
        Player player = Bukkit.getPlayer(user.getUniqueId());
        
        if (player == null || !player.isOnline()) {
            return;
        }
        
        // Prevent duplicate processing
        if (processingPlayers.contains(user.getUniqueId())) {
            return;
        }
        processingPlayers.add(user.getUniqueId());
        
        // Run on next tick to ensure all data is loaded
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    processRankUnlocks(player);
                } finally {
                    processingPlayers.remove(user.getUniqueId());
                }
            }
        }.runTask(Main.getInstance());
    }
    
    private static void processRankUnlocks(Player player) {
        // Check player's current rank
        RankManager rankManager = RankManager.getInstance();
        Rank rank = rankManager.getPlayerRank(player);
        
        if (rank == null) {
            return;
        }
        
        String rankId = rank.getIdentifier().toLowerCase();
        
        // Handle rank-specific unlocks
        switch (rankId) {
            case "pandora":
                unlockAllPerks(player);
                unlockAllPets(player);
                givePandoraKit(player);
                giveMonthlyKeys(player);
                break;
            case "obsidian":
                unlockAllPerks(player);
                unlockAllPets(player);
                giveObsidianKit(player);
                giveMonthlyKeys(player);
                break;
            case "imperator":
                unlockAllPerks(player);
                giveMonthlyKeys(player);
                break;
        }
    }
    
    /**
     * Unlock all perks for a player
     */
    private static void unlockAllPerks(Player player) {
        Plugin perkShop = Bukkit.getPluginManager().getPlugin("PerkShop");
        if (perkShop == null) {
            Main.getLogger(RankUnlockListener.class).debug("PerkShop not found, skipping perk unlock");
            return;
        }
        
        try {
            // Use reflection to access PerkShop API
            Object plugin = perkShop;
            java.lang.reflect.Method getDataManagerMethod = plugin.getClass().getMethod("getDataManager");
            Object dataManager = getDataManagerMethod.invoke(plugin);
            
            if (dataManager == null) {
                Main.getLogger(RankUnlockListener.class).warn("PerkShop DataManager is null!");
                return;
            }
            
            // Get all perk keys from config
            List<String> perkKeys = Arrays.asList("heal", "feed", "night", "repair");
            int unlockedCount = 0;
            
            for (String perkKey : perkKeys) {
                // Check if player already has perk using reflection
                java.lang.reflect.Method hasPerkMethod = dataManager.getClass().getMethod("hasPerk", UUID.class, String.class);
                boolean hasPerk = (Boolean) hasPerkMethod.invoke(dataManager, player.getUniqueId(), perkKey);
                
                if (!hasPerk) {
                    // Add perk using reflection
                    java.lang.reflect.Method addPerkMethod = dataManager.getClass().getMethod("addPerk", UUID.class, String.class);
                    addPerkMethod.invoke(dataManager, player.getUniqueId(), perkKey);
                    unlockedCount++;
                }
            }
            
            // Grant permissions
            grantPerkPermissions(player, perkKeys);
            
            if (unlockedCount > 0) {
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&e&lPandora &8» &7All perks have been unlocked for your rank!"));
                Main.getLogger(RankUnlockListener.class).info("Unlocked " + unlockedCount + " perks for " + player.getName());
            }
        } catch (Exception e) {
            Main.getLogger(RankUnlockListener.class).warn("Failed to unlock perks for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Unlock all pets for a player
     */
    private static void unlockAllPets(Player player) {
        Plugin petPlugin = Bukkit.getPluginManager().getPlugin("PetPlugin");
        if (petPlugin == null) {
            Main.getLogger(RankUnlockListener.class).debug("PetPlugin not found, skipping pet unlock");
            return;
        }
        
        try {
            // Use reflection to access PetPlugin API
            Object plugin = petPlugin;
            java.lang.reflect.Method getDataManagerMethod = plugin.getClass().getMethod("getDataManager");
            Object dataManager = getDataManagerMethod.invoke(plugin);
            
            if (dataManager == null) {
                Main.getLogger(RankUnlockListener.class).warn("PetPlugin DataManager is null!");
                return;
            }
            
            // Get all pet types from config
            List<String> petTypes = Arrays.asList("horse", "dog", "cat", "wolf", "parrot", "fox");
            int unlockedCount = 0;
            
            for (String petType : petTypes) {
                // Check if player already has pet type using reflection
                java.lang.reflect.Method hasPetTypeMethod = dataManager.getClass().getMethod("hasPetType", UUID.class, String.class);
                boolean hasPetType = (Boolean) hasPetTypeMethod.invoke(dataManager, player.getUniqueId(), petType);
                
                if (!hasPetType) {
                    try {
                        // Create pet using reflection
                        String generatedName = petType.substring(0, 1).toUpperCase() + petType.substring(1) + "_" + 
                            System.currentTimeMillis();
                        
                        // Create Pet object using reflection
                        Class<?> petClass = Class.forName("com.playpandora.petplugin.models.Pet");
                        java.lang.reflect.Constructor<?> petConstructor = petClass.getConstructor(UUID.class, String.class, String.class);
                        Object pet = petConstructor.newInstance(player.getUniqueId(), petType, generatedName);
                        
                        // Set max health from config if available
                        String configPath = "pet-types." + petType + ".max-health";
                        java.lang.reflect.Method getConfigMethod = plugin.getClass().getMethod("getConfig");
                        org.bukkit.configuration.file.FileConfiguration config = (org.bukkit.configuration.file.FileConfiguration) getConfigMethod.invoke(plugin);
                        if (config.contains(configPath)) {
                            double maxHealth = config.getDouble(configPath, 20.0);
                            java.lang.reflect.Method setMaxHealthMethod = petClass.getMethod("setMaxHealth", double.class);
                            setMaxHealthMethod.invoke(pet, maxHealth);
                            java.lang.reflect.Method setCurrentHealthMethod = petClass.getMethod("setCurrentHealth", double.class);
                            setCurrentHealthMethod.invoke(pet, maxHealth);
                        }
                        
                        // Add pet to data manager using reflection
                        java.lang.reflect.Method addPetMethod = dataManager.getClass().getMethod("addPet", UUID.class, petClass);
                        addPetMethod.invoke(dataManager, player.getUniqueId(), pet);
                        unlockedCount++;
                    } catch (ClassNotFoundException e) {
                        Main.getLogger(RankUnlockListener.class).warn("PetPlugin Pet class not found: " + e.getMessage());
                    }
                }
            }
            
            if (unlockedCount > 0) {
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&e&lPandora &8» &7All pets have been unlocked for your rank!"));
                Main.getLogger(RankUnlockListener.class).info("Unlocked " + unlockedCount + " pets for " + player.getName());
            }
        } catch (Exception e) {
            Main.getLogger(RankUnlockListener.class).warn("Failed to unlock pets for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Grant perk permissions
     */
    private static void grantPerkPermissions(Player player, List<String> perkKeys) {
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                // Load user if not loaded
                user = luckPerms.getUserManager().loadUser(player.getUniqueId()).join();
            }
            
            if (user == null) {
                Main.getLogger(RankUnlockListener.class).warn("Could not load LuckPerms user for " + player.getName());
                return;
            }
            
            boolean modified = false;
            for (String perkKey : perkKeys) {
                String permission = "perkshop." + perkKey;
                net.luckperms.api.node.Node permissionNode = net.luckperms.api.node.Node.builder(permission).build();
                
                // Check if user already has permission
                boolean hasPermission = false;
                for (net.luckperms.api.node.Node node : user.data().toCollection()) {
                    if (node.getKey().equals(permission)) {
                        hasPermission = true;
                        break;
                    }
                }
                
                if (!hasPermission) {
                    luckPerms.getUserManager().modifyUser(player.getUniqueId(), u -> {
                        u.data().add(permissionNode);
                    });
                    modified = true;
                }
            }
            
            // Save changes if modified
            if (modified) {
                luckPerms.getUserManager().saveUser(user);
            }
        } catch (Exception e) {
            Main.getLogger(RankUnlockListener.class).warn("Failed to grant perk permissions: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Give Pandora kit items (cannons and creeper eggs)
     */
    private static void givePandoraKit(Player player) {
        try {
            // Give 1 of each cannon using console commands (most reliable)
            Plugin pandoraItems = Bukkit.getPluginManager().getPlugin("PandoraItems");
            if (pandoraItems != null) {
                String[] cannons = {"basic", "scattershot", "sniper", "shotgun"};
                for (String cannon : cannons) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                        "pandoraitem give cannon " + cannon + " " + player.getName());
                }
            }
            
            // Give 5 creeper eggs
            Plugin ceggs = Bukkit.getPluginManager().getPlugin("PandoraCeggs");
            if (ceggs != null) {
                try {
                    // Use reflection to get CEggsUtil or command
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                        "ceggs give " + player.getName() + " 5");
                } catch (Exception e) {
                    Main.getLogger(RankUnlockListener.class).debug("Using fallback for creeper eggs: " + e.getMessage());
                }
            } else {
                // Fallback: give vanilla creeper spawn eggs
                ItemStack eggs = new ItemStack(Material.CREEPER_SPAWN_EGG, 5);
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(eggs);
                if (!overflow.isEmpty()) {
                    for (ItemStack overflowItem : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), overflowItem);
                    }
                }
            }
            
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&e&lPandora &8» &7You received your &ePandora &7rank kit items!"));
        } catch (Exception e) {
            Main.getLogger(RankUnlockListener.class).warn("Failed to give Pandora kit: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Give Obsidian kit items (cannons)
     */
    private static void giveObsidianKit(Player player) {
        try {
            // Give 1 of each cannon using console commands (most reliable)
            Plugin pandoraItems = Bukkit.getPluginManager().getPlugin("PandoraItems");
            if (pandoraItems != null) {
                String[] cannons = {"basic", "scattershot", "sniper", "shotgun"};
                for (String cannon : cannons) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                        "pandoraitem give cannon " + cannon + " " + player.getName());
                }
            }
            
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&e&lPandora &8» &7You received your &5Obsidian &7rank kit items!"));
        } catch (Exception e) {
            Main.getLogger(RankUnlockListener.class).warn("Failed to give Obsidian kit: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Give monthly keys (1 of each crate key)
     */
    private static void giveMonthlyKeys(Player player) {
        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check if enough time has passed since last distribution
        if (lastKeyDistribution.containsKey(uuid)) {
            long lastTime = lastKeyDistribution.get(uuid);
            if (currentTime - lastTime < MONTH_IN_MILLIS) {
                return; // Not yet time for monthly keys
            }
        }
        
        Plugin pandoraCrates = Bukkit.getPluginManager().getPlugin("PandoraCrates");
        if (pandoraCrates == null) {
            Main.getLogger(RankUnlockListener.class).debug("PandoraCrates not found, skipping monthly keys");
            return;
        }
        
        try {
            // Use PandoraCrates API via reflection
            Object plugin = pandoraCrates;
            java.lang.reflect.Method getKeyManagerMethod = plugin.getClass().getMethod("getKeyManager");
            Object keyManager = getKeyManagerMethod.invoke(plugin);
            
            if (keyManager == null) {
                Main.getLogger(RankUnlockListener.class).warn("PandoraCrates KeyManager is null!");
                return;
            }
            
            // Get crate manager
            java.lang.reflect.Method getCrateManagerMethod = plugin.getClass().getMethod("getCrateManager");
            Object crateManager = getCrateManagerMethod.invoke(plugin);
            
            // Give 1 of each crate key using API
            String[] crateKeys = {"default", "imperator", "obsidian", "pandora"};
            int givenCount = 0;
            
            for (String crateKey : crateKeys) {
                try {
                    // Check if crate exists using reflection
                    java.lang.reflect.Method getCrateMethod = crateManager.getClass().getMethod("getCrate", String.class);
                    Object crate = getCrateMethod.invoke(crateManager, crateKey);
                    if (crate != null) {
                        // Give key using reflection
                        java.lang.reflect.Method giveKeyMethod = keyManager.getClass().getMethod("giveKey", 
                            org.bukkit.entity.Player.class, String.class, int.class);
                        giveKeyMethod.invoke(keyManager, player, crateKey, 1);
                        givenCount++;
                    } else {
                        Main.getLogger(RankUnlockListener.class).debug("Crate '" + crateKey + "' not found, skipping");
                    }
                } catch (Exception e) {
                    Main.getLogger(RankUnlockListener.class).warn("Failed to give key for crate '" + crateKey + "': " + e.getMessage());
                }
            }
            
            if (givenCount > 0) {
                // Update last distribution time
                lastKeyDistribution.put(uuid, currentTime);
                
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&e&lPandora &8» &7You received your monthly crate keys!"));
                Main.getLogger(RankUnlockListener.class).info("Gave " + givenCount + " monthly keys to " + player.getName());
            }
        } catch (Exception e) {
            Main.getLogger(RankUnlockListener.class).warn("Failed to give monthly keys: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback to console commands
            try {
                String[] crateKeys = {"default", "imperator", "obsidian", "pandora"};
                for (String crateKey : crateKeys) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                        "crate key give " + player.getName() + " " + crateKey + " 1");
                }
                lastKeyDistribution.put(uuid, currentTime);
            } catch (Exception e2) {
                Main.getLogger(RankUnlockListener.class).error("Fallback also failed: " + e2.getMessage());
            }
        }
    }
    
    /**
     * Check and distribute monthly keys on player join
     */
    public static void checkMonthlyKeysOnJoin(Player player) {
        // Run on next tick to ensure rank is loaded
        new BukkitRunnable() {
            @Override
            public void run() {
                RankManager rankManager = RankManager.getInstance();
                Rank rank = rankManager.getPlayerRank(player);
                
                if (rank == null) {
                    return;
                }
                
                String rankId = rank.getIdentifier().toLowerCase();
                if (rankId.equals("pandora") || rankId.equals("obsidian") || rankId.equals("imperator")) {
                    giveMonthlyKeys(player);
                }
            }
        }.runTaskLater(Main.getInstance(), 20L); // 1 second delay
    }
    
    /**
     * Public method to trigger rank unlocks for a player
     * Can be called when rank is manually set
     */
    public static void triggerRankUnlocks(Player player) {
        // Run on next tick to ensure rank is loaded
        new BukkitRunnable() {
            @Override
            public void run() {
                processRankUnlocks(player);
            }
        }.runTaskLater(Main.getInstance(), 20L); // 1 second delay
    }
}
