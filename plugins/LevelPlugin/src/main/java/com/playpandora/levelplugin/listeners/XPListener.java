package com.playpandora.levelplugin.listeners;

import com.playpandora.levelplugin.LevelPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class XPListener implements Listener {
    
    private final LevelPlugin plugin;
    
    public XPListener(LevelPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getLevelManager().onPlayerJoin(event.getPlayer());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getLevelManager().onPlayerQuit(event.getPlayer());
        // Save player data when they quit
        plugin.getDataManager().saveData();
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled() || !plugin.getConfig().getBoolean("xp-sources.blocks.enabled", true)) {
            return;
        }
        
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();
        String blockName = blockType.name().toLowerCase();
        
        // Check for ore XP
        ConfigurationSection oresSection = plugin.getConfig().getConfigurationSection("xp-sources.blocks.ores");
        double xp = plugin.getConfig().getDouble("xp-sources.blocks.default", 0.1);
        
        if (oresSection != null && oresSection.contains(blockName)) {
            xp = oresSection.getDouble(blockName, xp);
        }
        
        if (xp > 0) {
            plugin.getLevelManager().addXP(player.getUniqueId(), xp, true);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!plugin.getConfig().getBoolean("xp-sources.mobs.enabled", true)) {
            return;
        }
        
        if (event.getEntity().getKiller() == null) {
            return;
        }
        
        Player player = event.getEntity().getKiller();
        EntityType entityType = event.getEntityType();
        String mobName = entityType.name().toLowerCase();
        
        // Check for mob XP
        ConfigurationSection mobsSection = plugin.getConfig().getConfigurationSection("xp-sources.mobs");
        double xp = plugin.getConfig().getDouble("xp-sources.mobs.default", 1.0);
        
        if (mobsSection != null && mobsSection.contains(mobName)) {
            xp = mobsSection.getDouble(mobName, xp);
        }
        
        // Check if LevelledMobs is installed and the entity is levelled
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) event.getEntity();
            int mobLevel = getLevelledMobLevel(livingEntity);
            
            if (mobLevel > 0) {
                // Apply level-based XP multiplier
                double levelMultiplier = plugin.getConfig().getDouble("xp-sources.mobs.level-multiplier", 1.0);
                String multiplierFormula = plugin.getConfig().getString("xp-sources.mobs.level-formula", "");
                
                double baseXP = xp; // Store base XP for debug
                
                if (!multiplierFormula.isEmpty()) {
                    // Use custom formula (e.g., "1 + (level * 0.1)" or "level * 0.5")
                    try {
                        String formula = multiplierFormula.replace("%level%", String.valueOf(mobLevel));
                        // Simple formula evaluation (supports basic math)
                        double multiplier = evaluateSimpleFormula(formula);
                        xp *= multiplier;
                    } catch (Exception e) {
                        plugin.getLogger().warning("Invalid level formula: " + multiplierFormula + ". Using default multiplier.");
                        xp *= (1.0 + (mobLevel * levelMultiplier));
                    }
                } else {
                    // Use simple multiplier: base XP * (1 + level * multiplier)
                    xp *= (1.0 + (mobLevel * levelMultiplier));
                }
                
                if (plugin.getConfig().getBoolean("xp-sources.mobs.debug", false)) {
                    plugin.getLogger().info("Mob kill XP: Base=" + baseXP + ", Level=" + mobLevel + ", Final=" + xp);
                }
            }
        }
        
        if (xp > 0) {
            plugin.getLevelManager().addXP(player.getUniqueId(), xp, true);
            if (plugin.getConfig().getBoolean("xp-sources.mobs.debug", false)) {
                plugin.getLogger().info("Added " + xp + " XP to player " + player.getName() + " for killing " + mobName);
            }
        } else if (plugin.getConfig().getBoolean("xp-sources.mobs.debug", false)) {
            plugin.getLogger().info("No XP given for " + mobName + " (xp=" + xp + ")");
        }
    }
    
    /**
     * Get the level of a mob from LevelledMobs if available
     * @param entity The living entity to check
     * @return The mob's level, or 0 if not levelled or LevelledMobs is not available
     */
    private int getLevelledMobLevel(LivingEntity entity) {
        if (!plugin.getConfig().getBoolean("xp-sources.mobs.use-levelledmobs", true)) {
            return 0;
        }
        
        Plugin levelledMobs = plugin.getServer().getPluginManager().getPlugin("LevelledMobs");
        if (levelledMobs == null || !levelledMobs.isEnabled()) {
            return 0;
        }
        
        try {
            // Method 1: Try using the API Main class (preferred method)
            Class<?> apiClass = Class.forName("io.github.arcaneplugins.levelledmobs.api.Main");
            Object apiInstance = apiClass.getMethod("getInstance").invoke(null);
            
            if (apiInstance != null) {
                // Check if mob is levelled
                Boolean isLevelled = (Boolean) apiClass.getMethod("isLevelled", org.bukkit.entity.LivingEntity.class)
                        .invoke(apiInstance, entity);
                
                if (isLevelled != null && isLevelled) {
                    // Get the mob's level
                    Integer level = (Integer) apiClass.getMethod("getLevelOfMob", org.bukkit.entity.LivingEntity.class)
                            .invoke(apiInstance, entity);
                    // getLevelOfMob returns -1 if not levelled, or the level if levelled
                    if (level != null && level > 0) {
                        if (plugin.getConfig().getBoolean("xp-sources.mobs.debug", false)) {
                            plugin.getLogger().info("LevelledMobs: Found mob level " + level + " via API");
                        }
                        return level;
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            // API class not found, try direct plugin access
        } catch (NullPointerException e) {
            // getInstance() returned null, try direct plugin access
        } catch (Exception e) {
            // API access failed, try direct plugin access
        }
        
        try {
            // Method 2: Access the plugin directly as LevelInterface using reflection
            // The LevelledMobs plugin itself implements LevelInterface
            Class<?> levelInterfaceClass = Class.forName("io.github.arcaneplugins.levelledmobs.LevelInterface");
            
            // Check if the plugin implements LevelInterface
            if (levelInterfaceClass.isInstance(levelledMobs)) {
                // Check if mob is levelled
                Boolean isLevelled = (Boolean) levelInterfaceClass.getMethod("isLevelled", org.bukkit.entity.LivingEntity.class)
                        .invoke(levelledMobs, entity);
                
                if (isLevelled != null && isLevelled) {
                    // Get the mob's level
                    Integer level = (Integer) levelInterfaceClass.getMethod("getLevelOfMob", org.bukkit.entity.LivingEntity.class)
                            .invoke(levelledMobs, entity);
                    // getLevelOfMob returns -1 if not levelled, or the level if levelled
                    if (level != null && level > 0) {
                        if (plugin.getConfig().getBoolean("xp-sources.mobs.debug", false)) {
                            plugin.getLogger().info("LevelledMobs: Found mob level " + level + " via LevelInterface");
                        }
                        return level;
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            // LevelInterface class not found
            plugin.getLogger().warning("LevelledMobs API classes not found. Integration may not work properly.");
        } catch (Exception e) {
            // LevelledMobs integration failed
            plugin.getLogger().warning("Could not access LevelledMobs API: " + e.getMessage());
            if (plugin.getConfig().getBoolean("xp-sources.mobs.debug", false)) {
                e.printStackTrace();
            }
        }
        
        // Method 3: Try accessing via PersistentDataContainer (fallback)
        // LevelledMobs stores level using NamespacedKey(plugin, "level")
        try {
            org.bukkit.NamespacedKey levelKey = new org.bukkit.NamespacedKey(levelledMobs, "level");
            if (entity.getPersistentDataContainer().has(levelKey, org.bukkit.persistence.PersistentDataType.INTEGER)) {
                Integer level = entity.getPersistentDataContainer().get(levelKey, org.bukkit.persistence.PersistentDataType.INTEGER);
                if (level != null && level > 0) {
                    if (plugin.getConfig().getBoolean("xp-sources.mobs.debug", false)) {
                        plugin.getLogger().info("LevelledMobs: Found mob level " + level + " via PDC");
                    }
                    return level;
                }
            }
        } catch (Exception e) {
            // Fallback method failed, ignore
            if (plugin.getConfig().getBoolean("xp-sources.mobs.debug", false)) {
                plugin.getLogger().fine("Could not access LevelledMobs level via PDC: " + e.getMessage());
            }
        }
        
        return 0;
    }
    
    /**
     * Evaluate a simple mathematical formula
     * Supports basic operations: +, -, *, /, parentheses, and %level% variable
     * Note: This is a simplified parser. For complex formulas, consider using a proper expression parser library.
     * @param formula The formula string to evaluate
     * @return The result of the formula
     */
    private double evaluateSimpleFormula(String formula) {
        // Remove whitespace
        formula = formula.replaceAll("\\s+", "");
        
        // Try using JavaScript engine if available (most servers have it)
        try {
            javax.script.ScriptEngineManager manager = new javax.script.ScriptEngineManager();
            javax.script.ScriptEngine engine = manager.getEngineByName("JavaScript");
            if (engine != null) {
                return ((Number) engine.eval(formula)).doubleValue();
            }
        } catch (Exception e) {
            // JavaScript engine not available, fall through to manual parsing
        }
        
        // Fallback: Simple manual parsing for basic formulas
        // This handles simple cases like "1 + level * 0.1" or "level * 0.5"
        try {
            return parseSimpleFormula(formula);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not parse formula: " + formula + ". Error: " + e.getMessage());
            throw new IllegalArgumentException("Could not evaluate formula: " + formula);
        }
    }
    
    /**
     * Parse a simple mathematical formula manually
     * Supports: numbers, +, -, *, /, and basic parentheses
     * @param formula The formula to parse
     * @return The result
     */
    private double parseSimpleFormula(String formula) {
        // Handle parentheses first
        while (formula.contains("(")) {
            int start = formula.lastIndexOf("(");
            int end = formula.indexOf(")", start);
            if (end == -1) {
                throw new IllegalArgumentException("Unmatched parenthesis in formula: " + formula);
            }
            String subFormula = formula.substring(start + 1, end);
            double result = parseSimpleFormula(subFormula);
            formula = formula.substring(0, start) + result + formula.substring(end + 1);
        }
        
        // Handle multiplication and division
        while (formula.contains("*") || formula.contains("/")) {
            int multIndex = formula.indexOf("*");
            int divIndex = formula.indexOf("/");
            int opIndex = -1;
            boolean isMult = true;
            
            if (multIndex != -1 && divIndex != -1) {
                opIndex = Math.min(multIndex, divIndex);
                isMult = multIndex < divIndex;
            } else if (multIndex != -1) {
                opIndex = multIndex;
                isMult = true;
            } else if (divIndex != -1) {
                opIndex = divIndex;
                isMult = false;
            }
            
            if (opIndex == -1) break;
            
            String left = extractNumberLeft(formula, opIndex);
            String right = extractNumberRight(formula, opIndex);
            double leftVal = Double.parseDouble(left);
            double rightVal = Double.parseDouble(right);
            double result = isMult ? leftVal * rightVal : leftVal / rightVal;
            formula = formula.substring(0, opIndex - left.length()) + result + formula.substring(opIndex + right.length() + 1);
        }
        
        // Handle addition and subtraction
        while (formula.contains("+") || (formula.contains("-") && !formula.startsWith("-"))) {
            int addIndex = formula.indexOf("+");
            int subIndex = formula.indexOf("-", 1); // Skip leading minus
            int opIndex = -1;
            boolean isAdd = true;
            
            if (addIndex != -1 && subIndex != -1) {
                opIndex = Math.min(addIndex, subIndex);
                isAdd = addIndex < subIndex;
            } else if (addIndex != -1) {
                opIndex = addIndex;
                isAdd = true;
            } else if (subIndex != -1) {
                opIndex = subIndex;
                isAdd = false;
            }
            
            if (opIndex == -1) break;
            
            String left = extractNumberLeft(formula, opIndex);
            String right = extractNumberRight(formula, opIndex);
            double leftVal = Double.parseDouble(left);
            double rightVal = Double.parseDouble(right);
            double result = isAdd ? leftVal + rightVal : leftVal - rightVal;
            formula = formula.substring(0, opIndex - left.length()) + result + formula.substring(opIndex + right.length() + 1);
        }
        
        return Double.parseDouble(formula);
    }
    
    private String extractNumberLeft(String formula, int index) {
        int start = index - 1;
        while (start >= 0 && (Character.isDigit(formula.charAt(start)) || formula.charAt(start) == '.' || 
                              formula.charAt(start) == '-' || formula.charAt(start) == '+')) {
            start--;
        }
        return formula.substring(start + 1, index);
    }
    
    private String extractNumberRight(String formula, int index) {
        int end = index + 1;
        while (end < formula.length() && (Character.isDigit(formula.charAt(end)) || formula.charAt(end) == '.' || 
                                         formula.charAt(end) == '-' || formula.charAt(end) == '+')) {
            end++;
        }
        return formula.substring(index + 1, end);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.isCancelled() || event.getState() != org.bukkit.event.player.PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        
        if (!plugin.getConfig().getBoolean("xp-sources.fishing.enabled", true)) {
            return;
        }
        
        Player player = event.getPlayer();
        Item caught = (Item) event.getCaught();
        if (caught == null) {
            return;
        }
        
        Material itemType = caught.getItemStack().getType();
        String itemName = itemType.name().toLowerCase();
        
        // Determine if it's treasure, junk, or a fish
        double xp = plugin.getConfig().getDouble("xp-sources.fishing.default", 1.0);
        
        ConfigurationSection fishingSection = plugin.getConfig().getConfigurationSection("xp-sources.fishing");
        if (fishingSection != null) {
            if (itemName.contains("book") || itemName.contains("bow") || itemName.contains("rod") || 
                itemName.contains("name_tag") || itemName.contains("saddle")) {
                xp = fishingSection.getDouble("treasure", 10.0);
            } else if (itemName.contains("leather") || itemName.contains("stick") || 
                      itemName.contains("string") || itemName.contains("bowl")) {
                xp = fishingSection.getDouble("junk", 0.5);
            } else if (fishingSection.contains(itemName)) {
                xp = fishingSection.getDouble(itemName, xp);
            }
        }
        
        if (xp > 0) {
            plugin.getLevelManager().addXP(player.getUniqueId(), xp, true);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCraftItem(CraftItemEvent event) {
        if (event.isCancelled() || !(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        if (!plugin.getConfig().getBoolean("xp-sources.crafting.enabled", true)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        double xp = plugin.getConfig().getDouble("xp-sources.crafting.xp-per-craft", 0.5);
        
        if (xp > 0) {
            plugin.getLevelManager().addXP(player.getUniqueId(), xp, true);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        if (!plugin.getConfig().getBoolean("xp-sources.smelting.enabled", true)) {
            return;
        }
        
        Player player = event.getPlayer();
        double xp = plugin.getConfig().getDouble("xp-sources.smelting.xp-per-smelt", 0.3);
        
        if (xp > 0) {
            plugin.getLevelManager().addXP(player.getUniqueId(), xp, true);
        }
    }
}

