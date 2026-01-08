package com.playpandora.esshats.config;

import com.playpandora.esshats.EssHats;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    
    private final EssHats plugin;
    private FileConfiguration config;
    
    public ConfigManager(EssHats plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
    
    public void reload() {
        loadConfig();
    }
    
    // Message prefix
    public String getPrefix() {
        return translateColors(config.getString("settings.prefix", "&e&lEssHats &8»"));
    }
    
    // GUI Settings
    public String getGUITitle() {
        return translateColors(config.getString("settings.gui.title", "&e&lHats"));
    }
    
    public int getGUISize() {
        return config.getInt("settings.gui.size", 54);
    }
    
    public Material getBorderMaterial() {
        String materialName = config.getString("settings.gui.border-material", "GRAY_STAINED_GLASS_PANE");
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid border material: " + materialName + ". Using GRAY_STAINED_GLASS_PANE");
            return Material.GRAY_STAINED_GLASS_PANE;
        }
    }
    
    public Material getCloseButtonMaterial() {
        String materialName = config.getString("settings.gui.close-button.material", "BARRIER");
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid close button material: " + materialName + ". Using BARRIER");
            return Material.BARRIER;
        }
    }
    
    public String getCloseButtonName() {
        return translateColors(config.getString("settings.gui.close-button.name", "&c✖ Close"));
    }
    
    public String getCloseButtonLore() {
        return translateColors(config.getString("settings.gui.close-button.lore.0", "&7Click to close"));
    }
    
    // Sound Settings
    private Sound parseSound(String soundString) {
        String[] parts = soundString.split(":");
        try {
            return Sound.valueOf(parts[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound: " + parts[0] + ". Using UI_BUTTON_CLICK");
            return Sound.UI_BUTTON_CLICK;
        }
    }
    
    private float[] parseSoundParams(String soundString) {
        String[] parts = soundString.split(":");
        float volume = 0.5f;
        float pitch = 1.0f;
        
        if (parts.length > 1) {
            try {
                volume = Float.parseFloat(parts[1]);
            } catch (NumberFormatException ignored) {}
        }
        if (parts.length > 2) {
            try {
                pitch = Float.parseFloat(parts[2]);
            } catch (NumberFormatException ignored) {}
        }
        
        return new float[]{volume, pitch};
    }
    
    public Sound getSoundGUIOpen() {
        return parseSound(config.getString("settings.sounds.gui-open", "UI_TOAST_IN:0.5:1.0"));
    }
    
    public float[] getSoundGUIOpenParams() {
        return parseSoundParams(config.getString("settings.sounds.gui-open", "UI_TOAST_IN:0.5:1.0"));
    }
    
    public Sound getSoundButtonClick() {
        return parseSound(config.getString("settings.sounds.button-click", "UI_BUTTON_CLICK:0.5:1.0"));
    }
    
    public float[] getSoundButtonClickParams() {
        return parseSoundParams(config.getString("settings.sounds.button-click", "UI_BUTTON_CLICK:0.5:1.0"));
    }
    
    public Sound getSoundHatEquip() {
        return parseSound(config.getString("settings.sounds.hat-equip", "ENTITY_PLAYER_LEVELUP:0.5:1.5"));
    }
    
    public float[] getSoundHatEquipParams() {
        return parseSoundParams(config.getString("settings.sounds.hat-equip", "ENTITY_PLAYER_LEVELUP:0.5:1.5"));
    }
    
    public Sound getSoundPermissionDenied() {
        return parseSound(config.getString("settings.sounds.permission-denied", "ENTITY_VILLAGER_NO:0.5:1.0"));
    }
    
    public float[] getSoundPermissionDeniedParams() {
        return parseSoundParams(config.getString("settings.sounds.permission-denied", "ENTITY_VILLAGER_NO:0.5:1.0"));
    }
    
    public Sound getSoundHatRemove() {
        return parseSound(config.getString("settings.sounds.hat-remove", "UI_BUTTON_CLICK:0.5:1.0"));
    }
    
    public float[] getSoundHatRemoveParams() {
        return parseSoundParams(config.getString("settings.sounds.hat-remove", "UI_BUTTON_CLICK:0.5:1.0"));
    }
    
    // Item Display Settings
    public String getHatNameColor() {
        return translateColors(config.getString("settings.items.hat-name-color", "&6"));
    }
    
    public String getClickToEquipText() {
        return translateColors(config.getString("settings.items.click-to-equip", "&eClick to equip!"));
    }
    
    public String getDescriptionColor() {
        return translateColors(config.getString("settings.items.description-color", "&7"));
    }
    
    // Messages
    public String getMessage(String key) {
        String message = config.getString("messages." + key, "");
        if (message.isEmpty()) {
            plugin.getLogger().warning("Message key not found: " + key);
            return "&cMessage not found: " + key;
        }
        return translateColors(getPrefix() + " " + message);
    }
    
    public String getMessageNoPrefix(String key) {
        String message = config.getString("messages." + key, "");
        if (message.isEmpty()) {
            plugin.getLogger().warning("Message key not found: " + key);
            return "&cMessage not found: " + key;
        }
        return translateColors(message);
    }
    
    public String getMessage(String key, String... replacements) {
        String message = getMessageNoPrefix(key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        return getPrefix() + " " + message;
    }
    
    // Tier Labels
    public String getTierLabel(String tier) {
        String label = config.getString("tiers." + tier.toLowerCase() + ".label", "");
        if (label.isEmpty()) {
            return "&e&l" + tier + "-Tier";
        }
        return translateColors(label);
    }
    
    // Utility method to translate color codes
    private String translateColors(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}

