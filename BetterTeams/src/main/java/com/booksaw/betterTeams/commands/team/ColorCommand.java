package com.booksaw.betterTeams.commands.team;

import com.booksaw.betterTeams.*;
import com.booksaw.betterTeams.commands.presets.TeamSubCommand;
import com.booksaw.betterTeams.text.LegacyTextUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ColorCommand extends TeamSubCommand {

	private final Set<NamedTextColor> banned = new HashSet<>();

	public ColorCommand() {
		for (char c : Main.plugin.getConfig().getString("bannedColors").toCharArray()) {
			NamedTextColor nc = LegacyTextUtils.namedColorByChar(c);
			if (nc != null) banned.add(nc);
		}
	}

	@Override
	public CommandResponse onCommand(TeamPlayer teamPlayer, String label, String[] args, Team team) {
		NamedTextColor color = LegacyTextUtils.parseNamedColor(args[0]);

		if (color == null) {
			return new CommandResponse("color.fail");
		}

		if (banned.contains(color)) {
			return new CommandResponse("color.banned");
		}

		team.setColor(color);

		return new CommandResponse(true, "color.success");
	}

	@Override
	public String getCommand() {
		return "color";
	}

	@Override
	public int getMinimumArguments() {
		return 1;
	}

	@Override
	public String getNode() {
		return "color";
	}

	@Override
	public String getHelp() {
		return "Change your teams color";
	}

	@Override
	public String getArguments() {
		return "<color code>";
	}

	@Override
	public int getMaximumArguments() {
		return 1;
	}

	@Override
	public void onTabComplete(List<String> options, CommandSender sender, String label, String[] args) {
		if (args.length == 1) {
			for (NamedTextColor c : NamedTextColor.NAMES.values()) {
				if (banned.contains(c)) continue;
				String name = NamedTextColor.NAMES.key(c);
				if (name.startsWith(args[0].toLowerCase())) {
					options.add(name);
				}
			}
		}
	}

	@Override
	public PlayerRank getDefaultRank() {
		return PlayerRank.OWNER;
	}

}
