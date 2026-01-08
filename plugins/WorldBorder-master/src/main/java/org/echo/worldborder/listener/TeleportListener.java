package org.echo.worldborder.listener;

import org.bukkit.WorldBorder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.echo.worldborder.Main;

public class TeleportListener implements Listener {

    private final Main plugin;

    public TeleportListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (plugin.getMyConfig().isEnforce()) {
            WorldBorder border = event.getTo().getWorld().getWorldBorder();
            if (!border.isInside(event.getTo())) {
                event.setTo(plugin.getManager().clampToBorder(event.getTo()));
                event.getPlayer().sendMessage(plugin.getMessages().getOutOfBounds(event.getTo().getWorld().getName()));
            }
        }
    }
}
