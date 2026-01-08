package com.pandora.spawners.api.events;

import com.pandora.spawners.api.spawner.IGenerator;
import com.pandora.spawners.api.spawner.ISpawner;

public interface IGeneratorEvent extends IEvent {
	
	IGenerator getGenerator();
	
	default ISpawner getSpawner() {
		return getGenerator().spawner();
	}

}
