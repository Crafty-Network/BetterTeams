package com.booksaw.betterTeams.cooldown;

import com.booksaw.betterTeams.Main;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class CooldownManager {

	final HashMap<String, CommandCooldown> cooldowns;

	public CooldownManager(String command) {
		File f = new File("plugins/BetterTeams/" + command + ".yml");

		if (!f.exists()) {
			Main.plugin.saveResource(command + ".yml", false);
		}

		YamlConfiguration cooldown = YamlConfiguration.loadConfiguration(f);
		cooldowns = new HashMap<>();

		List<String> cooldownsStr = cooldown.getStringList("cooldowns");

		for (String temp : cooldownsStr) {
			try {
				String[] split = temp.split(":");
				cooldowns.put(split[0], new CommandCooldown(split[0], Integer.parseInt(split[1])));
			} catch (Exception e) {
				Main.plugin.getLogger()
						.severe("Something went wrong while enabling a cooldown, there appears to be an error with "
								+ command + ".yml. (ERROR: " + e.getMessage() + ")");
				Main.plugin.getLogger().severe(e.getMessage());

			}
		}

	}

	public CommandCooldown getCooldown(String command) {
		if (cooldowns.containsKey(command)) {
			return cooldowns.get(command);
		}
		return new NoCooldown();
	}

	public static class NoCooldown extends CommandCooldown {

		public NoCooldown() {
			super("", 0);
		}

		@Override
		public void runCommand(Player player) {
		}

		@Override
		public int getRemaining(Player player) {
			return -1;
		}

	}

}
