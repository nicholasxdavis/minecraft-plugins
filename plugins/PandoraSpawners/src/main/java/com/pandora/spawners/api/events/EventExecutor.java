package com.pandora.spawners.api.events;

public interface EventExecutor<E> {
	
	void execute(E event);

}
