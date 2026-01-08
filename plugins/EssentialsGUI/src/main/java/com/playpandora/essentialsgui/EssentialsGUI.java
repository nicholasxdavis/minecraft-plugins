package com.playpandora.essentialsgui;

import com.playpandora.essentialsgui.commands.HomeCommand;
import com.playpandora.essentialsgui.commands.KitCommand;
import com.playpandora.essentialsgui.commands.RulesCommand;
import com.playpandora.essentialsgui.commands.WarpCommand;
import com.playpandora.essentialsgui.gui.HomeGUI;
import com.playpandora.essentialsgui.gui.KitGUI;
import com.playpandora.essentialsgui.gui.RulesGUI;
import com.playpandora.essentialsgui.gui.WarpGUI;
import com.playpandora.essentialsgui.listeners.GUIListener;
import org.bukkit.plugin.java.JavaPlugin;

public class EssentialsGUI extends JavaPlugin {
    
    private static EssentialsGUI instance;
    private WarpGUI warpGUI;
    private HomeGUI homeGUI;
    private KitGUI kitGUI;
    private RulesGUI rulesGUI;
    private EssentialsHook essentialsHook;
    
    @Override
    public void onEnable() {
        instance = this;
        
        try {
            // Save default config
            saveDefaultConfig();
            
            // Initialize Essentials hook
            essentialsHook = new EssentialsHook(this);
            
            // Wait a bit for Essentials to fully load if it's not available yet
            if (!essentialsHook.isEssentialsAvailable()) {
                getLogger().warning("Essentials not found immediately. Will retry when GUI is opened...");
                // Don't disable - allow retry when GUI is opened
            }
            
            // Initialize GUIs
            warpGUI = new WarpGUI(this);
            homeGUI = new HomeGUI(this);
            kitGUI = new KitGUI(this);
            rulesGUI = new RulesGUI(this);
            
            // Register commands with null checks
            if (getCommand("warp") != null) {
                getCommand("warp").setExecutor(new WarpCommand(this));
            } else {
                getLogger().warning("Command 'warp' not found in plugin.yml!");
            }
            if (getCommand("home") != null) {
                getCommand("home").setExecutor(new HomeCommand(this));
            } else {
                getLogger().warning("Command 'home' not found in plugin.yml!");
            }
            if (getCommand("kit") != null) {
                getCommand("kit").setExecutor(new KitCommand(this));
            } else {
                getLogger().warning("Command 'kit' not found in plugin.yml!");
            }
            if (getCommand("kits") != null) {
                getCommand("kits").setExecutor(new KitCommand(this));
            } else {
                getLogger().warning("Command 'kits' not found in plugin.yml!");
            }
            if (getCommand("rules") != null) {
                getCommand("rules").setExecutor(new RulesCommand(this));
            } else {
                getLogger().warning("Command 'rules' not found in plugin.yml!");
            }
            
            // Also register dedicated GUI commands as backup
            if (getCommand("warpgui") != null) {
                getCommand("warpgui").setExecutor((sender, command, label, args) -> {
                    try {
                        if (!(sender instanceof org.bukkit.entity.Player player)) {
                            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                                "&e&lPandora &8» &7This command can only be used by players!"));
                            return true;
                        }
                        if (warpGUI != null) {
                            warpGUI.openGUI(player);
                        }
                        return true;
                    } catch (Exception e) {
                        getLogger().warning("Error executing warpgui command: " + e.getMessage());
                        return true;
                    }
                });
            }
            if (getCommand("homegui") != null) {
                getCommand("homegui").setExecutor((sender, command, label, args) -> {
                    try {
                        if (!(sender instanceof org.bukkit.entity.Player player)) {
                            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                                "&e&lPandora &8» &7This command can only be used by players!"));
                            return true;
                        }
                        if (homeGUI != null) {
                            homeGUI.openGUI(player);
                        }
                        return true;
                    } catch (Exception e) {
                        getLogger().warning("Error executing homegui command: " + e.getMessage());
                        return true;
                    }
                });
            }
            if (getCommand("kitgui") != null) {
                getCommand("kitgui").setExecutor((sender, command, label, args) -> {
                    try {
                        if (!(sender instanceof org.bukkit.entity.Player player)) {
                            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                                "&e&lPandora &8» &7This command can only be used by players!"));
                            return true;
                        }
                        if (kitGUI != null) {
                            kitGUI.openGUI(player);
                        }
                        return true;
                    } catch (Exception e) {
                        getLogger().warning("Error executing kitgui command: " + e.getMessage());
                        return true;
                    }
                });
            }
            if (getCommand("rulesgui") != null) {
                getCommand("rulesgui").setExecutor((sender, command, label, args) -> {
                    try {
                        if (!(sender instanceof org.bukkit.entity.Player player)) {
                            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                                "&e&lPandora &8» &7This command can only be used by players!"));
                            return true;
                        }
                        if (rulesGUI != null) {
                            rulesGUI.openGUI(player);
                        }
                        return true;
                    } catch (Exception e) {
                        getLogger().warning("Error executing rulesgui command: " + e.getMessage());
                        return true;
                    }
                });
            }
            
            // Register listeners
            getServer().getPluginManager().registerEvents(new GUIListener(this), this);
            
            getLogger().info("EssentialsGUI v" + getDescription().getVersion() + " has been enabled successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable EssentialsGUI! " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            // Cancel all tasks
            getServer().getScheduler().cancelTasks(this);
            
            getLogger().info("EssentialsGUI has been disabled!");
        } catch (Exception e) {
            getLogger().severe("Error during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static EssentialsGUI getInstance() {
        return instance;
    }
    
    public WarpGUI getWarpGUI() {
        return warpGUI;
    }
    
    public HomeGUI getHomeGUI() {
        return homeGUI;
    }
    
    public KitGUI getKitGUI() {
        return kitGUI;
    }
    
    public RulesGUI getRulesGUI() {
        return rulesGUI;
    }
    
    public EssentialsHook getEssentialsHook() {
        return essentialsHook;
    }
}

