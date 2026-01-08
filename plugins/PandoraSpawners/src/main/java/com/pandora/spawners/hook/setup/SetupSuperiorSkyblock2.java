package com.pandora.spawners.hook.setup;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.bgsoftware.superiorskyblock.api.events.IslandKickEvent;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;

import com.pandora.spawners.PandoraSpawners;
import com.pandora.spawners.api.spawner.ISpawner;
import com.pandora.spawners.configuration.Settings;
import com.pandora.spawners.configuration.location.LocationRegistry;
import com.pandora.spawners.items.ItemMatcher;
import com.pandora.spawners.utility.reflect.Reflect.RF;

public class SetupSuperiorSkyblock2 implements Listener {
	
	@SuppressWarnings("deprecation")
	public static void load() {
		PandoraSpawners.scheduler().runNextTick(taks -> {
			if(Settings.settings.check_island_kick == false) return;
			Bukkit.getPluginManager().registerEvents(new SetupSuperiorSkyblock2(), PandoraSpawners.instance());
		});
	}
	
	@EventHandler
	private void onPlayerKick(IslandKickEvent event) {
		try {
			SuperiorPlayer kicked = event.getTarget();
			Player player = kicked.asPlayer();
			if(player != null && player.isOnline() == true) {
				var il = LocationRegistry.get(player);
				il.all().stream()
				.map(Location::getBlock)
				.map(ISpawner::of)
				.forEach(spawner -> {
					spawner.toItems()
					.forEach(item -> ItemMatcher.add(player, item));
					spawner.block().setType(Material.AIR);
				});
			} else {
				var il = LocationRegistry.get(kicked.getUniqueId());
				il.all().stream()
				.map(Location::getBlock)
				.map(ISpawner::of)
				.forEach(spawner -> {
					il.store(spawner.toData());
					spawner.block().setType(Material.AIR);
				});
			}
		} catch (Exception e) {
			RF.debug(e);
		}
	}

}
