package com.pandora.spawners.hook;

import java.util.ArrayList;
import java.util.List;

import com.pandora.spawners.text.Text;
import com.pandora.spawners.utility.reflect.Reflect.RF;

public class HookRegistry {

	private static final List<HookInstance> HOOKS = new ArrayList<>();

	public static final HookEconomy ECONOMY = new HookEconomy();
	public static final HookLevelPlugin LEVEL_PLUGIN = new HookLevelPlugin();
	public static final HookWildStacker WILD_STACKER = new HookWildStacker();
	public static final HookWildTools WILD_TOOLS = new HookWildTools();
	public static final HookShopGUI SHOP_GUI = new HookShopGUI();
	public static final HookFlareTokens FLARE_TOKENS = new HookFlareTokens();
	public static final HookSuperiorSkyblock2 SUPERIOR_SKYBLOCK_2 = new HookSuperiorSkyblock2();
	public static final HookPlayerPoints PLAYER_POINTS = new HookPlayerPoints();
	public static final HookPlotSquared PLOT_SQUARED = new HookPlotSquared();

	public static void load() {
		try {
			HOOKS.clear();
			HOOKS.add(ECONOMY);
			HOOKS.add(LEVEL_PLUGIN);
			HOOKS.add(WILD_STACKER);
			HOOKS.add(WILD_TOOLS);
			HOOKS.add(SHOP_GUI);
			HOOKS.add(FLARE_TOKENS);
			HOOKS.add(SUPERIOR_SKYBLOCK_2);
			HOOKS.add(PLAYER_POINTS);
			HOOKS.add(PLOT_SQUARED);
			
			HOOKS.forEach(i -> {
				try {
					i.load();
				} catch (Exception e) {
					RF.debug(e);
				}
			});
			HOOKS.stream()
				.filter(HookInstance::exists)
				.map(HookInstance::message)
				.forEach(Text::logInfo);
		} catch (Exception e) {
			RF.debug(e);
			Text.logFail("Unable to initialise hooks, make sure other supported plugins are enabled."
					+ " If the API has changed please contact developer!");
		}
	}

}
