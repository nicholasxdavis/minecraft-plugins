package com.pandora.spawners.api.events;

import com.pandora.spawners.api.spawner.IGenerator;

public class SpawnerPreSpawnEvent extends SpawnerEvent implements IGeneratorEvent {
	
	private final IGenerator generator;
	
	public int count;
	public boolean bypass_checks;
	
	public SpawnerPreSpawnEvent(IGenerator generator, int count) {
		this.generator = generator;
		this.count = count;
		this.bypass_checks = false;
	}
	
	public final IGenerator getGenerator() {
		return generator;
	}

}
