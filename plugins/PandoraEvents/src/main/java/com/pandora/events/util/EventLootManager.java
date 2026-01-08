package com.pandora.events.util;

import com.pandora.events.PandoraEventsPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Manages loot generation for event bases
 */
public class EventLootManager {
    
    private static final Random random = new Random();
    
    // Ore materials to place on ground
    private static final Material[] ORE_MATERIALS = {
        Material.DIAMOND_ORE,
        Material.EMERALD_ORE,
        Material.GOLD_ORE,
        Material.IRON_ORE,
        Material.COAL_ORE,
        Material.LAPIS_ORE,
        Material.REDSTONE_ORE
    };
    
    /**
     * Populate loot in the event base
     */
    public static void populateLoot(World world, Location center, int floorY, int baseSize) {
        // Calculate bounds from center - base is 50x50 cube (claim is 100x100)
        // So we want to place loot inside the walls (walls are at the edges)
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();
        int halfSize = baseSize / 2;
        int minX = centerX - halfSize + 5; // Inside the walls (5 blocks from wall)
        int maxX = centerX + halfSize - 5; // Inside the walls
        int minZ = centerZ - halfSize + 5; // Inside the walls
        int maxZ = centerZ + halfSize - 5; // Inside the walls
        
        // Place chests with loot
        placeChestsWithLoot(world, minX, maxX, minZ, maxZ, floorY);
        
        // Place ores on ground
        placeOresOnGround(world, minX, maxX, minZ, maxZ, floorY);
        
        PandoraEventsPlugin.getInstance().getLogger().info("Populated loot in event base");
    }
    
    /**
     * Place chests with loot throughout the base
     */
    private static void placeChestsWithLoot(World world, int minX, int maxX, int minZ, int maxZ, int floorY) {
        int chestCount = (int)Math.round((15 + random.nextInt(10)) * 0.4); // Reduced by 60%
        
        for (int i = 0; i < chestCount; i++) {
            int x = minX + 5 + random.nextInt(maxX - minX - 10);
            int z = minZ + 5 + random.nextInt(maxZ - minZ - 10);
            int y = floorY + 1;
            
            Location chestLoc = new Location(world, x, y, z);
            Block block = chestLoc.getBlock();
            
            // Make sure location is air
            if (block.getType() != Material.AIR) {
                continue;
            }
            
            // Place chest
            block.setType(Material.CHEST, false);
            
            if (block.getState() instanceof Chest) {
                Chest chest = (Chest) block.getState();
                fillChest(chest);
            }
        }
    }
    
    /**
     * Fill a chest with random loot
     */
    private static void fillChest(Chest chest) {
        List<ItemStack> items = new ArrayList<>();
        
        // Spawners: min 7, max 24 (reduced by 60% = 40% of original)
        int spawnerCount = (int)Math.round((7 + random.nextInt(18)) * 0.4);
        for (int i = 0; i < spawnerCount; i++) {
            items.add(new ItemStack(Material.SPAWNER));
        }
        
        // Prot 4 Netherite Armor: min 9, max 28 (reduced by 60% = 40% of original)
        int armorCount = (int)Math.round((9 + random.nextInt(20)) * 0.4);
        for (int i = 0; i < armorCount; i++) {
            Material[] armorTypes = {
                Material.NETHERITE_HELMET,
                Material.NETHERITE_CHESTPLATE,
                Material.NETHERITE_LEGGINGS,
                Material.NETHERITE_BOOTS
            };
            ItemStack armor = new ItemStack(armorTypes[random.nextInt(armorTypes.length)]);
            Enchantment protection = Enchantment.getByName("PROTECTION_ENVIRONMENTAL");
            if (protection != null) {
                armor.addEnchantment(protection, 4);
            }
            items.add(armor);
        }
        
        // Creeper Eggs: min 3, max 8 (reduced by 60% = 40% of original)
        int eggCount = (int)Math.round((3 + random.nextInt(6)) * 0.4);
        for (int i = 0; i < eggCount; i++) {
            items.add(new ItemStack(Material.CREEPER_SPAWN_EGG));
        }
        
        // Enchanted God Apple: min 2, max 9 (reduced by 60% = 40% of original)
        int appleCount = (int)Math.round((2 + random.nextInt(8)) * 0.4);
        for (int i = 0; i < appleCount; i++) {
            ItemStack apple = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE);
            items.add(apple);
        }
        
        // Diamond Blocks: min 100, max 150 (reduced by 60% = 40% of original)
        int diamondBlocks = (int)Math.round((100 + random.nextInt(51)) * 0.4);
        items.add(new ItemStack(Material.DIAMOND_BLOCK, diamondBlocks));
        
        // Gold Blocks: min 100, max 150 (reduced by 60% = 40% of original)
        int goldBlocks = (int)Math.round((100 + random.nextInt(51)) * 0.4);
        items.add(new ItemStack(Material.GOLD_BLOCK, goldBlocks));
        
        // Iron Blocks: min 100, max 150 (reduced by 60% = 40% of original)
        int ironBlocks = (int)Math.round((100 + random.nextInt(51)) * 0.4);
        items.add(new ItemStack(Material.IRON_BLOCK, ironBlocks));
        
        // Emerald Blocks: min 100, max 150 (reduced by 60% = 40% of original)
        int emeraldBlocks = (int)Math.round((100 + random.nextInt(51)) * 0.4);
        items.add(new ItemStack(Material.EMERALD_BLOCK, emeraldBlocks));
        
