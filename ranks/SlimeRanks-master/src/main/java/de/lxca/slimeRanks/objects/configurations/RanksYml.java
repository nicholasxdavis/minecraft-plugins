package de.lxca.slimeRanks.objects.configurations;

import de.lxca.slimeRanks.Main;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;

public class RanksYml extends Yml {

    private static final String filePath = "plugins/SlimeRanks/";
    private static final String fileName = "ranks.yml";

    public RanksYml() {
        super(filePath, fileName);
        // Only set default keys if the file is completely empty (no ranks defined)
        // Check if Ranks section exists - if it does, don't overwrite with defaults
        YamlConfiguration config = getYmlConfig();
        if (config == null || config.getKeys(true).isEmpty() || 
            !config.contains("Ranks") || 
            config.getConfigurationSection("Ranks") == null ||
            config.getConfigurationSection("Ranks").getKeys(false).isEmpty()) {
            // Try to copy from resources first
            if (copyFromResources()) {
                // Reload config after copying by getting it again
                config = getYmlConfig();
                if (config != null && config.contains("Ranks") && 
                    config.getConfigurationSection("Ranks") != null &&
                    !config.getConfigurationSection("Ranks").getKeys(false).isEmpty()) {
                    // Successfully copied from resources, don't set defaults
                    return;
                }
            }
            // If copying failed or file still empty, set defaults
            setDefaultYmlKeys();
        }
    }
    
    /**
     * Copy ranks.yml from plugin resources if it exists
     */
    private boolean copyFromResources() {
        try {
            InputStream resourceStream = Main.getInstance().getResource("ranks.yml");
            if (resourceStream == null) {
                return false;
            }
            
            // Load the resource config
            YamlConfiguration resourceConfig = YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(resourceStream, java.nio.charset.StandardCharsets.UTF_8));
            
            // Check if resource has ranks
            if (resourceConfig.contains("Ranks") && 
                resourceConfig.getConfigurationSection("Ranks") != null &&
                !resourceConfig.getConfigurationSection("Ranks").getKeys(false).isEmpty()) {
                
                // Copy all keys from resource to actual config
                YamlConfiguration actualConfig = getYmlConfig();
                for (String key : resourceConfig.getKeys(true)) {
                    if (!actualConfig.contains(key)) {
                        actualConfig.set(key, resourceConfig.get(key));
                    }
                }
                saveYmlConfig();
                Main.getLogger(this.getClass()).info("Successfully loaded ranks.yml from plugin resources!");
                return true;
            }
            resourceStream.close();
        } catch (Exception e) {
            Main.getLogger(this.getClass()).warn("Could not copy ranks.yml from resources: " + e.getMessage());
        }
        return false;
    }

    @Override
    protected void setDefaultYmlKeys() {
        createConfigKey("ConfigVersion", 1);
        createConfigKey("Ranks.admin.Tab.Active", true);
        createConfigKey("Ranks.admin.Tab.Format", "<color:#e63946>Admin</color> <dark_gray>|</dark_gray> <gray>{player}</gray>");
        createConfigKey("Ranks.admin.Tab.Priority", 2);
        createConfigKey("Ranks.admin.Chat.Active", true);
        createConfigKey("Ranks.admin.Chat.Format", "<color:#e63946>Admin</color> <dark_gray>|</dark_gray> <gray>{player}</gray> <dark_gray>»</dark_gray> <color:#ededed>{message}</color>");
        createConfigKey("Ranks.admin.Chat.ColoredMessages", true);
        createConfigKey("Ranks.admin.NameTag.Active", true);
        createConfigKey("Ranks.admin.NameTag.Format", "<color:#e63946>Admin</color> <dark_gray>|</dark_gray> <gray>{player}</gray>");
        createConfigKey("Ranks.admin.NameTag.HideOnSneak", false);
        createConfigKey("Ranks.admin.RankPriority", 1);
        createConfigKey("Ranks.admin.Permission", "slimeranks.rank.admin");
        createConfigKey("Ranks.default.Tab.Active", true);
        createConfigKey("Ranks.default.Tab.Format", "<color:#b0b0b0>Player</color> <dark_gray>|</dark_gray> <gray>{player}</gray>");
        createConfigKey("Ranks.default.Tab.Priority", 1);
        createConfigKey("Ranks.default.Chat.Active", true);
        createConfigKey("Ranks.default.Chat.Format", "<color:#b0b0b0>Player</color> <dark_gray>|</dark_gray> <gray>{player}</gray> <dark_gray>»</dark_gray> <color:#ededed>{message}</color>");
        createConfigKey("Ranks.default.Chat.ColoredMessages", false);
        createConfigKey("Ranks.default.NameTag.Active", true);
        createConfigKey("Ranks.default.NameTag.Format", "<color:#b0b0b0>Player</color> <dark_gray>|</dark_gray> <gray>{player}</gray>");
        createConfigKey("Ranks.default.NameTag.HideOnSneak", false);
        createConfigKey("Ranks.default.RankPriority", 0);
        saveYmlConfig();
    }
}
