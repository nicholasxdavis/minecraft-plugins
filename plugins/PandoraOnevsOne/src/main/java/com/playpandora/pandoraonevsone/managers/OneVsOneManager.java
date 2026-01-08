package com.playpandora.pandoraonevsone.managers;

import com.playpandora.pandoraonevsone.PandoraOnevsOne;
import com.playpandora.pandoraonevsone.models.Duel;
import com.playpandora.pandoraonevsone.util.ColorUtil;
import com.playpandora.pandoraonevsone.util.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Manages 1v1 duels, queue, arena, and player states
 */
public class OneVsOneManager {
    
    private final PandoraOnevsOne plugin;
    private final Set<UUID> inDuel; // Players currently in a duel
    private final Map<UUID, UUID> pendingChallenges; // Challenger UUID -> Target UUID
    private final Map<UUID, String> savedInventories; // UUID -> Base64 inventory data
    private final Map<UUID, Location> savedLocations; // UUID -> Original location
    private Duel activeDuel;
    private Location arenaLocation;
    private Location spawn1Location;
    private Location spawn2Location;
    private BukkitTask timerTask;
    
    public OneVsOneManager(PandoraOnevsOne plugin) {
        this.plugin = plugin;
        this.inDuel = new HashSet<>();
        this.pendingChallenges = new HashMap<>();
        this.savedInventories = new HashMap<>();
        this.savedLocations = new HashMap<>();
        
        // Load arena location from config
        loadArenaLocation();
        
        // Start timer task to check for timeouts
        startTimerTask();
    }
    
    /**
     * Check if a player is in a duel
     */
    public boolean isInDuel(Player player) {
        return inDuel.contains(player.getUniqueId());
    }
    
    /**
     * Check if there's an active duel
     */
    public boolean hasActiveDuel() {
        return activeDuel != null;
    }
    
    /**
     * Get the active duel
     */
    public Duel getActiveDuel() {
        return activeDuel;
    }
    
    /**
     * Challenge a player to a duel
     */
    public boolean challengePlayer(Player challenger, Player target) {
        UUID challengerUUID = challenger.getUniqueId();
        UUID targetUUID = target.getUniqueId();
        
        // Check if challenger is already in a duel
        if (isInDuel(challenger)) {
            challenger.sendMessage(ColorUtil.error("You are already in a duel!"));
            return false;
        }
        
        // Check if target is already in a duel
        if (isInDuel(target)) {
            challenger.sendMessage(ColorUtil.error(target.getName() + " is already in a duel!"));
            return false;
        }
        
        // Check if there's an active duel (only one at a time)
        if (hasActiveDuel()) {
            challenger.sendMessage(ColorUtil.error("A duel is already in progress! Please wait and try again later."));
            return false;
        }
        
        // Check if arena is set
        if (arenaLocation == null) {
            challenger.sendMessage(ColorUtil.error("Arena location not set! Please contact an admin."));
            return false;
        }
        
        // Store challenge
        pendingChallenges.put(challengerUUID, targetUUID);
        
        // Send messages
        challenger.sendMessage(ColorUtil.format(ColorUtil.text("Challenge sent to ") + 
            ColorUtil.highlight(target.getName()) + ColorUtil.text("!")));
        target.sendMessage(ColorUtil.format(ColorUtil.text("You have been challenged by ") + 
            ColorUtil.highlight(challenger.getName()) + ColorUtil.text("! Use ") + 
            ColorUtil.highlight("/onevsone accept") + ColorUtil.text(" to accept.")));
        
        return true;
    }
    
    /**
     * Accept a challenge
     */
    public boolean acceptChallenge(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // Find if anyone challenged this player
        UUID challengerUUID = null;
        for (Map.Entry<UUID, UUID> entry : pendingChallenges.entrySet()) {
            if (entry.getValue().equals(playerUUID)) {
                challengerUUID = entry.getKey();
                break;
            }
        }
        
        if (challengerUUID == null) {
            player.sendMessage(ColorUtil.error("No pending challenge found!"));
            return false;
        }
        
        Player challenger = Bukkit.getPlayer(challengerUUID);
        if (challenger == null || !challenger.isOnline()) {
            player.sendMessage(ColorUtil.error("The challenger is no longer online!"));
            pendingChallenges.remove(challengerUUID);
            return false;
        }
        
        // Check if there's an active duel
        if (hasActiveDuel()) {
            player.sendMessage(ColorUtil.error("A duel is already in progress! Please wait and try again later."));
            pendingChallenges.remove(challengerUUID);
            return false;
        }
        
        // Remove challenge
        pendingChallenges.remove(challengerUUID);
        
        // Start duel
        startDuel(challenger, player);
        
        return true;
    }
    
