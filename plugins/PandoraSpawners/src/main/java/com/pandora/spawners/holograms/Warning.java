package com.pandora.spawners.holograms;

import com.pandora.spawners.api.spawner.IGenerator;
import com.pandora.spawners.configuration.Language;
import com.pandora.spawners.configuration.Settings;
import com.pandora.spawners.text.content.Content;

public class Warning extends AbstractHologram {

	public Warning(IGenerator generator, boolean above) {
		super(generator, above, Settings.settings.holograms_warning_radius);
	}

	@Override
	public Content title() {
		return Language.get("Holograms.warning");
	}

}
