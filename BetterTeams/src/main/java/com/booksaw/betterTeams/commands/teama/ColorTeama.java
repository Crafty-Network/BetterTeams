package com.booksaw.betterTeams.commands.teama;

import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.commands.presets.TeamSelectSubCommand;
import com.booksaw.betterTeams.text.LegacyTextUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ColorTeama extends TeamSelectSubCommand {

	private static final String BANNED_CHARS = "lmnkor";

	@Override
	public CommandResponse onCommand(CommandSender sender, String label, String[] args, Team team) {
		NamedTextColor color = LegacyTextUtils.parseNamedColor(args[1]);

		if (color == null) {
			return new CommandResponse("color.fail");
		}

		team.setColor(color);

		return new CommandResponse(true, "admin.color.success");
	}

	@Override
	public String getCommand() {
		return "color";
	}

	@Override
	public int getMinimumArguments() {
		return 2;
	}

	@Override
	public String getNode() {
		return "admin.color";
	}

	@Override
	public String getHelp() {
		return "Change that teams color";
	}

	@Override
	public String getArguments() {
		return "<team> <color code>";
	}

	@Override
	public int getMaximumArguments() {
		return 2;
	}

	@Override
	public void onTabComplete(List<String> options, CommandSender sender, String label, String[] args) {
		if (args.length == 2) {
			for (NamedTextColor c : NamedTextColor.NAMES.values()) {
				String name = NamedTextColor.NAMES.key(c);
				if (name.startsWith(args[1].toLowerCase())) {
					options.add(name);
				}
			}
		} else if (args.length == 1) {
			addTeamStringList(options, args[0]);
		}
	}

}
