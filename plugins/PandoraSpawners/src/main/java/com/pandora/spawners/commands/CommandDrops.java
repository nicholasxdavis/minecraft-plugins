package com.pandora.spawners.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.pandora.spawners.configuration.Language;
import com.pandora.spawners.configuration.Settings;
import com.pandora.spawners.items.ItemCollector;

public class CommandDrops extends Command {

	protected CommandDrops(String name) {
		super(name);
	}

	@Override
	public boolean execute(CommandSender sender, String lable, String[] args) {
		if(Settings.settings.breaking_drop_on_ground == true) return false;
		if(sender instanceof Player == false) return false;
		Player player = (Player) sender;
		ItemCollector drop = ItemCollector.get(player);
		if(drop == null) player.sendMessage(Language.get("Items.spawner-drop.empty").text());
		else drop.get(false);
		return true;
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
		return new ArrayList<>();
	}

}
