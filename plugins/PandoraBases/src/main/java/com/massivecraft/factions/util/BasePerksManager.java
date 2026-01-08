package com.massivecraft.factions.util;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.data.FactionData;
import com.massivecraft.factions.data.helpers.FactionDataHelper;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BasePerksManager {

    private static final ConcurrentHashMap<UUID, Long> lastPerkUpdate = new ConcurrentHashMap<>();

    /**
     * Open perks menu for a faction
     */
    public static void openPerksMenu(Player player, Faction faction) {
        if (faction == null || !faction.isNormal() || !faction.hasBeacon()) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You must have a base to manage perks!")));
            return;
        }

        // Check if player is leader/owner
        com.massivecraft.factions.FPlayer fPlayer = com.massivecraft.factions.FPlayers.getInstance().getByPlayer(player);
        if (fPlayer == null || !fPlayer.hasFaction() || !fPlayer.getFaction().equals(faction)) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You must be a member of this base to manage perks!")));
            return;
        }

        if (!fPlayer.getRole().isAtLeast(com.massivecraft.factions.struct.Role.LEADER)) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Only the base owner can manage perks!")));
            return;
        }

        new BasePerksGUI(player, faction).openGUI(FactionsPlugin.getInstance());
    }

    /**
     * Apply base perks to a player
     */
    public static void applyPerks(Player player, Faction faction) {
        if (faction == null || !faction.isNormal() || !faction.hasBeacon()) {
            return;
        }

        // Check if player is member
        if (!faction.getFPlayers().contains(com.massivecraft.factions.FPlayers.getInstance().getByPlayer(player))) {
            return;
        }

        FactionDataHelper dataHelper = FactionsPlugin.getInstance().getFactionDataHelper();
        if (dataHelper == null) return;

        FactionData data = dataHelper.getOrLoadFactionData(faction);
        if (data == null) return;

        // Apply extra hearts
        Object heartsObj = data.get("perk.extraHearts");
        boolean extraHearts = heartsObj != null && heartsObj instanceof Boolean && (Boolean) heartsObj;
        if (extraHearts) {
            // Use setMaxHealth for compatibility
            player.setMaxHealth(24.0); // 12 hearts = 24 HP (default is 20)
        } else {
            // Reset to default
            player.setMaxHealth(20.0);
        }

        // Apply jump boost
        Object jumpObj = data.get("perk.jumpBoost");
        boolean jumpBoost = jumpObj != null && jumpObj instanceof Boolean && (Boolean) jumpObj;
        if (jumpBoost) {
            // Use 999999 ticks (about 13.8 hours) for "infinite" duration
            // PotionEffect constructor: type, duration (ticks), amplifier (0-based, so 0 = level 1)
            PotionEffectType jumpType = PotionEffectType.getByName("JUMP_BOOST");
            if (jumpType == null) jumpType = PotionEffectType.getByName("JUMP");
            if (jumpType != null) {
                player.addPotionEffect(new PotionEffect(jumpType, 999999, 0));
            }
        } else {
            PotionEffectType jumpType = PotionEffectType.getByName("JUMP_BOOST");
            if (jumpType == null) jumpType = PotionEffectType.getByName("JUMP");
            if (jumpType != null) {
                player.removePotionEffect(jumpType);
            }
        }

        // Apply speed boost
        Object speedObj = data.get("perk.speedBoost");
        boolean speedBoost = speedObj != null && speedObj instanceof Boolean && (Boolean) speedObj;
        if (speedBoost) {
            // Use 999999 ticks (about 13.8 hours) for "infinite" duration
            // PotionEffect constructor: type, duration (ticks), amplifier (0-based, so 0 = level 1)
            PotionEffectType speedType = PotionEffectType.getByName("SPEED");
            if (speedType != null) {
                player.addPotionEffect(new PotionEffect(speedType, 999999, 0));
            }
        } else {
            PotionEffectType speedType = PotionEffectType.getByName("SPEED");
            if (speedType != null) {
                player.removePotionEffect(speedType);
            }
        }
    }

    /**
     * Remove perks from a player
     */
    public static void removePerks(Player player) {
        // Reset health
        player.setMaxHealth(20.0);

        // Remove potion effects
        PotionEffectType jumpType = PotionEffectType.getByName("JUMP_BOOST");
        if (jumpType == null) jumpType = PotionEffectType.getByName("JUMP");
        if (jumpType != null) {
            player.removePotionEffect(jumpType);
        }
        
        PotionEffectType speedType = PotionEffectType.getByName("SPEED");
        if (speedType != null) {
            player.removePotionEffect(speedType);
        }
    }

    /**
     * Toggle a perk for a faction
     */
    public static void togglePerk(Faction faction, String perkName) {
        FactionDataHelper dataHelper = FactionsPlugin.getInstance().getFactionDataHelper();
        if (dataHelper == null) return;

        FactionData data = dataHelper.getOrLoadFactionData(faction);
        if (data == null) return;
        
        Object currentObj = data.get("perk." + perkName);
        boolean current = currentObj != null && currentObj instanceof Boolean && (Boolean) currentObj;
        boolean newValue = !current;
        
        // Update the data object directly (this updates the cached object in memory)
        data.set("perk." + perkName, newValue);
        
        // Save asynchronously (the cached object is already updated, so GUI will see the change)
        dataHelper.saveFactionData(data);
        
        // Don't invalidate cache - we want to keep the updated data in memory
        // The data object we just modified IS the cached object, so it's already updated

        // Update beacon beam color
        if (faction.hasBeacon()) {
            Location beaconLoc = faction.getBeaconLocation();
            if (beaconLoc != null) {
                com.massivecraft.factions.util.BeaconBeamManager.updateBeamColor(faction, beaconLoc);
            }
        }

        // Apply to all online members
        for (com.massivecraft.factions.FPlayer member : faction.getFPlayers()) {
            if (member.isOnline() && member.getPlayer() != null) {
                applyPerks(member.getPlayer(), faction);
            }
        }
    }

    /**
     * Get perk status
     */
    public static boolean getPerkStatus(Faction faction, String perkName) {
        FactionDataHelper dataHelper = FactionsPlugin.getInstance().getFactionDataHelper();
        if (dataHelper == null) return false;

        FactionData data = dataHelper.getOrLoadFactionData(faction);
        if (data == null) return false;
        
        Object statusObj = data.get("perk." + perkName);
        return statusObj != null && statusObj instanceof Boolean && (Boolean) statusObj;
    }

    /**
     * Perks GUI
     */
    private static class BasePerksGUI extends com.massivecraft.factions.util.SaberGUI {
        private final Faction faction;

        public BasePerksGUI(Player player, Faction faction) {
            super(player, ChatColor.DARK_GRAY + "Base Perks", 27);
            this.faction = faction;
        }

        @Override
        public void redraw() {
            // Don't invalidate cache - getOrLoadFactionData will return the cached (updated) object
            // Fill background
            ItemStack background = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta bgMeta = background.getItemMeta();
            bgMeta.setDisplayName(" ");
            background.setItemMeta(bgMeta);
            for (int i = 0; i < 27; i++) {
                if (i != 10 && i != 12 && i != 14 && i != 16) {
                    this.setItem(i, background, () -> {});
                }
            }

            // Extra Hearts perk
            boolean heartsEnabled = getPerkStatus(faction, "extraHearts");
            ItemStack heartsItem = new ItemStack(heartsEnabled ? Material.RED_DYE : Material.GRAY_DYE);
            ItemMeta heartsMeta = heartsItem.getItemMeta();
            heartsMeta.setDisplayName((heartsEnabled ? ChatColor.YELLOW : ChatColor.RED) + "Extra Hearts");
            heartsMeta.setLore(Arrays.asList(
                    "",
                    ChatColor.GRAY + "Grants +2 hearts (24 HP total)",
                    "",
                    heartsEnabled ? ChatColor.YELLOW + "✓ Enabled" : ChatColor.RED + "✗ Disabled",
                    "",
                    ChatColor.YELLOW + "Click to toggle!"
            ));
            heartsItem.setItemMeta(heartsMeta);
            this.setItem(10, heartsItem, () -> {
                togglePerk(faction, "extraHearts");
                this.redraw();
            });

            // Jump Boost perk
            boolean jumpEnabled = getPerkStatus(faction, "jumpBoost");
            ItemStack jumpItem = new ItemStack(jumpEnabled ? Material.RABBIT_FOOT : Material.GRAY_DYE);
            ItemMeta jumpMeta = jumpItem.getItemMeta();
            jumpMeta.setDisplayName((jumpEnabled ? ChatColor.YELLOW : ChatColor.RED) + "Jump Boost");
            jumpMeta.setLore(Arrays.asList(
                    "",
                    ChatColor.GRAY + "Grants Jump Boost II",
                    "",
                    jumpEnabled ? ChatColor.YELLOW + "✓ Enabled" : ChatColor.RED + "✗ Disabled",
                    "",
                    ChatColor.YELLOW + "Click to toggle!"
            ));
            jumpItem.setItemMeta(jumpMeta);
            this.setItem(12, jumpItem, () -> {
                togglePerk(faction, "jumpBoost");
                this.redraw();
            });

            // Speed Boost perk
            boolean speedEnabled = getPerkStatus(faction, "speedBoost");
            ItemStack speedItem = new ItemStack(speedEnabled ? Material.SUGAR : Material.GRAY_DYE);
            ItemMeta speedMeta = speedItem.getItemMeta();
            speedMeta.setDisplayName((speedEnabled ? ChatColor.YELLOW : ChatColor.RED) + "Speed Boost");
            speedMeta.setLore(Arrays.asList(
                    "",
                    ChatColor.GRAY + "Grants Speed II",
                    "",
                    speedEnabled ? ChatColor.YELLOW + "✓ Enabled" : ChatColor.RED + "✗ Disabled",
                    "",
                    ChatColor.YELLOW + "Click to toggle!"
            ));
            speedItem.setItemMeta(speedMeta);
            this.setItem(14, speedItem, () -> {
                togglePerk(faction, "speedBoost");
                this.redraw();
            });

            // Back button
            ItemStack backItem = new ItemStack(Material.BARRIER);
            ItemMeta backMeta = backItem.getItemMeta();
            backMeta.setDisplayName(ChatColor.RED + "Back");
            backMeta.setLore(Arrays.asList(
                    "",
                    ChatColor.GRAY + "Return to base menu"
            ));
            backItem.setItemMeta(backMeta);
            this.setItem(16, backItem, () -> {
                this.close();
                BeaconMenuGUI.openMenu(player, faction);
            });
        }
    }
}

