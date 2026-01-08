package com.pandora.spawners.api.events;

import org.bukkit.entity.Player;

import com.pandora.spawners.api.spawner.IGenerator;

public class SpawnerSwitchEvent extends SpawnerModifyEvent {
	
	public final boolean switched;

	public SpawnerSwitchEvent(Player player, IGenerator generator, boolean switched) {
		super(player, ModifyType.SWITCH, generator);
		this.switched = switched;
	}

}
