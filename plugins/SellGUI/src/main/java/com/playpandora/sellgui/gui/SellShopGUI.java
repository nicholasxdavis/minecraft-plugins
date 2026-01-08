package com.playpandora.sellgui.gui;

import com.playpandora.sellgui.SellGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SellShopGUI implements Listener {
    
    private final SellGUI plugin;
    private static final int SELL_ALL_SLOT = 53; // Bottom right corner
    private static final int CLOSE_SLOT = 45;
    private static final int PREV_PAGE_SLOT = 47;
    private static final int NEXT_PAGE_SLOT = 51;
    
    public SellShopGUI(SellGUI plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public void openShop(Player player) {
        openShop(player, 1);
    }
    
    public void openShop(Player player, int page) {
        if (page < 1) page = 1;
        
        String baseTitle = plugin.getConfig().getString("gui.title", "&8Sell Shop");
        // Strip any bold formatting from the title
        baseTitle = org.bukkit.ChatColor.stripColor(org.bukkit.ChatColor.translateAlternateColorCodes('&', baseTitle)).replace(" ", "");
        baseTitle = "&8Sell Shop"; // Force &8 style (no bold)
        int size = 54; // Always use 54 for sell shop (has player inventory below)
        
        // Build sellable material list and layout slots to compute pages
        List<Material> sellableMaterials = new ArrayList<>(plugin.getPriceManager().getAllPrices().keySet());
        sellableMaterials.sort((a, b) -> {
            double priceA = plugin.getPriceManager().getPrice(a);
            double priceB = plugin.getPriceManager().getPrice(b);
            return Double.compare(priceB, priceA); // Descending order
        });
        
        List<Integer> layoutSlots = getLayoutSlots();
        int itemsPerPage = layoutSlots.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) sellableMaterials.size() / itemsPerPage));
        
        if (page > totalPages) page = totalPages;
        
        String title = baseTitle + " &7(Page " + page + "/" + totalPages + ")";
        
        Inventory inv = Bukkit.createInventory(null, size,
            org.bukkit.ChatColor.translateAlternateColorCodes('&', title));
        
        // Fill borders first
        fillBorders(inv, size, true, true, true);
        
        // Add sellable items display (showing farmable mats and valuable ones) with pagination
        addSellableItems(inv, player, sellableMaterials, layoutSlots, page);
        
        // Add sell all button (center bottom)
        ItemStack sellAllItem = createSellAllButton(player);
        inv.setItem(SELL_ALL_SLOT, sellAllItem);
        
        // Add close button (bottom left)
        ItemStack closeItem = createCloseButton();
        inv.setItem(CLOSE_SLOT, closeItem);
        
        // Add page navigation buttons if needed
        if (totalPages > 1) {
            if (page > 1) {
                inv.setItem(PREV_PAGE_SLOT, createPrevPageButton(page, totalPages));
            }
            if (page < totalPages) {
                inv.setItem(NEXT_PAGE_SLOT, createNextPageButton(page, totalPages));
            }
        }
        
        // Fill remaining empty slots with borders
        fillEmptySlots(inv, size);
        
        player.openInventory(inv);
    }
    
    private void addSellableItems(Inventory inv, Player player,
                                  List<Material> sellableMaterials,
                                  List<Integer> layoutSlots,
                                  int page) {
        int itemsPerPage = layoutSlots.size();
        int totalItems = sellableMaterials.size();
        if (totalItems == 0 || itemsPerPage == 0) {
            return;
        }
        
        int startIndex = (page - 1) * itemsPerPage;
        if (startIndex >= totalItems) {
            startIndex = 0;
        }
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);
        
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Material material = sellableMaterials.get(i);
            if (slotIndex >= layoutSlots.size()) {
                break;
            }
            int slot = layoutSlots.get(slotIndex++);
            ItemStack item = createSellableItemDisplay(material, player);
            inv.setItem(slot, item);
        }
    }
    
    private List<Integer> getLayoutSlots() {
        List<Integer> slots = new ArrayList<>();
        // Rows 2-4 (index 1-3): slots 9-35, skip borders (col 0 and 8) and bottom row
        for (int row = 1; row <= 3; row++) {
            for (int col = 1; col <= 7; col++) {
                int slot = row * 9 + col;
                // Ensure we don't conflict with control buttons (which are on bottom row anyway)
                if (slot == SELL_ALL_SLOT || slot == CLOSE_SLOT || slot == PREV_PAGE_SLOT || slot == NEXT_PAGE_SLOT) {
                    continue;
                }
                slots.add(slot);
            }
        }
        return slots;
    }
    
    private ItemStack createSellableItemDisplay(Material material, Player player) {
        double price = plugin.getPriceManager().getPrice(material);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Format material name nicely
            String materialName = material.name().toLowerCase().replace("_", " ");
            String[] words = materialName.split(" ");
            StringBuilder displayName = new StringBuilder();
            for (String word : words) {
                if (displayName.length() > 0) {
                    displayName.append(" ");
                }
                displayName.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
            }
            
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&8" + displayName.toString()));
            
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7&m                              "));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&7Price per item: &6$" + String.format("%.2f", price)));
            
            // Count how many of this item player has
            int playerAmount = 0;
            for (ItemStack invItem : player.getInventory().getContents()) {
                if (invItem != null && invItem.getType() == material) {
                    playerAmount += invItem.getAmount();
                }
            }
            
            if (playerAmount > 0) {
                double totalValue = price * playerAmount;
                lore.add("");
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                    "&7You have: &a" + playerAmount + " &7items"));
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                    "&7Total value: &6$" + String.format("%.2f", totalValue)));
            } else {
                lore.add("");
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                    "&7You have: &c0 &7items"));
            }
            
            lore.add("");
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&7Drag items from your inventory"));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&7to this GUI to sell them!"));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7&m                              "));
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack createSellAllButton(Player player) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&6SELL ALL ITEMS"));
            
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Sell all sellable items from"));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7your inventory instantly!"));
            lore.add("");
            
            // Calculate total value
            double totalValue = calculateTotalInventoryValue(player);
            if (totalValue > 0) {
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                    "&7Estimated Value: &6" + plugin.getEconomyManager().formatMoney(totalValue)));
            } else {
                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7No sellable items found"));
            }
            
            lore.add("");
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Click to sell all"));
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createPrevPageButton(int page, int totalPages) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8Previous Page"));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&7Page &8" + (page - 1) + " &7of &8" + totalPages));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createNextPageButton(int page, int totalPages) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8Next Page"));
            List<String> lore = new ArrayList<>();
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&7Page &8" + (page + 1) + " &7of &8" + totalPages));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private double calculateTotalInventoryValue(Player player) {
        double total = 0.0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                if (plugin.getPriceManager().isSellable(item.getType())) {
                    total += plugin.getPriceManager().getPrice(item.getType()) * item.getAmount();
                }
            }
        }
        return total;
    }
    
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        String baseTitleColored = org.bukkit.ChatColor.translateAlternateColorCodes('&',
            plugin.getConfig().getString("gui.title", "&8Sell Shop"));
        
        if (!title.startsWith(baseTitleColored)) {
            return;
        }
        
        // Handle clicking items from player's inventory to sell
        if (event.getClickedInventory() != null && 
            event.getClickedInventory().equals(event.getView().getBottomInventory())) {
            // Player clicked item from their inventory - sell it
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                event.setCancelled(true);
                sellItem(player, clickedItem);
            }
            return;
        }
        
        // Cancel clicks in the GUI itself
        event.setCancelled(true);
        
        int slot = event.getSlot();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        
        // Handle close button
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        
        // Handle sell all button
        if (slot == SELL_ALL_SLOT) {
            handleSellAllClick(player, event);
            return;
        }
        
        // Handle page navigation
        if (slot == PREV_PAGE_SLOT || slot == NEXT_PAGE_SLOT) {
            int currentPage = getPageFromTitle(title);
            int totalPages = getTotalPagesFromTitle(title);
            if (slot == PREV_PAGE_SLOT && currentPage > 1) {
                int targetPage = currentPage - 1;
                player.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin, () -> openShop(player, targetPage));
            } else if (slot == NEXT_PAGE_SLOT && currentPage < totalPages) {
                int targetPage = currentPage + 1;
                player.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin, () -> openShop(player, targetPage));
            }
            return;
        }
    }
    
    @EventHandler
    public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        String baseTitleColored = org.bukkit.ChatColor.translateAlternateColorCodes('&',
            plugin.getConfig().getString("gui.title", "&8Sell Shop"));
        
        if (!title.startsWith(baseTitleColored)) {
            return;
        }
        
        // Allow dragging from player inventory, but cancel if dragging into GUI slots
        for (int slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize()) {
                // Dragging into GUI - cancel and sell the item instead
                event.setCancelled(true);
                ItemStack dragged = event.getOldCursor();
                if (dragged != null && dragged.getType() != Material.AIR) {
                    sellItem(player, dragged);
                }
                return;
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Nothing to do - no pending confirmations
    }
    
    private void handleSellAllClick(Player player, InventoryClickEvent event) {
        // Sell immediately - no confirmation needed
        sellAllItems(player);
    }
    
    private void sellAllItems(Player player) {
        double totalEarned = 0.0;
        double totalXP = 0.0;
        int itemsSold = 0;
        
        // Check all items in player's inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            
            Material material = item.getType();
            if (!plugin.getPriceManager().isSellable(material)) {
                continue;
            }
            
            double price = plugin.getPriceManager().getPrice(material);
            int amount = item.getAmount();
            double itemTotal = price * amount;
            
            totalEarned += itemTotal;
            itemsSold += amount;
            
            // Calculate XP
            double xp = calculateXP(itemTotal);
            totalXP += xp;
            
            // Remove item from inventory
            item.setAmount(0);
        }
        
        if (totalEarned > 0) {
            // Give money
            plugin.getEconomyManager().giveMoney(player, totalEarned);
            
            // Give XP
            if (plugin.getLevelIntegration().isAvailable() && totalXP > 0) {
                plugin.getLevelIntegration().giveXP(player, totalXP);
                player.sendMessage(plugin.getMessage("xp-gained").replace("{xp}", 
                    String.format("%.1f", totalXP)));
            }
            
            // Send Hook notification
            sendSellNotification(player, totalEarned, itemsSold);
            
            player.sendMessage(plugin.getMessage("sold-all").replace("{total}", 
                plugin.getEconomyManager().formatMoney(totalEarned)));
        } else {
            player.sendMessage(plugin.getMessage("nothing-to-sell"));
        }
        
        // Refresh GUI
        player.closeInventory();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            openShop(player);
        }, 5L);
    }
    
    private void sellItem(Player player, ItemStack item) {
        Material material = item.getType();
        
        if (!plugin.getPriceManager().isSellable(material)) {
            player.sendMessage(plugin.getMessage("item-not-sellable"));
            return;
        }
        
        double price = plugin.getPriceManager().getPrice(material);
        int amount = item.getAmount();
        double total = price * amount;
        
        // Give money
        plugin.getEconomyManager().giveMoney(player, total);
        
        // Calculate and give XP
        double xp = calculateXP(total);
        if (plugin.getLevelIntegration().isAvailable() && xp > 0) {
            plugin.getLevelIntegration().giveXP(player, xp);
        }
        
        // Remove item from inventory
        item.setAmount(0);
        
        // Send Hook notification (only for larger sales to avoid spam)
        if (total >= 100.0) {
            sendSellNotification(player, total, amount);
        }
        
        // Send message
        String itemName = material.name().toLowerCase().replace("_", " ");
        player.sendMessage(plugin.getMessage("sold-item")
            .replace("{amount}", String.valueOf(amount))
            .replace("{item}", itemName)
            .replace("{total}", plugin.getEconomyManager().formatMoney(total)));
        
        // Refresh GUI
        player.closeInventory();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            openShop(player);
        }, 5L);
    }
    
    private void sendSellNotification(Player player, double amount, int itemsSold) {
        try {
            org.bukkit.plugin.Plugin hookPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("Hook");
            if (hookPlugin != null && hookPlugin.isEnabled()) {
                Object hookInstance = hookPlugin.getClass().getMethod("getInstance").invoke(null);
                Object hookAPI = hookInstance.getClass().getMethod("getAPI").invoke(hookInstance);
                hookAPI.getClass().getMethod("sendSell", org.bukkit.entity.Player.class, 
                    double.class, int.class)
                    .invoke(hookAPI, player, amount, itemsSold);
            }
        } catch (Exception e) {
            // Silently fail if Hook is not available
        }
    }
    
    private double calculateXP(double moneyEarned) {
        if (!plugin.getConfig().getBoolean("xp-rewards.enabled", true)) {
            return 0.0;
        }
        
        double multiplier = plugin.getConfig().getDouble("xp-rewards.multiplier", 0.1);
        double xp = moneyEarned * multiplier;
        
        double minXP = plugin.getConfig().getDouble("xp-rewards.min-xp", 0.5);
        double maxXP = plugin.getConfig().getDouble("xp-rewards.max-xp", 10.0);
        
        xp = Math.max(minXP, Math.min(maxXP, xp));
        
        return xp;
    }
    
    private int getPageFromTitle(String title) {
        try {
            int idx = title.lastIndexOf("(Page ");
            if (idx == -1) return 1;
            int slash = title.indexOf('/', idx);
            int end = title.indexOf(')', idx);
            if (slash == -1 || end == -1) return 1;
            String pageStr = title.substring(idx + 6, slash).trim();
            return Integer.parseInt(pageStr);
        } catch (Exception e) {
            return 1;
        }
    }
    
    private int getTotalPagesFromTitle(String title) {
        try {
            int idx = title.lastIndexOf("(Page ");
            if (idx == -1) return 1;
            int slash = title.indexOf('/', idx);
            int end = title.indexOf(')', idx);
            if (slash == -1 || end == -1) return 1;
            String totalStr = title.substring(slash + 1, end).trim();
            return Integer.parseInt(totalStr);
        } catch (Exception e) {
            return 1;
        }
    }
    
    // GUI utility methods (Hypixel standard)
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
}

