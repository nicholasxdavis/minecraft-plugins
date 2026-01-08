package com.pandora.spawners.api.events;

import org.bukkit.entity.Player;

import com.pandora.spawners.api.spawner.IGenerator;
import com.pandora.spawners.prices.Price;
import com.pandora.spawners.spawner.type.SpawnerType;

public class SpawnerChangeEvent extends SpawnerInteractEvent {
	
	private SpawnerType type;
	public final boolean empty;

	public SpawnerChangeEvent(Player player, IGenerator generator, Price price,
			SpawnerType type, boolean empty) {
		super(player, generator, BlockAction.CHANGE, price);
		this.type = type;
		this.empty = empty;
	}
	
	public SpawnerType getNewType() {
		return type == null ? SpawnerType.PIG : type;
	}
	
	public void setNewType(SpawnerType type) {
		this.type = type;
	}


}
