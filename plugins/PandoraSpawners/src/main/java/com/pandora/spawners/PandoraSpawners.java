package com.pandora.spawners;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.impl.PlatformScheduler;

import com.pandora.spawners.api.APIInstance;
import com.pandora.spawners.api.APIRegistry;
import com.pandora.spawners.commands.CommandManager;
import com.pandora.spawners.configuration.Configuration;
import com.pandora.spawners.configuration.location.LocationRegistry;
import com.pandora.spawners.events.EventListeners;
import com.pandora.spawners.hook.HookRegistry;
import com.pandora.spawners.shop.ShopRegistry;
import com.pandora.spawners.spawner.generator.GeneratorRegistry;
import com.pandora.spawners.spawner.generator.SpawningManager;
import com.pandora.spawners.text.Text;
import com.pandora.spawners.utility.AutoSaveManager;
import com.pandora.spawners.utility.DataManager;
import com.pandora.spawners.utility.DropLimitStorage;
import com.pandora.spawners.utility.Metrics;
import com.pandora.spawners.utility.SpawnerDataStorage;
import com.pandora.spawners.utility.Utility;
import com.pandora.spawners.version.Version;
import com.pandora.spawners.integration.PandoraBasesIntegration;

public final class PandoraSpawners extends JavaPlugin {
	
	public static final double PLUGIN_VERSION = 1.0;
	
	private static PandoraSpawners plugin;
    
    private static boolean loaded;
    
    private static FoliaLib folia;
    private static PlatformScheduler scheduler;
    
    private APIInstance api;
    
    @Override
    public void onLoad() {
		loaded = Version.version != null;
		
		if(loaded == true) {
			plugin = this;
			this.api = new APIRegistry();
		}
    }

	@Override
	public void onEnable() {
		// Check for PandoraBases dependency
		if(Bukkit.getPluginManager().getPlugin("PandoraBases") == null) {
			getLogger().severe("PandoraBases is required! Disabling plugin.");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		
		folia = new FoliaLib(this);
		scheduler = folia.getScheduler();
		
		if(loaded == true) {
			Text.logLoad();
			Utility.check(74188, s -> {
				if(Utility.isDouble(s) == false) return;
				double v = Double.parseDouble(s);
				if(v > PLUGIN_VERSION) Text.logOutdated(v);
			});
			// Initialize PandoraBases integration
			PandoraBasesIntegration.initialize();
			HookRegistry.load();
			Configuration.initialize();
			CommandManager.initialize();
			DataManager.initialize();
			ShopRegistry.initialize();
			EventListeners.initialize();
			GeneratorRegistry.initialize();
			SpawningManager.initialize();
			LocationRegistry.initialize();
			com.pandora.spawners.spawner.DropLimitManager.initialize();
			
			// Load saved data after a delay to ensure chunks are loaded
			getLogger().info("Loading saved data...");
			DropLimitStorage.loadAll();
			scheduler().runLater(() -> {
				SpawnerDataStorage.loadAll();
			}, 100); // 5 second delay
			
			// Start auto-save
			AutoSaveManager.start();
			
			initializeMetrics();
			
			getLogger().info("PandoraSpawners has been enabled! Integrated with PandoraBases.");
		} else {
			Text.logFail("failed to load, invalid server version!");
			Bukkit.getPluginManager().disablePlugin(this);
		}
	}
	
	@Override
	public void onDisable() {
		if(loaded == true) {
			Text.logUnload();
			
			// Stop auto-save
			AutoSaveManager.stop();
			
			// Save all data before shutdown
			getLogger().info("Saving all data...");
			AutoSaveManager.saveAll();
			
			GeneratorRegistry.clear();
			LocationRegistry.clear();
		}
	}
	
	/**
	 * @return Instance of this plugin
	 */
	
	public static PandoraSpawners instance() {
		return plugin;
	}
	
	/**
	 * @return PandoraSpawners API
	 */
	
	public APIInstance getAPI() {
		return api;
	}
	
	/**
	 * @return PandoraSpawners API
	 */
	
	public static APIInstance API() {
		return plugin.api;
	}

	/**
	 * @return FoliaLib
	 */

	public static FoliaLib foliaLib() {
		return folia;
	}

	/**
	 * @return Task scheduler
	 */

	public static PlatformScheduler scheduler() {
		return scheduler;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		return CommandManager.onCommand(sender, command, label, args);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return CommandManager.onTabComplete(sender, command, alias, args);
	}
	
	private void initializeMetrics() {
		new Metrics(plugin, 8373);
	}
}

