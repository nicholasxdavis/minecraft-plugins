package com.massivecraft.factions.util;

import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.data.FactionData;
import com.massivecraft.factions.data.helpers.FactionDataHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BaseChestManager {

    private static FactionDataHelper getDataHelper() {
        return FactionsPlugin.getInstance().getFactionDataHelper();
    }
    private static final ConcurrentHashMap<UUID, Inventory> openChests = new ConcurrentHashMap<>();

    /**
     * Open the base chest for a player
     */
    public static void openChest(Player player, Faction faction) {
        if (faction == null || !faction.isNormal() || !faction.hasBeacon()) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("You must have a base to access the base chest!")));
            return;
        }

        // Check if player is member
        if (!faction.getFPlayers().contains(com.massivecraft.factions.FPlayers.getInstance().getByPlayer(player))) {
            player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.error("Only base members can access the base chest!")));
            return;
        }

        Inventory chest = getOrCreateChest(faction);
        openChests.put(player.getUniqueId(), chest);
        player.openInventory(chest);
        player.sendMessage(PandoraMessage.formatWithPrefix(PandoraMessage.success("Opened base chest!")));
    }

    /**
     * Get or create the base chest inventory for a faction
     */
    private static Inventory getOrCreateChest(Faction faction) {
        FactionDataHelper dataHelper = getDataHelper();
        if (dataHelper == null) {
            return Bukkit.createInventory(null, 54, "Base Chest - " + faction.getTag());
        }
        
        FactionData data = dataHelper.getOrLoadFactionData(faction);
        
        // Try to load from data
        String chestData = (String) data.get("baseChest");
        if (chestData != null && !chestData.isEmpty()) {
            try {
                ItemStack[] contents = deserializeInventory(chestData);
                Inventory inv = Bukkit.createInventory(null, 54, "Base Chest - " + faction.getTag());
                inv.setContents(contents);
                return inv;
            } catch (Exception e) {
                Logger.print("Error loading base chest for " + faction.getTag() + ": " + e.getMessage(), Logger.PrefixType.WARNING);
            }
        }

        // Create new chest
        Inventory inv = Bukkit.createInventory(null, 54, "Base Chest - " + faction.getTag());
        return inv;
    }

    /**
     * Save chest inventory when player closes it
     */
    public static void saveChest(Player player, Inventory inventory) {
        UUID playerId = player.getUniqueId();
        Inventory chest = openChests.remove(playerId);
        
        if (chest == null || !chest.equals(inventory)) {
            return; // Not our chest
        }

        // Find which faction this chest belongs to
        String title = inventory.getViewers().isEmpty() ? "" : inventory.getViewers().get(0).getOpenInventory().getTitle();
        if (!title.startsWith("Base Chest - ")) {
            return;
        }

        String factionTag = title.substring("Base Chest - ".length());
        Faction faction = com.massivecraft.factions.Factions.getInstance().getByTag(factionTag);
        
        if (faction == null || !faction.isNormal()) {
            return;
        }

        saveInventoryToFaction(faction, inventory);
    }

    private static void saveInventoryToFaction(Faction faction, Inventory inventory) {
        // Save inventory
        try {
            FactionDataHelper dataHelper = getDataHelper();
            if (dataHelper == null) return;
            
            String serialized = serializeInventory(inventory.getContents());
            FactionData data = dataHelper.getOrLoadFactionData(faction);
            data.set("baseChest", serialized);
            dataHelper.saveFactionData(data);
        } catch (Exception e) {
            Logger.print("Error saving base chest for " + faction.getTag() + ": " + e.getMessage(), Logger.PrefixType.WARNING);
        }
    }

    /**
     * Serialize inventory to base64 string
     */
    private static String serializeInventory(ItemStack[] contents) {
        try {
            YamlConfiguration config = new YamlConfiguration();
            for (int i = 0; i < contents.length; i++) {
                if (contents[i] != null && contents[i].getType() != Material.AIR) {
                    config.set("item." + i, contents[i]);
                }
            }
            String yaml = config.saveToString();
            return Base64.getEncoder().encodeToString(yaml.getBytes());
        } catch (Exception e) {
            Logger.print("Error serializing inventory: " + e.getMessage(), Logger.PrefixType.WARNING);
            return "";
        }
    }

    /**
     * Deserialize inventory from base64 string
     */
    private static ItemStack[] deserializeInventory(String data) {
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            String yaml = new String(bytes);
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(yaml);
            
            ItemStack[] contents = new ItemStack[54];
            if (config.contains("item")) {
                for (String key : config.getConfigurationSection("item").getKeys(false)) {
                    int slot = Integer.parseInt(key);
                    contents[slot] = config.getItemStack("item." + key);
                }
            }
            return contents;
        } catch (Exception e) {
            Logger.print("Error deserializing inventory: " + e.getMessage(), Logger.PrefixType.WARNING);
            return new ItemStack[54];
        }
    }

    /**
     * Check if an inventory is a base chest
     */
    public static boolean isBaseChest(Inventory inventory) {
        if (inventory == null) return false;
        try {
            String title = "";
            if (!inventory.getViewers().isEmpty()) {
                title = inventory.getViewers().get(0).getOpenInventory().getTitle();
            } else {
                // Check inventory holder or other methods
                return false;
            }
            return title.startsWith("Base Chest - ");
        } catch (Exception e) {
            return false;
        }
    }
}

