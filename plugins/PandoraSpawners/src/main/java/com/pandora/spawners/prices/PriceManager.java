package com.pandora.spawners.prices;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.pandora.spawners.configuration.Configuration.CF;
import com.pandora.spawners.hook.HookRegistry;
import com.pandora.spawners.items.ItemMatcher;
import com.pandora.spawners.prices.Price.PriceCurrency;
import com.pandora.spawners.prices.Price.PriceExperience;
import com.pandora.spawners.prices.Price.PriceLevels;
import com.pandora.spawners.prices.Price.PriceMaterial;

public final class PriceManager {

	private static final Map<Group, IPrice> PRICES = new HashMap<>();
	
	public static IPrice of(Group group) {
		return PRICES.get(group);
	}
	
	public static Price price(Group group, int i) {
		return of(group).of(i);
	}

	public static void reload() {
		PRICES.clear();
		Stream.of(Group.values()).forEach(group -> {
			String path = "Prices." + group.name();
			String type_name = CF.s.file().getString(path + ".price-type");
			PriceType type = PriceType.of(type_name);
			if(type == null) type = PriceType.EXPERIENCE;
			else if(type == PriceType.ECONOMY && HookRegistry.ECONOMY.exists() == false)
				type = PriceType.EXPERIENCE;
			else if(type == PriceType.FLARE_TOKENS && HookRegistry.FLARE_TOKENS.exists() == false)
				type = PriceType.EXPERIENCE;
			IPrice price;
			if(type == PriceType.EXPERIENCE) price = PriceExperience::new;
			else if(type == PriceType.LEVELS) price = PriceLevels::new;
			else if(type == PriceType.ECONOMY) {
				// For upgrades, use composite price (money + levels) if LevelPlugin is available
				if(group == Group.upgrades && HookRegistry.LEVEL_PLUGIN.exists()) {
					int levelCost = CF.s.file().getInt(path + ".level-cost", 0);
					if(levelCost > 0) {
						price = i -> new PriceComposite(i, levelCost);
					} else {
						price = i -> new PriceCurrency(PriceType.ECONOMY, i, HookRegistry.ECONOMY.currency());
					}
				} else {
					price = i -> new PriceCurrency(PriceType.ECONOMY, i, HookRegistry.ECONOMY.currency());
				}
			} else if(type == PriceType.FLARE_TOKENS) price = i -> new PriceCurrency(PriceType.FLARE_TOKENS,
					i, HookRegistry.FLARE_TOKENS.currency());
			else if(type == PriceType.PLAYER_POINTS) price = i -> new PriceCurrency(PriceType.PLAYER_POINTS,
					i, HookRegistry.PLAYER_POINTS.currency());
			else {
				ItemMatcher matcher = ItemMatcher.from(CF.s.file(), path + ".item");
				price = i -> new PriceMaterial(i, matcher);
			}
			PRICES.put(group, price);
		});
	}
	
	public static interface IPrice {
		
		Price of(int i);
		
	}

}
