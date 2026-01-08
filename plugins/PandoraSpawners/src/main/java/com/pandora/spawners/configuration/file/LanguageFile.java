package com.pandora.spawners.configuration.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.pandora.spawners.configuration.AbstractFile;
import com.pandora.spawners.configuration.Configuration.CF;
import com.pandora.spawners.spawner.type.SpawnerType;
import com.pandora.spawners.text.content.Content;
import com.pandora.spawners.text.content.ContentParser;

public class LanguageFile extends AbstractFile {
	
	private final Map<String, List<Content>> text = new HashMap<>();
	private final List<String> keys = new ArrayList<>();

	public LanguageFile() {
		super("language");
	}

	@Override
	protected void initialize() {
		text.clear();
		
		if(CF.version() < 5) {
			copy("Inventory.upgrades", "Upgrade-GUI");
			copy("Inventory.spawner-view", "Spawner-view");
			copy("Spawners.hologram", "Holograms");
			copy("Inventory.buy-shop", "Buy-shop");
			copy("Inventory.sell-shop", "Sell-shop");
			copy("Inventory.select-shop", "Shop-select");
			copy("Spawners.item", "Spawner-item");
		}
		if(CF.version() < 6) {
			if(getStrings("Upgrade-GUI.items.upgrade.info.range").isEmpty() == true)
				delete("Upgrade-GUI.items.upgrade.info.range");
			if(getStrings("Upgrade-GUI.items.upgrade.info.delay").isEmpty() == true)
				delete("Upgrade-GUI.items.upgrade.info.delay");
			if(getStrings("Upgrade-GUI.items.upgrade.info.amount").isEmpty() == true)
				delete("Upgrade-GUI.items.upgrade.info.amount");
		}
		
		// Pandora color scheme: &7 (gray) for normal, &e (yellow) for special, &6 (orange) for values, &8 (dark gray) for GUI titles
		put("Upgrade-GUI.purchase.range", "&7(!) &eUpgraded &erange &7to level &6%level%");
		put("Upgrade-GUI.purchase.delay", "&7(!) &eUpgraded &edelay &7to level &6%level%");
		put("Upgrade-GUI.purchase.amount", "&7(!) &eUpgraded &eamount &7to level &6%level%");
		
		put("Upgrade-GUI.name", "&8Spawner");
		
		put("Upgrade-GUI.items.upgrade.name.range", "&e-= Range &6%level% &e=-");
		put("Upgrade-GUI.items.upgrade.name.delay", "&e-= Delay &6%level% &e=-");
		put("Upgrade-GUI.items.upgrade.name.amount", "&e-= Amount &6%level% &e=-");
		put("Upgrade-GUI.items.upgrade.help", "&7Click to upgrade!");
		put("Upgrade-GUI.items.upgrade.info.range", List.of("  &7Shows required player distance",
				"&7from the spawner to be active"));
		put("Upgrade-GUI.items.upgrade.info.delay", List.of("  &7Shows time between each spawning",
				"&7if active"));
		put("Upgrade-GUI.items.upgrade.info.amount", List.of("  &7Shows amount of entities that",
				"&7will spawn each time"));
		put("Upgrade-GUI.items.upgrade.current.range", "&7Current range: &6%value% Blocks");
		put("Upgrade-GUI.items.upgrade.current.delay", "&7Current delay: &6%value% Seconds");
		put("Upgrade-GUI.items.upgrade.current.amount", "&7Current amount: &6%value% Entities");
		put("Upgrade-GUI.items.upgrade.next.range", "&7Next range: &6%value% Blocks");
		put("Upgrade-GUI.items.upgrade.next.delay", "&7Next delay: &6%value% Seconds");
		put("Upgrade-GUI.items.upgrade.next.amount", "&7Next amount: &6%value% Entities");
		put("Upgrade-GUI.items.upgrade.maximum-reached", "&eMaximum level has been reached!");
		put("Upgrade-GUI.items.upgrade.price", "&7Price: &6%price%");
		
		put("Upgrade-GUI.items.disabled-upgrade.name.range", "&7Range");
		put("Upgrade-GUI.items.disabled-upgrade.name.delay", "&7Delay");
		put("Upgrade-GUI.items.disabled-upgrade.name.amount", "&7Amount");
		put("Upgrade-GUI.items.disabled-upgrade.help", "&7Cannot be upgraded!");
		put("Upgrade-GUI.items.disabled-upgrade.current.range", "&7Current range: &6%value% Blocks");
		put("Upgrade-GUI.items.disabled-upgrade.current.delay", "&7Current delay: &6%value% Seconds");
		put("Upgrade-GUI.items.disabled-upgrade.current.amount", "&7Current amount: &6%value% Entities");
		
		put("Upgrade-GUI.items.stats.name", "&e%type% Spawner");
		put("Upgrade-GUI.items.stats.disabled", "&7DISABLED &7(Click to enable)");
		put("Upgrade-GUI.items.stats.enabled", "&eENABLED &7(Click to disable)");
		put("Upgrade-GUI.items.stats.empty", "&7Shift-right-click on this spawner to empty it.");
		put("Upgrade-GUI.items.stats.location", "&7Location: &6%x%&7, &6%y%&7, &6%z%");
		put("Upgrade-GUI.items.stats.stacking.infinite", "&7Stacked: &6%stack% Spawner");
		put("Upgrade-GUI.items.stats.stacking.finite", "&7Stacked: &6%stack%&7/&6%limit% Spawners");
		put("Upgrade-GUI.items.stats.spawnable", "&7Spawnable Entities: &e%spawnable%");
		put("Upgrade-GUI.items.stats.warnings.header", "&eSpawner has &6%count% &ewarning(s):");
		put("Upgrade-GUI.items.stats.warnings.light", "  &7insufficient light level");
		put("Upgrade-GUI.items.stats.warnings.environment", "  &7insufficient spawn space");
		put("Upgrade-GUI.items.stats.warnings.ground", "  &7missing correct ground type");
		put("Upgrade-GUI.items.stats.warnings.charges", "  &7out of charges");
		put("Upgrade-GUI.items.stats.warnings.power", "  &7insufficient redstone power");
		put("Upgrade-GUI.items.stats.warnings.unknown", "  &7invalid spawn conditions");
		put("Upgrade-GUI.items.stats.owner-offline", List.of(
				"&eUnable to spawn because the owner",
				"  &7of this spawner is offline!"));
		put("Upgrade-GUI.items.stats.lore", List.of());
		
		put("Upgrade-GUI.items.charges.name", "&eSpawning Charges: &6%charges%");
		put("Upgrade-GUI.items.charges.purchase.first", "&7Left-Click to purchase &6%charges% charges &7(&6%price%&7)");
		put("Upgrade-GUI.items.charges.purchase.second", "&7Right-Click to purchase &6%charges% charges &7(&6%price%&7)");
		put("Upgrade-GUI.items.charges.purchase.all", "&7Shift-Click to purchase &6%charges% charges &7(&6%price%&7)");
		
		put("Upgrade-GUI.charges.purchase", "&7(!) &eYou bought &6%charges% &espawner charges");
		put("Upgrade-GUI.disabled-upgrade", "&7(!) &eYou cannot upgrade this!");
		
		put("Spawner-item.regular.name", "&eSpawner &7(&6%type%&7)");
		put("Spawner-item.empty.name", "&7<Empty> &eSpawner");
		put("Spawner-item.empty-stored.name", "&7<Empty : %type%> &eSpawner");
		put("Spawner-item.header", "&7Upgrades:");
		put("Spawner-item.upgrade.range", "&7- &eRange &6%level%");
		put("Spawner-item.upgrade.delay", "&7- &eDelay &6%level%");
		put("Spawner-item.upgrade.amount", "&7- &eAmount &6%level%");
		put("Spawner-item.charges", "&eCharges: &6%charges%");
		put("Spawner-item.spawnable", "&eSpawnable Entities: &6%spawnable%");
		put("Spawner-item.info", List.of());
		
		put("Spawner-view.name", "&8All Spawners");
		put("Spawner-view.items.name", "&e%type% Spawner");
		put("Spawner-view.items.header.range", "&eRange:");
		put("Spawner-view.items.header.delay", "&eDelay:");
		put("Spawner-view.items.header.amount", "&eAmount:");
		put("Spawner-view.items.price", "&7- &7Price: &6%price%");
		put("Spawner-view.items.price-increase", "&7- &7Price Increase: &6%increase%");
		put("Spawner-view.items.maximum-level", "&7- &7Maximum Level: &6%level%");
		put("Spawner-view.items.spawnable", "&7Spawnable Entities: &e%spawnable%");
		put("Spawner-view.items.page.current", "&7Page &6%page%");
		put("Spawner-view.items.page.next", "&eNext Page");
		put("Spawner-view.items.page.previous", "&ePrevious Page");
		put("Spawner-view.permission", "&7You do not have permission");
		
		put("Prices.type.experience.insufficient", "Not enough experience!");
		put("Prices.type.experience.amount", "%amount% Experience");
		put("Prices.type.levels.insufficient", "Not enough experience levels!");
		put("Prices.type.levels.amount", "%amount% Experience Levels");
		put("Prices.type.material.insufficient", "Not enough materials!");
		put("Prices.type.material.amount", "%amount% � %material%");
		put("Prices.type.economy.insufficient", "Insufficient funds!");
		put("Prices.type.economy.amount", "$%amount%");
		put("Prices.type.flare-tokens.insufficient", "Insufficient tokens!");
		put("Prices.type.flare-tokens.amount", "%amount% Tokens");
		put("Prices.type.player-points.insufficient", "Insufficient player points!");
		put("Prices.type.player-points.amount", "%amount% Player points");
		put("Prices.type.composite.insufficient", "Insufficient funds or level!");
		put("Prices.type.composite.amount", "&7$&6%money% &7+ &6%levels% &7Levels");
		put("Prices.type.composite.requires", "&7Missing: $&6%money% &7+ &6%levels% &7Levels");
		put("Prices.insufficient", "&7(!) &e%insufficient% &7[Missing %price%]");
		
		put("Holograms.empty.single", "&e<Empty> &eSpawner");
		put("Holograms.empty.multiple", "&e%stack% &7� &e<Empty> &eSpawner");
		put("Holograms.regular.single", "&e%name% Spawner");
		put("Holograms.regular.multiple", "&e%stack% &7� &e%name% Spawner");
		put("Holograms.warning", "&e( &7!!! &e)");
		
		put("Shop-buy.name", "&8Spawner Shop &7(&e%page_current%&7/&e%page_total%&7)");
		put("Shop-buy.items.page.current", "&ePage %page%");
		put("Shop-buy.items.page.next", "&eNext Page");
		put("Shop-buy.items.page.previous", "&ePrevious Page");
		put("Shop-buy.items.spawner.name", "&eSpawner &e(%type%)");
		put("Shop-buy.items.spawner.price", "&7Price: &6%price%");
		put("Shop-buy.items.spawner.purchase.first", "&7Left-click to purchase %amount%");
		put("Shop-buy.items.spawner.purchase.second", "&7Right-click to purchase %amount%");
		put("Shop-buy.items.spawner.purchase.third", "&7Shift-left-click to purchase %amount%");
		put("Shop-buy.items.spawner.purchase.all", "&7Shift-right-click to purchase maximum");
		put("Shop-buy.purchase.success", "&7(!) &ePurchased &e%amount% � %type%&e Spawner(s)!");
		put("Shop-buy.permission.opening", "&7(!) &eYou do not have a permission to open this!");
		put("Shop-buy.permission.purchase", "&7(!) &eYou do not have a permission to purchase this!");
		
		put("Shop-sell.name", "&8Spawners Selling");
		put("Shop-sell.accept", "&eSell");
		put("Shop-sell.cancel", "&7Close");
		put("Shop-sell.items.selling.name", "&eSelling for:");
		put("Shop-sell.items.selling.price", "&7- &6%price%");
		put("Shop-sell.selling.success", "&7(!) &eSuccessfully sold spawners for: &6%price%");
		put("Shop-sell.selling.empty", "&7(!) &eNothing to sell!");
		put("Shop-sell.selling.unable", "&7(!) &eUnable to sell that!");
		put("Shop-sell.disabled", "&7(!) &eSpawner shop has been disabled!");
		put("Shop-sell.permission.opening", "&7(!) &eYou do not have a permission to open this!");
		put("Shop-sell.permission.selling", "&7(!) &eYou do not have a permission to sell this!");
		
		put("Shop-select.name", "&8Spawner Shop");
		put("Shop-select.buy-shop", "&eClick to purchase spawners");
		put("Shop-select.sell-shop", "&eClick to sell spawners");
		
		put("Shop-select.permission.opening", "&7(!) &eYou do not have a permission to open this!");
		
		put("Inventory.insufficient-space", "&7(!) &eYou do not have enough space in your inventory!");
		
		put("Items.spawner-drop.alert", "&eYou have &6%seconds% seconds &eto take your spawner items! &7(click or /spawnerdrops)");
		put("Items.spawner-drop.cleared", "&7(!) &eYour spawner drops disappeared, was not taken in time!");
		put("Items.spawner-drop.try-breaking", "&7(!) &eCannot break spawners while you have not taken previously dropped items!");
		put("Items.spawner-drop.empty", "&7(!) &eNo items to give!");
		
		put("Spawners.placing.permission", "&7(!) &eYou do not have a permission to place this!");
		
		put("Spawners.breaking.success", "&7(!) &eSpawner successfully mined!");
		put("Spawners.breaking.failure", "&7(!) &eSpawner failed to mine!");
		put("Spawners.breaking.permission", "&7(!) &eYou do not have a permission to break this!");
		
		put("Spawners.stacking.stacked.infinite", "&7(!) &eSpawners have been stacked! &7(&6%stack% &7Stacked)");
		put("Spawners.stacking.stacked.finite", "&7(!) &eSpawners have been stacked! &7(&6%stack%&7/&6%limit% &7Stacked)");
		put("Spawners.stacking.unequal-spawner", "&7(!) &eSpawners must be the same to stack!");
		put("Spawners.stacking.disabled-type", "&7(!) &eYou cannot stack this spawner!");
		put("Spawners.stacking.limit-reached", "&7(!) &eThis spawner has reached its stacking limit!");
		put("Spawners.stacking.nearby.none-match", "&7(!) &eUnable to find any nearby spawner to stack to that matches!");
		put("Spawners.stacking.permission", "&7(!) &eYou do not have a permission to stack this!");
		
		put("Spawners.chunks.limit-reached", "&7(!) &eThis chunk has reached its spawner limit!");
		
		put("Spawners.ownership.limit.place", "&7(!) &eSpawner placed &7(&6%placed%&7/&6%limit%&7)");
		put("Spawners.ownership.limit.reached", "&7(!) &eYou have reach your spawner limit! &7(&6%limit%&7)");
		put("Spawners.ownership.stacking.warning", "&7(!) &eYou cannot stack a spawner that you do not own!");
		put("Spawners.ownership.breaking.warning", "&7(!) &eYou cannot break a spawner that you do not own!");
		put("Spawners.ownership.upgrading.warning", "&7(!) &eYou cannot upgrade a spawner that you do not own!");
		put("Spawners.ownership.opening.warning", "&7(!) &eYou cannot open a spawner that you do not own!");
		put("Spawners.ownership.changing.warning", "&7(!) &eYou cannot change a spawner that you do not own!");
		put("Spawners.ownership.show-owner", "&7(!) &eThis spawner is owner by &6%player%");
		
		put("Spawners.natural.changing.warning", "&7(!) &eYou cannot change a natural spawner!");
		put("Spawners.natural.breaking.warning", "&7(!) &eYou cannot break a natural spawner!");
		put("Spawners.natural.stacking.warning", "&7(!) &eYou cannot stack a natural spawner!");
		put("Spawners.natural.opening.warning", "&7(!) &eYou cannot open a natural spawner!");
		put("Spawners.natural.upgrading.warning", "&7(!) &eYou cannot upgrade a natural spawner!");
		
		put("Spawners.changing.type-changed", "&7(!) &eSpawner type set to &e%type%");
		put("Spawners.changing.same-type", "&7(!) &eYou cannot set the same entity type!");
		put("Spawners.changing.dany.from", "&7(!) &eYou cannot change this entity type spawner!");
		put("Spawners.changing.dany.to", "&7(!) &eYou cannot set this entity type!");
		put("Spawners.changing.permission", "&7(!) &eYou do not have a permission to use this!");
		put("Spawners.changing.eggs.insufficient", "&7(!) &eNot enough spawn eggs &7(Requires %required%)");
		
		put("Spawners.charges.lose-by-stacking", "&7(!) &eLost &e%charges% charge(s) &ewhen stacking!");
		
		put("Spawners.upgrades.disabled", "&7(!) &eYou cannot upgrade this!");
		put("Spawners.upgrades.permission.opening", "&7(!) &eYou do not have a permission to open this!");
		put("Spawners.upgrades.permission.purchase", "&7(!) &eYou do not have a permission to upgrade this!");
		
		put("Spawners.empty.disabled", "&7(!) &7Empty spawners are disabled!");
		put("Spawners.empty.try-open", "&7(!) &7Cannot open empty spawners!");
		put("Spawners.empty.hand-full", "&7(!) &7You must have an empty hand to remove spawner egg(s)!");
		put("Spawners.empty.verify-removing.first", "&e(!) &eLeft click to verify removing eggs from this spawner!");
		put("Spawners.empty.verify-removing.try-again", "&7(!) &7You first have to sneak and right click the empty spawner!");
		
		put("Spawners.view.empty", "&7(!) &7Nothing to view!");
		put("Spawners.view.disabled", "&7(!) &7Spawner viewing is disabled!");
		
		put("Spawners.give.success", "&e(!) &7Added &e%amount% &7� &e%type% Spawner &7to your inventory!");
		put("Spawners.give.success-single", "&e(!) &7Added &e%type% Spawner &7to your inventory!");
		
		put("Locations.header", "&eYou have placed %count% spawner(s) at:");
		put("Locations.world", "  &7(%world%)");
		put("Locations.position", "&6%index%. &7%x%, %y%, %z%");
		put("Locations.none-owned", "&eYou do not own any spawners!");
		
		put("Trusted.help.primary",
				List.of("&eUsage: &7/spawnertrust",
						"  &eadd [player] &7- add trusted player",
						"  &eremove [player] &7- remove trusted player",
						"  &eclear &7- remove all trusted players",
						"  &eview &7- view all trusted players"));
		put("Trusted.help.add", "&eUsage: &7/spawnertrust add [player]");
		put("Trusted.help.remove", "&eUsage: &7/spawnertrust remove [player]");
		put("Trusted.info.unknow-player", "&7Unknown player!");
		put("Trusted.info.already-trusted", "&7You already trust this player!");
		put("Trusted.info.not-trusted", "&7You already do not trust this player!");
		put("Trusted.info.empty", "&7You do not trust any players!");
		put("Trusted.info.added", "&7Added this player to your trust list!");
		put("Trusted.info.removed", "&7Removed this player from your trust list!");
		put("Trusted.info.cleared", "&7Removed &6%count% &7player(s) from your trust list!");
		put("Trusted.header", "&eYou have &6%count% &eplayer(s) in your trust list:");
		put("Trusted.player", "&6%index%. &7%player%");
		
		Stream.of(SpawnerType.values())
			.filter(SpawnerType::regular)
			.forEach(type -> put("Entities.name." + type.name(), type.text().text()));
		
		file.options().copyDefaults(true);
		
		header("""
				
				Language file has been updated!
				
				(!!!) Legacy colors (&a&1&b...) are NO longer available.
				
				New text formatting:
				
				Color format:
				  <#123abc>
				  <#ABC987>
				  ...
				
				Gradient format:
				  <#ff0000-#00ff00>
				  <#ff0000-#ffff00-#00ff00>
				  ...
				
				Modifier format:
				  bold - <!bold> or <!b>
				  italic -  or <!i>
				  underline -  or <!u>
				  strikethrough - <!strikethrough> or <!s>
				  obfuscated - <!obfuscated> or <!o>
				  
				If you find any errors or bugs, or any text shows
				  incorrectly then be sure to report it.
				
				""");
		
		save();
		
		read();
	}
	
	private void put(String path, Object value) {
		defaulted(path, value);
		keys.add(path);
	}

	private void read() {
		keys.forEach(key -> {
			List<Content> list;
			if(file.isString(key) == true) {
				String s = file.getString(key);
				if(s == null || s.isEmpty() == true) list = of();
				else list = of(ContentParser.parse(s));
			} else list = ContentParser.parse(file.getStringList(key));
			if(list == null || list.isEmpty() == true) return; 
			text.put(key, list);
		});
		keys.clear();
	}
	
	private static List<Content> of(Content... cs) {
		return cs == null ? new ArrayList<>()
				: Stream.of(cs).collect(Collectors.toList());
	}
	
	public List<Content> get(String key) {
		return text.get(key);
	}

}
