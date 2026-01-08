package com.pandora.spawners.configuration;

import com.pandora.spawners.configuration.file.LanguageFile;
import com.pandora.spawners.configuration.file.LayoutsFile;
import com.pandora.spawners.configuration.file.RequirementFile;
import com.pandora.spawners.configuration.file.SettingsFile;

public final class Configuration {
	
	public static void initialize() {
		CF.s.load();
		CF.l.load();
		CF.r.load();
		CF.y.load();
		
		CF.s.free();
		CF.l.free();
		CF.r.free();
		CF.y.free();
	}
	
	public static final class CF {
		
		public static final SettingsFile s = new SettingsFile();
		public static final LanguageFile l = new LanguageFile();
		public static final RequirementFile r = new RequirementFile();
		public static final LayoutsFile y = new LayoutsFile();
		
		public static int version;
		
		public static int version() {
			return version;
		}
		
	}

}
