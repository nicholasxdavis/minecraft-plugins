package com.pandora.spawners.api.events;

import java.util.Optional;

import org.bukkit.entity.Player;

import com.pandora.spawners.api.spawner.IGenerator;
import com.pandora.spawners.prices.Price;

public class SpawnerChargeEvent extends SpawnerModifyEvent implements IPriceEvent {
	
	public int charges;
	
	private Price price;

	public SpawnerChargeEvent(Player player, IGenerator generator, Price price, int charges) {
		super(player, ModifyType.CHARGE, generator);
		this.price = price;
		this.charges = charges;
	}

	@Override
	public Optional<Price> getPrice() {
		return Optional.ofNullable(price);
	}

	@Override
	public void setPrice(Price price) {
		this.price = price;
	}

}
