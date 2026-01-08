package com.massivecraft.factions.integration;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.event.FactionCreateEvent;
import com.massivecraft.factions.event.FactionDisbandEvent;
import com.massivecraft.factions.event.FPlayerJoinEvent;
import com.massivecraft.factions.event.FPlayerLeaveEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Integration with Hook plugin for base notifications
 */
public class HookIntegration implements Listener {
    
    private final FactionsPlugin plugin;
    private Object hookAPI;
    private boolean hookAvailable;
    
    public HookIntegration(FactionsPlugin plugin) {
        this.plugin = plugin;
        setupHook();
    }
    
    private void setupHook() {
        Plugin hookPlugin = Bukkit.getPluginManager().getPlugin("Hook");
        if (hookPlugin == null) {
            hookAvailable = false;
            plugin.getLogger().info("Hook plugin not found! Base notifications disabled.");
            return;
        }
        
        try {
            // Get Hook instance using getInstance() static method
            Method getInstanceMethod = hookPlugin.getClass().getMethod("getInstance");
            Object hookInstance = getInstanceMethod.invoke(null);
            
            if (hookInstance != null) {
                // Get API using getAPI() method
                Method getAPIMethod = hookInstance.getClass().getMethod("getAPI");
                hookAPI = getAPIMethod.invoke(hookInstance);
                
                if (hookAPI != null) {
                    hookAvailable = true;
                    plugin.getLogger().info("Successfully connected to Hook plugin! Base notifications enabled.");
                } else {
                    hookAvailable = false;
                    plugin.getLogger().warning("Hook API is null! Base notifications disabled.");
                }
            } else {
                hookAvailable = false;
                plugin.getLogger().warning("Hook instance is null! Base notifications disabled.");
            }
        } catch (Exception e) {
            hookAvailable = false;
            plugin.getLogger().warning("Error setting up Hook integration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean isHookAvailable() {
        return hookAvailable && hookAPI != null;
    }
    
    private void sendTitle(Player player, String title, String subtitle) {
        if (!isHookAvailable()) {
            return;
        }
        
        try {
            // Use proper color scheme: &e for special, &6 for values, &7 for normal
            String formattedTitle = com.massivecraft.factions.util.PandoraMessage.highlight(title);
            String formattedSubtitle = subtitle != null ? com.massivecraft.factions.util.PandoraMessage.text(subtitle) : "";
            
            // Use sendTitle method from HookAPI
            Method sendTitleMethod = hookAPI.getClass().getMethod("sendTitle", Player.class, String.class, String.class);
            sendTitleMethod.invoke(hookAPI, player, formattedTitle, formattedSubtitle);
        } catch (Exception e) {
            plugin.getLogger().warning("Error sending title via Hook: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Send base event notification via Hook
     */
    public void sendBaseEvent(Player player, String eventType) {
        if (!isHookAvailable()) {
            return;
        }
        
        try {
            Method sendBaseEventMethod = hookAPI.getClass().getMethod("sendBaseEvent", Player.class, String.class);
            sendBaseEventMethod.invoke(hookAPI, player, eventType);
        } catch (Exception e) {
            plugin.getLogger().warning("Error sending base event via Hook: " + e.getMessage());
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFactionCreate(FactionCreateEvent event) {
        if (!isHookAvailable()) {
            return;
        }
        
        Player player = event.getFPlayer().getPlayer();
        if (player != null && player.isOnline()) {
            // Send title notification
            sendTitle(player, "Base Created!", "Welcome to " + event.getFactionTag() + "!");
            
            // Also send base event notification
            sendBaseEvent(player, "base_created");
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFactionDisband(FactionDisbandEvent event) {
        Faction faction = event.getFaction();
        if (faction == null) {
            return;
        }
        
        // Clean up tripwire alarm system
        if (plugin.getTripwireAlarmManager() != null) {
            plugin.getTripwireAlarmManager().removeAlarmSystem(faction);
        }
        
        if (!isHookAvailable()) {
            return;
        }
        
        String factionTag = faction.getTag();
        String reason = getDisbandReason(event.getReason());
        
        // Notify all online members
        for (FPlayer member : faction.getFPlayers()) {
            if (member.isOnline() && member.getPlayer() != null) {
                sendTitle(member.getPlayer(), "Base Disbanded!", 
                    factionTag + " has been " + reason);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(FPlayerJoinEvent event) {
        if (!isHookAvailable()) {
            return;
        }
        
        // Only notify if it's not a CREATE event (we handle that separately)
        if (event.getReason() == FPlayerJoinEvent.PlayerJoinReason.CREATE) {
            return; // Already handled in onFactionCreate
        }
        
        Faction faction = event.getFaction();
        FPlayer fPlayer = event.getfPlayer();
        
        if (faction == null || fPlayer == null) {
            return;
        }
        
        Player player = fPlayer.getPlayer();
        if (player != null && player.isOnline()) {
            sendTitle(player, "Joined Base!", "Welcome to " + faction.getTag() + "!");
        }
        
        // Notify all other members
        String playerName = fPlayer.getName();
        for (FPlayer member : faction.getFPlayers()) {
            if (member.isOnline() && member.getPlayer() != null && !member.equals(fPlayer)) {
                sendTitle(member.getPlayer(), "New Member!", playerName + " joined " + faction.getTag());
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLeave(FPlayerLeaveEvent event) {
        if (!isHookAvailable()) {
            return;
        }
        
        Faction faction = event.getFaction();
        FPlayer fPlayer = event.getfPlayer();
        
        if (faction == null || fPlayer == null) {
            return;
        }
        
        String playerName = fPlayer.getName();
        String reason = getLeaveReason(event.getReason());
        
        // Notify all remaining members
        for (FPlayer member : faction.getFPlayers()) {
            if (member.isOnline() && member.getPlayer() != null) {
                sendTitle(member.getPlayer(), "Member Left!", 
                    playerName + " " + reason + " " + faction.getTag());
            }
        }
        
        // Notify the leaving player if they're online
        Player player = fPlayer.getPlayer();
        if (player != null && player.isOnline()) {
            sendTitle(player, "Left Base!", "You left " + faction.getTag());
        }
    }
    
    private String getDisbandReason(FactionDisbandEvent.PlayerDisbandReason reason) {
        switch (reason) {
            case COMMAND:
                return "disbanded by command";
            case PLUGIN:
                return "disbanded";
            case INACTIVITY:
                return "disbanded due to inactivity";
            case LEAVE:
                return "disbanded (no members left)";
            default:
                return "disbanded";
        }
    }
    
    private String getLeaveReason(FPlayerLeaveEvent.PlayerLeaveReason reason) {
        switch (reason) {
            case KICKED:
                return "was kicked from";
            case BANNED:
                return "was banned from";
            case LEAVE:
                return "left";
            case JOINOTHER:
                return "left to join another base";
            case DISBAND:
                return "left (base disbanded)";
            case RESET:
                return "left (reset)";
            default:
                return "left";
        }
    }
}

