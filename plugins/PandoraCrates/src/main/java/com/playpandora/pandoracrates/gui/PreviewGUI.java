package com.playpandora.pandoracrates.gui;

import com.playpandora.pandoracrates.PandoraCrates;
import com.playpandora.pandoracrates.models.Crate;
import com.playpandora.pandoracrates.models.RarityRewards;
import com.playpandora.pandoracrates.models.Reward;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class PreviewGUI {
    
    private final PandoraCrates plugin;
    private final Map<Player, Integer> playerPages = new HashMap<>(); // Player -> current page
    private final Map<Player, Crate> playerCrates = new HashMap<>(); // Player -> crate being viewed
    private static final int ITEMS_PER_PAGE = 28; // 4 rows × 7 columns
    
    public PreviewGUI(PandoraCrates plugin) {
        this.plugin = plugin;
    }
    
    public void openPreview(Player player, Crate crate) {
        openPreview(player, crate, 0);
    }
    
    public void openPreview(Player player, Crate crate, int page) {
        try {
            if (player == null || !player.isOnline()) {
                plugin.getLogger().warning("Attempted to open preview for null or offline player");
                return;
            }
            
            if (crate == null) {
                plugin.getLogger().warning("Attempted to open preview for null crate");
                player.sendMessage(plugin.formatMessage("invalid-crate",
                    "{prefix} &cInvalid crate!"));
                return;
            }
            
            // Collect all rewards in order
            List<RewardDisplayInfo> allRewards = new ArrayList<>();
            String[] rarities = {"common", "uncommon", "rare", "epic", "legendary"};
            String[] colorCodes = {"&7", "&7", "&e", "&e", "&6"};
            
            for (int i = 0; i < rarities.length; i++) {
                RarityRewards rarityRewards = crate.getRarityRewards(rarities[i]);
                if (rarityRewards != null && !rarityRewards.getRewards().isEmpty()) {
                    for (Reward reward : rarityRewards.getRewards()) {
                        allRewards.add(new RewardDisplayInfo(reward, rarities[i], colorCodes[i]));
                    }
                }
            }
            
            int totalItems = allRewards.size();
            // Calculate total pages using integer ceiling division: (totalItems + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE
            int totalPages = totalItems > 0 ? (totalItems + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE : 1;
            
            // Ensure totalPages is at least 1
            if (totalPages < 1) totalPages = 1;
            
            // Clamp page to valid range
            if (page < 0) page = 0;
            if (page >= totalPages && totalPages > 0) page = totalPages - 1;
            
            // Store player's crate and clamped page AFTER calculation
            playerCrates.put(player, crate);
            playerPages.put(player, page);
            
            // Format title with page info
            String title = "&6&l" + crate.getDisplayName() + " &7Preview";
            if (totalPages > 1) {
                title += " &7(" + (page + 1) + "/" + totalPages + ")";
            }
            title = ChatColor.translateAlternateColorCodes('&', title);
            
            // Use 54-slot inventory (6 rows × 9 columns)
            Inventory inv = Bukkit.createInventory(null, 54, title);
        
            // Fill borders with gray stained glass
            ItemStack border = createBorderItem();
            // Top row (slots 0-8)
            for (int i = 0; i < 9; i++) {
                inv.setItem(i, border);
            }
            // Bottom row (slots 45-53)
            for (int i = 45; i < 54; i++) {
                inv.setItem(i, border);
            }
            // Left and right borders
            for (int i = 9; i < 45; i += 9) {
                inv.setItem(i, border);      // Left border
                inv.setItem(i + 8, border);   // Right border
            }
            
            // Calculate total chance for percentage display
            int totalChance = 0;
            Map<String, RarityRewards> allRewardsMap = crate.getRewards();
            for (RarityRewards rarityRewards : allRewardsMap.values()) {
                totalChance += rarityRewards.getChance();
            }
            
            // Add crate info item in top center (slot 4)
            inv.setItem(4, createCrateInfoItem(crate, allRewardsMap, totalChance));
            
            // Place items for current page (no gaps, no limits)
            int startIndex = page * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);
            int slotIndex = 10; // Start at slot 10 (first content slot)
            
            for (int i = startIndex; i < endIndex; i++) {
                RewardDisplayInfo rewardInfo = allRewards.get(i);
                
                // Skip border slots
                while (isBorderSlot(slotIndex)) {
                    slotIndex++;
                }
                
                if (slotIndex >= 45) break; // Reached bottom border
                
                try {
                    inv.setItem(slotIndex, createRewardPreviewItem(rewardInfo.reward, rewardInfo.rarity, rewardInfo.colorCode));
                    slotIndex++;
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to place reward in preview GUI at slot " + slotIndex + ": " + e.getMessage());
                    slotIndex++;
                }
            }
            
            // Add navigation buttons
            if (totalPages > 1) {
                // Previous page button (slot 45)
                if (page > 0) {
                    inv.setItem(45, createPreviousPageButton(page));
                }
                
                // Next page button (slot 53)
                if (page < totalPages - 1) {
                    inv.setItem(53, createNextPageButton(page, totalPages));
                }
            }
            
            // Add close button in bottom center (slot 49)
            inv.setItem(49, createCloseButton());
            
            // Log preview GUI stats
            System.out.println("[PandoraCrates] Preview GUI for crate '" + crate.getId() + "' page " + (page + 1) + "/" + totalPages + ": " + (endIndex - startIndex) + " items displayed (total: " + totalItems + ", startIndex: " + startIndex + ", endIndex: " + endIndex + ")");
        
            player.openInventory(inv);
        } catch (Exception e) {
            plugin.getLogger().severe("Error opening preview GUI: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            e.printStackTrace();
            if (player != null && player.isOnline()) {
                player.sendMessage(plugin.formatMessage("invalid-crate",
                    "{prefix} &cAn error occurred while opening the preview. Please try again."));
            }
        }
    }
    
    private boolean isBorderSlot(int slot) {
        // Top row (0-8)
        if (slot >= 0 && slot <= 8) return true;
        // Bottom row (45-53)
        if (slot >= 45 && slot <= 53) return true;
        // Left border (9, 18, 27, 36)
        if (slot == 9 || slot == 18 || slot == 27 || slot == 36) return true;
        // Right border (17, 26, 35, 44)
        if (slot == 17 || slot == 26 || slot == 35 || slot == 44) return true;
        return false;
    }
    
    private ItemStack createPreviousPageButton(int currentPage) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&7Previous Page"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Click to go to page " + currentPage));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createNextPageButton(int currentPage, int totalPages) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&7Next Page"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Click to go to page " + (currentPage + 2) + "/" + totalPages));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    public void handlePageNavigation(Player player, int slot) {
        Crate crate = playerCrates.get(player);
        if (crate == null) {
            plugin.getLogger().warning("Player " + player.getName() + " clicked navigation but has no crate stored");
            return;
        }
        
        Integer currentPage = playerPages.get(player);
        if (currentPage == null) currentPage = 0;
        
        // Collect all rewards to calculate total pages
        List<RewardDisplayInfo> allRewards = new ArrayList<>();
        String[] rarities = {"common", "uncommon", "rare", "epic", "legendary"};
        String[] colorCodes = {"&7", "&7", "&e", "&e", "&6"};
        
        for (int i = 0; i < rarities.length; i++) {
            RarityRewards rarityRewards = crate.getRarityRewards(rarities[i]);
            if (rarityRewards != null && !rarityRewards.getRewards().isEmpty()) {
                for (Reward reward : rarityRewards.getRewards()) {
                    allRewards.add(new RewardDisplayInfo(reward, rarities[i], colorCodes[i]));
                }
            }
        }
        
        int totalItems = allRewards.size();
        // Calculate total pages using integer ceiling division: (totalItems + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE
        int totalPages = totalItems > 0 ? (totalItems + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE : 1;
        
        // Ensure totalPages is at least 1
        if (totalPages < 1) totalPages = 1;
        
        // Debug logging
        plugin.getLogger().info("[PreviewGUI] Navigation: currentPage=" + currentPage + ", totalPages=" + totalPages + ", totalItems=" + totalItems + ", slot=" + slot);
        
        if (slot == 45) {
            // Previous page
            if (currentPage > 0) {
                plugin.getLogger().info("[PreviewGUI] Navigating to previous page: " + (currentPage - 1));
                openPreview(player, crate, currentPage - 1);
            } else {
                plugin.getLogger().info("Player " + player.getName() + " clicked previous but already on page 0");
            }
        } else if (slot == 53) {
            // Next page - ensure we can go to the next page
            int nextPage = currentPage + 1;
            plugin.getLogger().info("[PreviewGUI] Next page calculation: currentPage=" + currentPage + ", nextPage=" + nextPage + ", totalPages=" + totalPages + ", condition=" + (nextPage < totalPages));
            if (nextPage < totalPages) {
                plugin.getLogger().info("[PreviewGUI] Navigating to next page: " + nextPage);
                openPreview(player, crate, nextPage);
            } else {
                plugin.getLogger().warning("Player " + player.getName() + " clicked next but already on last page (current: " + currentPage + ", total: " + totalPages + ", totalItems: " + totalItems + ")");
            }
        }
    }
    
    public void cleanup(Player player) {
        playerPages.remove(player);
        playerCrates.remove(player);
    }
    
    private static class RewardDisplayInfo {
        final Reward reward;
        final String rarity;
        final String colorCode;
        
        RewardDisplayInfo(Reward reward, String rarity, String colorCode) {
            this.reward = reward;
            this.rarity = rarity;
            this.colorCode = colorCode;
        }
    }
    
    private ItemStack createBorderItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createCrateInfoItem(Crate crate, Map<String, RarityRewards> allRewards, int totalChance) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
                "&6&l" + crate.getDisplayName()));
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7&m                    "));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&6&lRarity Rates:"));
            lore.add("");
            
            // Add rarity rates (use only &7, &e, &6)
            String[] rarities = {"common", "uncommon", "rare", "epic", "legendary"};
            String[] colorCodes = {"&7", "&7", "&e", "&e", "&6"};
            
            for (int i = 0; i < rarities.length; i++) {
                RarityRewards rarityRewards = allRewards.get(rarities[i]);
                if (rarityRewards != null && rarityRewards.getChance() > 0) {
                    double percentage = totalChance > 0 ? (rarityRewards.getChance() * 100.0 / totalChance) : 0.0;
                    String rarityName = rarities[i].substring(0, 1).toUpperCase() + rarities[i].substring(1);
                    lore.add(ChatColor.translateAlternateColorCodes('&', 
                        colorCodes[i] + rarityName + ": &6" + 
                        String.format("%.1f", percentage) + "% &7(" + rarityRewards.getChance() + "%)"));
                }
            }
            
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7&m                    "));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&eRight-click with a key to open!"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Visit &e/crates &7to view all crates!"));
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createRarityHeader(String rarity, String colorCode, int chance, double percentage) {
        Material material = Material.PAPER;
        // Use different materials for visual distinction
        switch (rarity.toLowerCase()) {
            case "common":
                material = Material.WHITE_DYE;
                break;
            case "uncommon":
                material = Material.LIME_DYE;
                break;
            case "rare":
                material = Material.LIGHT_BLUE_DYE;
                break;
            case "epic":
                material = Material.MAGENTA_DYE;
                break;
            case "legendary":
                material = Material.ORANGE_DYE;
                break;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String rarityName = rarity.substring(0, 1).toUpperCase() + rarity.substring(1);
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
                colorCode + "&l" + rarityName + " &7Rewards"));
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', 
                "&7Drop Rate: " + colorCode + "&l" + String.format("%.1f", percentage) + "%"));
            lore.add(ChatColor.translateAlternateColorCodes('&', 
                "&7Chance Weight: " + colorCode + "&l" + chance + "%"));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Possible rewards:"));
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createRewardPreviewItem(Reward reward, String rarity, String colorCode) {
        Material material = Material.DIAMOND;
        String name = "Reward";
        List<String> lore = new ArrayList<>();
        
        switch (reward.getType()) {
            case ITEM:
                material = reward.getMaterial();
                name = reward.getName() != null ? reward.getName() : material.name();
                if (reward.getLore() != null) {
                    lore.addAll(reward.getLore());
                }
                break;
            case MONEY:
                // MONEY rewards now use banknotes
                material = Material.PAPER;
                name = "&6$" + reward.getMoneyAmount() + " &7Banknote";
                lore.add("&7Value: &6$" + reward.getMoneyAmount());
                lore.add("&7Type: &eBanknote Reward");
                break;
            case PANDORA_ITEM_CANNON:
            case PANDORA_ITEM_FARM:
            case PANDORA_ITEM_PERK:
            case PANDORA_ITEM_PET:
            case PANDORA_ITEM_KIT:
                // Try to get actual item from PandoraItems plugin
                ItemStack pandoraItem = getPandoraItem(reward.getType(), reward.getItemKey());
                if (pandoraItem != null && pandoraItem.hasItemMeta()) {
                    ItemMeta pandoraMeta = pandoraItem.getItemMeta();
                    if (pandoraMeta != null) {
                        material = pandoraItem.getType();
                        name = pandoraMeta.hasDisplayName() ? pandoraMeta.getDisplayName() : "&e" + reward.getItemKey();
                        if (pandoraMeta.hasLore()) {
                            lore.addAll(pandoraMeta.getLore());
                        } else {
                            lore.add("&7Type: &eSpecial Item");
                            lore.add("&7Item: &6" + reward.getItemKey());
                        }
                    } else {
                        material = Material.TRIPWIRE_HOOK;
                        name = "&e" + reward.getItemKey();
                        lore.add("&7Type: &eSpecial Item");
                        lore.add("&7Item: &6" + reward.getItemKey());
                    }
                } else {
                    material = Material.TRIPWIRE_HOOK;
                    name = "&e" + reward.getItemKey();
                    lore.add("&7Type: &eSpecial Item");
                    lore.add("&7Item: &6" + reward.getItemKey());
                }
                break;
            case PERK:
                material = Material.GOLDEN_APPLE;
                name = "&e" + reward.getItemKey() + " &7Perk";
                lore.add("&7Type: &ePerk Reward");
                lore.add("&7Perk: &6" + reward.getItemKey());
                break;
            case PET:
                material = Material.BONE;
                name = "&e" + reward.getItemKey() + " &7Pet";
                lore.add("&7Type: &ePet Reward");
                lore.add("&7Pet: &6" + reward.getItemKey());
                break;
            case KIT:
                material = Material.CHEST;
                name = "&e" + reward.getItemKey() + " &7Kit";
                lore.add("&7Type: &eKit Reward");
                lore.add("&7Kit: &6" + reward.getItemKey());
                break;
            case BANKNOTE:
                material = Material.PAPER;
                name = "&6$" + reward.getMoneyAmount() + " &7Banknote";
                lore.add("&7Value: &6$" + reward.getMoneyAmount());
                lore.add("&7Type: &eBanknote Reward");
                break;
            case SPAWNER:
                material = Material.SPAWNER;
                name = "&6" + reward.getItemKey() + " &7Spawner";
                lore.add("&7Type: &eSpawner Reward");
                lore.add("&7Spawner: &6" + reward.getItemKey());
                if (reward.getAmount() > 1) {
                    lore.add("&7Amount: &6" + reward.getAmount());
                }
                break;
            case GODSET:
                material = Material.DIAMOND_CHESTPLATE;
                name = "&6" + reward.getItemKey().toUpperCase() + " &7Godset";
                lore.add("&7Type: &eGodset Reward");
                lore.add("&7Tier: &6" + reward.getItemKey().toUpperCase());
                lore.add("&7Includes: &6Helmet, Chestplate, Leggings, Boots, Sword, Bow");
                break;
            case CREEPER_EGG:
                material = Material.CREEPER_SPAWN_EGG;
                name = "&6Creeper Egg";
                lore.add("&7Type: &eCreeper Egg Reward");
                if (reward.getAmount() > 1) {
                    lore.add("&7Amount: &6" + reward.getAmount());
                }
                break;
            case ENCHANTED_GEAR:
                String gearType = reward.getItemKey().toLowerCase();
                String[] parts = gearType.split("_");
                if (parts.length >= 2) {
                    String materialName = parts[0] + "_" + parts[1];
                    try {
                        material = Material.valueOf(materialName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        material = Material.DIAMOND_CHESTPLATE;
                    }
                } else {
                    material = Material.DIAMOND_CHESTPLATE;
                }
                name = "&6" + reward.getItemKey().replace("_", " ").toUpperCase();
                lore.add("&7Type: &eEnchanted Gear");
                if (reward.getEnchantments() != null && !reward.getEnchantments().isEmpty()) {
                    lore.add("&7Enchantments:");
                    for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : reward.getEnchantments().entrySet()) {
                        String enchantName = entry.getKey().getKey().getKey().replace("_", " ");
                        enchantName = enchantName.substring(0, 1).toUpperCase() + enchantName.substring(1);
                        lore.add("&7  &6" + enchantName + " " + entry.getValue());
                    }
                }
                break;
            case CRATE_KEY:
                Crate keyCrate = plugin.getCrateManager().getCrate(reward.getItemKey());
                if (keyCrate != null) {
                    material = keyCrate.getKeyMaterial();
                    name = keyCrate.getKeyName();
                    lore.add("&7Type: &eCrate Key");
                    lore.add("&7Crate: &6" + keyCrate.getDisplayName());
                    if (reward.getAmount() > 1) {
                        lore.add("&7Amount: &6" + reward.getAmount());
                    }
                } else {
                    material = Material.TRIPWIRE_HOOK;
                    name = "&6" + reward.getItemKey() + " &7Key";
                    lore.add("&7Type: &eCrate Key");
                    lore.add("&7Crate: &6" + reward.getItemKey());
                    if (reward.getAmount() > 1) {
                        lore.add("&7Amount: &6" + reward.getAmount());
                    }
                }
                break;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            
            // Translate all lore lines that contain color codes
            List<String> translatedLore = new ArrayList<>();
            for (String loreLine : lore) {
                if (loreLine != null && !loreLine.isEmpty()) {
                    translatedLore.add(ChatColor.translateAlternateColorCodes('&', loreLine));
                }
            }
            
            // Add nice lore to all items
            if (translatedLore.isEmpty()) {
                translatedLore.add(ChatColor.translateAlternateColorCodes('&', "&7Crate Reward"));
            }
            translatedLore.add("");
            translatedLore.add(ChatColor.translateAlternateColorCodes('&', 
                "&7Rarity: " + colorCode + "&l" + rarity.toUpperCase()));
            
            meta.setLore(translatedLore);
            
            // Add glow for rare+
            if (!rarity.equalsIgnoreCase("common") && !rarity.equalsIgnoreCase("uncommon")) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack getPandoraItem(Reward.RewardType type, String itemKey) {
        if (itemKey == null || itemKey.isEmpty()) {
            return null;
        }
        
        if (plugin.getServer().getPluginManager().getPlugin("PandoraItems") == null) {
            return null;
        }
        
        try {
            org.bukkit.plugin.Plugin pandoraItemsPlugin = plugin.getServer().getPluginManager().getPlugin("PandoraItems");
            if (pandoraItemsPlugin == null) {
                return null;
            }
            
            Class<?> pandoraItemsClass = Class.forName("com.playpandora.pandoraitems.PandoraItems");
            Object pandoraItemsInstance = pandoraItemsPlugin;
            
            String managerMethod = null;
            switch (type) {
                case PANDORA_ITEM_CANNON:
                    managerMethod = "getCannonItemManager";
                    break;
                case PANDORA_ITEM_FARM:
                    managerMethod = "getFarmItemManager";
                    break;
                case PANDORA_ITEM_PERK:
                    managerMethod = "getPerkItemManager";
                    break;
                case PANDORA_ITEM_PET:
                    managerMethod = "getPetItemManager";
                    break;
                case PANDORA_ITEM_KIT:
                    managerMethod = "getKitItemManager";
                    break;
                default:
                    return null;
            }
            
            if (managerMethod != null) {
                java.lang.reflect.Method getManagerMethod = pandoraItemsClass.getMethod(managerMethod);
                Object manager = getManagerMethod.invoke(pandoraItemsInstance);
                
                // Get the correct method name based on type
                String createMethodName = "createCannonItem";
                if (type == Reward.RewardType.PANDORA_ITEM_FARM) {
                    createMethodName = "createFarmItem";
                } else if (type == Reward.RewardType.PANDORA_ITEM_PERK) {
                    createMethodName = "createPerkItem";
                } else if (type == Reward.RewardType.PANDORA_ITEM_PET) {
                    createMethodName = "createPetItem";
                } else if (type == Reward.RewardType.PANDORA_ITEM_KIT) {
                    createMethodName = "createKitItem";
                }
                
                java.lang.reflect.Method createItemMethod = manager.getClass().getMethod(createMethodName, String.class);
                return (ItemStack) createItemMethod.invoke(manager, itemKey);
            }
        } catch (Exception e) {
            // Fallback to placeholder
        }
        
        return null;
    }
    
    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&7Close"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Click to close this menu"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}

