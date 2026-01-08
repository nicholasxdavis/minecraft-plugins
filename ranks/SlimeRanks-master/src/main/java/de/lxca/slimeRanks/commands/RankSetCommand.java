package de.lxca.slimeRanks.commands;

import de.lxca.slimeRanks.Main;
import de.lxca.slimeRanks.objects.Message;
import de.lxca.slimeRanks.objects.Rank;
import de.lxca.slimeRanks.objects.RankManager;
import de.lxca.slimeRanks.utils.EffectUtils;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Professional rank management command - Hypixel Standard
 * Usage: /rankset <player> <rankname>
 * Features: Validation, verification, logging, error recovery
 */
public class RankSetCommand implements CommandExecutor, TabCompleter {
    
    private static final Set<String> RANK_GROUPS = Set.of("pandora", "obsidian", "imperator", "default");
    
    // Prevent concurrent rank changes for the same player
    private static final Set<UUID> processingPlayers = ConcurrentHashMap.newKeySet();
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("slimeranks.admin")) {
            sendMessage(sender, "&e&lPandora &8» &cYou don't have permission to use this command!");
            return true;
        }
        
        if (args.length < 2) {
            sendMessage(sender, "&e&lPandora &8» &7Usage: &e/rankset <player> <rankname>");
            sendMessage(sender, "&7Available ranks: &epandora&7, &5obsidian&7, &6imperator&7, &7default");
            return true;
        }
        
        String playerName = args[0];
        String rankName = args[1].toLowerCase();
        
        // Validate rank name
        if (!RANK_GROUPS.contains(rankName)) {
            sendMessage(sender, "&e&lPandora &8» &cInvalid rank! Available ranks: &epandora&7, &5obsidian&7, &6imperator&7, &7default");
            return true;
        }
        
        // Get player (online or offline)
        OfflinePlayer targetPlayer = null;
        
        // Try to find online player first
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            targetPlayer = onlinePlayer;
        } else {
            // Try offline player
            targetPlayer = Bukkit.getOfflinePlayer(playerName);
            if (targetPlayer == null || (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline())) {
                sendMessage(sender, "&e&lPandora &8» &cPlayer &6" + playerName + " &cnot found!");
                sendMessage(sender, "&7Make sure the player name is spelled correctly.");
                return true;
            }
        }
        
        UUID targetUUID = targetPlayer.getUniqueId();
        
        // Prevent duplicate operations
        if (processingPlayers.contains(targetUUID)) {
            sendMessage(sender, "&e&lPandora &8» &cThis player's rank is already being changed. Please wait...");
            return true;
        }
        
        // Check if LuckPerms is available
        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            
            // Get current rank before changing
            String currentRank = getCurrentRankGroup(luckPerms, targetUUID);
            
            // Check if already has the rank
            if (currentRank != null && currentRank.equalsIgnoreCase(rankName)) {
                sendMessage(sender, "&e&lPandora &8» &7Player &6" + playerName + " &7already has the &e" + getRankDisplayName(rankName) + " &7rank!");
                return true;
            }
            
            // Log the rank change
            Main.getLogger(RankSetCommand.class).info(String.format(
                "Rank change initiated: %s -> %s (Player: %s, UUID: %s, Executor: %s)",
                currentRank != null ? currentRank : "unknown",
                rankName,
                playerName,
                targetUUID,
                sender.getName()
            ));
            
            // Set the rank
            setPlayerRank(luckPerms, targetUUID, rankName, sender, targetPlayer.getName(), currentRank);
            
        } catch (IllegalStateException e) {
            sendMessage(sender, "&e&lPandora &8» &cLuckPerms is not available! Please install LuckPerms to use this command.");
            Main.getLogger(RankSetCommand.class).warn("LuckPerms not available: " + e.getMessage());
            return true;
        } catch (Exception e) {
            sendMessage(sender, "&e&lPandora &8» &cAn error occurred while setting the rank: &7" + e.getMessage());
            Main.getLogger(RankSetCommand.class).error("Error setting rank: " + e.getMessage(), e);
            return true;
        }
        
        return true;
    }
    
    /**
     * Get the current rank group for a player
     */
    private String getCurrentRankGroup(LuckPerms luckPerms, UUID playerUUID) {
        User user = luckPerms.getUserManager().getUser(playerUUID);
        if (user == null) {
            // Try to load user
            try {
                user = luckPerms.getUserManager().loadUser(playerUUID).join();
            } catch (Exception e) {
                return null;
            }
        }
        
        if (user == null) {
            return null;
        }
        
        // Check primary group first
        String primaryGroup = user.getPrimaryGroup();
        if (RANK_GROUPS.contains(primaryGroup) && !primaryGroup.equals("default")) {
            return primaryGroup;
        }
        
        // Check which rank group the player has in their data
        for (String rankGroup : RANK_GROUPS) {
            if (rankGroup.equals("default")) continue; // Skip default
            
            String groupName = "group." + rankGroup;
            // Check if user has this group by checking inheritance nodes
            for (Node node : user.data().toCollection()) {
                if (node.getKey().equals(groupName)) {
                    return rankGroup;
                }
            }
        }
        
        return "default";
    }
    
    /**
     * Verify that rank change was successful
     * Hypixel-standard: Always verify changes took effect
     */
    private boolean verifyRankChange(LuckPerms luckPerms, UUID playerUUID, String expectedRank, int maxAttempts) {
        for (int i = 0; i < maxAttempts; i++) {
            try {
                User user = luckPerms.getUserManager().loadUser(playerUUID).join();
                if (user == null) {
                    Thread.sleep(100);
                    continue;
                }
                
                String actualRank = getCurrentRankGroup(luckPerms, playerUUID);
                if (expectedRank.equalsIgnoreCase(actualRank)) {
                    return true;
                }
                
                Thread.sleep(200); // Wait before retry
            } catch (Exception e) {
                Main.getLogger(RankSetCommand.class).debug("Verification attempt " + (i + 1) + " failed: " + e.getMessage());
            }
        }
        return false;
    }
    
    /**
     * Set player rank and handle all permission cleanup
     * Hypixel-standard: Professional implementation with validation, verification, and error recovery
     */
    private void setPlayerRank(LuckPerms luckPerms, UUID playerUUID, String rankName, 
                               CommandSender sender, String playerName, String currentRank) {
        
        // Mark player as being processed
        processingPlayers.add(playerUUID);
        
        CompletableFuture<User> userFuture = luckPerms.getUserManager().loadUser(playerUUID);
        
        userFuture.thenAcceptAsync(user -> {
            try {
                if (user == null) {
                    throw new IllegalStateException("Failed to load user from LuckPerms");
                }
                // If setting to default, clear all rank-related permissions
                if (rankName.equals("default")) {
                    // Clear all rank permissions first
                    clearAllRankPermissions(luckPerms, user, playerUUID);
                    
                    // Wait a moment for the clear to complete, then ensure user is properly set to default
                    CompletableFuture.runAsync(() -> {
                        try {
                            Thread.sleep(300); // Small delay to ensure clear completes
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).thenCompose(v -> {
                        // Reload user to get fresh data after clearing
                        return luckPerms.getUserManager().loadUser(playerUUID);
                    }).thenCompose(updatedUser -> {
                        // Ensure user is set to default group and verify all rank groups are removed
                        return luckPerms.getUserManager().modifyUser(playerUUID, u -> {
                            // Set primary group to default
                            u.setPrimaryGroup("default");
                            
                            // Double-check: remove any remaining rank group nodes
                            Set<Node> additionalNodesToRemove = new HashSet<>();
                            for (Node node : u.data().toCollection()) {
                                String permission = node.getKey();
                                
                                // Remove any remaining rank group permissions
                                if (permission.equals("group.pandora") || 
                                    permission.equals("group.obsidian") || 
                                    permission.equals("group.imperator") ||
                                    permission.startsWith("group.pandora.") || 
                                    permission.startsWith("group.obsidian.") || 
                                    permission.startsWith("group.imperator.")) {
                                    additionalNodesToRemove.add(node);
                                }
                                
                                // Check inheritance nodes
                                if (node instanceof net.luckperms.api.node.types.InheritanceNode) {
                                    net.luckperms.api.node.types.InheritanceNode inheritanceNode = (net.luckperms.api.node.types.InheritanceNode) node;
                                    String groupName = inheritanceNode.getGroupName();
                                    if (RANK_GROUPS.contains(groupName) && !groupName.equals("default")) {
                                        additionalNodesToRemove.add(node);
                                    }
                                }
                            }
                            
                            // Remove any additional nodes found
                            for (Node node : additionalNodesToRemove) {
                                u.data().remove(node);
                            }
                        });
                    }).thenCompose(v -> luckPerms.getUserManager().saveUser(user))
                    .thenRun(() -> {
                        // Verify the change took effect
                        boolean verified = verifyRankChange(luckPerms, playerUUID, "default", 5);
                        if (!verified) {
                            Main.getLogger(RankSetCommand.class).warn("Rank change verification failed for " + playerName + " (UUID: " + playerUUID + ")");
                        }
                        
                        // After permissions are updated, refresh everything
                        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                            // Clear rank cache
                            RankManager.getInstance().clearPlayerRankCache(playerUUID);
                            
                            // Reload displays first
                            RankManager.getInstance().reloadDisplays();
                            
                            // Get player if online and force permission refresh
                            Player onlinePlayer = Bukkit.getPlayer(playerUUID);
                            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                                // Force permission recalculation
                                onlinePlayer.recalculatePermissions();
                                
                                // Reload user from LuckPerms to refresh cache
                                luckPerms.getUserManager().loadUser(playerUUID).thenAccept(updatedUser -> {
                                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                                        // Force another permission refresh
                                        onlinePlayer.recalculatePermissions();
                                        
                                        // Update display
                                        RankManager.getInstance().updatePlayerDisplay(onlinePlayer);
                                        
                                        // Notify player
                                        sendMessage(onlinePlayer, "&e&lPandora &8» &7Your rank has been set to &7Default&7.");
                                        sendMessage(onlinePlayer, "&7All rank-related permissions, perks, and pets have been removed.");
                                        
                                        // Final display refresh
                                        RankManager.getInstance().reloadDisplays();
                                        
                                        // Remove from processing set
                                        processingPlayers.remove(playerUUID);
                                    });
                                });
                            } else {
                                processingPlayers.remove(playerUUID);
                            }
                            
                            // Also revoke perks and pets if they were auto-unlocked
                            revokeRankBenefits(playerUUID, playerName);
                            
                            // Send confirmation to sender
                            sendMessage(sender, "&e&lPandora &8» &7Set &6" + playerName + " &7to &7Default &7rank!");
                            if (!verified) {
                                sendMessage(sender, "&7&oNote: Rank change verification had issues. Please verify manually.");
                            }
                            sendMessage(sender, "&7All rank-related permissions, perks, and pets have been cleared.");
                            
                            // Log success
                            Main.getLogger(RankSetCommand.class).info(String.format(
                                "Rank change completed: %s -> default (Player: %s, UUID: %s, Verified: %s)",
                                currentRank != null ? currentRank : "unknown",
                                playerName,
                                playerUUID,
                                verified
                            ));
                        }, 20L); // Wait 1 second for LuckPerms to fully update
                    }).exceptionally(throwable -> {
                        processingPlayers.remove(playerUUID);
                        Main.getLogger(RankSetCommand.class).error("Error in default rank change: " + throwable.getMessage(), throwable);
                        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                            sendMessage(sender, "&cAn error occurred while setting rank to default. Check console for details.");
                        });
                        return null;
                    });
                    
                    return;
                }
                
                // Always clear and re-add rank, even if player already has it
                // This ensures permissions are properly refreshed and rank benefits are re-applied
                
                // Get Hook API for notifications using reflection
                Object hookAPI = getHookAPI();
                
                // Get rank display name
                Rank rank = new Rank(rankName);
                String rankDisplayName = rank.exists() ? getRankDisplayName(rankName) : rankName;
                
                // FIRST: Clear ALL old rank permissions and groups
                clearAllRankPermissions(luckPerms, user, playerUUID);
                
                // Wait a moment for the clear to complete, then set new rank
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(200); // Small delay to ensure clear completes
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).thenCompose(v -> {
                    // NOW: Set new rank group
                    String newGroupName = "group." + rankName;
                    return luckPerms.getUserManager().modifyUser(playerUUID, u -> {
                        // Set primary group
                        u.setPrimaryGroup(rankName);
                        
                        // Add as inheritance node
                        u.data().add(Node.builder(newGroupName).build());
                        
                        // Grant EssentialsX permissions for paid ranks
                        if (rankName.equals("pandora") || rankName.equals("obsidian") || rankName.equals("imperator")) {
                            grantEssentialsXPermissions(u);
                        }
                    });
                }).thenCompose(v -> luckPerms.getUserManager().saveUser(user))
                .thenRun(() -> {
                    // Verify the change took effect
                    boolean verified = verifyRankChange(luckPerms, playerUUID, rankName, 5);
                    if (!verified) {
                        Main.getLogger(RankSetCommand.class).warn("Rank change verification failed for " + playerName + " (UUID: " + playerUUID + ")");
                    }
                    
                    // After permissions are updated, refresh everything
                    Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                        // Clear rank cache
                        RankManager.getInstance().clearPlayerRankCache(playerUUID);
                        
                        // Reload displays first
                        RankManager.getInstance().reloadDisplays();
                        
                        // Get player if online
                        Player onlinePlayer = Bukkit.getPlayer(playerUUID);
                        if (onlinePlayer != null && onlinePlayer.isOnline()) {
                            // Refresh LuckPerms permission cache by reloading user
                            luckPerms.getUserManager().loadUser(playerUUID).thenAccept(updatedUser -> {
                                // Trigger permission refresh on main thread
                                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                                    // Force permission recalculation - this is critical!
                                    onlinePlayer.recalculatePermissions();
                                    
                                    // Also try to refresh LuckPerms cache using reflection
                                    try {
                                        // Get LuckPerms permission manager
                                        Object permissionManager = luckPerms.getClass().getMethod("getPermissionManager").invoke(luckPerms);
                                        if (permissionManager != null) {
                                            // Try to refresh user permissions
                                            java.lang.reflect.Method refreshUserMethod = permissionManager.getClass().getMethod("refreshUser", User.class);
                                            refreshUserMethod.invoke(permissionManager, updatedUser);
                                        }
                                    } catch (Exception e) {
                                        // Ignore if method doesn't exist - recalculatePermissions should be enough
                                        Main.getLogger(RankSetCommand.class).debug("Could not refresh LuckPerms cache: " + e.getMessage());
                                    }
                                    
                                    // Update commands
                                    try {
                                        java.lang.reflect.Method updateCommandsMethod = onlinePlayer.getClass().getMethod("updateCommands");
                                        updateCommandsMethod.invoke(onlinePlayer);
                                    } catch (Exception e) {
                                        // Ignore if method doesn't exist
                                    }
                                    
                                    // Update display
                                    RankManager.getInstance().updatePlayerDisplay(onlinePlayer);
                                    
                                    // Send chat message
                                    sendMessage(onlinePlayer, "&e&lPandora &8» &7Your rank has been set to &e" + rankDisplayName + "&7!");
                                    
                                    // Send title notification via Hook using reflection
                                    if (hookAPI != null) {
                                        try {
                                            // Check if enabled
                                            java.lang.reflect.Method isEnabledMethod = hookAPI.getClass().getMethod("isEnabled");
                                            boolean enabled = (Boolean) isEnabledMethod.invoke(hookAPI);
                                            
                                            if (enabled) {
                                                // Send custom notification
                                                java.lang.reflect.Method sendCustomMethod = hookAPI.getClass().getMethod("sendCustom", 
                                                    org.bukkit.entity.Player.class, String.class, String.class, int.class, int.class, int.class);
                                                sendCustomMethod.invoke(hookAPI, onlinePlayer, 
                                                    "Rank Promoted!", 
                                                    "You are now " + rankDisplayName.replace("&e&l", "").replace("&5&l", "").replace("&6&l", "").replace("&7", ""),
                                                    500, 4000, 1000);
                                            }
                                        } catch (Exception e) {
                                            Main.getLogger(RankSetCommand.class).debug("Failed to send Hook notification: " + e.getMessage());
                                        }
                                    }
                                    
                                    // Spawn fireworks and play sound
                                    EffectUtils.spawnRankFireworks(onlinePlayer, 3);
                                    EffectUtils.playRankSound(onlinePlayer);
                                    
                                    // Trigger rank unlock listener to apply perks/pets/keys
                                    if (rankName.equals("pandora") || rankName.equals("obsidian") || rankName.equals("imperator")) {
                                        de.lxca.slimeRanks.listeners.RankUnlockListener.triggerRankUnlocks(onlinePlayer);
                                    }
                                    
                                    // Final display refresh
                                    RankManager.getInstance().reloadDisplays();
                                    
                                    // Remove from processing set
                                    processingPlayers.remove(playerUUID);
                                });
                            });
                        } else {
                            processingPlayers.remove(playerUUID);
                        }
                        
                        // Send confirmation to sender
                        sendMessage(sender, "&e&lPandora &8» &7Set &6" + playerName + " &7to &e" + rankDisplayName + " &7rank!");
                        if (!verified) {
                            sendMessage(sender, "&7&oNote: Rank change verification had issues. Please verify manually.");
                        }
                        
                        // Log success
                        Main.getLogger(RankSetCommand.class).info(String.format(
                            "Rank change completed: %s -> %s (Player: %s, UUID: %s, Verified: %s)",
                            currentRank != null ? currentRank : "unknown",
                            rankName,
                            playerName,
                            playerUUID,
                            verified
                        ));
                    }, 40L); // Wait 2 seconds for LuckPerms to fully update
                }).exceptionally(throwable -> {
                    processingPlayers.remove(playerUUID);
                    Main.getLogger(RankSetCommand.class).error("Error in rank change: " + throwable.getMessage(), throwable);
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                        sendMessage(sender, "&cAn error occurred while setting rank. Check console for details.");
                    });
                    return null;
                });
                
                // Send confirmation to sender immediately
                sendMessage(sender, "&e&lPandora &8» &7Processing rank change for &6" + playerName + "&7...");
                
            } catch (Exception e) {
                processingPlayers.remove(playerUUID);
                Main.getLogger(RankSetCommand.class).error("Error in setPlayerRank: " + e.getMessage(), e);
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    sendMessage(sender, "&cAn error occurred: &7" + e.getMessage());
                });
            }
        }).exceptionally(throwable -> {
            processingPlayers.remove(playerUUID);
            Main.getLogger(RankSetCommand.class).error("Error loading user: " + throwable.getMessage(), throwable);
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                sendMessage(sender, "&cFailed to load player data from LuckPerms!");
            });
            return null;
        });
    }
    
    /**
     * Grant EssentialsX permissions for paid ranks
     * Grants: /fly, /feed, /heal (and their .others variants)
     */
    private void grantEssentialsXPermissions(net.luckperms.api.model.user.User user) {
        // EssentialsX permissions for paid ranks
        String[] essentialsPermissions = {
            "essentials.fly",
            "essentials.fly.*",
            "essentials.feed",
            "essentials.feed.*",
            "essentials.heal",
            "essentials.heal.*",
            "essentials.hat",
            "essentials.nick",
            "essentials.nick.*",
            "essentials.itemname",
            "essentials.itemname.*"
        };
        
        for (String permission : essentialsPermissions) {
            // Check if user already has permission
            boolean hasPermission = false;
            for (Node node : user.data().toCollection()) {
                if (node.getKey().equals(permission)) {
                    hasPermission = true;
                    break;
                }
            }
            
            // Add permission if not already present
            if (!hasPermission) {
                user.data().add(Node.builder(permission).build());
            }
        }
    }
    
    /**
     * Clear all rank-related permissions (used when switching ranks or setting to default)
     */
    private void clearAllRankPermissions(LuckPerms luckPerms, User user, UUID playerUUID) {
        luckPerms.getUserManager().modifyUser(playerUUID, u -> {
            // Remove all rank groups and related permissions
            Set<Node> nodesToRemove = new HashSet<>();
            
            // Collect all rank-related nodes
            for (Node node : u.data().toCollection()) {
                String permission = node.getKey();
                
                // Remove ALL rank group permissions (including wildcards like group.pandora.*)
                if (permission.equals("group.pandora") || 
                    permission.equals("group.obsidian") || 
                    permission.equals("group.imperator") ||
                    permission.startsWith("group.pandora.") || 
                    permission.startsWith("group.obsidian.") || 
                    permission.startsWith("group.imperator.")) {
                    nodesToRemove.add(node);
                }
                
                // Also check inheritance nodes for rank groups
                if (node instanceof net.luckperms.api.node.types.InheritanceNode) {
                    net.luckperms.api.node.types.InheritanceNode inheritanceNode = (net.luckperms.api.node.types.InheritanceNode) node;
                    String groupName = inheritanceNode.getGroupName();
                    if (RANK_GROUPS.contains(groupName) && !groupName.equals("default")) {
                        nodesToRemove.add(node);
                    }
                }
                
                // Remove perkshop permissions (they'll be re-granted if needed)
                if (permission.startsWith("perkshop.")) {
                    nodesToRemove.add(node);
                }
                
                // Remove slimeranks rank permissions
                if (permission.startsWith("slimeranks.rank.")) {
                    nodesToRemove.add(node);
                }
                
                // Remove EssentialsX permissions (they'll be re-granted if needed for paid ranks)
                if (permission.equals("essentials.fly") || 
                    permission.equals("essentials.feed") || 
                    permission.equals("essentials.heal") ||
                    permission.startsWith("essentials.fly.") || 
                    permission.startsWith("essentials.feed.") || 
                    permission.startsWith("essentials.heal.") ||
                    permission.equals("essentials.hat") ||
                    permission.equals("essentials.nick") ||
                    permission.startsWith("essentials.nick.") ||
                    permission.equals("essentials.itemname") ||
                    permission.startsWith("essentials.itemname.")) {
                    nodesToRemove.add(node);
                }
                
                // Remove any prefix/suffix meta that might be set directly on the user
                if (node instanceof net.luckperms.api.node.types.MetaNode) {
                    net.luckperms.api.node.types.MetaNode metaNode = (net.luckperms.api.node.types.MetaNode) node;
                    String metaKey = metaNode.getMetaKey();
                    // Remove prefix and suffix meta nodes
                    if (metaKey.equals("prefix") || metaKey.equals("suffix")) {
                        nodesToRemove.add(node);
                    }
                }
            }
            
            // Remove collected permissions
            for (Node node : nodesToRemove) {
                u.data().remove(node);
            }
            
            // Also reset primary group to default if it's a rank group
            String currentPrimary = u.getPrimaryGroup();
            if (RANK_GROUPS.contains(currentPrimary) && !currentPrimary.equals("default")) {
                u.setPrimaryGroup("default");
            }
        });
        
        // Save user
        luckPerms.getUserManager().saveUser(user);
    }
    
    /**
     * Revoke rank benefits (perks and pets) when demoting to default
     */
    private void revokeRankBenefits(UUID playerUUID, String playerName) {
        try {
            // Revoke perks from PerkShop using reflection
            org.bukkit.plugin.Plugin perkShop = Bukkit.getPluginManager().getPlugin("PerkShop");
            if (perkShop != null) {
                try {
                    Object plugin = perkShop;
                    java.lang.reflect.Method getDataManagerMethod = plugin.getClass().getMethod("getDataManager");
                    Object dataManager = getDataManagerMethod.invoke(plugin);
                    
                    if (dataManager != null) {
                        // Get all perks player has using reflection
                        java.lang.reflect.Method getPlayerPerksMethod = dataManager.getClass().getMethod("getPlayerPerks", UUID.class);
                        Set<String> playerPerks = (Set<String>) getPlayerPerksMethod.invoke(dataManager, playerUUID);
                        
                        // Note: PerkShop doesn't have a removePerk method, so we'll just remove permissions
                        // The perks will remain in data but won't be usable without permissions
                        Main.getLogger(RankSetCommand.class).info("Player " + playerName + " had " + playerPerks.size() + " perks (permissions removed)");
                    }
                } catch (Exception e) {
                    Main.getLogger(RankSetCommand.class).debug("Could not revoke perks: " + e.getMessage());
                }
            }
            
            // Note: Pets are kept in PetPlugin - we don't remove them as they're permanent purchases
            // Only permissions are removed, so they can't use rank-specific features
        } catch (Exception e) {
            Main.getLogger(RankSetCommand.class).warn("Error revoking rank benefits: " + e.getMessage());
        }
    }
    
    /**
     * Get display name for rank
     */
    private String getRankDisplayName(String rankName) {
        switch (rankName.toLowerCase()) {
            case "pandora":
                return "&e&lPandora";
            case "obsidian":
                return "&5&lObsidian";
            case "imperator":
                return "&6&lImperator";
            case "default":
                return "&7Default";
            default:
                return rankName;
        }
    }
    
    /**
     * Get Hook API instance using reflection
     */
    private Object getHookAPI() {
        try {
            Plugin hookPlugin = Bukkit.getPluginManager().getPlugin("Hook");
            if (hookPlugin != null && hookPlugin.isEnabled()) {
                // Use reflection to get API
                java.lang.reflect.Method getAPIMethod = hookPlugin.getClass().getMethod("getAPI");
                return getAPIMethod.invoke(hookPlugin);
            }
        } catch (Exception e) {
            Main.getLogger(RankSetCommand.class).debug("Hook plugin not available: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Send formatted message
     */
    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', message));
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("slimeranks.admin")) {
            return Collections.emptyList();
        }
        
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Tab complete player names
            String input = args[0].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(input)) {
                    completions.add(player.getName());
                }
            }
            // Also add offline players from LuckPerms if available
            try {
                LuckPerms luckPerms = LuckPermsProvider.get();
                luckPerms.getUserManager().getLoadedUsers().forEach(user -> {
                    String name = user.getUsername();
                    if (name != null && name.toLowerCase().startsWith(input)) {
                        completions.add(name);
                    }
                });
            } catch (Exception e) {
                // Ignore
            }
        } else if (args.length == 2) {
            // Tab complete rank names
            String input = args[1].toLowerCase();
            for (String rank : RANK_GROUPS) {
                if (rank.startsWith(input)) {
                    completions.add(rank);
                }
            }
        }
        
        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }
}

