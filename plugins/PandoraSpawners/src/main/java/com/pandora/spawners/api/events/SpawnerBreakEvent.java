package com.pandora.spawners.api.events;

import org.bukkit.entity.Player;

import com.pandora.spawners.api.spawner.IGenerator;
import com.pandora.spawners.prices.Price;

public class SpawnerBreakEvent extends SpawnerInteractEvent {
	
	public double chance;

	public SpawnerBreakEvent(Player player, IGenerator generator, Price price, double chance) {
		super(player, generator, BlockAction.BREAK, price);
		this.chance = chance;
	}

}
