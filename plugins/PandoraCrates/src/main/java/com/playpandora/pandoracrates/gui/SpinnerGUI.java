package com.playpandora.pandoracrates.gui;

import com.playpandora.pandoracrates.PandoraCrates;
import com.playpandora.pandoracrates.models.Crate;
import com.playpandora.pandoracrates.models.Reward;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.scheduler.BukkitRunnable;

public class SpinnerGUI {
    
    private final PandoraCrates plugin;
    private final Map<Player, SpinnerSession> activeSpinners;
    
    public SpinnerGUI(PandoraCrates plugin) {
        this.plugin = plugin;
        this.activeSpinners = new HashMap<>();
    }
    
    public void openSpinner(Player player, Crate crate, List<Reward> rewardPool, Runnable onFinish) {
        // Format title with Hypixel-style formatting
        String title = "&6&l" + crate.getDisplayName() + " &7Opening...";
        title = ChatColor.translateAlternateColorCodes('&', title);
        
        Inventory inv = Bukkit.createInventory(null, 27, title);
        
        // Create polished border with gradient effect
        ItemStack borderGlass = createBorderGlass();
        ItemStack cornerGlass = createCornerGlass();
        
        // Fill borders (top and bottom rows)
        for (int i = 0; i < 9; i++) {
            if (i == 0 || i == 8) {
                inv.setItem(i, cornerGlass);
                inv.setItem(i + 18, cornerGlass);
            } else {
                inv.setItem(i, borderGlass);
                inv.setItem(i + 18, borderGlass);
            }
        }
        
        // Fill side borders
        for (int i = 9; i < 18; i += 9) {
            inv.setItem(i, borderGlass);
            inv.setItem(i + 8, borderGlass);
        }
        
        // Fill center area with darker glass
        ItemStack centerGlass = createGlassPane();
        for (int i = 10; i < 17; i++) {
            inv.setItem(i, centerGlass);
        }
        
        // Center row (slots 10-16) for spinner - will be filled by animation
        player.openInventory(inv);
        
        SpinnerSession session = new SpinnerSession(player, inv, rewardPool, onFinish);
        activeSpinners.put(player, session);
        session.start();
    }
    
    public void updateSpinner(Player player, int index, List<Reward> rewardPool) {
        SpinnerSession session = activeSpinners.get(player);
        if (session == null) {
            return;
        }
        
        Inventory inv = session.getInventory();
        
        // Update center row (slots 10-16)
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        
        for (int i = 0; i < slots.length; i++) {
            int poolIndex = (index + i - 3 + rewardPool.size()) % rewardPool.size();
            if (poolIndex >= 0 && poolIndex < rewardPool.size()) {
                Reward reward = rewardPool.get(poolIndex);
                // Don't highlight during animation - only highlight in finishSpinner
                inv.setItem(slots[i], createRewardDisplayItem(reward, false));
            }
        }
    }
    
    public void finishSpinner(Player player, Reward finalReward) {
        SpinnerSession session = activeSpinners.remove(player);
        if (session != null) {
            session.cancel();
            
            Inventory inv = session.getInventory();
            
            // Highlight center slot
            int[] slots = {10, 11, 12, 13, 14, 15, 16};
            for (int i = 0; i < slots.length; i++) {
                if (i == 3) {
                    inv.setItem(slots[i], createRewardDisplayItem(finalReward, true));
                } else {
                    inv.setItem(slots[i], createGlassPane());
                }
            }
            
            // Close after 3 seconds
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && player.getOpenInventory().getTopInventory().equals(inv)) {
                    player.closeInventory();
                }
            }, 60L);
        }
    }
    
    private ItemStack createGlassPane() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createBorderGlass() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createCornerGlass() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createRewardDisplayItem(Reward reward, boolean highlight) {
        Material material = Material.DIAMOND;
        String name = "Reward";
        List<String> lore = new ArrayList<>();
        
        switch (reward.getType()) {
            case ITEM:
                material = reward.getMaterial();
                name = reward.getName() != null ? reward.getName() : "&7" + material.name();
                if (reward.getLore() != null && !reward.getLore().isEmpty()) {
                    lore.addAll(reward.getLore());
                } else {
                    lore.add("&7Type: &eItem Reward");
                    lore.add("&7Material: &6" + material.name());
                }
                break;
            case MONEY:
                // MONEY rewards now use banknotes
                material = Material.PAPER;
                name = "&6$" + reward.getMoneyAmount() + " &7Banknote";
                lore.add("&7Value: &6$" + reward.getMoneyAmount());
                lore.add("&7Type: &eBanknote Reward");
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
            default:
                material = Material.DIAMOND;
                name = "Reward";
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Set display name (no >>> <<<)
            if (highlight) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6&l" + name));
            } else {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            }
            
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
            
            // Only show "YOU WON THIS" when highlight is true (final reward display)
            if (highlight) {
                translatedLore.add("");
                translatedLore.add(ChatColor.translateAlternateColorCodes('&', "&6&lYOU WON THIS!"));
            }
            
            meta.setLore(translatedLore);
            
            if (highlight) {
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
    
    private class SpinnerSession {
        private final Player player;
        private final Inventory inventory;
        private final List<Reward> rewardPool;
        private final Runnable onFinish;
        private BukkitTask task;
        private int currentIndex;
        private int tick;
        private final int rollDuration = 60;
        private final int slowDownTicks = 20;
        
        public SpinnerSession(Player player, Inventory inventory, List<Reward> rewardPool, Runnable onFinish) {
            this.player = player;
            this.inventory = inventory;
            this.rewardPool = rewardPool;
            this.onFinish = onFinish;
            this.currentIndex = 0;
            this.tick = 0;
        }
        
        public void start() {
            if (rewardPool.isEmpty()) {
                player.closeInventory();
                return;
            }
            
            task = new BukkitRunnable() {
                @Override
                public void run() {
                    tick++;
                    
                    // Update spinner
                    if (tick < rollDuration - slowDownTicks) {
                        // Fast rolling
                        currentIndex = (currentIndex + 1) % rewardPool.size();
                    } else {
                        // Slow down
                        int remaining = rollDuration - tick;
                        if (remaining > 0 && tick % Math.max(1, remaining / 5) == 0) {
                            currentIndex = (currentIndex + 1) % rewardPool.size();
                        }
                    }
                    
                    updateSpinner(player, currentIndex, rewardPool);
                    
                    // End animation
                    if (tick >= rollDuration) {
                        cancel();
                        if (onFinish != null) {
                            onFinish.run();
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
        
        public void cancel() {
            if (task != null) {
                task.cancel();
            }
        }
        
        public Inventory getInventory() {
            return inventory;
        }
        
        public int getCurrentIndex() {
            return currentIndex;
        }
    }
}

