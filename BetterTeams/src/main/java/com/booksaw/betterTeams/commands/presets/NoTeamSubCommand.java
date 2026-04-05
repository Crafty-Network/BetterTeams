package com.booksaw.betterTeams.commands.presets;

import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.commands.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class NoTeamSubCommand extends SubCommand {

	@Override
	public CommandResponse onCommand(CommandSender sender, String label, String[] args) {
		Player player = (Player) sender;
		Team team = Team.getTeam(player);

		if (team != null) {
			return new CommandResponse("notInTeam");
		}
		return onCommand(player, label, args);
	}

	public abstract CommandResponse onCommand(Player player, String label, String[] args);

	@Override
	public boolean needPlayer() {
		return true;
	}
}
