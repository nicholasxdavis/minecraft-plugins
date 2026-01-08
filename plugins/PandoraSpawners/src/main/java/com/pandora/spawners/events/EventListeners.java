package com.pandora.spawners.events;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.pandora.spawners.PandoraSpawners;
import com.pandora.spawners.api.spawner.IGenerator;
import com.pandora.spawners.configuration.Settings;
import com.pandora.spawners.configuration.location.LocationRegistry;
import com.pandora.spawners.hook.HookRegistry;
import com.pandora.spawners.integration.PandoraBasesIntegration;
import com.pandora.spawners.items.ItemMatcher;
import com.pandora.spawners.spawner.generator.GeneratorRegistry;
import com.pandora.spawners.spawner.generator.SpawningManager.SpawnerMetaSpawnEvent;
import com.pandora.spawners.text.Text;
import com.pandora.spawners.utility.DataManager;
import com.pandora.spawners.utility.Messagable;
import com.pandora.spawners.utility.reflect.Reflect.RF;

public class EventListeners implements Listener {
	
	private static final List<RegistryAbstract> REGISTRIES =
			List.of(new RegistryTarget(), new RegistryLinking(), new RegistrySpawnerRename());
	
	public static void initialize() {
		Bukkit.getPluginManager().registerEvents(new EventListeners(), PandoraSpawners.instance());
		update();
	}
	