        // Crate Keys: min 5, max 11 (reduced by 60% = 40% of original)
        int keys = (int)Math.round((5 + random.nextInt(7)) * 0.4);
        // Assuming crate keys are a custom item - using a placeholder
        // You may need to adjust this based on your crate system
        for (int i = 0; i < keys; i++) {
            ItemStack key = new ItemStack(Material.TRIPWIRE_HOOK); // Placeholder
            ItemMeta meta = key.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§6Crate Key");
                key.setItemMeta(meta);
            }
            items.add(key);
        }
        
        // God Set Pieces: min 1, max 3 (reduced by 60% = 40% of original)
        int godSetPieces = (int)Math.round((1 + random.nextInt(3)) * 0.4);
        for (int i = 0; i < godSetPieces; i++) {
            // Assuming god set is custom armor - using netherite with special name
            Material[] godArmor = {
                Material.NETHERITE_HELMET,
                Material.NETHERITE_CHESTPLATE,
                Material.NETHERITE_LEGGINGS,
                Material.NETHERITE_BOOTS
            };
            ItemStack piece = new ItemStack(godArmor[random.nextInt(godArmor.length)]);
            Enchantment protection = Enchantment.getByName("PROTECTION_ENVIRONMENTAL");
            Enchantment mending = Enchantment.getByName("MENDING");
            Enchantment unbreaking = Enchantment.getByName("DURABILITY");
            if (protection != null) {
                piece.addEnchantment(protection, 4);
            }
            if (mending != null) {
                piece.addEnchantment(mending, 1);
            }
            if (unbreaking != null) {
                piece.addEnchantment(unbreaking, 3);
            }
            ItemMeta meta = piece.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§6§lGod Set Piece");
                piece.setItemMeta(meta);
            }
            items.add(piece);
        }
        
        // Tokens: min 2, max 3 (reduced by 60% = 40% of original)
        int tokens = (int)Math.round((2 + random.nextInt(2)) * 0.4);
        for (int i = 0; i < tokens; i++) {
            ItemStack token = new ItemStack(Material.EMERALD); // Placeholder
            ItemMeta meta = token.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§bToken");
                token.setItemMeta(meta);
            }
            items.add(token);
        }
        
        // TNT, Food, Wood (reduced by 60% = 40% of original)
        int tntCount = (int)Math.round((32 + random.nextInt(32)) * 0.4);
        items.add(new ItemStack(Material.TNT, tntCount));
        
        Material[] foodTypes = {
            Material.COOKED_BEEF,
            Material.COOKED_PORKCHOP,
            Material.GOLDEN_CARROT,
            Material.BREAD
        };
        int foodCount = (int)Math.round((16 + random.nextInt(32)) * 0.4); // Reduced by 60%
        items.add(new ItemStack(foodTypes[random.nextInt(foodTypes.length)], foodCount));
        
        Material[] woodTypes = {
            Material.OAK_PLANKS,
            Material.SPRUCE_PLANKS,
            Material.BIRCH_PLANKS
        };
        int woodCount = (int)Math.round((64 + random.nextInt(64)) * 0.4); // Reduced by 60%
        items.add(new ItemStack(woodTypes[random.nextInt(woodTypes.length)], woodCount));
        
        // F Shop Items: min 1, max 3 (reduced by 60% = 40% of original)
        int shopItemCount = (int)Math.round((1 + random.nextInt(3)) * 0.4);
        for (int i = 0; i < shopItemCount; i++) {
            ItemStack shopItem = getRandomShopItem();
            if (shopItem != null) {
                items.add(shopItem);
            }
        }
        
        // Shuffle and add to chest
        Collections.shuffle(items);
        for (int i = 0; i < Math.min(items.size(), 27); i++) {
            chest.getInventory().setItem(i, items.get(i));
        }
    }
    
    /**
     * Get a random shop item from PandoraBases shop
     */
    private static ItemStack getRandomShopItem() {
        try {
            // Access BaseShopManager
            Class<?> shopManagerClass = Class.forName("com.massivecraft.factions.managers.BaseShopManager");
            Method getAllShopItemsMethod = shopManagerClass.getMethod("getAllShopItems");
            @SuppressWarnings("unchecked")
            Map<String, Object> shopItems = (Map<String, Object>) getAllShopItemsMethod.invoke(null);
            
            if (shopItems == null || shopItems.isEmpty()) {
                return null;
            }
            
            // Get random shop item
            List<String> keys = new ArrayList<>(shopItems.keySet());
            String randomKey = keys.get(random.nextInt(keys.size()));
            Object shopItem = shopItems.get(randomKey);
            
            // Create ItemStack from shop item
            Method createItemStackMethod = shopItem.getClass().getMethod("createItemStack");
            return (ItemStack) createItemStackMethod.invoke(shopItem);
            
        } catch (Exception e) {
            PandoraEventsPlugin.getInstance().getLogger().warning("Failed to get shop item: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Place ores on the ground (not too many)
     */
    private static void placeOresOnGround(World world, int minX, int maxX, int minZ, int maxZ, int floorY) {
        int oreCount = (int)Math.round((50 + random.nextInt(50)) * 0.4); // Reduced by 60%
        
        for (int i = 0; i < oreCount; i++) {
            int x = minX + 5 + random.nextInt(maxX - minX - 10);
            int z = minZ + 5 + random.nextInt(maxZ - minZ - 10);
            int y = floorY + 1;
            
            Location oreLoc = new Location(world, x, y, z);
            Block block = oreLoc.getBlock();
            
            // Only place if air
            if (block.getType() == Material.AIR) {
                Material ore = ORE_MATERIALS[random.nextInt(ORE_MATERIALS.length)];
                block.setType(ore, false);
            }
        }
    }
}

