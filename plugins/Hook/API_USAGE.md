# Hook API Usage Guide

The Hook plugin provides a central notification system that all plugins can use to display titlebar notifications.

## Getting the API

```java
import com.playpandora.hook.Hook;
import com.playpandora.hook.api.HookAPI;

// Get the Hook plugin instance
Hook hookPlugin = (Hook) Bukkit.getPluginManager().getPlugin("Hook");
if (hookPlugin == null) {
    // Hook plugin is not installed
    return;
}

// Get the API
HookAPI hookAPI = hookPlugin.getAPI();
```

## Available Methods

### Basic Title Notification
```java
hookAPI.sendTitle(player, "Title Text", "Subtitle Text");
```

### Welcome Messages
```java
// For first join
hookAPI.sendWelcome(player, true);  // "Welcome!"

// For returning players
hookAPI.sendWelcome(player, false); // "Welcome Back!"
```

### Respawn Notification
```java
hookAPI.sendRespawn(player);
```

### Level Up Notification
```java
hookAPI.sendLevelUp(player, newLevel);
```

### Perk Purchase Notification
```java
hookAPI.sendPerkPurchase(player, "Perk Name");
```

### Sell Notification
```java
hookAPI.sendSell(player, 1500.50, 25); // amount, itemsSold
```

### Weather Alert
```java
hookAPI.sendWeatherAlert(player, "Heavy Rain");
```

### Season Change (Broadcast)
```java
hookAPI.sendSeasonChange("Spring");
```

### Base Event
```java
hookAPI.sendBaseEvent(player, "Base Raid Incoming!");
```

### Custom Notification
```java
hookAPI.sendCustom(player, "Custom Title", "Custom Subtitle", 
                  500, 3000, 1000); // fadeIn, stay, fadeOut (milliseconds)
```

### Broadcast to All Players
```java
hookAPI.broadcastTitle("Server Announcement", "Welcome everyone!");
```

## Example Integration

Here's an example of how to integrate Hook into your plugin:

```java
public class YourPlugin extends JavaPlugin {
    
    private HookAPI hookAPI;
    
    @Override
    public void onEnable() {
        // Get Hook API
        Plugin hookPlugin = getServer().getPluginManager().getPlugin("Hook");
        if (hookPlugin != null && hookPlugin.isEnabled()) {
            hookAPI = ((Hook) hookPlugin).getAPI();
            getLogger().info("Hook integration enabled!");
        } else {
            getLogger().warning("Hook plugin not found! Notifications disabled.");
        }
        
        // ... rest of your plugin initialization
    }
    
    private void onSomethingHappens(Player player) {
        // Send notification
        if (hookAPI != null && hookAPI.isEnabled()) {
            hookAPI.sendCustom(player, "Something Happened!", 
                             "This is a custom notification", 500, 3000, 1000);
        }
    }
}
```

## Color Scheme

The Hook plugin uses a consistent color scheme:
- **Orange/Gold** (#FFD700) - Main titles
- **Gray** (#808080) - Subtitles
- **Yellow** (#FFFF00) - Accent colors (when needed)

All colors are automatically applied, so you just need to provide the text.








