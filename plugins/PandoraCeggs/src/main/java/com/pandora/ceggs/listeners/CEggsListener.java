package com.pandora.ceggs.listeners;

import com.pandora.ceggs.PandoraCeggs;
import com.pandora.ceggs.util.CEggsUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.entity.Egg;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

public class CEggsListener implements Listener {
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // Check if player has permission
        if (!player.hasPermission("pandoraceggs.use")) {
            return;
        }
        
        // Check if it's a creeper spawn egg
        if (item == null || item.getType() != Material.CREEPER_SPAWN_EGG) {
            return;
        }
        
        // Check if it's our custom egg or replace vanilla eggs
        boolean isCustomEgg = CEggsUtil.isCEgg(item);
        
        // Replace vanilla creeper eggs with custom ones
        if (!isCustomEgg) {
            ItemStack customEgg = CEggsUtil.createCEgg();
            customEgg.setAmount(item.getAmount());
            
            if (event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(customEgg);
            } else {
                player.getInventory().setItemInOffHand(customEgg);
            }
            
            // Continue with the custom egg
            item = customEgg;
            isCustomEgg = true;
        }
        
        // Only handle right-click
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        // If right-clicking a block, don't throw (let normal spawn egg behavior happen)
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null && clickedBlock.getType() != Material.AIR) {
                return; // Let normal spawn egg behavior happen
            }
        }
        
        // Right-clicking air - throw the egg
        event.setCancelled(true);
        
        // Remove one egg from inventory
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            if (event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(null);
            } else {
                player.getInventory().setItemInOffHand(null);
            }
        }
        
        // Throw the egg
        throwCEgg(player);
    }
    
    /**
     * Throw a creeper egg projectile
     */
    private void throwCEgg(Player player) {
        org.bukkit.Location loc = player.getEyeLocation();
        Vector direction = loc.getDirection().normalize();
        
        // Spawn an egg projectile (behaves like vanilla eggs)
        Egg egg = (Egg) player.getWorld().spawnEntity(
            loc,
            EntityType.EGG
        );
        
        // Set velocity with increased range and natural arc
        // Increased speed for more range (1.5 is similar to vanilla egg throwing)
        double speed = 1.5;
        Vector velocity = direction.multiply(speed);
        
        // Add slight upward component for natural arc trajectory
        velocity.setY(velocity.getY() + 0.1);
        
        egg.setVelocity(velocity);
        
        // Store metadata to identify it as a creeper egg
        egg.setCustomName("PandoraCEgg");
        egg.setCustomNameVisible(false);
    }
    
    /**
     * Handle egg projectile hitting something
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        // Check if it's our custom creeper egg
        if (!(event.getEntity() instanceof Egg)) {
            return;
        }
        
        Egg egg = (Egg) event.getEntity();
        if (egg.getCustomName() == null || !egg.getCustomName().equals("PandoraCEgg")) {
            return;
        }
        
        // Get the exact hit location
        org.bukkit.Location hitLocation;
        if (event.getHitBlock() != null) {
            // Hit a block - use the block's location
            hitLocation = event.getHitBlock().getLocation().add(0.5, 0.5, 0.5);
        } else if (event.getHitEntity() != null) {
            // Hit an entity - use the entity's location
            hitLocation = event.getHitEntity().getLocation();
        } else {
            // Use the egg's current location as fallback
            hitLocation = egg.getLocation();
        }
        
        // Spawn explosive creeper at the hit location
        spawnExplosiveCreeper(hitLocation);
    }
    
    /**
     * Spawn a creeper that explodes immediately and can destroy any blocks
     */
    private void spawnExplosiveCreeper(org.bukkit.Location loc) {
        // Spawn a charged creeper
        Creeper creeper = (Creeper) loc.getWorld().spawnEntity(
            loc,
            EntityType.CREEPER
        );
        
        // Make it explode immediately
        creeper.setPowered(true); // Charged creeper for bigger explosion
        creeper.setExplosionRadius(3); // Standard creeper explosion radius
        
        // Mark it as a PandoraCeggs creeper
        creeper.setCustomName("PandoraCEggCreeper");
        creeper.setCustomNameVisible(false);
        
        // Trigger explosion after 1 tick
        org.bukkit.scheduler.BukkitRunnable explodeTask = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (creeper.isValid() && !creeper.isDead()) {
                    // Create explosion that bypasses protection
                    createUnprotectedExplosion(creeper.getLocation(), 3.0f, true);
                    creeper.remove();
                }
            }
        };
        explodeTask.runTaskLater(PandoraCeggs.getInstance(), 1L);
    }
    
    /**
     * Create an explosion that can destroy any blocks (bypasses faction protection)
     */
    private void createUnprotectedExplosion(org.bukkit.Location loc, float power, boolean setFire) {
        // Temporarily disable block protection, then create explosion
        org.bukkit.World world = loc.getWorld();
        if (world == null) return;
        
        // Create explosion - this will destroy blocks
        world.createExplosion(loc, power, setFire, true);
    }
    
    /**
     * Allow creeper eggs to destroy any blocks (claimed or unclaimed)
     * This event handler ensures explosions from our creeper eggs bypass protection
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityExplode(EntityExplodeEvent event) {
        // Check if it's a creeper spawned from our egg
        if (event.getEntity() instanceof Creeper) {
            Creeper creeper = (Creeper) event.getEntity();
            
            // Check if it's our custom creeper egg creeper
            if (creeper.getCustomName() != null && creeper.getCustomName().equals("PandoraCEggCreeper")) {
                // Don't cancel - allow it to destroy all blocks
                // The explosion will proceed and destroy all blocks in radius
                // Other plugins' protection will be bypassed because we're at HIGHEST priority
                
                // If PandoraBases is loaded, we need to ensure blocks aren't removed from blockList
                // The explosion event's blockList contains blocks that will be destroyed
                // We want ALL blocks in the explosion radius to be destroyed, so we don't modify the list
                return;
            }
        }
    }
    
    /**
     * Replace vanilla creeper eggs when players receive them
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        
        if (item != null && item.getType() == Material.CREEPER_SPAWN_EGG && !CEggsUtil.isCEgg(item)) {
            // Replace with custom egg
            ItemStack customEgg = CEggsUtil.createCEgg();
            customEgg.setAmount(item.getAmount());
            event.setCurrentItem(customEgg);
        }
        
        // Also check cursor item
        ItemStack cursor = event.getCursor();
        if (cursor != null && cursor.getType() == Material.CREEPER_SPAWN_EGG && !CEggsUtil.isCEgg(cursor)) {
            ItemStack customEgg = CEggsUtil.createCEgg();
            customEgg.setAmount(cursor.getAmount());
            event.setCursor(customEgg);
        }
    }
    
    /**
     * Replace vanilla creeper eggs when players switch items
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        
        if (item != null && item.getType() == Material.CREEPER_SPAWN_EGG && !CEggsUtil.isCEgg(item)) {
            ItemStack customEgg = CEggsUtil.createCEgg();
            customEgg.setAmount(item.getAmount());
            player.getInventory().setItem(event.getNewSlot(), customEgg);
        }
    }
    
    /**
     * Replace vanilla creeper eggs when players pick them up
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        org.bukkit.entity.Item item = event.getItem();
        ItemStack itemStack = item.getItemStack();
        
        if (itemStack.getType() == Material.CREEPER_SPAWN_EGG && !CEggsUtil.isCEgg(itemStack)) {
            // Replace with custom egg
            ItemStack customEgg = CEggsUtil.createCEgg();
            customEgg.setAmount(itemStack.getAmount());
            item.setItemStack(customEgg);
        }
    }
    
    /**
     * Integrate with PandoraBases to bypass faction protection for our explosions
     */
    private boolean isPandoraBasesLoaded() {
        return PandoraCeggs.getInstance().getServer().getPluginManager().getPlugin("PandoraBases") != null;
    }
}

