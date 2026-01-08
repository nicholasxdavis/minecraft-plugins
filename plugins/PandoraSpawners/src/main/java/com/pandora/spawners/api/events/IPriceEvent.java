package com.pandora.spawners.api.events;

import java.util.Optional;

import org.bukkit.entity.Player;

import com.pandora.spawners.prices.Price;

public interface IPriceEvent extends IEvent {
	
	Optional<Price> getPrice();
	
	void setPrice(Price price);
	
	default boolean withdraw(Player player) {
		return getPrice()
				.map(price -> {
					boolean b = price.has(player);
					if(b == true) price.remove(player);
					return b;
				})
				.orElse(true);
	}
	
	default Price getUnsafePrice() {
		return getPrice().orElseThrow();
	}

}
