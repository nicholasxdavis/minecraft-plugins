package com.playpandora.moremobs.handlers;

import com.playpandora.moremobs.MoreMobs;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DropModifier implements Listener {
    
    private final MoreMobs plugin;
    private final Random random = new Random();
    
    public DropModifier(MoreMobs plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!plugin.getConfig().getBoolean("drop-improvements.enabled", true)) {
            return;
        }
        
        LivingEntity entity = event.getEntity();
        List<ItemStack> drops = event.getDrops();
        
        // Handle specific mob types
        switch (entity.getType()) {
            case DROWNED:
                handleDrownedDrops((Drowned) entity, drops);
                break;
            case PHANTOM:
                handlePhantomDrops((Phantom) entity, drops);
                break;
            case GOAT:
                handleGoatDrops((Goat) entity, drops);
                break;
            case FROG:
                handleFrogDrops((Frog) entity, drops);
                break;
            case PIGLIN_BRUTE:
                handlePiglinBruteDrops((PiglinBrute) entity, drops);
                break;
            case ARMADILLO:
                handleArmadilloDrops((Armadillo) entity, drops);
                break;
            default:
                break;
        }
    }
    
    private void handleDrownedDrops(Drowned drowned, List<ItemStack> drops) {
        if (!plugin.getConfig().getBoolean("drop-improvements.drowned.enabled", true)) return;
        
        double tridentBoost = plugin.getConfig().getDouble("drop-improvements.drowned.trident-drop-chance-boost", 0.01);
        
        // Check if drowned has trident
        ItemStack heldItem = drowned.getEquipment().getItemInMainHand();
        if (heldItem.getType() == Material.TRIDENT) {
            // Vanilla trident drop chance is already calculated, we add extra chance
            if (random.nextDouble() < tridentBoost) {
                drops.add(new ItemStack(Material.TRIDENT));
            }
        }
    }
    
    private void handlePhantomDrops(Phantom phantom, List<ItemStack> drops) {
        if (!plugin.getConfig().getBoolean("drop-improvements.phantom.enabled", true)) return;
        
        double membraneMultiplier = plugin.getConfig().getDouble("drop-improvements.phantom.membrane-drop-multiplier", 1.5);
        
        // Find membrane drops and multiply them
        List<ItemStack> newDrops = new ArrayList<>();
        for (ItemStack drop : drops) {
            if (drop.getType() == Material.PHANTOM_MEMBRANE) {
                int originalAmount = drop.getAmount();
                int newAmount = (int) Math.ceil(originalAmount * membraneMultiplier);
                newDrops.add(new ItemStack(Material.PHANTOM_MEMBRANE, newAmount));
            } else {
                newDrops.add(drop);
            }
        }
        
        // If no membrane was dropped, add one with chance
        boolean hasMembrane = drops.stream().anyMatch(d -> d.getType() == Material.PHANTOM_MEMBRANE);
        if (!hasMembrane && random.nextDouble() < (membraneMultiplier - 1.0) * 0.3) {
            newDrops.add(new ItemStack(Material.PHANTOM_MEMBRANE, 1));
        }
        
        drops.clear();
        drops.addAll(newDrops);
    }
    
    private void handleGoatDrops(Goat goat, List<ItemStack> drops) {
        if (!plugin.getConfig().getBoolean("drop-improvements.goat.enabled", true)) return;
        
        double hornMultiplier = plugin.getConfig().getDouble("drop-improvements.goat.horn-drop-multiplier", 1.4);
        
        // Check if goat drops horn and multiply
        List<ItemStack> newDrops = new ArrayList<>();
        boolean hasHorn = false;
        
        for (ItemStack drop : drops) {
            if (drop.getType() == Material.GOAT_HORN) {
                hasHorn = true;
                int originalAmount = drop.getAmount();
                int newAmount = (int) Math.ceil(originalAmount * hornMultiplier);
                newDrops.add(new ItemStack(Material.GOAT_HORN, newAmount));
            } else {
                newDrops.add(drop);
            }
        }
        
        // Add extra horn chance
        if (hasHorn && random.nextDouble() < (hornMultiplier - 1.0) * 0.25) {
            newDrops.add(new ItemStack(Material.GOAT_HORN, 1));
        }
        
        drops.clear();
        drops.addAll(newDrops);
    }
    
    private void handleFrogDrops(Frog frog, List<ItemStack> drops) {
        if (!plugin.getConfig().getBoolean("drop-improvements.frog.enabled", true)) return;
        
        double slimeMultiplier = plugin.getConfig().getDouble("drop-improvements.frog.slime-drop-multiplier", 1.6);
        
        // Find slime drops
        List<ItemStack> newDrops = new ArrayList<>();
        boolean hasSlime = false;
        
        for (ItemStack drop : drops) {
            if (drop.getType() == Material.SLIME_BALL) {
                hasSlime = true;
                int originalAmount = drop.getAmount();
                int newAmount = (int) Math.ceil(originalAmount * slimeMultiplier);
                newDrops.add(new ItemStack(Material.SLIME_BALL, newAmount));
            } else {
                newDrops.add(drop);
            }
        }
        
        // Add extra slime chance if already dropping slime
        if (hasSlime && random.nextDouble() < (slimeMultiplier - 1.0) * 0.3) {
            newDrops.add(new ItemStack(Material.SLIME_BALL, 1));
        }
        
        drops.clear();
        drops.addAll(newDrops);
    }
    
    private void handlePiglinBruteDrops(PiglinBrute brute, List<ItemStack> drops) {
        if (!plugin.getConfig().getBoolean("drop-improvements.piglin-brute.enabled", true)) return;
        
        double goldMultiplier = plugin.getConfig().getDouble("drop-improvements.piglin-brute.gold-loot-multiplier", 1.3);
        
        // Multiply gold-related drops
        List<ItemStack> newDrops = new ArrayList<>();
        
        for (ItemStack drop : drops) {
            Material type = drop.getType();
            if (type == Material.GOLD_INGOT || type == Material.GOLD_NUGGET || type == Material.GOLDEN_SWORD) {
                int originalAmount = drop.getAmount();
                int newAmount = (int) Math.ceil(originalAmount * goldMultiplier);
                newDrops.add(new ItemStack(type, newAmount));
            } else {
                newDrops.add(drop);
            }
        }
        
        // Add extra gold chance
        if (random.nextDouble() < (goldMultiplier - 1.0) * 0.2) {
            newDrops.add(new ItemStack(Material.GOLD_NUGGET, 2 + random.nextInt(4)));
        }
        
        drops.clear();
        drops.addAll(newDrops);
    }
    
    private void handleArmadilloDrops(Armadillo armadillo, List<ItemStack> drops) {
        if (!plugin.getConfig().getBoolean("drop-improvements.armadillo.enabled", true)) return;
        
        double scuteMultiplier = plugin.getConfig().getDouble("drop-improvements.armadillo.scute-drop-multiplier", 1.5);
        
        // Find scute drops and multiply
        List<ItemStack> newDrops = new ArrayList<>();
        boolean hasScute = false;
        
        for (ItemStack drop : drops) {
            if (drop.getType() == Material.ARMADILLO_SCUTE) {
                hasScute = true;
                int originalAmount = drop.getAmount();
                int newAmount = (int) Math.ceil(originalAmount * scuteMultiplier);
                newDrops.add(new ItemStack(Material.ARMADILLO_SCUTE, newAmount));
            } else {
                newDrops.add(drop);
            }
        }
        
        // Add extra scute chance
        if (hasScute && random.nextDouble() < (scuteMultiplier - 1.0) * 0.3) {
            newDrops.add(new ItemStack(Material.ARMADILLO_SCUTE, 1));
        }
        
        drops.clear();
        drops.addAll(newDrops);
    }
    
    public void cleanup() {
        // Nothing to clean up
    }
}

