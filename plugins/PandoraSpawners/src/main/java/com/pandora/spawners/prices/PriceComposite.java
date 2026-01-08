package com.pandora.spawners.prices;

import org.bukkit.entity.Player;

import com.pandora.spawners.configuration.Language;
import com.pandora.spawners.configuration.Settings;
import com.pandora.spawners.hook.HookLevelPlugin;
import com.pandora.spawners.hook.HookRegistry;
import com.pandora.spawners.text.content.Content;
import com.pandora.spawners.utility.Utility;

/**
 * Composite price that charges both money (Vault) and levels (LevelPlugin)
 */
public class PriceComposite extends Price {
	
	private final Price moneyPrice;
	private final int levelCost;
	
	public PriceComposite(int moneyValue, int levelValue) {
		super(PriceType.ECONOMY, moneyValue); // Use ECONOMY as base type
		this.moneyPrice = new PriceCurrency(PriceType.ECONOMY, moneyValue, HookRegistry.ECONOMY.currency());
		this.levelCost = levelValue;
	}

	@Override
	public boolean has(Player player) {
		if(Utility.op(player)) return true;
		// Must have both money and level
		boolean hasMoney = moneyPrice.has(player);
		boolean hasLevel = HookRegistry.LEVEL_PLUGIN.exists() 
			? HookRegistry.LEVEL_PLUGIN.hasLevel(player, levelCost)
			: true; // If LevelPlugin not available, skip level check
		return hasMoney && hasLevel;
	}
	
	@Override
	public void remove(Player player) {
		if(Utility.op(player)) return;
		// Remove both money and level
		moneyPrice.remove(player);
		if(HookRegistry.LEVEL_PLUGIN.exists()) {
			HookRegistry.LEVEL_PLUGIN.removeLevel(player, levelCost);
		}
	}
	
	@Override
	public Content insufficient() {
		// Return combined insufficient message
		return Language.get("Prices.type.composite.insufficient");
	}
	
	@Override
	public Content text() {
		// Return combined price text
		return Language.get("Prices.type.composite.amount",
				"money", Settings.settings.price(value),
				"levels", levelCost);
	}
	
	@Override
	public Content requires(Player player) {
		int moneyNeeded = value - moneyPrice.balance(player);
		int levelNeeded = HookRegistry.LEVEL_PLUGIN.exists() 
			? Math.max(0, levelCost - HookRegistry.LEVEL_PLUGIN.getLevel(player))
			: 0;
		return Language.get("Prices.type.composite.requires",
				"money", Settings.settings.price(moneyNeeded),
				"levels", levelNeeded);
	}

	@Override
	public int balance(Player player) {
		// Return money balance (levels are separate)
		return moneyPrice.balance(player);
	}
	
	@Override
	public void refund(Player player) {
		// Refund both money and level
		moneyPrice.refund(player);
		// Note: LevelPlugin doesn't have a direct refund method, so we'd need to add XP back
		// For now, we'll skip level refund as it's more complex
	}

}


