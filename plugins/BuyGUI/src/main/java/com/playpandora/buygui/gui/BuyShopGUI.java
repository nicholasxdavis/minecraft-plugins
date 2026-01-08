package com.playpandora.buygui.gui;

import com.playpandora.buygui.BuyGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuyShopGUI implements Listener {
    
    public enum ShopType {
        GENERAL,
        SPAWNER
    }
    
    private final BuyGUI plugin;
    private final java.util.Map<java.util.UUID, Integer> cursorClearTasks = new java.util.HashMap<>();
    private final java.util.Set<java.util.UUID> playersWithShopOpen = new java.util.HashSet<>();
    private static final int CLOSE_SLOT = 53; // Bottom right
    private static final int BACK_SLOT = 45; // Bottom left
    private static final int PREV_PAGE_SLOT = 47;
    private static final int NEXT_PAGE_SLOT = 51;
    
    // Store spawner shop data
    private List<SpawnerShopItem> spawnerItems = new ArrayList<>();
    
    public BuyShopGUI(BuyGUI plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadSpawnerShopData();
    }
    
    private void loadSpawnerShopData() {
        if (Bukkit.getPluginManager().getPlugin("PandoraSpawners") == null) {
            return;
        }
        
        try {
            // Read from PandoraSpawners shop.yml config file
            org.bukkit.plugin.Plugin pandoraSpawners = Bukkit.getPluginManager().getPlugin("PandoraSpawners");
            if (pandoraSpawners == null) {
                return;
            }
            
            java.io.File shopFile = new java.io.File(pandoraSpawners.getDataFolder(), "shop.yml");
            if (!shopFile.exists()) {
                plugin.getLogger().warning("PandoraSpawners shop.yml not found!");
                return;
            }
            
            org.bukkit.configuration.file.YamlConfiguration shopConfig = 
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(shopFile);
            
            // Load all spawner types from shop.yml
            // Only include spawners that have upgrade support
            org.bukkit.configuration.ConfigurationSection buySection = shopConfig.getConfigurationSection("Shop.Buy");
            if (buySection != null) {
                for (String spawnerTypeKey : buySection.getKeys(false)) {
                    String path = "Shop.Buy." + spawnerTypeKey;
                    if (shopConfig.getBoolean(path + ".Toggle", false)) {
                        int cost = shopConfig.getInt(path + ".Cost", 0);
                        if (cost > 0) {
                            // Check if spawner has upgrade support using reflection
                            // Only include spawners that have upgrade support
                            try {
                                // Use reflection to check upgrade support
                                Class<?> spawnerTypeClass = Class.forName("com.pandora.spawners.spawner.type.SpawnerType");
                                java.lang.reflect.Method ofMethod = spawnerTypeClass.getMethod("of", String.class);
                                Object type = ofMethod.invoke(null, spawnerTypeKey);
                                
                                if (type != null) {
                                    Class<?> settingsClass = Class.forName("com.pandora.spawners.configuration.Settings");
                                    java.lang.reflect.Field settingsField = settingsClass.getField("settings");
                                    Object settings = settingsField.get(null);
                                    
                                    java.lang.reflect.Field upgradeableField = settingsClass.getField("upgrades_upgradeable");
                                    Object upgradeableMap = upgradeableField.get(settings);
                                    
                                    java.lang.reflect.Method getMethod = upgradeableMap.getClass().getMethod("get", Object.class);
                                    boolean[] upgradeable = (boolean[]) getMethod.invoke(upgradeableMap, type);
                                    
                                    // Only add if at least one upgrade is enabled
                                    boolean hasUpgradeSupport = upgradeable != null && 
                                        (upgradeable[0] || upgradeable[1] || upgradeable[2]);
                                    if (hasUpgradeSupport) {
                                        spawnerItems.add(new SpawnerShopItem(spawnerTypeKey, cost, null));
                                    }
                                } else {
                                    // Type not found, skip it
                                }
                            } catch (Exception e) {
                                // If we can't check (PandoraSpawners not loaded or reflection fails), 
                                // include it anyway to avoid breaking the shop
                                spawnerItems.add(new SpawnerShopItem(spawnerTypeKey, cost, null));
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load PandoraSpawners shop data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void openShop(Player player, ShopType shopType) {
        openShop(player, shopType, null, 1);
    }
    
    public void openShop(Player player, ShopType shopType, String category) {
        openShop(player, shopType, category, 1);
    }
    
    public void openShop(Player player, ShopType shopType, String category, int page) {
        if (page < 1) page = 1;
        
        plugin.getLogger().info("openShop called - shopType: " + shopType + ", category: " + category + ", page: " + page);
        
        // For GENERAL shop, category is REQUIRED - if missing, go back to category selector
        if (shopType == ShopType.GENERAL && (category == null || category.isEmpty())) {
            plugin.getLogger().warning("General shop opened without category! Redirecting to category selector.");
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getGeneralCategorySelectorGUI().openSelector(player);
            }, 2L);
            return;
        }
        
        // Validate category exists for general shop
        if (shopType == ShopType.GENERAL && category != null) {
            Map<Material, Double> categoryPrices = plugin.getPriceManager().getPricesByCategory(category);
            if (categoryPrices.isEmpty()) {
                plugin.getLogger().warning("Category '" + category + "' has no items! Showing empty shop.");
                // Don't redirect - just show empty shop
            }
        }
        
        String baseTitle = "&8Buy Shop";
        int size;
        
        if (shopType == ShopType.SPAWNER) {
            baseTitle = "&8Spawner Shop";
            // Use half chest (27 slots) for spawner shop
            size = 27;
        } else if (shopType == ShopType.GENERAL) {
            baseTitle = "&8General Shop - " + formatCategoryName(category);
            size = 54; // Always use full chest for general shop with pagination
        } else {
            size = 54;
        }
        
        // Get items for the shop type - get fresh items each time
        List<ShopItem> shopItems = getShopItems(shopType, category);
        
        plugin.getLogger().info("Retrieved " + shopItems.size() + " items for category: " + category);
        
        if (shopType == ShopType.SPAWNER) {
            // Single row layout for spawner shop with pagination
            openSpawnerShop(player, shopItems, page, size);
        } else {
            // Full chest with pagination for general shop
            openGeneralShop(player, shopItems, category, page, size);
        }
    }
    
    private void openSpawnerShop(Player player, List<ShopItem> shopItems, int page, int size) {
        // Half chest layout: items go in row 2 (slots 9-17, but skip borders so slots 10-16)
        // 7 items per page for consistency
        int itemsPerPage = 7;
        int totalPages = Math.max(1, (int) Math.ceil((double) shopItems.size() / itemsPerPage));
        
        if (page < 1) page = 1;
        if (page > totalPages) {
            page = totalPages;
        }
        
        String title = "&8Spawner Shop &7(Page " + page + "/" + totalPages + ")";
        
        Inventory inv = Bukkit.createInventory(null, size,
            org.bukkit.ChatColor.translateAlternateColorCodes('&', title));
        
        // Add shop items FIRST - single row in row 4 (slots 28-34, skipping borders)
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, shopItems.size());
        
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            ShopItem shopItem = shopItems.get(i);
            // Row 2 (index 1): slots 10-16 (skip border at 9 and 17)
            int slot = 9 + 1 + slotIndex; // Start at slot 10
            ItemStack item = createShopItemDisplay(shopItem, player, ShopType.SPAWNER);
            inv.setItem(slot, item);
            slotIndex++;
        }
        
        // Fill borders after items are placed (borders only fill NULL slots)
        fillBorders(inv, size, true, true, true);
        
        // Add back button (bottom left) - for 27 slot inventory, use slot 18
        int backSlotForSize = size == 27 ? 18 : BACK_SLOT;
        inv.setItem(backSlotForSize, createBackButton());
        
        // Add close button (bottom right) - for 27 slot inventory, use slot 26
        int closeSlotForSize = size == 27 ? 26 : CLOSE_SLOT;
        inv.setItem(closeSlotForSize, createCloseButton());
        
        // Add page navigation buttons if needed (using nether star)
        // For 27 slot inventory, use slots 20 and 24 (middle row)
        int prevSlot = size == 27 ? 20 : PREV_PAGE_SLOT;
        int nextSlot = size == 27 ? 24 : NEXT_PAGE_SLOT;
        if (totalPages > 1) {
            if (page > 1) {
                inv.setItem(prevSlot, createPrevPageButton(page, totalPages));
            }
            if (page < totalPages) {
                inv.setItem(nextSlot, createNextPageButton(page, totalPages));
            }
        }
        
        // Fill remaining empty slots with borders (only fills NULL slots, won't overwrite items or buttons)
        fillEmptySlots(inv, size);
        
        // Track that player has shop open
        playersWithShopOpen.add(player.getUniqueId());
        player.openInventory(inv);
    }
    
    private void openGeneralShop(Player player, List<ShopItem> shopItems, String category, int page, int size) {
        plugin.getLogger().info("Opening general shop - category: " + category + ", page: " + page + ", items: " + shopItems.size());
        
        List<Integer> layoutSlots = getLayoutSlots();
        int itemsPerPage = layoutSlots.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) shopItems.size() / itemsPerPage));
        
        if (page < 1) page = 1;
        if (page > totalPages) {
            plugin.getLogger().warning("Page " + page + " exceeds total pages " + totalPages + ", setting to " + totalPages);
            page = totalPages;
        }
        
        String title = "&8General Shop";
        if (category != null) {
            String categoryName = formatCategoryName(category);
            // Shorten category name if title would be too long (Minecraft limit is 32 chars)
            String fullTitle = "General Shop - " + categoryName + " (Page " + page + "/" + totalPages + ")";
            if (fullTitle.length() > 32) {
                // Use shorter format
                title = "&8" + categoryName + " &7(" + page + "/" + totalPages + ")";
            } else {
                title = "&8General Shop - " + categoryName + " &7(Page " + page + "/" + totalPages + ")";
            }
        } else {
            title = title + " &7(Page " + page + "/" + totalPages + ")";
        }
        
        // Ensure title doesn't exceed 32 characters (Minecraft limit)
        String strippedTitle = org.bukkit.ChatColor.stripColor(title);
        if (strippedTitle.length() > 32) {
            // Fallback to shorter title
            String categoryName = category != null ? formatCategoryName(category) : "Shop";
            title = "&8" + categoryName + " &7(" + page + "/" + totalPages + ")";
        }
        
        plugin.getLogger().info("Opening shop with title: " + org.bukkit.ChatColor.stripColor(title));
        
        Inventory inv = Bukkit.createInventory(null, size,
            org.bukkit.ChatColor.translateAlternateColorCodes('&', title));
        
        // Add shop items with pagination FIRST - items go in rows 2-4 (slots 9-35)
        // Add items BEFORE borders so they don't get overwritten
        addShopItems(inv, player, shopItems, layoutSlots, page, ShopType.GENERAL);
        
        // Fill borders after items are placed (borders only fill NULL slots)
        fillBorders(inv, size, true, true, true);
        
        // Add back button (bottom left)
        inv.setItem(BACK_SLOT, createBackButton());
        
        // Add close button (bottom right)
        inv.setItem(CLOSE_SLOT, createCloseButton());
        
        // Add page navigation buttons if needed
        if (totalPages > 1) {
            if (page > 1) {
                inv.setItem(PREV_PAGE_SLOT, createPrevPageButton(page, totalPages));
            }
            if (page < totalPages) {
                inv.setItem(NEXT_PAGE_SLOT, createNextPageButton(page, totalPages));
            }
        }
        
        // Fill remaining empty slots with borders (only fills NULL slots, won't overwrite items or buttons)
        fillEmptySlots(inv, size);
        
        try {
            // Track that player has shop open
            playersWithShopOpen.add(player.getUniqueId());
            player.openInventory(inv);
        } catch (Exception e) {
            playersWithShopOpen.remove(player.getUniqueId());
            plugin.getLogger().severe("Error opening general shop for category '" + category + "': " + e.getMessage());
            e.printStackTrace();
            // Don't redirect - just show error message
            player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&ePandora &cAn error occurred opening the shop. Please try again."));
        }
    }
    
    private String formatCategoryName(String category) {
        if (category == null) return "";
        if (category.equals("tnt_redstone")) return "TNT/Redstone";
        if (category.equals("building")) return "Building";
        if (category.equals("food")) return "Food";
        if (category.equals("misc")) return "Misc";
        // Fallback - capitalize first letter
        return category.substring(0, 1).toUpperCase() + category.substring(1);
    }
    
    private List<ShopItem> getShopItems(ShopType shopType, String category) {
        List<ShopItem> items = new ArrayList<>();
        
        if (shopType == ShopType.GENERAL) {
            // Get general shop items from config by category
            Map<Material, Double> prices = plugin.getPriceManager().getPricesByCategory(category);
            plugin.getLogger().info("Loading items for category: " + category + ", found " + prices.size() + " items");
            for (Map.Entry<Material, Double> entry : prices.entrySet()) {
                items.add(new ShopItem(entry.getKey(), entry.getValue(), null));
            }
        } else if (shopType == ShopType.SPAWNER) {
            // Get spawner items
            for (SpawnerShopItem spawnerItem : spawnerItems) {
                items.add(new ShopItem(Material.SPAWNER, spawnerItem.price, spawnerItem));
            }
        }
        
        // Sort by price (ascending for buy shop)
        items.sort((a, b) -> Double.compare(a.price, b.price));
        
        plugin.getLogger().info("Returning " + items.size() + " shop items for " + shopType + " category: " + category);
        return items;
    }
    
    private void addShopItems(Inventory inv, Player player,
                              List<ShopItem> shopItems,
                              List<Integer> layoutSlots,
                              int page,
                              ShopType shopType) {
        int itemsPerPage = layoutSlots.size();
        int totalItems = shopItems.size();
        
        plugin.getLogger().info("Adding shop items: totalItems=" + totalItems + ", itemsPerPage=" + itemsPerPage + ", page=" + page + ", layoutSlots=" + layoutSlots.size());
        
        if (totalItems == 0 || itemsPerPage == 0) {
            plugin.getLogger().warning("No items to add! totalItems=" + totalItems + ", itemsPerPage=" + itemsPerPage);
            return;
        }
        
        int startIndex = (page - 1) * itemsPerPage;
        if (startIndex >= totalItems) {
            startIndex = 0;
        }
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);
        
        int slotIndex = 0;
        int itemsAdded = 0;
        for (int i = startIndex; i < endIndex; i++) {
            ShopItem shopItem = shopItems.get(i);
            if (slotIndex >= layoutSlots.size()) {
                break;
            }
            int slot = layoutSlots.get(slotIndex++);
            ItemStack item = createShopItemDisplay(shopItem, player, shopType);
            inv.setItem(slot, item);
            itemsAdded++;
        }
        
        plugin.getLogger().info("Added " + itemsAdded + " items to inventory slots " + startIndex + "-" + (endIndex-1));
    }
    
    private List<Integer> getLayoutSlots() {
        List<Integer> slots = new ArrayList<>();
        // Rows 2-4 (index 1-3): slots 9-35, skip borders (col 0 and 8)
        for (int row = 1; row <= 3; row++) {
            for (int col = 1; col <= 7; col++) {
                int slot = row * 9 + col;
                // Ensure we don't conflict with control buttons
                if (slot == CLOSE_SLOT || slot == BACK_SLOT || slot == PREV_PAGE_SLOT || slot == NEXT_PAGE_SLOT) {
                    continue;
                }
                slots.add(slot);
            }
        }
        return slots;
    }
    
    private ItemStack createShopItemDisplay(ShopItem shopItem, Player player, ShopType shopType) {
        ItemStack item;
        
        if (shopItem.spawnerItem != null) {
            // Spawner item - use spawner material
            item = new ItemStack(Material.SPAWNER);
        } else {
            // Validate material before creating ItemStack
            try {
                // Check if material is valid and is an item (not a block-only material)
                if (shopItem.material == null || !shopItem.material.isItem()) {
                    plugin.getLogger().warning("Invalid material for shop item: " + shopItem.material + ", using STONE as fallback");
                    item = new ItemStack(Material.STONE);
                } else {
                    item = new ItemStack(shopItem.material);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error creating ItemStack for material " + shopItem.material + ": " + e.getMessage());
                item = new ItemStack(Material.STONE);
            }
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        
        double price = shopItem.price;
        double balance = plugin.getEconomyManager().getBalance(player);
        boolean canAfford = balance >= price;
        
        // Format material name nicely
        String displayName;
        if (shopItem.spawnerItem != null) {
            // Format spawner type name
            String typeName = shopItem.spawnerItem.typeName.toLowerCase().replace("_", " ");
            String[] words = typeName.split(" ");
            StringBuilder nameBuilder = new StringBuilder();
            for (String word : words) {
                if (nameBuilder.length() > 0) {
                    nameBuilder.append(" ");
                }
                nameBuilder.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
            }
            displayName = nameBuilder.toString() + " Spawner";
        } else {
            String materialName = shopItem.material.name().toLowerCase().replace("_", " ");
            String[] words = materialName.split(" ");
            StringBuilder nameBuilder = new StringBuilder();
            for (String word : words) {
                if (nameBuilder.length() > 0) {
                    nameBuilder.append(" ");
                }
                nameBuilder.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
            }
            displayName = nameBuilder.toString();
        }
        
        meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
            "&8" + displayName));
        
        List<String> lore = new ArrayList<>();
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7&m                              "));
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
            "&7Price: &6" + plugin.getEconomyManager().formatMoney(price)));
        lore.add("");
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
            "&7Your balance: &6" + plugin.getEconomyManager().formatMoney(balance)));
        
        if (canAfford) {
            lore.add("");
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&eClick to purchase!"));
        } else {
            double needed = price - balance;
            lore.add("");
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&cInsufficient funds!"));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&7You need &6" + plugin.getEconomyManager().formatMoney(needed) + " &7more."));
        }
        
        lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7&m                              "));
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    
    private ItemStack createPrevPageButton(int page, int totalPages) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e← Previous Page"));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&7Page &6" + (page - 1) + " &7of &6" + totalPages));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createNextPageButton(int page, int totalPages) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&eNext Page →"));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&7Page &6" + (page + 1) + " &7of &6" + totalPages));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8← Back"));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Click to go back"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        String strippedTitle = org.bukkit.ChatColor.stripColor(title);
        
        // Check both by title and by tracking set (more reliable)
        boolean isSpawnerShop = strippedTitle.startsWith("Spawner Shop");
        boolean isGeneralShop = isGeneralShopTitle(strippedTitle) || playersWithShopOpen.contains(player.getUniqueId());
        
        if (!isSpawnerShop && !isGeneralShop) {
            return;
        }
        
        // CRITICAL: Cancel ALL clicks FIRST - prevent item extraction
        // Cancel immediately before any other processing
        event.setCancelled(true);
        
        // Set result to DENY to prevent any item movement
        event.setResult(org.bukkit.event.Event.Result.DENY);
        
        // Start cursor monitoring task for this player if not already running
        startCursorMonitor(player);
        
        // IMPORTANT: Only process clicks in the TOP inventory (shop inventory)
        // Ignore all clicks in bottom inventory (player inventory)
        if (event.getClickedInventory() == null) {
            // Clear cursor even if inventory is null
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                event.setCursor(null);
            }
            return;
        }
        
        // Block ALL interactions with player inventory while shop is open
        if (event.getClickedInventory().equals(event.getView().getBottomInventory())) {
            // Cancel and clear cursor to prevent item extraction
            event.setCursor(null);
            if (event.getCurrentItem() != null) {
                event.setCurrentItem(null);
            }
            return;
        }
        
        // Ensure we're only processing top inventory clicks
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) {
            // Clear cursor if clicking outside
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                event.setCursor(null);
            }
            return;
        }
        
        // Block shift-click to prevent moving items
        if (event.isShiftClick()) {
            event.setCursor(null);
            if (event.getCurrentItem() != null) {
                event.setCurrentItem(null);
            }
            return;
        }
        
        // Block number key clicks (1-9) to prevent hotbar swapping
        if (event.getHotbarButton() >= 0) {
            event.setCursor(null);
            if (event.getCurrentItem() != null) {
                event.setCurrentItem(null);
            }
            return;
        }
        
        // Block ALL item movement actions
        if (event.getAction() == org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY ||
            event.getAction() == org.bukkit.event.inventory.InventoryAction.HOTBAR_SWAP ||
            event.getAction() == org.bukkit.event.inventory.InventoryAction.COLLECT_TO_CURSOR ||
            event.getAction() == org.bukkit.event.inventory.InventoryAction.SWAP_WITH_CURSOR) {
            event.setCursor(null);
            if (event.getCurrentItem() != null) {
                event.setCurrentItem(null);
            }
            return;
        }
        
        // Clear cursor on any click to prevent item pickup
        if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
            event.setCursor(null);
        }
        
        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        
        // Handle close button
        int inventorySize = event.getView().getTopInventory().getSize();
        int closeSlot = inventorySize >= 54 ? 53 : (inventorySize - 1);
        if (slot == closeSlot) {
            // Clear cursor before closing
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                event.setCursor(null);
            }
            player.closeInventory();
            return;
        }
        
        // Calculate back button slot based on inventory size
        int backSlot;
        if (inventorySize >= 54) {
            backSlot = 45; // Bottom left for 54-slot inventory
        } else if (inventorySize >= 27) {
            backSlot = 18;
        } else {
            backSlot = inventorySize - 2;
        }
        
        // Handle back button
        if (slot == backSlot) {
            // Clear cursor before closing
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                event.setCursor(null);
            }
            if (isSpawnerShop) {
                // Go back to shop selector
                player.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getShopSelectorGUI().openSelector(player);
                });
            } else if (isGeneralShop) {
                // Go back to general category selector
                player.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getGeneralCategorySelectorGUI().openSelector(player);
                });
            }
            return;
        }
        
        // Handle page navigation (both shop types) - handle both 27 and 54 slot sizes
        int prevSlot = inventorySize == 27 ? 20 : PREV_PAGE_SLOT;
        int nextSlot = inventorySize == 27 ? 24 : NEXT_PAGE_SLOT;
        if ((isGeneralShop || isSpawnerShop) && (slot == prevSlot || slot == nextSlot)) {
            String category = extractCategoryFromTitle(title);
            int currentPage = getPageFromTitle(title);
            int totalPages = getTotalPagesFromTitle(title);
            
            plugin.getLogger().info("Page navigation clicked - slot: " + slot + ", currentPage: " + currentPage + ", totalPages: " + totalPages);
            plugin.getLogger().info("Title was: " + org.bukkit.ChatColor.stripColor(title));
            
            // Validate and recalculate total pages based on actual items
            List<ShopItem> shopItems;
            int itemsPerPage;
            if (isSpawnerShop) {
                shopItems = getShopItems(ShopType.SPAWNER, null);
                itemsPerPage = 7; // Single row = 7 items per page
            } else {
                shopItems = getShopItems(ShopType.GENERAL, category);
                List<Integer> layoutSlots = getLayoutSlots();
                itemsPerPage = layoutSlots.size();
            }
            int actualTotalPages = Math.max(1, (int) Math.ceil((double) shopItems.size() / itemsPerPage));
            
            // Validate page numbers
            if (currentPage < 1) currentPage = 1;
            if (currentPage > actualTotalPages) currentPage = actualTotalPages;
            totalPages = actualTotalPages;
            
            if (slot == prevSlot && currentPage > 1) {
                int targetPage = currentPage - 1;
                plugin.getLogger().info("Navigating to previous page: " + targetPage);
                player.closeInventory();
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    try {
                        if (isSpawnerShop) {
                            openShop(player, ShopType.SPAWNER, null, targetPage);
                        } else {
                            openShop(player, ShopType.GENERAL, category, targetPage);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error opening shop page: " + e.getMessage());
                        e.printStackTrace();
                        // Don't let exception crash
                        if (isSpawnerShop) {
                            plugin.getShopSelectorGUI().openSelector(player);
                        } else {
                            plugin.getGeneralCategorySelectorGUI().openSelector(player);
                        }
                    }
                }, 2L);
            } else if (slot == nextSlot && currentPage < totalPages) {
                int targetPage = currentPage + 1;
                plugin.getLogger().info("Navigating to next page: " + targetPage);
                player.closeInventory();
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    try {
                        if (isSpawnerShop) {
                            openShop(player, ShopType.SPAWNER, null, targetPage);
                        } else {
                            openShop(player, ShopType.GENERAL, category, targetPage);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error opening shop page: " + e.getMessage());
                        e.printStackTrace();
                        // Don't let exception crash
                        if (isSpawnerShop) {
                            plugin.getShopSelectorGUI().openSelector(player);
                        } else {
                            plugin.getGeneralCategorySelectorGUI().openSelector(player);
                        }
                    }
                }, 2L);
            } else {
                plugin.getLogger().warning("Invalid page navigation - currentPage: " + currentPage + ", totalPages: " + totalPages + ", slot: " + slot);
            }
            return;
        }
        
        // Handle item purchase
        Material material = clicked.getType();
        // Don't handle clicks on navigation buttons or borders
        if (material == Material.SPAWNER || 
            (material != Material.GRAY_STAINED_GLASS_PANE && 
             material != Material.BARRIER && 
             material != Material.ARROW &&
             material != Material.NETHER_STAR)) {
            // Ensure cursor is clear before purchase
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                event.setCursor(null);
            }
            ShopType shopType = isSpawnerShop ? ShopType.SPAWNER : ShopType.GENERAL;
            String category = isGeneralShop ? extractCategoryFromTitle(title) : null;
            handlePurchase(player, clicked, shopType, category);
            
            // Clear cursor after purchase to prevent item pickup
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.getOpenInventory().getCursor() != null && 
                    player.getOpenInventory().getCursor().getType() != Material.AIR) {
                    player.getOpenInventory().setCursor(null);
                }
            });
        } else {
            // Clear cursor even for non-purchasable items
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                event.setCursor(null);
            }
            // Also clear cursor in next tick to be safe
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.getOpenInventory().getCursor() != null && 
                    player.getOpenInventory().getCursor().getType() != Material.AIR) {
                    player.getOpenInventory().setCursor(null);
                }
            });
        }
    }
    
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        String strippedTitle = org.bukkit.ChatColor.stripColor(title);
        
        // Check both by title and by tracking set (more reliable)
        boolean isSpawnerShop = strippedTitle.startsWith("Spawner Shop");
        boolean isGeneralShop = isGeneralShopTitle(strippedTitle) || playersWithShopOpen.contains(player.getUniqueId());
        
        if (isSpawnerShop || isGeneralShop) {
            // CRITICAL: Block ALL drag events to prevent item extraction
            event.setCancelled(true);
            event.setResult(org.bukkit.event.Event.Result.DENY);
        }
    }
    
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        String strippedTitle = org.bukkit.ChatColor.stripColor(title);
        
        boolean isSpawnerShop = strippedTitle.startsWith("Spawner Shop");
        boolean isGeneralShop = isGeneralShopTitle(strippedTitle);
        
        if (isSpawnerShop || isGeneralShop) {
            // Remove from tracking set
            playersWithShopOpen.remove(player.getUniqueId());
            
            // Stop cursor monitoring task
            stopCursorMonitor(player);
            
            // Clear cursor when closing shop to prevent item duplication
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.getOpenInventory().getCursor() != null && 
                    player.getOpenInventory().getCursor().getType() != Material.AIR) {
                    player.getOpenInventory().setCursor(null);
                }
            });
        }
    }
    
    private void startCursorMonitor(Player player) {
        java.util.UUID playerId = player.getUniqueId();
        
        // Cancel existing task if any
        stopCursorMonitor(player);
        
        // Start new monitoring task - runs every tick to clear cursor
        int taskId = org.bukkit.Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!player.isOnline()) {
                stopCursorMonitor(player);
                return;
            }
            
            // Check if player still has shop open (use tracking set for reliability)
            if (!playersWithShopOpen.contains(playerId)) {
                stopCursorMonitor(player);
                return;
            }
            
            // Also check title as backup
            String title = player.getOpenInventory().getTitle();
            String strippedTitle = org.bukkit.ChatColor.stripColor(title);
            boolean isSpawnerShop = strippedTitle.startsWith("Spawner Shop");
            boolean isGeneralShop = isGeneralShopTitle(strippedTitle);
            
            if (!isSpawnerShop && !isGeneralShop) {
                // Title doesn't match either, remove from tracking and stop monitoring
                playersWithShopOpen.remove(playerId);
                stopCursorMonitor(player);
                return;
            }
            
            // Clear cursor if it has an item
            if (player.getOpenInventory().getCursor() != null && 
                player.getOpenInventory().getCursor().getType() != Material.AIR) {
                player.getOpenInventory().setCursor(null);
            }
        }, 1L, 1L); // Run every tick
        
        cursorClearTasks.put(playerId, taskId);
    }
    
    private void stopCursorMonitor(Player player) {
        java.util.UUID playerId = player.getUniqueId();
        Integer taskId = cursorClearTasks.remove(playerId);
        if (taskId != null) {
            org.bukkit.Bukkit.getScheduler().cancelTask(taskId);
        }
    }
    
    /**
     * Helper method to detect if a title represents a general shop.
     * Handles both full titles ("General Shop - Category") and shortened titles ("Category (X/Y)").
     */
    private boolean isGeneralShopTitle(String strippedTitle) {
        // Check for full title format
        if (strippedTitle.startsWith("General Shop") && strippedTitle.contains(" - ")) {
            return true;
        }
        // Check for shortened title format - category name followed by page indicator like "(1/14)"
        // Known category names that appear in shortened titles
        String[] categoryNames = {"Building", "TNT/Redstone", "Food", "Misc", "Miscellaneous"};
        for (String categoryName : categoryNames) {
            // Check if title starts with category name
            if (strippedTitle.startsWith(categoryName)) {
                // Also check if it contains page pattern like "(X/Y)" or just "(" with numbers
                if (strippedTitle.contains("(") && strippedTitle.contains("/") && strippedTitle.contains(")")) {
                    return true;
                }
                // Or if it's just the category name (fallback)
                if (strippedTitle.equals(categoryName) || strippedTitle.equals(categoryName + " ")) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private String extractCategoryFromTitle(String title) {
        // Extract category from title like "&8General Shop - Building (Page 1/1)" or "Building (1/14)"
        try {
            // Strip color codes first
            String stripped = org.bukkit.ChatColor.stripColor(title);
            
            // Check for shortened format first (e.g., "Building (1/14)")
            String[] categoryNames = {"Building", "TNT/Redstone", "Food", "Misc", "Miscellaneous"};
            for (String categoryName : categoryNames) {
                if (stripped.startsWith(categoryName)) {
                    // Found a category name at the start - this is a shortened title
                    // Extract the category key
                    if (categoryName.equals("TNT/Redstone")) return "tnt_redstone";
                    if (categoryName.equals("Building")) return "building";
                    if (categoryName.equals("Food")) return "food";
                    if (categoryName.equals("Misc") || categoryName.equals("Miscellaneous")) return "misc";
                }
            }
            
            // Check for full format (e.g., "General Shop - Building (Page 1/1)")
            int dashIndex = stripped.indexOf(" - ");
            if (dashIndex == -1) {
                plugin.getLogger().warning("Could not find ' - ' in title: " + stripped + " - trying alternative extraction");
                // Try to extract from shortened format more carefully
                for (String categoryName : categoryNames) {
                    if (stripped.startsWith(categoryName)) {
                        String categoryKey = categoryName.equals("TNT/Redstone") ? "tnt_redstone" :
                                           categoryName.equals("Building") ? "building" :
                                           categoryName.equals("Food") ? "food" :
                                           categoryName.equals("Misc") || categoryName.equals("Miscellaneous") ? "misc" : null;
                        if (categoryKey != null) {
                            plugin.getLogger().info("Extracted category '" + categoryName + "' -> '" + categoryKey + "' from shortened title: " + stripped);
                            return categoryKey;
                        }
                    }
                }
                return null;
            }
            
            int pageIndex = stripped.indexOf(" (Page ", dashIndex);
            if (pageIndex == -1) {
                // Try alternative page format like "(1/14)"
                pageIndex = stripped.indexOf(" (", dashIndex);
            }
            
            String categoryPart;
            if (pageIndex == -1) {
                // No page info, just get everything after dash
                categoryPart = stripped.substring(dashIndex + 3).trim();
            } else {
                categoryPart = stripped.substring(dashIndex + 3, pageIndex).trim();
            }
            
            if (categoryPart == null || categoryPart.isEmpty()) {
                plugin.getLogger().warning("Category part is empty from title: " + stripped);
                return null;
            }
            
            // Convert display name back to category key
            if (categoryPart.equals("TNT/Redstone") || categoryPart.equals("TNT & Redstone")) return "tnt_redstone";
            if (categoryPart.equals("Building")) return "building";
            if (categoryPart.equals("Food")) return "food";
            if (categoryPart.equals("Misc") || categoryPart.equals("Miscellaneous")) return "misc";
            
            plugin.getLogger().info("Extracted category '" + categoryPart + "' -> '" + categoryPart.toLowerCase() + "' from title: " + stripped);
            return categoryPart.toLowerCase();
        } catch (Exception e) {
            plugin.getLogger().warning("Error extracting category from title '" + title + "': " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private void handlePurchase(Player player, ItemStack clickedItem, ShopType shopType, String category) {
        if (shopType == ShopType.SPAWNER) {
            handleSpawnerPurchase(player, clickedItem);
        } else {
            handleGeneralPurchase(player, clickedItem, category);
        }
    }
    
    private void handleGeneralPurchase(Player player, ItemStack clickedItem, String category) {
        Material material = clickedItem.getType();
        double price = plugin.getPriceManager().getPrice(material, category);
        
        if (price <= 0) {
            player.sendMessage(plugin.getMessage("item-not-buyable"));
            return;
        }
        
        // Open quantity selection GUI instead of direct purchase
        player.closeInventory();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getQuantitySelectionGUI().openQuantitySelection(
                player, clickedItem, ShopType.GENERAL, category, price);
        }, 2L);
    }
    
    private void handleSpawnerPurchase(Player player, ItemStack clickedItem) {
        // Find the spawner item from the display name
        String displayName = clickedItem.getItemMeta() != null ? clickedItem.getItemMeta().getDisplayName() : "";
        
        SpawnerShopItem spawnerItem = null;
        for (SpawnerShopItem item : spawnerItems) {
            String expectedName = item.typeName.toLowerCase().replace("_", " ");
            String[] words = expectedName.split(" ");
            StringBuilder nameBuilder = new StringBuilder();
            for (String word : words) {
                if (nameBuilder.length() > 0) {
                    nameBuilder.append(" ");
                }
                nameBuilder.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
            }
            if (displayName.contains(nameBuilder.toString())) {
                spawnerItem = item;
                break;
            }
        }
        
        if (spawnerItem == null) {
            player.sendMessage(plugin.getMessage("spawner-not-found"));
            return;
        }
        
        // Check balance
        double price = spawnerItem.price;
        double balance = plugin.getEconomyManager().getBalance(player);
        if (balance < price) {
            player.sendMessage(plugin.getMessage("insufficient-funds"));
            return;
        }
        
        // Withdraw money first
        if (!plugin.getEconomyManager().withdrawMoney(player, price)) {
            player.sendMessage(plugin.getMessage("purchase-failed"));
            return;
        }
        
        // Use PandoraSpawners API to create and give spawner
        try {
            org.bukkit.plugin.Plugin pandoraSpawners = Bukkit.getPluginManager().getPlugin("PandoraSpawners");
            if (pandoraSpawners == null) {
                // Refund money if plugin not found
                try {
                    Method depositMethod = plugin.getEconomy().getClass().getMethod("depositPlayer", org.bukkit.entity.Player.class, double.class);
                    depositMethod.invoke(plugin.getEconomy(), player, price);
                } catch (Exception refundEx) {
                    plugin.getLogger().warning("Failed to refund money after spawner purchase failure: " + refundEx.getMessage());
                }
                player.sendMessage(plugin.getMessage("purchase-failed"));
                return;
            }
            
            // Get SpawnerType enum
            Class<?> spawnerTypeClass = Class.forName("com.pandora.spawners.spawner.type.SpawnerType");
            Object spawnerType = java.lang.Enum.valueOf((Class<Enum>) spawnerTypeClass, spawnerItem.typeName);
            
            // Get DataManager class and create spawner
            Class<?> dataManagerClass = Class.forName("com.pandora.spawners.utility.DataManager");
            Method getSpawnersMethod = dataManagerClass.getMethod("getSpawners", spawnerTypeClass, int.class, boolean.class, boolean.class);
            
            @SuppressWarnings("unchecked")
            java.util.List<ItemStack> spawnerItems = (java.util.List<ItemStack>) getSpawnersMethod.invoke(null, spawnerType, 1, false, true);
            
            if (spawnerItems == null || spawnerItems.isEmpty()) {
                // Refund money if spawner creation failed
                try {
                    Method depositMethod = plugin.getEconomy().getClass().getMethod("depositPlayer", org.bukkit.entity.Player.class, double.class);
                    depositMethod.invoke(plugin.getEconomy(), player, price);
                } catch (Exception refundEx) {
                    plugin.getLogger().warning("Failed to refund money after spawner creation failure: " + refundEx.getMessage());
                }
                player.sendMessage(plugin.getMessage("purchase-failed"));
                return;
            }
            
            // Give spawner to player
            ItemStack spawnerItemStack = spawnerItems.get(0);
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(spawnerItemStack);
            
            if (!leftover.isEmpty()) {
                // Inventory full, refund money
                try {
                    Method depositMethod = plugin.getEconomy().getClass().getMethod("depositPlayer", org.bukkit.entity.Player.class, double.class);
                    depositMethod.invoke(plugin.getEconomy(), player, price);
                } catch (Exception refundEx) {
                    plugin.getLogger().warning("Failed to refund money after inventory full: " + refundEx.getMessage());
                }
                player.sendMessage(plugin.getMessage("inventory-full"));
                return;
            }
            
            // Send success message
            String spawnerName = spawnerItem.typeName.toLowerCase().replace("_", " ");
            String[] words = spawnerName.split(" ");
            StringBuilder nameBuilder = new StringBuilder();
            for (String word : words) {
                if (nameBuilder.length() > 0) {
                    nameBuilder.append(" ");
                }
                nameBuilder.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
            }
            player.sendMessage(plugin.getMessage("purchased-item")
                .replace("{item}", nameBuilder.toString() + " Spawner")
                .replace("{price}", plugin.getEconomyManager().formatMoney(price)));
            
            // Refresh GUI - need to store page before closing
            final String currentTitle = player.getOpenInventory().getTitle();
            final int currentPage = Math.max(1, getPageFromTitle(currentTitle));
            
            player.closeInventory();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                openShop(player, ShopType.SPAWNER, null, currentPage);
            }, 5L);
        } catch (Exception e) {
            // Refund money on error
            try {
                Method depositMethod = plugin.getEconomy().getClass().getMethod("depositPlayer", org.bukkit.entity.Player.class, double.class);
                depositMethod.invoke(plugin.getEconomy(), player, price);
            } catch (Exception refundEx) {
                plugin.getLogger().warning("Failed to refund money after spawner purchase error: " + refundEx.getMessage());
            }
            plugin.getLogger().warning("Failed to purchase spawner: " + e.getMessage());
            e.printStackTrace();
            player.sendMessage(plugin.getMessage("purchase-failed"));
        }
    }
    
    private int getPageFromTitle(String title) {
        try {
            // Strip color codes first
            String stripped = org.bukkit.ChatColor.stripColor(title);
            
            // Try full format first: "(Page X/Y)"
            int idx = stripped.lastIndexOf("(Page ");
            int offset = 6; // Length of "(Page "
            
            // If not found, try shortened format: "(X/Y)"
            if (idx == -1) {
                idx = stripped.lastIndexOf("(");
                offset = 1; // Length of "("
                
                if (idx == -1) {
                    plugin.getLogger().warning("Could not find page indicator in title: " + stripped);
                    return 1;
                }
            }
            
            int slash = stripped.indexOf('/', idx);
            int end = stripped.indexOf(')', idx);
            if (slash == -1 || end == -1) {
                plugin.getLogger().warning("Could not parse page format in title: " + stripped);
                return 1;
            }
            String pageStr = stripped.substring(idx + offset, slash).trim();
            int page = Integer.parseInt(pageStr);
            plugin.getLogger().info("Extracted page " + page + " from title: " + stripped);
            return page;
        } catch (Exception e) {
            plugin.getLogger().warning("Error parsing page from title '" + title + "': " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
    
    private int getTotalPagesFromTitle(String title) {
        try {
            // Strip color codes first
            String stripped = org.bukkit.ChatColor.stripColor(title);
            
            // Try full format first: "(Page X/Y)"
            int idx = stripped.lastIndexOf("(Page ");
            
            // If not found, try shortened format: "(X/Y)"
            if (idx == -1) {
                idx = stripped.lastIndexOf("(");
                if (idx == -1) return 1;
            }
            
            int slash = stripped.indexOf('/', idx);
            int end = stripped.indexOf(')', idx);
            if (slash == -1 || end == -1) return 1;
            String totalStr = stripped.substring(slash + 1, end).trim();
            return Integer.parseInt(totalStr);
        } catch (Exception e) {
            plugin.getLogger().warning("Error parsing total pages from title '" + title + "': " + e.getMessage());
            return 1;
        }
    }
    
    // GUI utility methods
    private int calculateOptimalSizeWithNav(int itemCount) {
        int neededSlots = itemCount + 2; // +1 for close button, +1 for back button
        return calculateOptimalSize(neededSlots);
    }
    
    private int calculateOptimalSize(int itemCount) {
        if (itemCount <= 9) return 9;
        if (itemCount <= 18) return 18;
        if (itemCount <= 27) return 27;
        if (itemCount <= 36) return 36;
        if (itemCount <= 45) return 45;
        return 54;
    }
    
    private void fillBorders(Inventory inv, int size, boolean fillTop, boolean fillBottom, boolean fillSides) {
        ItemStack border = createBorder();
        int rows = size / 9;
        
        if (fillTop && rows > 0) {
            for (int i = 0; i < 9; i++) {
                if (inv.getItem(i) == null) {
                    inv.setItem(i, border);
                }
            }
        }
        
        if (fillBottom && rows > 0) {
            int startBottom = (rows - 1) * 9;
            for (int i = startBottom; i < size; i++) {
                if (inv.getItem(i) == null) {
                    inv.setItem(i, border);
                }
            }
        }
        
        if (fillSides && rows > 0) {
            for (int row = 0; row < rows; row++) {
                int leftSlot = row * 9;
                int rightSlot = row * 9 + 8;
                if (inv.getItem(leftSlot) == null) {
                    inv.setItem(leftSlot, border);
                }
                if (inv.getItem(rightSlot) == null) {
                    inv.setItem(rightSlot, border);
                }
            }
        }
    }
    
    private void fillEmptySlots(Inventory inv, int size) {
        ItemStack border = createBorder();
        for (int i = 0; i < size; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, border);
            }
        }
    }
    
    private ItemStack createBorder() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&c✖ Close"));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Click to close this menu"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    // Helper classes
    private static class ShopItem {
        Material material;
        double price;
        SpawnerShopItem spawnerItem;
        
        ShopItem(Material material, double price, SpawnerShopItem spawnerItem) {
            this.material = material;
            this.price = price;
            this.spawnerItem = spawnerItem;
        }
    }
    
    private static class SpawnerShopItem {
        String typeName;
        double price;
        
        SpawnerShopItem(String typeName, double price, Object buyData) {
            this.typeName = typeName;
            this.price = price;
        }
    }
}
