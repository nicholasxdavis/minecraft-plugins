package com.playpandora.welcome.managers;

import com.playpandora.welcome.Welcome;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

public class BookletManager {
    
    private final Welcome plugin;
    
    public BookletManager(Welcome plugin) {
        this.plugin = plugin;
    }
    
    public void giveBooklet(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        
        if (meta == null) {
            return;
        }
        
        // Set title and author
        String title = plugin.getConfig().getString("welcome.booklet-title", "&6&lPlayPandora Guide");
        String author = plugin.getConfig().getString("welcome.booklet-author", "PlayPandora Staff");
        
        meta.setTitle(org.bukkit.ChatColor.translateAlternateColorCodes('&', title));
        meta.setAuthor(org.bukkit.ChatColor.translateAlternateColorCodes('&', author));
        
        // Create pages
        List<String> pages = new ArrayList<>();
        
        // Page 1: Welcome
        pages.add(org.bukkit.ChatColor.translateAlternateColorCodes('&',
            "&6&lWelcome to PlayPandora!\n\n" +
            "&7This is a custom factions\n" +
            "&7server inspired by old-school\n" +
            "&7factions with never seen\n" +
            "&7before plugins and features!\n\n" +
            "&eUse this guide to learn\n" +
            "&eabout our unique features."));
        
        // Page 2: Pets
        pages.add(org.bukkit.ChatColor.translateAlternateColorCodes('&',
            "&6&lPets System\n\n" +
            "&7Use &6/pet &7to access the\n" +
            "&7pet shop where you can\n" +
            "&7purchase and manage your\n" +
            "&7companions!\n\n" +
            "&7Pets can help you in\n" +
            "&7various ways during your\n" +
            "&7adventures."));
        
        // Page 3: Selling
        pages.add(org.bukkit.ChatColor.translateAlternateColorCodes('&',
            "&6&lSell GUI\n\n" +
            "&7Use &6/sell &7to open the\n" +
            "&7sell GUI where you can\n" +
            "&7quickly sell your items\n" +
            "&7for money!\n\n" +
            "&7This is the fastest way\n" +
            "&7to convert your loot into\n" +
            "&7currency."));
        
        // Page 4: Farm Shop
        pages.add(org.bukkit.ChatColor.translateAlternateColorCodes('&',
            "&6&lFarm Shop\n\n" +
            "&7Use &6/farm &7to access the\n" +
            "&7farm shop where you can\n" +
            "&7purchase seeds, animals,\n" +
            "&7and farming supplies!\n\n" +
            "&7Build your own farm and\n" +
            "&7become self-sufficient."));
        
        // Page 5: Perks
        pages.add(org.bukkit.ChatColor.translateAlternateColorCodes('&',
            "&6&lPerk Shop\n\n" +
            "&7Use &6/perks &7to browse and\n" +
            "&7purchase powerful perks\n" +
            "&7that enhance your gameplay!\n\n" +
            "&7Perks can give you\n" +
            "&7advantages in combat,\n" +
            "&7mining, and more."));
        
        // Page 6: Base System
        pages.add(org.bukkit.ChatColor.translateAlternateColorCodes('&',
            "&6&lBase System\n\n" +
            "&7Use &6/base create &7to\n" +
            "&7create your own base!\n\n" +
            "&7Bases are protected areas\n" +
            "&7where you can store items\n" +
            "&7and build safely.\n\n" +
            "&7Defend your base from\n" +
            "&7raiders!"));
        
        // Page 7: Tips
        pages.add(org.bukkit.ChatColor.translateAlternateColorCodes('&',
            "&6&lTips & Tricks\n\n" +
            "&7You can toggle helpful tips\n" +
            "&7by using &6/tips toggle\n\n" +
            "&7Tips will appear in chat\n" +
            "&7periodically to help you\n" +
            "&7discover new features.\n\n" +
            "&7Good luck and have fun!"));
        
        // Set pages
        for (String page : pages) {
            meta.addPage(page);
        }
        
        book.setItemMeta(meta);
        
        // Give book to player
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(book);
        } else {
            // Drop at player location if inventory is full
            player.getWorld().dropItemNaturally(player.getLocation(), book);
        }
        
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
            "&e&lPandora &7You received the Welcome Guide! Check your inventory."));
    }
}


