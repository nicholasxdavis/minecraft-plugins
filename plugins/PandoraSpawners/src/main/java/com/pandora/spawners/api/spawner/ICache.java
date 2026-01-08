package com.pandora.spawners.api.spawner;

import java.util.UUID;

import com.pandora.spawners.spawner.ActiveCache;
import com.pandora.spawners.spawner.type.SpawnerType;

public interface ICache {
	
	static ICache of(ISpawner spawner) {
		return new ActiveCache(spawner);
	}
	
	void cache();
	
	SpawnerType type();
	
	int stack();
	
	int charges();
	
	int spawnable();
	
	boolean empty();
	
	boolean enabled();
	
	boolean natural();
	
	boolean owned();
	
	UUID owner();
	
	int range();
	
	int delay();
	
	int amount();

}
