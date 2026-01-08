package com.pandora.spawners.api.events;

import java.util.Optional;

import org.bukkit.entity.Player;

import com.pandora.spawners.api.spawner.IGenerator;
import com.pandora.spawners.prices.Price;
import com.pandora.spawners.spawner.type.UpgradeType;

public class SpawnerUpgradeEvent extends SpawnerModifyEvent implements IPriceEvent {
	
	public final UpgradeType upgrade;
	public int upgrade_level;
	public final int upgrade_maximum;
	
	private Price price;

	public SpawnerUpgradeEvent(Player player, IGenerator generator, UpgradeType upgrade,
			int upgrade_level, int upgrade_maximum, Price price) {
		super(player, ModifyType.UPGRADE, generator);
		this.upgrade = upgrade;
		this.upgrade_level = upgrade_level;
		this.upgrade_maximum = upgrade_maximum;
		this.price = price;
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
