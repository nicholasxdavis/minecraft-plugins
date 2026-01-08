package com.pandora.spawners.api.events;

import java.util.List;

import org.bukkit.entity.Entity;

import com.pandora.spawners.api.spawner.IGenerator;

public class SpawnerPostSpawnEvent implements IEvent, IGeneratorEvent {
	
	private final IGenerator generator;
	public final List<Entity> entities;
	
	public SpawnerPostSpawnEvent(IGenerator generator, List<Entity> entities) {
		this.generator = generator;
		this.entities = entities;
	}
	
	public IGenerator getGenerator() {
		return generator;	
	}

}
