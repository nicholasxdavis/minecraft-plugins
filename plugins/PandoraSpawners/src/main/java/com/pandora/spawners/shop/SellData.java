package com.pandora.spawners.shop;

import com.pandora.spawners.spawner.type.SpawnerType;

public class SellData {
	
	public final SpawnerType type;
	public final int refund;
	public double up;
	
	public SellData(SpawnerType type, int refund, double up) {
		this.type = type;
		this.refund = refund;
		this.up = up;
	}

}
