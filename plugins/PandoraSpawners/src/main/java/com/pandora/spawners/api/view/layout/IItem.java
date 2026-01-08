package com.pandora.spawners.api.view.layout;

import org.bukkit.inventory.ItemStack;

public interface IItem {
	
	ItemStack toItem(boolean denied);
	
	default ItemStack toItem() {
		return toItem(false);
	}

}
