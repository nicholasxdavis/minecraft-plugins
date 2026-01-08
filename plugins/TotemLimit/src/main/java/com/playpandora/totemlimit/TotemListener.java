package com.playpandora.totemlimit;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class TotemListener implements Listener {
    
    private final TotemLimit plugin;
    private static final Material TOTEM_MATERIAL = Material.TOTEM_OF_UNDYING;
    private static final Material SHIELD_MATERIAL = Material.SHIELD;
    
    // Track players who are blocking with swords
    private final Map<UUID, BukkitTask> blockingPlayers = new HashMap<>();
    
    // Track players who have used a totem this life
    private final Map<UUID, Boolean> usedTotemThisLife = new HashMap<>();
    
    // Random for chance calculations
    private final Random random = new Random();
    
    // Chance for totem to grant opponent an extra life (5% = 0.05)
    private static final double OPPONENT_EXTRA_LIFE_CHANCE = 0.05;
    
    // Sword materials (all sword types)
    private static final Material[] SWORD_MATERIALS = {
        Material.WOODEN_SWORD,
        Material.STONE_SWORD,
        Material.IRON_SWORD,
        Material.GOLDEN_SWORD,
        Material.DIAMOND_SWORD,
        Material.NETHERITE_SWORD
    };
    
    // Message prefix
    private static final String PREFIX = "&e&lPandora &8» &r";
    
    public TotemListener(TotemLimit plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Formats a message with the plugin prefix
     */
    private String formatMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', PREFIX + message);
    }
    
    /**
     * Adds lore to a totem explaining the mechanic
     */
    private void addTotemLore(ItemStack totem) {
        if (totem == null || totem.getType() != TOTEM_MATERIAL) {
            return;
        }
        
        ItemMeta meta = totem.getItemMeta();
        if (meta == null) {
            return;
        }
        
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        } else {
            // Check if lore already exists to avoid duplicates
            if (!lore.isEmpty() && lore.get(0).contains("Only grants")) {
                return;
            }
        }
        
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Only grants &6&l1 &7extra life per life"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7&oMay grant your opponent an extra life"));
        
        meta.setLore(lore);
        totem.setItemMeta(meta);
    }
    
    /**
     * Applies lore to all totems in a player's inventory
     */
    private void applyLoreToTotems(Player player) {
        PlayerInventory inventory = player.getInventory();
        
        // Check all inventory contents
        ItemStack[] contents = inventory.getContents();
        for (ItemStack item : contents) {
            if (item != null && item.getType() == TOTEM_MATERIAL) {
                addTotemLore(item);
            }
        }
        
        // Check offhand
        ItemStack offhand = inventory.getItemInOffHand();
        if (offhand != null && offhand.getType() == TOTEM_MATERIAL) {
            addTotemLore(offhand);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if the clicked item or cursor item is a totem
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        
        // Apply lore to totems when they're moved
        if (clickedItem != null && clickedItem.getType() == TOTEM_MATERIAL) {
            addTotemLore(clickedItem);
        }
        if (cursorItem != null && cursorItem.getType() == TOTEM_MATERIAL) {
            addTotemLore(cursorItem);
        }
        
        // Schedule lore application after the event completes
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            applyLoreToTotems(player);
        });
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        
        // Apply lore to totems when closing inventory
        applyLoreToTotems(player);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        ItemStack item = event.getItem().getItemStack();
        
        if (item.getType() == TOTEM_MATERIAL) {
            // Add lore to the totem
            addTotemLore(item);
            
            // Schedule lore application after pickup completes
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                applyLoreToTotems(player);
            });
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            removeShields(player);
        });
    }
    
    /**
     * Handles totem resurrection - enforces 1 life per life limit
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        UUID uuid = player.getUniqueId();
        
        // Check if player has already used a totem this life
        if (usedTotemThisLife.getOrDefault(uuid, false)) {
            // Cancel the resurrection - player already used a totem this life
            event.setCancelled(true);
            player.sendMessage(formatMessage("&7You can only get &6&l1 &7extra life per life!"));
            return;
        }
        
        // Mark that player has used a totem this life
        usedTotemThisLife.put(uuid, true);
        
        // Check for nearby opponents to potentially grant an extra life
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            checkAndGrantOpponentLife(player);
        });
    }
    
    /**
     * Checks if totem should grant opponent an extra life and does so
     */
    private void checkAndGrantOpponentLife(Player player) {
        // Roll chance for opponent extra life
        if (random.nextDouble() >= OPPONENT_EXTRA_LIFE_CHANCE) {
            return; // Chance didn't trigger
        }
        
        // Find nearest opponent (player within 50 blocks)
        Player nearestOpponent = null;
        double nearestDistance = 50.0;
        
        for (Player other : plugin.getServer().getOnlinePlayers()) {
            if (other.equals(player) || !other.getWorld().equals(player.getWorld())) {
                continue;
            }
            
            double distance = other.getLocation().distance(player.getLocation());
            if (distance < nearestDistance) {
                nearestOpponent = other;
                nearestDistance = distance;
            }
        }
        
        if (nearestOpponent != null) {
            // Grant opponent an extra life (reset their used totem flag)
            usedTotemThisLife.put(nearestOpponent.getUniqueId(), false);
            
            // Notify both players
            player.sendMessage(formatMessage("&c&lWARNING! &7Your totem's dark magic has granted &6" + 
                nearestOpponent.getName() + " &7an extra life!"));
            nearestOpponent.sendMessage(formatMessage("&a&lLUCKY! &7You have been granted an extra life by dark magic!"));
        }
    }
    
    /**
     * Resets totem usage when player respawns (new life)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Reset totem usage for new life
        usedTotemThisLife.put(uuid, false);
        
        // Apply lore to totems after respawn
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            applyLoreToTotems(player);
        });
    }
    
    /**
     * Checks if a material is a sword
     */
    private boolean isSword(Material material) {
        for (Material sword : SWORD_MATERIALS) {
            if (material == sword) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Removes all shields from a player's inventory
     */
    private void removeShields(Player player) {
        PlayerInventory inventory = player.getInventory();
        
        // Remove shields from main inventory
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() == SHIELD_MATERIAL) {
                inventory.setItem(i, null);
            }
        }
        
        // Remove shield from offhand
        ItemStack offhand = inventory.getItemInOffHand();
        if (offhand != null && offhand.getType() == SHIELD_MATERIAL) {
            inventory.setItemInOffHand(null);
        }
        
        // Update inventory
        player.updateInventory();
    }
    
    /**
     * Enforces shield removal when inventory changes
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClickShield(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if the clicked item is a shield
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        
        boolean isShieldClick = (clickedItem != null && clickedItem.getType() == SHIELD_MATERIAL) ||
                               (cursorItem != null && cursorItem.getType() == SHIELD_MATERIAL);
        
        if (isShieldClick) {
            // Cancel the event to prevent shield usage
            event.setCancelled(true);
            player.sendMessage(formatMessage("&7Shields are &6&ldisabled &7on this server!"));
            
            // Schedule removal after the event completes
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                removeShields(player);
            });
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryCloseShield(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        
        // Remove shields when closing inventory
        removeShields(player);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityPickupShield(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        ItemStack item = event.getItem().getItemStack();
        
        if (item.getType() == SHIELD_MATERIAL) {
            // Cancel shield pickup
            event.setCancelled(true);
            player.sendMessage(formatMessage("&7Shields are &6&ldisabled &7on this server!"));
        }
    }
    
    /**
     * Prevents shield usage/interaction
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractShield(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        // Check if player is trying to use a shield
        if (item != null && item.getType() == SHIELD_MATERIAL) {
            event.setCancelled(true);
            player.sendMessage(formatMessage("&7Shields are &6&ldisabled &7on this server!"));
            removeShields(player);
            return;
        }
        
        // Check offhand
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && offhand.getType() == SHIELD_MATERIAL) {
            event.setCancelled(true);
            player.sendMessage(formatMessage("&7Shields are &6&ldisabled &7on this server!"));
            removeShields(player);
        }
    }
    
    /**
     * Implements sword blocking (1.7/1.8 style)
     * When right-clicking with a sword, apply resistance and slowness
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractSword(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        // Check if player is holding a sword
        if (item == null || !isSword(item.getType())) {
            // Stop blocking if they were blocking before
            stopBlocking(player);
            return;
        }
        
        // Start blocking on right-click
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            startBlocking(player);
        } else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Stop blocking on left-click
            stopBlocking(player);
        }
    }
    
    /**
     * Stops blocking when player releases right-click or switches items
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSwapHandItems(org.bukkit.event.player.PlayerSwapHandItemsEvent event) {
        stopBlocking(event.getPlayer());
    }
    
    /**
     * Stops blocking when player attacks (left-click)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerAttack(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            stopBlocking((Player) event.getDamager());
        }
    }
    
    /**
     * Stops blocking when player takes damage (like old sword blocking)
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        // If player is blocking, reduce damage by 50% (like old sword blocking)
        if (isBlocking(player)) {
            double damage = event.getDamage();
            event.setDamage(damage * 0.5);
        }
    }
    
    /**
     * Starts sword blocking for a player
     */
    private void startBlocking(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Cancel existing blocking task if any
        stopBlocking(player);
        
        // Apply resistance and slowness effects (like old sword blocking)
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 0, false, false));
        
        // Schedule task to check if player is still blocking
        // Check more frequently to maintain smooth blocking
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            // Check if player is online and still holding a sword
            if (!player.isOnline()) {
                stopBlocking(player);
                return;
            }
            
            ItemStack item = player.getInventory().getItemInMainHand();
            
            // Check if player is still holding a sword
            if (item == null || !isSword(item.getType())) {
                stopBlocking(player);
                return;
            }
            
            // Re-apply effects to maintain blocking (40 ticks = 2 seconds)
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0, false, false));
        }, 0L, 10L); // Check every 0.5 seconds
        
        blockingPlayers.put(uuid, task);
    }
    
    /**
     * Stops sword blocking for a player
     */
    private void stopBlocking(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Cancel blocking task
        BukkitTask task = blockingPlayers.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        
        // Remove blocking effects
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
    }
    
    /**
     * Checks if a player is currently blocking
     */
    private boolean isBlocking(Player player) {
        return blockingPlayers.containsKey(player.getUniqueId());
    }
    
    /**
     * Clean up blocking when player quits
     */
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        stopBlocking(player);
        
        // Clean up totem usage tracking
        usedTotemThisLife.remove(player.getUniqueId());
    }
}

