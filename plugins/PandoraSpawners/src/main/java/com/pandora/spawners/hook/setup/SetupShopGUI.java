package com.pandora.spawners.hook.setup;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import com.pandora.spawners.PandoraSpawners;
import com.pandora.spawners.api.spawner.IVirtual;
import com.pandora.spawners.spawner.type.SpawnerType;
import com.pandora.spawners.utility.DataManager;
import com.pandora.spawners.utility.reflect.Reflect.RF;
import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.event.ShopGUIPlusPostEnableEvent;
import net.brcdev.shopgui.exception.api.ExternalSpawnerProviderNameConflictException;
import net.brcdev.shopgui.spawner.external.provider.ExternalSpawnerProvider;

public class SetupShopGUI {
	
	public static void load() {
		Bukkit.getPluginManager().registerEvents(new ShopGUIPlusHook(), PandoraSpawners.instance());
	}
	
	public static class ShopGUIPlusHook implements Listener {

		@EventHandler
		public void onEnable(ShopGUIPlusPostEnableEvent event) {
			try {
				ShopGuiPlusApi.registerSpawnerProvider(new SpawnerMetaSpawnerProvider());
			} catch (ExternalSpawnerProviderNameConflictException e) {
				// ignore
			} catch(Exception e) {
				RF.debug(e);
			}
		}
	}
	
	public static class SpawnerMetaSpawnerProvider implements ExternalSpawnerProvider {

		@Override
		public String getName() {
			return "PandoraSpawners";
		}

		@Override
		public ItemStack getSpawnerItem(EntityType entity) {
			SpawnerType type = SpawnerType.of(entity);
			if(type == null) return null;
			return DataManager.getSpawner(type, 1);
		}

		@Override
		public EntityType getSpawnerEntityType(ItemStack item) {
			IVirtual virtual = IVirtual.of(item);
			return virtual == null ? null : virtual.getType().entity();
		}
		
	}

}
