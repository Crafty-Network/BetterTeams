package com.booksaw.betterTeams.cooldown;

import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class CommandCooldown {

	final HashMap<Player, Long> nextTime;

	@Getter
	private final int cooldown;
	@Getter
	private final String command;

	public CommandCooldown(String command, int cooldown) {
		this.command = command;
		this.cooldown = cooldown * 1000;
		nextTime = new HashMap<>();
	}

	public void runCommand(Player player) {
		if (player.hasPermission("betterteams.cooldown.bypass")) {
			return;
		}
		nextTime.put(player, System.currentTimeMillis() + cooldown);
	}

	public int getRemaining(Player player) {

		if (player.hasPermission("betterteams.cooldown.bypass")) {
			return -1;
		}

		Long end = nextTime.get(player);

		if (end == null) {
			return -1;
		}

		if (end < System.currentTimeMillis()) {
			nextTime.remove(player);
			return -1;
		}

		return (int) ((end - System.currentTimeMillis()) / 1000);
	}
}