    /**
     * Start a duel between two players
     */
    public void startDuel(Player player1, Player player2) {
        if (hasActiveDuel()) {
            return;
        }
        
        // Create duel
        int maxTime = plugin.getConfig().getInt("duel.max-time", 300);
        activeDuel = new Duel(player1.getUniqueId(), player2.getUniqueId(), maxTime);
        
        // Save inventories and locations
        savePlayerData(player1);
        savePlayerData(player2);
        activeDuel.setPlayer1OriginalLocation(player1.getLocation().clone());
        activeDuel.setPlayer2OriginalLocation(player2.getLocation().clone());
        
        // Save backpack items before clearing (if any)
        org.bukkit.inventory.ItemStack p1Backpack = com.playpandora.pandoraonevsone.integration.MinepacksIntegration.getBackpackItem(player1);
        org.bukkit.inventory.ItemStack p2Backpack = com.playpandora.pandoraonevsone.integration.MinepacksIntegration.getBackpackItem(player2);
        
        // Clear inventories
        player1.getInventory().clear();
        player2.getInventory().clear();
        
        // Give kits
        giveDuelKit(player1);
        giveDuelKit(player2);
        
        // Restore backpacks to empty slots AFTER hotbar (protection) - AFTER giving kit
        // Place backpack AFTER hotbar (slots 9-35) to avoid slot 0
        if (p1Backpack != null) {
            for (int i = 9; i < 36; i++) { // Start from slot 9 (after hotbar slots 0-8)
                if (player1.getInventory().getItem(i) == null || player1.getInventory().getItem(i).getType() == org.bukkit.Material.AIR) {
                    player1.getInventory().setItem(i, p1Backpack);
                    break;
                }
            }
        }
        if (p2Backpack != null) {
            for (int i = 9; i < 36; i++) {
                if (player2.getInventory().getItem(i) == null || player2.getInventory().getItem(i).getType() == org.bukkit.Material.AIR) {
                    player2.getInventory().setItem(i, p2Backpack);
                    break;
                }
            }
        }
        
        // CRITICAL: Force sword to slot 0 AFTER backpack placement
        // This ensures sword is in main hand even if backpack was placed in slot 0
        forceSwordToSlot0(player1);
        forceSwordToSlot0(player2);
        
        // Teleport to arena
        teleportToArena(player1, player2);
        
        // Add to in-duel set
        inDuel.add(player1.getUniqueId());
        inDuel.add(player2.getUniqueId());
        
        // Set metadata to bypass friend/team protection
        com.playpandora.pandoraonevsone.listeners.OneVsOnePvPListener.setBypassMetadata(player1);
        com.playpandora.pandoraonevsone.listeners.OneVsOnePvPListener.setBypassMetadata(player2);
        
        // Start countdown
        startCountdown(player1, player2);
    }
    
