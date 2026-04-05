package com.booksaw.betterTeams.commands.presets;

import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.commands.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public abstract class TeamSelectSubCommand extends SubCommand {

	@Override
	public CommandResponse onCommand(CommandSender sender, String label, String[] args) {

		if (args.length == 0) {
			return new CommandResponse("help");
		}

		Team team = resolveTeam(args[0]);

		if (team == null) {
			return new CommandResponse("admin.noTeam");
		}
		return onCommand(sender, label, args, team);
	}

	public abstract CommandResponse onCommand(CommandSender sender, String label, String[] args, Team team);

	@Nullable
	protected Team resolveTeam(String identifier) {
		if (identifier == null || identifier.isEmpty()) {
			return null;
		}

		Team team = Team.getTeam(identifier);

		if (team == null) {
			Player player = Bukkit.getPlayer(identifier);
			if (player != null) {
				team = Team.getTeam(player);
			}
		}
		return team;
	}
}
