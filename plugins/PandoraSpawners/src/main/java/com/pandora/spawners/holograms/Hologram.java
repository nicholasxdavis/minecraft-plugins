package com.pandora.spawners.holograms;

import com.pandora.spawners.api.spawner.IGenerator;
import com.pandora.spawners.configuration.Language;
import com.pandora.spawners.configuration.Settings;
import com.pandora.spawners.spawner.type.SpawnerType;
import com.pandora.spawners.text.content.Content;

public class Hologram extends AbstractHologram {
	
	public Hologram(IGenerator generator) {
		super(generator, false, Settings.settings.holograms_regular_radius);
	}
	
	@Override
	public Content title() {
		SpawnerType type = generator.cache().type();
		String r = type == SpawnerType.EMPTY ? "empty" : "regular";
		int stack = generator.cache().stack();
		return stack > 1 ? Language.get("Holograms." + r + ".multiple",
				"name", type, "stack", stack)
				: Language.get("Holograms." + r + ".single",
						"name", type);
	}

}