    /**
     * End a duel
     */
    public void endDuel(Duel duel, UUID winnerUUID) {
        if (duel == null || !duel.equals(activeDuel)) {
            return;
        }
        
        Player player1 = duel.getPlayer1();
        Player player2 = duel.getPlayer2();
        
        // Stop timer
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        
        // Determine winner message
        String winnerName = winnerUUID != null ? 
            (winnerUUID.equals(duel.getPlayer1UUID()) ? player1.getName() : player2.getName()) : "Timeout";
        
        // Send messages
        if (player1 != null && player1.isOnline()) {
            String message = winnerUUID != null ? 
                (winnerUUID.equals(duel.getPlayer1UUID()) ? "You won!" : player2.getName() + " won!") :
                "Duel timed out!";
            player1.sendMessage(ColorUtil.format(ColorUtil.text("Duel ended! ") + ColorUtil.highlight(message)));
            restorePlayer(player1, duel);
        }
        
        if (player2 != null && player2.isOnline()) {
            String message = winnerUUID != null ? 
                (winnerUUID.equals(duel.getPlayer2UUID()) ? "You won!" : player1.getName() + " won!") :
                "Duel timed out!";
            player2.sendMessage(ColorUtil.format(ColorUtil.text("Duel ended! ") + ColorUtil.highlight(message)));
            restorePlayer(player2, duel);
        }
        
        // Send Hook notifications
        if (player1 != null && player1.isOnline()) {
            com.playpandora.pandoraonevsone.integration.HookIntegration.sendDuelEnd(player1, 
                winnerUUID != null ? (winnerUUID.equals(duel.getPlayer1UUID()) ? "Victory!" : "Defeat!") : "Timeout!");
        }
        if (player2 != null && player2.isOnline()) {
            com.playpandora.pandoraonevsone.integration.HookIntegration.sendDuelEnd(player2, 
                winnerUUID != null ? (winnerUUID.equals(duel.getPlayer2UUID()) ? "Victory!" : "Defeat!") : "Timeout!");
        }
        
        // Remove bypass metadata
        if (player1 != null && player1.isOnline()) {
            com.playpandora.pandoraonevsone.listeners.OneVsOnePvPListener.removeBypassMetadata(player1);
        }
        if (player2 != null && player2.isOnline()) {
            com.playpandora.pandoraonevsone.listeners.OneVsOnePvPListener.removeBypassMetadata(player2);
        }
        
        // Clear duel
        activeDuel = null;
        inDuel.remove(duel.getPlayer1UUID());
        inDuel.remove(duel.getPlayer2UUID());
    }
    
    /**
     * Player leaves duel manually
     */
    public void leaveDuel(Player player) {
        if (!isInDuel(player)) {
            return;
        }
        
        Duel duel = getActiveDuel();
        if (duel == null || !duel.containsPlayer(player.getUniqueId())) {
            inDuel.remove(player.getUniqueId());
            return;
        }
        
        // Get opponent
        UUID opponentUUID = duel.getOpponent(player.getUniqueId());
        
        // End duel (opponent wins)
        endDuel(duel, opponentUUID);
    }
    
