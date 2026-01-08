package com.massivecraft.factions.listeners;

import com.massivecraft.factions.*;
import com.massivecraft.factions.util.CombatTagManager;
import com.massivecraft.factions.util.PandoraMessage;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Arrays;
import java.util.List;

/**
 * Handles combat tagging events
 */
public class CombatTagListener implements Listener {

    private static final List<String> BLOCKED_COMMANDS = Arrays.asList(
        "spawn", "home", "wild", "rtp", "warp", "tp", "tpa", "tpahere",
        "tpaccept", "tpdeny", "essentials:spawn", "essentials:home",
        "essentials:warp", "cmi:spawn", "cmi:home", "cmi:warp",
        "f home", "f h", "f warp", "f w", "base home", "base h"
    );

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!Conf.combatTagEnabled) {
            return;
        }

        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        // Only tag players
        if (!(victim instanceof Player)) {
            return;
        }

        Player victimPlayer = (Player) victim;
        Player attackerPlayer = null;

        // Get attacker player
        if (damager instanceof Player) {
            attackerPlayer = (Player) damager;
        } else if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player) {
                attackerPlayer = (Player) shooter;
            }
        } else if (damager instanceof Tameable) {
            Tameable pet = (Tameable) damager;
            if (pet.getOwner() instanceof Player) {
                attackerPlayer = (Player) pet.getOwner();
            }
        }

        // Tag both players if PvP
        if (attackerPlayer != null && attackerPlayer != victimPlayer) {
            // Check if both can be tagged
            CombatTagManager manager = CombatTagManager.getInstance();
            if (manager.canTagPlayer(attackerPlayer) && manager.canTagPlayer(victimPlayer)) {
                // If already tagged, refresh instead of full tag
                if (manager.isTagged(attackerPlayer) || manager.isTagged(victimPlayer)) {
                    manager.refreshTag(attackerPlayer);
                    manager.refreshTag(victimPlayer);
                } else {
                    manager.tagPlayers(attackerPlayer, victimPlayer);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!Conf.combatTagEnabled) {
            return;
        }

        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();
        
        if (!(shooter instanceof Player)) {
            return;
        }

        Player attacker = (Player) shooter;
        
        // Check if projectile hit a player
        if (event.getHitEntity() instanceof Player) {
            Player victim = (Player) event.getHitEntity();
            
            if (attacker != victim) {
                CombatTagManager manager = CombatTagManager.getInstance();
                if (manager.canTagPlayer(attacker) && manager.canTagPlayer(victim)) {
                    // If already tagged, refresh instead of full tag
                    if (manager.isTagged(attacker) || manager.isTagged(victim)) {
                        manager.refreshTag(attacker);
                        manager.refreshTag(victim);
                    } else {
                        manager.tagPlayers(attacker, victim);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!Conf.combatTagEnabled) {
            return;
        }

        Player player = event.getPlayer();
        CombatTagManager manager = CombatTagManager.getInstance();
        
        if (!manager.isTagged(player)) {
            return; // Not tagged, allow command
        }

        String command = event.getMessage().toLowerCase().substring(1); // Remove '/'
        String[] args = command.split(" ");
        String baseCommand = args[0];

        // Check if command is blocked
        for (String blocked : BLOCKED_COMMANDS) {
            if (baseCommand.equals(blocked) || baseCommand.startsWith(blocked + ":")) {
                event.setCancelled(true);
                int remaining = manager.getRemainingSeconds(player);
                player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("✗ Command Blocked!")));
                player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You cannot use this command while in combat!")));
                player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("Combat tag expires in ") + 
                    PandoraMessage.highlight(remaining + " seconds") + PandoraMessage.text(".")));
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!Conf.combatTagEnabled) {
            return;
        }

        Player player = event.getPlayer();
        CombatTagManager manager = CombatTagManager.getInstance();
        
        if (!manager.isTagged(player)) {
            return; // Not tagged, allow teleport
        }

        // Allow teleport if it's within same world and close (like ender pearl)
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (to != null && from.getWorld() == to.getWorld()) {
            double distance = from.distance(to);
            if (distance <= Conf.combatTagAllowEnderPearlDistance) {
                return; // Allow short-distance teleports (ender pearls)
            }
        }

        // Block teleportation
        event.setCancelled(true);
        int remaining = manager.getRemainingSeconds(player);
        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("✗ Teleportation Blocked!")));
        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You cannot teleport while in combat!")));
        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("Combat tag expires in ") + 
            PandoraMessage.highlight(remaining + " seconds") + PandoraMessage.text(".")));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!Conf.combatTagEnabled) {
            return;
        }

        Player player = event.getPlayer();
        CombatTagManager manager = CombatTagManager.getInstance();
        
        if (!manager.isTagged(player)) {
            return; // Not tagged, allow normal logout
        }

        // Handle combat logout
        handleCombatLogout(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!Conf.combatTagEnabled || !Conf.combatTagBlockSafezoneEntry) {
            return;
        }

        Player player = event.getPlayer();
        CombatTagManager manager = CombatTagManager.getInstance();
        
        if (!manager.isTagged(player)) {
            return; // Not tagged, allow movement
        }

        Location to = event.getTo();
        if (to == null) {
            return;
        }

        FLocation floc = FLocation.wrap(to);
        Faction faction = Board.getInstance().getFactionAt(floc);
        
        // Check if trying to enter safezone
        if (faction != null && faction.isSafeZone()) {
            // Block entry with knockback
            event.setCancelled(true);
            
            // Knockback player away from safezone
            Location from = event.getFrom();
            double dx = to.getX() - from.getX();
            double dz = to.getZ() - from.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            
            if (distance > 0) {
                double knockback = Conf.combatTagSafezoneKnockback;
                org.bukkit.util.Vector knockbackVec = new org.bukkit.util.Vector(
                    -dx / distance * knockback,
                    0.2,
                    -dz / distance * knockback);
                player.setVelocity(player.getVelocity().add(knockbackVec));
            }
            
            int remaining = manager.getRemainingSeconds(player);
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("✗ Safezone Entry Blocked!")));
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You cannot enter safezones while in combat!")));
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("Combat tag expires in ") + 
                PandoraMessage.highlight(remaining + " seconds") + PandoraMessage.text(".")));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        if (!Conf.combatTagEnabled || !Conf.combatTagDisableFlight) {
            return;
        }

        Player player = event.getPlayer();
        CombatTagManager manager = CombatTagManager.getInstance();
        
        if (manager.isTagged(player) && event.isFlying()) {
            // Disable flight if trying to fly while tagged
            event.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);
            
            int remaining = manager.getRemainingSeconds(player);
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("✗ Flight Disabled!")));
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You cannot fly while in combat!")));
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.text("Combat tag expires in ") + 
                PandoraMessage.highlight(remaining + " seconds") + PandoraMessage.text(".")));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!Conf.combatTagEnabled) {
            return;
        }

        // Handle fishing rod and other indirect damage
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) event;
            Entity damager = damageEvent.getDamager();
            
            // Check for fishing rod (hook entity)
            if (damager instanceof org.bukkit.entity.FishHook) {
                org.bukkit.entity.FishHook hook = (org.bukkit.entity.FishHook) damager;
                if (hook.getShooter() instanceof Player && event.getEntity() instanceof Player) {
                    Player attacker = (Player) hook.getShooter();
                    Player victim = (Player) event.getEntity();
                    
                    if (attacker != victim) {
                        CombatTagManager manager = CombatTagManager.getInstance();
                        if (manager.canTagPlayer(attacker) && manager.canTagPlayer(victim)) {
                            if (manager.isTagged(attacker) || manager.isTagged(victim)) {
                                manager.refreshTag(attacker);
                                manager.refreshTag(victim);
                            } else {
                                manager.tagPlayers(attacker, victim);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle combat logout based on configuration
     */
    private void handleCombatLogout(Player player) {
        switch (Conf.combatTagLogoutAction) {
            case "npc":
                spawnCombatLoggerNPC(player);
                break;
            case "kill":
                killPlayerOnLogout(player);
                break;
            case "drop":
                dropInventoryOnLogout(player);
                break;
            default:
                // Default: kill player
                killPlayerOnLogout(player);
                break;
        }
    }

    /**
     * Spawn NPC combat logger
     */
    private void spawnCombatLoggerNPC(Player player) {
        // TODO: Implement NPC logger if NPC plugin is available
        // For now, fall back to kill
        killPlayerOnLogout(player);
    }

    /**
     * Kill player on logout
     */
    private void killPlayerOnLogout(Player player) {
        Location loc = player.getLocation();
        
        // Drop inventory
        if (Conf.combatTagDropInventory) {
            for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() != org.bukkit.Material.AIR) {
                    loc.getWorld().dropItemNaturally(loc, item);
                }
            }
            for (org.bukkit.inventory.ItemStack item : player.getInventory().getArmorContents()) {
                if (item != null && item.getType() != org.bukkit.Material.AIR) {
                    loc.getWorld().dropItemNaturally(loc, item);
                }
            }
        }
        
        // Kill player
        player.setHealth(0);
        
        // Log action
        if (Conf.logFactionCreate) {
            com.massivecraft.factions.util.Logger.print(player.getName() + 
                " combat logged and was killed", com.massivecraft.factions.util.Logger.PrefixType.DEFAULT);
        }
    }

    /**
     * Drop inventory on logout (without killing)
     */
    private void dropInventoryOnLogout(Player player) {
        Location loc = player.getLocation();
        
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                loc.getWorld().dropItemNaturally(loc, item);
            }
        }
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                loc.getWorld().dropItemNaturally(loc, item);
            }
        }
        
        player.getInventory().clear();
        
        if (Conf.logFactionCreate) {
            com.massivecraft.factions.util.Logger.print(player.getName() + 
                " combat logged and dropped inventory", com.massivecraft.factions.util.Logger.PrefixType.DEFAULT);
        }
    }
}

