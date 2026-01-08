package com.playpandora.pandoraworldlock;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorldLockManager {
    
    private final PandoraWorldLock plugin;
    private final Map<String, LockInfo> locks = new ConcurrentHashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final Pattern durationPattern = Pattern.compile("(\\d+)([dhms])", Pattern.CASE_INSENSITIVE);
    
    private File locksFile;
    private FileConfiguration locksConfig;
    
    public WorldLockManager(PandoraWorldLock plugin) {
        this.plugin = plugin;
        setupStorage();
        loadLocks();
        seedDefaultsFromConfig();
        saveLocks(); // Persist any defaults we added
    }
    
    private void setupStorage() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        locksFile = new File(plugin.getDataFolder(), "locks.yml");
        if (!locksFile.exists()) {
            try {
                locksFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create locks.yml: " + e.getMessage());
            }
        }
        locksConfig = YamlConfiguration.loadConfiguration(locksFile);
    }
    
    public void loadLocks() {
        // Reload config file in case it was modified externally
        locksConfig = YamlConfiguration.loadConfiguration(locksFile);
        
        locks.clear();
        if (locksConfig.getConfigurationSection("worlds") == null) {
            return;
        }
        
        for (String worldName : locksConfig.getConfigurationSection("worlds").getKeys(false)) {
            boolean locked = locksConfig.getBoolean("worlds." + worldName + ".locked", false);
            String untilRaw = locksConfig.getString("worlds." + worldName + ".until", "").trim();
            LocalDateTime until = parseDateTime(untilRaw).orElse(null);
            locks.put(worldName.toLowerCase(), new LockInfo(locked, until));
        }
    }
    
    public void saveLocks() {
        locksConfig.set("worlds", null);
        for (Map.Entry<String, LockInfo> entry : locks.entrySet()) {
            String path = "worlds." + entry.getKey();
            locksConfig.set(path + ".locked", entry.getValue().locked);
            locksConfig.set(path + ".until", entry.getValue().until == null ? null : formatter.format(entry.getValue().until));
        }
        try {
            locksConfig.save(locksFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save locks.yml: " + e.getMessage());
        }
    }
    
    /**
     * Seed Nether/End defaults from config so existing behavior is preserved.
     */
    private void seedDefaultsFromConfig() {
        for (World world : plugin.getServer().getWorlds()) {
            if (locks.containsKey(world.getName().toLowerCase())) continue;
            
            if (world.getEnvironment() == World.Environment.NETHER) {
                LocalDateTime until = buildDateFromConfig("nether");
                if (until != null) {
                    locks.put(world.getName().toLowerCase(), new LockInfo(true, until));
                }
            } else if (world.getEnvironment() == World.Environment.THE_END) {
                LocalDateTime until = buildDateFromConfig("end");
                if (until != null) {
                    locks.put(world.getName().toLowerCase(), new LockInfo(true, until));
                }
            }
        }
    }
    
    private LocalDateTime buildDateFromConfig(String prefix) {
        int year = plugin.getConfig().getInt(prefix + "-unlock-year", 0);
        int month = plugin.getConfig().getInt(prefix + "-unlock-month", 0);
        int day = plugin.getConfig().getInt(prefix + "-unlock-day", 0);
        int hour = plugin.getConfig().getInt(prefix + "-unlock-hour", 0);
        int minute = plugin.getConfig().getInt(prefix + "-unlock-minute", 0);
        if (year == 0 || month == 0 || day == 0) return null;
        return LocalDateTime.of(year, month, day, hour, minute);
    }
    
    public boolean isWorldLocked(World world) {
        if (world == null) return false;
        LockInfo info = locks.get(world.getName().toLowerCase());
        if (info == null || !info.locked) return false;
        if (info.until == null) return true; // locked forever
        return LocalDateTime.now().isBefore(info.until);
    }
    
    public Optional<LockInfo> getLockInfo(String worldName) {
        if (worldName == null) return Optional.empty();
        return Optional.ofNullable(locks.get(worldName.toLowerCase()));
    }
    
    public LockResult lockWorld(String worldName, LocalDateTime until) {
        if (worldName == null || worldName.isEmpty()) {
            return LockResult.error("World name required.");
        }
        locks.put(worldName.toLowerCase(), new LockInfo(true, until));
        saveLocks();
        return LockResult.success(until == null ? "Locked " + worldName + " indefinitely." :
                "Locked " + worldName + " until " + formatUnlockDate(until) + ".");
    }
    
    public LockResult unlockWorld(String worldName) {
        if (worldName == null || worldName.isEmpty()) {
            return LockResult.error("World name required.");
        }
        LockInfo existing = locks.remove(worldName.toLowerCase());
        saveLocks();
        if (existing == null || !existing.locked) {
            return LockResult.success("World " + worldName + " was already unlocked.");
        }
        return LockResult.success("Unlocked world " + worldName + ".");
    }
    
    public String getUnlockMessage(World world) {
        if (world == null) return "unknown";
        LockInfo info = locks.get(world.getName().toLowerCase());
        if (info == null || !info.locked) return "unlocked";
        if (info.until == null) return "indefinitely";
        return formatUnlockDatePretty(info.until);
    }
    
    public String getColoredLockedMessage(World world) {
        String dimension = world == null ? "This world" : getWorldDisplayName(world);
        String unlock = getUnlockMessage(world);
        return ChatColor.translateAlternateColorCodes('&',
                "&e&lPandora &8» &7" + dimension + " is currently locked! " +
                        (unlock.equals("indefinitely") ? "&7Unlock date: &cIndefinite" :
                                "&7It will unlock on &6" + unlock + "&7."));
    }
    
    private String getWorldDisplayName(World world) {
        if (world == null) return "This world";
        World.Environment env = world.getEnvironment();
        switch (env) {
            case NETHER:
                return "The Nether";
            case THE_END:
                return "The End";
            default:
                return world.getName();
        }
    }
    
    public void teleportToSpawn(Player player) {
        World spawnWorld = plugin.getServer().getWorlds().stream()
                .filter(w -> w.getEnvironment() == World.Environment.NORMAL)
                .findFirst()
                .orElse(plugin.getServer().getWorlds().isEmpty() ? null : plugin.getServer().getWorlds().get(0));
        
        if (spawnWorld != null) {
            Location spawnLocation = spawnWorld.getSpawnLocation();
            spawnLocation = spawnWorld.getHighestBlockAt(spawnLocation).getLocation().add(0, 1, 0);
            player.teleport(spawnLocation);
        } else {
            plugin.getLogger().severe("Could not find a valid spawn world for player: " + player.getName());
        }
    }
    
    public Optional<LocalDateTime> parseDateTime(String raw) {
        if (raw == null || raw.isEmpty()) return Optional.empty();
        try {
            return Optional.of(LocalDateTime.parse(raw, formatter));
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }
    
    public Optional<LocalDateTime> parseDuration(String raw) {
        if (raw == null || raw.isEmpty()) return Optional.empty();
        Matcher matcher = durationPattern.matcher(raw);
        long seconds = 0;
        boolean matched = false;
        while (matcher.find()) {
            matched = true;
            long value = Long.parseLong(matcher.group(1));
            switch (matcher.group(2).toLowerCase()) {
                case "d": seconds += Duration.ofDays(value).getSeconds(); break;
                case "h": seconds += Duration.ofHours(value).getSeconds(); break;
                case "m": seconds += Duration.ofMinutes(value).getSeconds(); break;
                case "s": seconds += value; break;
                default: break;
            }
        }
        if (!matched) return Optional.empty();
        return Optional.of(LocalDateTime.now().plusSeconds(seconds));
    }
    
    public String formatUnlockDate(LocalDateTime date) {
        return formatter.format(date);
    }
    
    public String formatUnlockDatePretty(LocalDateTime date) {
        // Format: "Friday 5pm Feb 20th" or "Friday 5pm April 17th"
        String[] months = {"", "January", "February", "March", "April", "May", "June", 
                          "July", "August", "September", "October", "November", "December"};
        String[] monthAbbrev = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        
        String dayOfWeek = getDayOfWeek(date.getDayOfWeek().getValue());
        // Use abbreviated month name (Feb, Apr) instead of full name
        String month = monthAbbrev[date.getMonthValue()];
        int day = date.getDayOfMonth();
        String daySuffix = getDaySuffix(day);
        int hour = date.getHour();
        String amPm = hour >= 12 ? "pm" : "am";
        int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
        
        return dayOfWeek + " " + displayHour + amPm + " " + month + " " + day + daySuffix;
    }
    
    private String getDayOfWeek(int dayOfWeek) {
        String[] days = {"", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        return days[dayOfWeek];
    }
    
    private String getDaySuffix(int day) {
        if (day >= 11 && day <= 13) {
            return "th";
        }
        switch (day % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }
    
    public record LockInfo(boolean locked, LocalDateTime until) {}
    
    public record LockResult(boolean success, String message) {
        public static LockResult success(String message) { return new LockResult(true, message); }
        public static LockResult error(String message) { return new LockResult(false, message); }
    }
}