    /**
     * Save player inventory and location
     */
    private void savePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        
        try {
            // Save inventory
            ItemStack[] contents = player.getInventory().getContents();
            ItemStack[] armor = player.getInventory().getArmorContents();
            
            ItemStack[] fullInventory = new ItemStack[45];
            System.arraycopy(contents, 0, fullInventory, 0, 36);
            System.arraycopy(armor, 0, fullInventory, 36, 4);
            fullInventory[40] = player.getInventory().getItemInOffHand();
            
            String inventoryData = InventoryUtil.inventoryToString(fullInventory);
            savedInventories.put(uuid, inventoryData);
            
            // Save location
            savedLocations.put(uuid, player.getLocation().clone());
            
            plugin.getLogger().fine("Saved data for " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save data for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Restore player inventory and location
     */
    private void restorePlayer(Player player, Duel duel) {
        UUID uuid = player.getUniqueId();
        
        // Clear inventory first
        player.getInventory().clear();
        
        // Clear all potion effects
        for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        
        // Restore inventory
        String inventoryData = savedInventories.get(uuid);
        if (inventoryData != null) {
            try {
                ItemStack[] fullInventory = InventoryUtil.stringToInventory(inventoryData);
                
                ItemStack[] contents = new ItemStack[36];
                System.arraycopy(fullInventory, 0, contents, 0, 36);
                player.getInventory().setContents(contents);
                
                ItemStack[] armor = new ItemStack[4];
                System.arraycopy(fullInventory, 36, armor, 0, 4);
                player.getInventory().setArmorContents(armor);
                
                if (fullInventory.length > 40 && fullInventory[40] != null) {
                    player.getInventory().setItemInOffHand(fullInventory[40]);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to restore inventory for " + player.getName() + ": " + e.getMessage());
            }
        }
        
        // Restore location - try duel location first, then saved location, then spawn
        Location originalLocation = null;
        
        if (duel != null) {
            originalLocation = uuid.equals(duel.getPlayer1UUID()) ? 
                duel.getPlayer1OriginalLocation() : duel.getPlayer2OriginalLocation();
        }
        
        // Fallback to saved location if duel location is null
        if (originalLocation == null) {
            originalLocation = savedLocations.get(uuid);
        }
        
        // Make final for lambda
        final Location finalLocation = originalLocation != null ? originalLocation.clone() : null;
        final Player finalPlayer = player;
        
        // Teleport player back to original location with delay to ensure cleanup
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (finalLocation != null && finalLocation.getWorld() != null) {
                try {
                    // Verify world is still loaded
                    org.bukkit.World world = finalLocation.getWorld();
                    if (world == null) {
                        world = plugin.getServer().getWorld(finalLocation.getWorld().getName());
                    }
                    
                    if (world != null) {
                        // Use teleport directly (more reliable than console command)
                        finalPlayer.teleport(finalLocation);
                        plugin.getLogger().info("Restored " + finalPlayer.getName() + " to original location");
                    } else {
                        plugin.getLogger().warning("World not found for " + finalPlayer.getName() + " - teleporting to spawn");
                        teleportToSpawn(finalPlayer);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to teleport " + finalPlayer.getName() + " to saved location: " + e.getMessage());
                    e.printStackTrace();
                    // Fallback to spawn
                    teleportToSpawn(finalPlayer);
                }
            } else {
                // No saved location - teleport to spawn
                plugin.getLogger().info("No saved location for " + finalPlayer.getName() + " - teleporting to spawn");
                teleportToSpawn(finalPlayer);
            }
        });
        
        // Cleanup
        savedInventories.remove(uuid);
        savedLocations.remove(uuid);
    }
    
    /**
     * Give duel kit to player (full netherite + items)
     */
    private void giveDuelKit(Player player) {
        // Netherite armor
        ItemStack helmet = new ItemStack(Material.NETHERITE_HELMET);
        ItemMeta helmetMeta = helmet.getItemMeta();
        helmetMeta.addEnchant(Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("protection")), 4, true);
        helmetMeta.setUnbreakable(true);
        helmet.setItemMeta(helmetMeta);
        player.getInventory().setHelmet(helmet);
        
        ItemStack chestplate = new ItemStack(Material.NETHERITE_CHESTPLATE);
        ItemMeta chestplateMeta = chestplate.getItemMeta();
        chestplateMeta.addEnchant(Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("protection")), 4, true);
        chestplateMeta.setUnbreakable(true);
        chestplate.setItemMeta(chestplateMeta);
        player.getInventory().setChestplate(chestplate);
        
        ItemStack leggings = new ItemStack(Material.NETHERITE_LEGGINGS);
        ItemMeta leggingsMeta = leggings.getItemMeta();
        leggingsMeta.addEnchant(Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("protection")), 4, true);
        leggingsMeta.setUnbreakable(true);
        leggings.setItemMeta(leggingsMeta);
        player.getInventory().setLeggings(leggings);
        
