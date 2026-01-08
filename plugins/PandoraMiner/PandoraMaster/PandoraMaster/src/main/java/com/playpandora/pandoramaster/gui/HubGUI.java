package com.playpandora.pandoramaster.gui;

import com.playpandora.pandoramaster.PandoraMaster;
import com.playpandora.pandoramaster.managers.SkriptHook;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class HubGUI {
    
    private final PandoraMaster plugin;
    private final SkriptHook skriptHook;
    
    public HubGUI(PandoraMaster plugin) {
        this.plugin = plugin;
        this.skriptHook = plugin.getSkriptHook();
    }
    
    public void openGUI(Player player) {
        String title = "&8Hub Menu - hide using /hubmenu";
        title = org.bukkit.ChatColor.translateAlternateColorCodes('&', title);
        
        Inventory inv = Bukkit.createInventory(null, 54, title);
        
        // Fill borders
        fillBorders(inv);
        
        // Add player head with stats (slot 4 - center of first row)
        inv.setItem(4, createPlayerHead(player));
        
        // Add teleport options (row 2)
        inv.setItem(19, createSpawnItem());
        inv.setItem(21, createHomesItem());
        inv.setItem(23, createBaseItem(player));
        
        // Add shop GUIs (row 3)
        inv.setItem(28, createPetShopItem());
        inv.setItem(30, createPerkShopItem());
        inv.setItem(32, createFarmShopItem());
        inv.setItem(34, createSellGUIItem());
        
        // Add skills above sell GUI (row 2, same column)
        inv.setItem(25, createSkillsItem());
        
        // Add close button (bottom right)
        inv.setItem(53, createCloseButton());
        
        // Fill remaining empty slots
        fillEmptySlots(inv);
        
        player.openInventory(inv);
    }
    
    private ItemStack createPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&e&l" + player.getName() + "'s Stats"));
            
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7&m                                    "));
            
            // Get stats from Skript (Master stats)
            int kills = skriptHook.getKills(player);
            int deaths = skriptHook.getDeaths(player);
            int mobKills = skriptHook.getMobKills(player);
            double totalMoney = skriptHook.getTotalMoney(player);
            String joined = skriptHook.getJoinedDate(player);
            
            // Master stats section
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lMaster Stats"));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&7Kills: &6" + kills));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&7Deaths: &6" + deaths));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&7Mobs Killed: &6" + mobKills));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&7Lifetime Earnings: &6$" + skriptHook.formatMoney(totalMoney)));
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                "&7Joined: &6" + joined));
            
            // Get AuraSkills stats if available (using reflection to avoid compile-time dependency)
            Plugin auraSkillsPlugin = Bukkit.getPluginManager().getPlugin("AuraSkills");
            if (auraSkillsPlugin != null) {
                try {
                    // Access AuraSkills API via reflection
                    Class<?> apiClass = Class.forName("dev.aurelium.auraskills.api.AuraSkillsApi");
                    Method getMethod = apiClass.getMethod("get");
                    Object api = getMethod.invoke(null);
                    
                    // Get UserManager
                    Method getUserManagerMethod = apiClass.getMethod("getUserManager");
                    Object userManager = getUserManagerMethod.invoke(api);
                    
                    // Get user
                    Method getUserMethod = userManager.getClass().getMethod("getUser", java.util.UUID.class);
                    Object skillsUser = getUserMethod.invoke(userManager, player.getUniqueId());
                    
                    if (skillsUser != null) {
                        // Check if user is loaded
                        Method isLoadedMethod = skillsUser.getClass().getMethod("isLoaded");
                        Boolean isLoaded = (Boolean) isLoadedMethod.invoke(skillsUser);
                        
                        if (isLoaded != null && isLoaded) {
                            // Get GlobalRegistry
                            Method getGlobalRegistryMethod = apiClass.getMethod("getGlobalRegistry");
                            Object globalRegistry = getGlobalRegistryMethod.invoke(api);
                            
                            // Get skills
                            Method getSkillsMethod = globalRegistry.getClass().getMethod("getSkills");
                            @SuppressWarnings("unchecked")
                            java.util.Collection<Object> allSkills = (java.util.Collection<Object>) getSkillsMethod.invoke(globalRegistry);
                            
                            lore.add("");
                            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lSkills"));
                            
                            // Get all enabled skills and display their levels
                            List<Object> enabledSkills = new ArrayList<>();
                            for (Object skill : allSkills) {
                                Method isEnabledMethod = skill.getClass().getMethod("isEnabled");
                                Boolean enabled = (Boolean) isEnabledMethod.invoke(skill);
                                if (enabled != null && enabled) {
                                    enabledSkills.add(skill);
                                }
                            }
                            
                            // Sort skills by name for consistency
                            enabledSkills.sort((s1, s2) -> {
                                try {
                                    Method getIdMethod = s1.getClass().getMethod("getId");
                                    Object id1 = getIdMethod.invoke(s1);
                                    Object id2 = getIdMethod.invoke(s2);
                                    Method getKeyMethod = id1.getClass().getMethod("getKey");
                                    String key1 = (String) getKeyMethod.invoke(id1);
                                    String key2 = (String) getKeyMethod.invoke(id2);
                                    return key1.compareToIgnoreCase(key2);
                                } catch (Exception e) {
                                    return 0;
                                }
                            });
                            
                            // Display top skills (limit to avoid too long lore)
                            int displayed = 0;
                            int maxSkills = 8;
                            for (Object skill : enabledSkills) {
                                if (displayed >= maxSkills) break;
                                Method getSkillLevelMethod = skillsUser.getClass().getMethod("getSkillLevel", skill.getClass().getInterfaces()[0]);
                                Integer level = (Integer) getSkillLevelMethod.invoke(skillsUser, skill);
                                Method getDisplayNameMethod = skill.getClass().getMethod("getDisplayName", java.util.Locale.class, boolean.class);
                                String skillName = (String) getDisplayNameMethod.invoke(skill, java.util.Locale.ENGLISH, false);
                                lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                                    "&7" + skillName + ": &6" + level));
                                displayed++;
                            }
                            
                            lore.add("");
                            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&e&lStats"));
                            
                            // Display important stats
                            try {
                                Class<?> statsClass = Class.forName("dev.aurelium.auraskills.api.stat.Stats");
                                Method getStatLevelMethod = skillsUser.getClass().getMethod("getStatLevel", Class.forName("dev.aurelium.auraskills.api.stat.Stat"));
                                
                                // Get HEALTH stat
                                java.lang.reflect.Field healthField = statsClass.getField("HEALTH");
                                Object healthStat = healthField.get(null);
                                Double health = (Double) getStatLevelMethod.invoke(skillsUser, healthStat);
                                
                                // Get STRENGTH stat
                                java.lang.reflect.Field strengthField = statsClass.getField("STRENGTH");
                                Object strengthStat = strengthField.get(null);
                                Double strength = (Double) getStatLevelMethod.invoke(skillsUser, strengthStat);
                                
                                // Get LUCK stat
                                java.lang.reflect.Field luckField = statsClass.getField("LUCK");
                                Object luckStat = luckField.get(null);
                                Double luck = (Double) getStatLevelMethod.invoke(skillsUser, luckStat);
                                
                                // Get WISDOM stat
                                java.lang.reflect.Field wisdomField = statsClass.getField("WISDOM");
                                Object wisdomStat = wisdomField.get(null);
                                Double wisdom = (Double) getStatLevelMethod.invoke(skillsUser, wisdomStat);
                                
                                if (health != null && health > 0) {
                                    lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                                        "&7Health: &6" + String.format("%.1f", health)));
                                }
                                if (strength != null && strength > 0) {
                                    lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                                        "&7Strength: &6" + String.format("%.1f", strength)));
                                }
                                if (luck != null && luck > 0) {
                                    lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                                        "&7Luck: &6" + String.format("%.1f", luck)));
                                }
                                if (wisdom != null && wisdom > 0) {
                                    lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', 
                                        "&7Wisdom: &6" + String.format("%.1f", wisdom)));
                                }
                            } catch (Exception e) {
                                // Stats might not be available, skip
                                plugin.getLogger().fine("Could not get AuraSkills stats: " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    // AuraSkills API not available or error accessing it
                    plugin.getLogger().fine("Could not access AuraSkills API: " + e.getMessage());
                }
            }
            
            lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7&m                                    "));
            
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        
        return head;
    }
    
    private ItemStack createSpawnItem() {
        String name = plugin.getConfig().getString("items.spawn.name", "&eTeleport to Spawn");
        List<String> lore = plugin.getConfig().getStringList("items.spawn.lore");
        if (lore.isEmpty()) {
            lore.add("&7Click to teleport to spawn");
        }
        return createMenuItem(Material.GRASS_BLOCK, name, lore);
    }
    
    private ItemStack createHomesItem() {
        String name = plugin.getConfig().getString("items.homes.name", "&ePlayer Homes");
        List<String> lore = plugin.getConfig().getStringList("items.homes.lore");
        if (lore.isEmpty()) {
            lore.add("&7Click to view your homes");
        }
        return createMenuItem(Material.RED_BED, name, lore);
    }
    
    private ItemStack createBaseItem(Player player) {
        // Check if player is in a base
        boolean hasBase = checkIfPlayerHasBase(player);
        
        Material material = hasBase ? Material.DIAMOND_SWORD : Material.BARRIER;
        String name;
        List<String> lore;
        
        if (hasBase) {
            name = plugin.getConfig().getString("items.base.name", "&eBase");
            lore = plugin.getConfig().getStringList("items.base.lore");
            if (lore.isEmpty()) {
                lore.add("&7Click to manage your base");
            }
        } else {
            name = plugin.getConfig().getString("items.base.locked.name", "&7Base &8(Locked)");
            lore = plugin.getConfig().getStringList("items.base.locked.lore");
            if (lore.isEmpty()) {
                lore.add("&7You must be in a base");
                lore.add("&7to access this menu");
            }
        }
        
        return createMenuItem(material, name, lore);
    }
    
    private ItemStack createPetShopItem() {
        String name = plugin.getConfig().getString("items.petshop.name", "&ePet Shop");
        List<String> lore = plugin.getConfig().getStringList("items.petshop.lore");
        if (lore.isEmpty()) {
            lore.add("&7Click to open the pet shop");
        }
        return createMenuItem(Material.BONE, name, lore);
    }
    
    private ItemStack createPerkShopItem() {
        String name = plugin.getConfig().getString("items.perkshop.name", "&ePerk Shop");
        List<String> lore = plugin.getConfig().getStringList("items.perkshop.lore");
        if (lore.isEmpty()) {
            lore.add("&7Click to open the perk shop");
        }
        return createMenuItem(Material.NETHER_STAR, name, lore);
    }
    
    private ItemStack createFarmShopItem() {
        String name = plugin.getConfig().getString("items.farmshop.name", "&eFarm Shop");
        List<String> lore = plugin.getConfig().getStringList("items.farmshop.lore");
        if (lore.isEmpty()) {
            lore.add("&7Click to open the farm shop");
        }
        return createMenuItem(Material.GOLDEN_HOE, name, lore);
    }
    
    private ItemStack createSkillsItem() {
        String name = plugin.getConfig().getString("items.skills.name", "&eSkills");
        List<String> lore = plugin.getConfig().getStringList("items.skills.lore");
        if (lore.isEmpty()) {
            lore.add("&7Click to view your skills");
        }
        return createMenuItem(Material.EXPERIENCE_BOTTLE, name, lore);
    }
    
    private ItemStack createSellGUIItem() {
        String name = plugin.getConfig().getString("items.sellgui.name", "&eSell GUI");
        List<String> lore = plugin.getConfig().getStringList("items.sellgui.lore");
        if (lore.isEmpty()) {
            lore.add("&7Click to open the sell menu");
        }
        return createMenuItem(Material.EMERALD, name, lore);
    }
    
    private ItemStack createCloseButton() {
        String name = plugin.getConfig().getString("items.close.name", "&c✖ Close");
        List<String> lore = plugin.getConfig().getStringList("items.close.lore");
        if (lore.isEmpty()) {
            lore.add("&7Click to close this menu");
        }
        return createMenuItem(Material.BARRIER, name, lore);
    }
    
    private ItemStack createMenuItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', name));
            
            List<String> formattedLore = new ArrayList<>();
            for (String line : lore) {
                formattedLore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(formattedLore);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private boolean checkIfPlayerHasBase(Player player) {
        try {
            // Check if player is in a base using Factions plugin
            org.bukkit.plugin.Plugin factionsPlugin = Bukkit.getPluginManager().getPlugin("Factions");
            if (factionsPlugin != null) {
                try {
                    // Get FPlayers instance
                    Class<?> fPlayersClass = Class.forName("com.massivecraft.factions.FPlayers");
                    Object fPlayersInstance = fPlayersClass.getMethod("getInstance").invoke(null);
                    
                    if (fPlayersInstance != null) {
                        // Get FPlayer for this player
                        Method getByPlayerMethod = fPlayersClass.getMethod("getByPlayer", Player.class);
                        Object fPlayer = getByPlayerMethod.invoke(fPlayersInstance, player);
                        
                        if (fPlayer != null) {
                            // Check if player has a faction
                            Method hasFactionMethod = fPlayer.getClass().getMethod("hasFaction");
                            Boolean hasFaction = (Boolean) hasFactionMethod.invoke(fPlayer);
                            
                            if (hasFaction != null && hasFaction) {
                                // Get faction
                                Method getFactionMethod = fPlayer.getClass().getMethod("getFaction");
                                Object faction = getFactionMethod.invoke(fPlayer);
                                
                                if (faction != null) {
                                    // Check if faction is normal and has a beacon (base)
                                    Method isNormalMethod = faction.getClass().getMethod("isNormal");
                                    Method hasBeaconMethod = faction.getClass().getMethod("hasBeacon");
                                    
                                    Boolean isNormal = (Boolean) isNormalMethod.invoke(faction);
                                    Boolean hasBeacon = (Boolean) hasBeaconMethod.invoke(faction);
                                    
                                    if (isNormal != null && isNormal && hasBeacon != null && hasBeacon) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().fine("Factions check failed: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().fine("Base check failed: " + e.getMessage());
        }
        
        return false;
    }
    
    private void fillBorders(Inventory inv) {
        ItemStack border = createBorder();
        int size = inv.getSize();
        int rows = size / 9;
        
        // Top row
        for (int i = 0; i < 9; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, border);
            }
        }
        
        // Bottom row
        int startBottom = (rows - 1) * 9;
        for (int i = startBottom; i < size; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, border);
            }
        }
        
        // Left and right columns
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
    
    private void fillEmptySlots(Inventory inv) {
        ItemStack border = createBorder();
        for (int i = 0; i < inv.getSize(); i++) {
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
    
    public void handleClick(Player player, int slot, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        
        Material type = item.getType();
        
        // Close button
        if (type == Material.BARRIER && slot == 53) {
            player.closeInventory();
            return;
        }
        
        // Spawn teleport
        if (type == Material.GRASS_BLOCK && slot == 19) {
            player.closeInventory();
            player.performCommand("spawn");
            return;
        }
        
        // Homes GUI
        if (type == Material.RED_BED && slot == 21) {
            player.closeInventory();
            // Try to open EssentialsGUI home GUI
            org.bukkit.plugin.Plugin essentialsGUI = Bukkit.getPluginManager().getPlugin("EssentialsGUI");
            if (essentialsGUI != null) {
                try {
                    Object homeGUI = essentialsGUI.getClass().getMethod("getHomeGUI").invoke(essentialsGUI);
                    if (homeGUI != null) {
                        homeGUI.getClass().getMethod("openGUI", Player.class).invoke(homeGUI, player);
                        return;
                    }
                } catch (Exception e) {
                    // Fallback to command
                }
            }
            player.performCommand("home");
            return;
        }
        
        // Base GUI
        if ((type == Material.DIAMOND_SWORD || type == Material.BARRIER) && slot == 23) {
            if (type == Material.BARRIER) {
                // Locked - don't do anything
                player.sendMessage(plugin.formatMessage("messages.no-permission",
                    "&e&lPandora &8» &r&7You must be in a base to access this menu!"));
                return;
            }
            player.closeInventory();
            player.performCommand("base");
            return;
        }
        
        // Pet Shop
        if (type == Material.BONE && slot == 28) {
            player.closeInventory();
            org.bukkit.plugin.Plugin petPlugin = Bukkit.getPluginManager().getPlugin("PetPlugin");
            if (petPlugin != null) {
                try {
                    Object shopGUI = petPlugin.getClass().getMethod("getShopGUI").invoke(petPlugin);
                    if (shopGUI != null) {
                        // PetPlugin uses openShop() method, not openGUI()
                        shopGUI.getClass().getMethod("openShop", Player.class).invoke(shopGUI, player);
                        return;
                    }
                } catch (Exception e) {
                    plugin.getLogger().fine("Failed to open PetPlugin GUI via reflection: " + e.getMessage());
                    // Fallback to command
                }
            }
            // Use /pet command (not /petshop)
            player.performCommand("pet");
            return;
        }
        
        // Perk Shop
        if (type == Material.NETHER_STAR && slot == 30) {
            player.closeInventory();
            org.bukkit.plugin.Plugin perkShop = Bukkit.getPluginManager().getPlugin("PerkShop");
            if (perkShop != null) {
                try {
                    Object shopGUI = perkShop.getClass().getMethod("getShopGUI").invoke(perkShop);
                    if (shopGUI != null) {
                        // PerkShop uses openShop() method
                        shopGUI.getClass().getMethod("openShop", Player.class).invoke(shopGUI, player);
                        return;
                    }
                } catch (Exception e) {
                    plugin.getLogger().fine("Failed to open PerkShop GUI via reflection: " + e.getMessage());
                    // Fallback to command
                }
            }
            player.performCommand("shop");
            return;
        }
        
        // Farm Shop
        if (type == Material.GOLDEN_HOE && slot == 32) {
            player.closeInventory();
            // FarmShop doesn't have a getShopGUI method, just use command
            player.performCommand("farmshop");
            return;
        }
        
        // Skills
        if (type == Material.EXPERIENCE_BOTTLE && slot == 25) {
            player.closeInventory();
            player.performCommand("skills");
            return;
        }
        
        // Sell GUI
        if (type == Material.EMERALD && slot == 34) {
            player.closeInventory();
            org.bukkit.plugin.Plugin sellGUI = Bukkit.getPluginManager().getPlugin("SellGUI");
            if (sellGUI != null) {
                try {
                    Object gui = sellGUI.getClass().getMethod("getGUI").invoke(sellGUI);
                    if (gui != null) {
                        gui.getClass().getMethod("openGUI", Player.class).invoke(gui, player);
                        return;
                    }
                } catch (Exception e) {
                    // Fallback to command
                }
            }
            player.performCommand("sell");
            return;
        }
    }
}