	public static void update() {
		REGISTRIES.forEach(RegistryAbstract::update);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	private void onInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		
		if(Settings.inactive(player.getWorld()) == true) return;
			
		Messagable m = new Messagable(player);
		if(event.getAction() == Action.LEFT_CLICK_BLOCK) {
			EventRegistry.verify_removing(event, player, m);
			return;
		}
		if(event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		Block block = event.getClickedBlock();
		if(event.getHand() == EquipmentSlot.OFF_HAND) {
			ItemStack item = event.getItem();
			if(item != null && item.getType().name().endsWith("_EGG") == true
					&& block.getType() == Material.SPAWNER) event.setCancelled(true);
			return;
		}
		if(block.getType() != Material.SPAWNER) {
			EventRegistry.stack_nearby(event, player, m, block);
			return;
		}
		IGenerator generator = fetch(block);
		if(generator == null) return;
		try {
			EventRegistry.interact(event, player, m, generator);
		} catch (Exception e) {
			RF.debug(e);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	private void onBreakFirst(BlockBreakEvent event) {
		try {
			Block block = event.getBlock();
			if(Settings.inactive(block.getWorld()) == true) return;
			if(Settings.settings.ignored(block) == true) return;
			IGenerator generator = GeneratorRegistry.raw(block);
			if(generator == null) return;
			event.setExpToDrop(0);
		} catch (Exception e) {
			RF.debug(e);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	private void onBreak(BlockBreakEvent event) {
		try {
			Block block = event.getBlock();
			
			if(Settings.inactive(block.getWorld()) == true) return;
			
			if(block.getType() != Material.SPAWNER) return;
			
			// PandoraBases integration: Check if player can break spawner in this territory
			Player player = event.getPlayer();
			if(player != null && PandoraBasesIntegration.isEnabled()) {
				if(!PandoraBasesIntegration.canBreak(player, block.getLocation())) {
					// Territory check failed - let PandoraBases handle the cancellation
					return;
				}
			}
			
			IGenerator generator = fetch(block);
			if(generator == null) return;
			EventRegistry.breaking(event, generator);
		} catch (Exception e) {
			RF.debug(e);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	private void onBlockExplodeByBlock(BlockExplodeEvent event) {
		List<Block> list = event.blockList();
		
		if(list.size() > 0 && Settings.inactive(list.get(0).getWorld()) == true) return;
		
		Iterator<Block> it = list.iterator();
		try {
			EventRegistry.explode_block(it);
		} catch (Exception e) {
			RF.debug(e);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	private void onBlockExplodeByEntity(EntityExplodeEvent event) {
		try {
			
			if(Settings.inactive(event.getEntity().getWorld()) == true) return;
			
			EventRegistry.explode_entity(event);
		} catch (Exception e) {
			RF.debug(e);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	private void onPlace(BlockPlaceEvent event) {
		if(event.isCancelled() == true) return;
		Block block = event.getBlockPlaced();
		
		if(Settings.inactive(block.getWorld()) == true) return;
		
		if(block.getType() != Material.SPAWNER) return;
		
		// PandoraBases integration: Check if player can place spawner in this territory
		Player player = event.getPlayer();
		if(player != null && PandoraBasesIntegration.isEnabled()) {
			if(!PandoraBasesIntegration.canBuild(player, block.getLocation())) {
				// Territory check failed - let PandoraBases handle the cancellation
				return;
			}
		}
		
		try {
			EventRegistry.place(event, block);
		} catch (Exception e) {
			RF.debug(e);
		}
	}
	
	@EventHandler(priority = EventPriority.LOW)
	private void onSpawn(SpawnerSpawnEvent event) {
		if(event instanceof SpawnerMetaSpawnEvent) return;
		
		Entity entity = event.getEntity();
		
		World world = entity.getWorld();
		if(Settings.disabled(world) == true) {
			event.setCancelled(true);
			return;
		}
		if(Settings.ignored(world) == true) return;
		
		if(entity.getCustomName() != null) return;
		if(Settings.settings.cancel_spawning_event == true) event.setCancelled(true);
		else entity.remove();
		try {
			EventRegistry.spawn(event, entity);
		} catch(Exception e) {
			RF.debug(e);
		}
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGH)
	private void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		PandoraSpawners.scheduler().runAtEntityLater(player, () -> {
			if(player.isOnline() == false) return;
			var il = LocationRegistry.raw(player);
			if(il == null) return;
			il.stored().forEach(item -> ItemMatcher.add(player, item));
		}, 10);
	}
	
	protected static IGenerator fetch(Block block) {
		if(Settings.inactive(block.getWorld()) == true) return null;
		if(Settings.settings.ignored(block) == true) return null;
		IGenerator generator = GeneratorRegistry.get(block);
		if(generator == null) {
			Text.failure("Unable to get spawner generator at #0, this should never happen,"
					+ " contact plugin developer.", "[world: " + block.getWorld() + ", x: " + block.getX()
					+ ", y: " + block.getY() + ", z: " + block.getZ() + "]");
			return null;
		}
		return generator;
	}
	
	private static abstract class RegistryAbstract implements Listener {
		
		private boolean registered;
		
		protected void register() {
			if(registered == true) return;
			Bukkit.getPluginManager().registerEvents(this, PandoraSpawners.instance());
		}
		
		protected void unregister() {
			if(registered == false) return;
			HandlerList.unregisterAll(this);
		}
		
		public abstract void update();
		
	}
	
	private static final class RegistryTarget extends RegistryAbstract {
		
		@Override
		public void update() {
			if(Settings.settings.entity_target == true) unregister();
			else register();
		}

		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		private void onEntityTarget(EntityTargetEvent event) {
			Entity entity = event.getEntity();
			if(DataManager.isSpawned(entity) == false) return;
			event.setCancelled(true);
		}

		@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
		private void onPlayerDamage(EntityDamageByEntityEvent event) {
			if(event.getEntity() instanceof Player) {
				if(DataManager.isSpawned(event.getDamager()) == false) return;
				event.setCancelled(true);
			}
		}
		
	}
	
	private static final class RegistryLinking extends RegistryAbstract {

		@Override
		public void update() {
			if(HookRegistry.WILD_STACKER.exists() == false) unregister();
			else register();
		}

		@EventHandler(priority = EventPriority.HIGHEST)
		private void onUnloadLink(ChunkUnloadEvent event) {
			Stream.of(event.getChunk().getTileEntities())
				.filter(CreatureSpawner.class::isInstance)
				.map(BlockState::getBlock)
				.forEach(HookRegistry.WILD_STACKER::unlink);
		}
		
	}
	
	private static final class RegistrySpawnerRename extends RegistryAbstract {

		@Override
		public void update() {
			if(Settings.settings.allow_renaming == true) unregister();
			else register();
		}

		@EventHandler(priority = EventPriority.HIGHEST)
		private void onAnvilPrep(PrepareAnvilEvent event) {
			var res = event.getResult();
			if(res == null || res.getType() != Material.SPAWNER) return;
			event.setResult(null);
		}
		
	}
	
}