        ItemStack boots = new ItemStack(Material.NETHERITE_BOOTS);
        ItemMeta bootsMeta = boots.getItemMeta();
        bootsMeta.addEnchant(Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("protection")), 4, true);
        bootsMeta.setUnbreakable(true);
        boots.setItemMeta(bootsMeta);
        player.getInventory().setBoots(boots);
        
        // Bow
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.addEnchant(Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("power")), 5, true);
        bowMeta.addEnchant(Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("punch")), 2, true);
        bowMeta.addEnchant(Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("unbreaking")), 3, true);
        bowMeta.addEnchant(Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("mending")), 1, true);
        bowMeta.setUnbreakable(true);
        bow.setItemMeta(bowMeta);
        player.getInventory().setItem(1, bow);
        
        // Arrows
        ItemStack arrows = new ItemStack(Material.ARROW, 64);
        player.getInventory().setItem(2, arrows);
        
        // Golden apples
        ItemStack gapples = new ItemStack(Material.GOLDEN_APPLE, 5);
        player.getInventory().setItem(3, gapples);
        
        // Potions - Speed II
        ItemStack speedPot = new ItemStack(Material.POTION);
        org.bukkit.inventory.meta.PotionMeta speedMeta = (org.bukkit.inventory.meta.PotionMeta) speedPot.getItemMeta();
        if (speedMeta != null) {
            speedMeta.addCustomEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SPEED, 3600, 1), true); // 3 minutes, Level II
            speedPot.setItemMeta(speedMeta);
        }
        player.getInventory().setItem(4, speedPot);
        
        // Potions - Strength II
        ItemStack strengthPot = new ItemStack(Material.POTION);
        org.bukkit.inventory.meta.PotionMeta strengthMeta = (org.bukkit.inventory.meta.PotionMeta) strengthPot.getItemMeta();
        if (strengthMeta != null) {
            strengthMeta.addCustomEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.STRENGTH, 3600, 1), true); // 3 minutes, Level II
            strengthPot.setItemMeta(strengthMeta);
        }
        player.getInventory().setItem(5, strengthPot);
        
        // Splash Potions - Instant Heal II
        ItemStack healPot = new ItemStack(Material.SPLASH_POTION);
        org.bukkit.inventory.meta.PotionMeta healMeta = (org.bukkit.inventory.meta.PotionMeta) healPot.getItemMeta();
        if (healMeta != null) {
            healMeta.addCustomEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.INSTANT_HEALTH, 1, 1), true); // Instant, Level II
            healPot.setItemMeta(healMeta);
        }
        player.getInventory().setItem(6, healPot);
        player.getInventory().setItem(7, healPot.clone());
        
        // Netherite sword - Give LAST to ensure it's in main hand
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.addEnchant(Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("sharpness")), 5, true);
        swordMeta.addEnchant(Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("unbreaking")), 3, true);
        swordMeta.addEnchant(Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("mending")), 1, true);
        swordMeta.setUnbreakable(true);
        sword.setItemMeta(swordMeta);
        
        // Set sword in main hand - this will override slot 0 even if backpack is there
        player.getInventory().setItemInMainHand(sword);
    }
    
    /**
     * Force sword to slot 0 (main hand) - CRITICAL for ensuring sword spawns
     * This method ensures sword is ALWAYS in slot 0, even if backpack is there
     */
    private void forceSwordToSlot0(Player player) {
        // Find sword in inventory
        ItemStack sword = null;
        int swordSlot = -1;
        
        for (int i = 0; i < 36; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.NETHERITE_SWORD) {
                sword = item;
                swordSlot = i;
                break;
            }
        }
        
        // If sword not found, create it (shouldn't happen but safety check)
        if (sword == null) {
            sword = new ItemStack(Material.NETHERITE_SWORD);
            ItemMeta swordMeta = sword.getItemMeta();
            swordMeta.addEnchant(Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("sharpness")), 5, true);
            swordMeta.addEnchant(Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("unbreaking")), 3, true);
            swordMeta.addEnchant(Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft("mending")), 1, true);
            swordMeta.setUnbreakable(true);
            sword.setItemMeta(swordMeta);
        }
        
        // Get what's currently in slot 0
        ItemStack slot0Item = player.getInventory().getItem(0);
        
        // If slot 0 has backpack, move it to another slot
        if (slot0Item != null && com.playpandora.pandoraonevsone.integration.MinepacksIntegration.isBackpackItem(slot0Item)) {
            // Find empty slot after hotbar for backpack
            for (int i = 9; i < 36; i++) {
                if (player.getInventory().getItem(i) == null || player.getInventory().getItem(i).getType() == Material.AIR) {
                    player.getInventory().setItem(i, slot0Item);
                    break;
                }
            }
        } else if (slot0Item != null && slot0Item.getType() != Material.NETHERITE_SWORD) {
            // If slot 0 has something else (not sword, not backpack), move it to sword's old slot or find empty slot
            if (swordSlot > 0 && swordSlot < 36) {
                player.getInventory().setItem(swordSlot, slot0Item);
            } else {
                // Find empty slot
                for (int i = 1; i < 36; i++) {
                    if (player.getInventory().getItem(i) == null || player.getInventory().getItem(i).getType() == Material.AIR) {
                        player.getInventory().setItem(i, slot0Item);
                        break;
                    }
                }
            }
        }
        
        // FORCE sword to slot 0 (main hand)
        player.getInventory().setItem(0, sword);
        player.getInventory().setItemInMainHand(sword);
    }
    
    /**
     * Teleport both players to arena
     * Tries warp command first, then falls back to direct teleport
     */
    private void teleportToArena(Player player1, Player player2) {
        if (arenaLocation == null) {
            plugin.getLogger().warning("Arena location not set! Cannot teleport players.");
            return;
        }
        
        // Try warp command via console (as requested)
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // Try common warp command formats
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), 
                "warp 1v1 " + player1.getName());
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), 
                "warp 1v1 " + player2.getName());
            
            // Fallback to direct teleport after short delay
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player1.isOnline()) {
                    player1.teleport(arenaLocation);
                }
                if (player2.isOnline()) {
                    player2.teleport(arenaLocation);
                }
            }, 10L); // Half second delay to let warp execute
        });
    }
    
    /**
     * Start countdown before duel begins
     */
    private void startCountdown(Player player1, Player player2) {
        int countdown = plugin.getConfig().getInt("duel.countdown", 5);
        
        new BukkitRunnable() {
            int count = countdown;
            
            @Override
            public void run() {
                if (count > 0) {
                    String message = ColorUtil.highlight(String.valueOf(count));
                    player1.sendMessage(ColorUtil.format(message));
                    player2.sendMessage(ColorUtil.format(message));
                    count--;
                } else {
                    // Start duel
                    player1.sendMessage(ColorUtil.format(ColorUtil.text("Duel started! Good luck!")));
                    player2.sendMessage(ColorUtil.format(ColorUtil.text("Duel started! Good luck!")));
                    
                    // Send Hook notifications
                    com.playpandora.pandoraonevsone.integration.HookIntegration.sendDuelStart(player1, player2);
                    
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    /**
     * Start timer task to check for timeouts
     */
    private void startTimerTask() {
        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (activeDuel == null) {
                    return;
                }
                
                // Check timeout
                if (activeDuel.isTimeout()) {
                    endDuel(activeDuel, null); // No winner - timeout
                    return;
                }
                
                // Check if players are still online
                Player player1 = activeDuel.getPlayer1();
                Player player2 = activeDuel.getPlayer2();
                
                if (player1 == null || !player1.isOnline()) {
                    endDuel(activeDuel, activeDuel.getPlayer2UUID());
                    return;
                }
                
                if (player2 == null || !player2.isOnline()) {
                    endDuel(activeDuel, activeDuel.getPlayer1UUID());
                    return;
                }
                
                // Check if players left arena (optional - can be implemented)
            }
        }.runTaskTimer(plugin, 20L, 20L); // Check every second
    }
    
    /**
     * Set arena location
     */
    public void setArenaLocation(Location location) {
        this.arenaLocation = location.clone();
        saveArenaLocation();
    }
    
    /**
     * Get arena location
     */
    public Location getArenaLocation() {
        return arenaLocation;
    }
    
    /**
     * Set spawn locations (optional - for separate spawn points)
     */
    public void setSpawnLocations(Location spawn1, Location spawn2) {
        this.spawn1Location = spawn1 != null ? spawn1.clone() : null;
        this.spawn2Location = spawn2 != null ? spawn2.clone() : null;
        saveArenaLocation();
    }
    
    /**
     * Load arena location from config
     */
    private void loadArenaLocation() {
        FileConfiguration config = plugin.getConfig();
        
        if (config.contains("arena.world") && !config.getString("arena.world").isEmpty()) {
            org.bukkit.World world = Bukkit.getWorld(config.getString("arena.world"));
            if (world != null) {
                double x = config.getDouble("arena.x");
                double y = config.getDouble("arena.y");
                double z = config.getDouble("arena.z");
                float yaw = (float) config.getDouble("arena.yaw");
                float pitch = (float) config.getDouble("arena.pitch");
                
                arenaLocation = new Location(world, x, y, z, yaw, pitch);
                
                // Load spawn locations if set
                if (config.contains("arena.spawn1.x")) {
                    double s1x = config.getDouble("arena.spawn1.x");
                    double s1y = config.getDouble("arena.spawn1.y");
                    double s1z = config.getDouble("arena.spawn1.z");
                    float s1yaw = (float) config.getDouble("arena.spawn1.yaw");
                    float s1pitch = (float) config.getDouble("arena.spawn1.pitch");
                    spawn1Location = new Location(world, s1x, s1y, s1z, s1yaw, s1pitch);
                }
                
                if (config.contains("arena.spawn2.x")) {
                    double s2x = config.getDouble("arena.spawn2.x");
                    double s2y = config.getDouble("arena.spawn2.y");
                    double s2z = config.getDouble("arena.spawn2.z");
                    float s2yaw = (float) config.getDouble("arena.spawn2.yaw");
                    float s2pitch = (float) config.getDouble("arena.spawn2.pitch");
                    spawn2Location = new Location(world, s2x, s2y, s2z, s2yaw, s2pitch);
                }
            }
        }
    }
    
    /**
     * Save arena location to config
     */
    private void saveArenaLocation() {
        FileConfiguration config = plugin.getConfig();
        
        if (arenaLocation != null) {
            config.set("arena.world", arenaLocation.getWorld().getName());
            config.set("arena.x", arenaLocation.getX());
            config.set("arena.y", arenaLocation.getY());
            config.set("arena.z", arenaLocation.getZ());
            config.set("arena.yaw", arenaLocation.getYaw());
            config.set("arena.pitch", arenaLocation.getPitch());
            
            if (spawn1Location != null) {
                config.set("arena.spawn1.x", spawn1Location.getX());
                config.set("arena.spawn1.y", spawn1Location.getY());
                config.set("arena.spawn1.z", spawn1Location.getZ());
                config.set("arena.spawn1.yaw", spawn1Location.getYaw());
                config.set("arena.spawn1.pitch", spawn1Location.getPitch());
            }
            
            if (spawn2Location != null) {
                config.set("arena.spawn2.x", spawn2Location.getX());
                config.set("arena.spawn2.y", spawn2Location.getY());
                config.set("arena.spawn2.z", spawn2Location.getZ());
                config.set("arena.spawn2.yaw", spawn2Location.getYaw());
                config.set("arena.spawn2.pitch", spawn2Location.getPitch());
            }
            
            plugin.saveConfig();
        }
    }
    
    /**
     * Teleport player to spawn location
     */
    private void teleportToSpawn(Player player) {
        try {
            Location spawn = player.getWorld().getSpawnLocation();
            if (spawn != null) {
                player.teleport(spawn);
                player.sendMessage(ColorUtil.format(ColorUtil.text("Teleported to spawn - your saved location was not found.")));
            } else {
                // Fallback to world spawn
                org.bukkit.World world = plugin.getServer().getWorlds().get(0);
                if (world != null) {
                    player.teleport(world.getSpawnLocation());
                    player.sendMessage(ColorUtil.format(ColorUtil.text("Teleported to spawn.")));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to teleport " + player.getName() + " to spawn: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Check if player has saved inventory/location
     */
    public boolean hasSavedInventory(Player player) {
        return savedInventories.containsKey(player.getUniqueId()) || 
               savedLocations.containsKey(player.getUniqueId());
    }
    
    /**
     * Clear player's saved data (used when they quit)
     */
    public void clearSavedData(UUID uuid) {
        savedInventories.remove(uuid);
        savedLocations.remove(uuid);
        inDuel.remove(uuid);
        
        // Clear from pending challenges
        pendingChallenges.entrySet().removeIf(entry -> 
            entry.getKey().equals(uuid) || entry.getValue().equals(uuid));
    }
    
    /**
     * Clear all player state (for quit/cleanup)
     */
    public void clearPlayerState(Player player) {
        UUID uuid = player.getUniqueId();
        inDuel.remove(uuid);
        clearSavedData(uuid);
        
        // Clear inventory if still has duel items
        player.getInventory().clear();
        
        // Clear potion effects
        for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }
    
    /**
     * End all active duels (for server shutdown)
     */
    public void endAllDuels() {
        if (activeDuel != null) {
            endDuel(activeDuel, null);
        }
        
        // Restore any remaining players
        for (UUID uuid : new HashSet<>(inDuel)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                inDuel.remove(uuid);
                // Inventory should be restored by endDuel, but cleanup just in case
            }
        }
    }
}

