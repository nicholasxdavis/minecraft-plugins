package com.pandora.spawners.api.events;

import org.bukkit.entity.Player;

import com.pandora.spawners.api.spawner.IGenerator;
import com.pandora.spawners.api.spawner.IVirtual;
import com.pandora.spawners.prices.Price;

public class SpawnerStackEvent extends SpawnerInteractEvent {
	
	private final IVirtual item;
	public final boolean direct;

	public SpawnerStackEvent(Player player, IGenerator generator, Price price,
			IVirtual item, boolean direct) {
		super(player, generator, BlockAction.STACK, price);
		this.item = item;
		this.direct = direct;
	}
	
	public final IVirtual getItem() {
		return item;
	}

}
