package com.booksaw.betterTeams.commands.team;

import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
/**
* This class handles the command /team demote [player]
*
* @author booksaw
*/
import com.booksaw.betterTeams.commands.presets.TeamSubCommand;
import com.booksaw.betterTeams.message.MessageManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Objects;

public class DemoteCommand extends TeamSubCommand {

	@Override
	public CommandResponse onCommand(TeamPlayer teamPlayer, String label, String[] args, Team team) {

		TeamPlayerResult teamPlayerResult = getTeamPlayer(team, args[0]);
		if (teamPlayerResult.isCR()) {
			return teamPlayerResult.getCr();
		}

		TeamPlayer demotePlayer = teamPlayerResult.getPlayer();

		if (Objects.requireNonNull(demotePlayer).getRank() == PlayerRank.DEFAULT) {
			return new CommandResponse("demote.min");
		}

		if (demotePlayer.getRank() == PlayerRank.OWNER && team.getRank(PlayerRank.OWNER).size() == 1) {
			return new CommandResponse("demote.lastOwner");
		}
		
		if (demotePlayer.getRank().value >= teamPlayer.getRank().value
				&& !demotePlayer.getPlayer().getUniqueId().equals(teamPlayer.getPlayer().getUniqueId())) {
			
			return new CommandResponse("demote.noPerm");
		}

		if (demotePlayer.getRank() == PlayerRank.OWNER && team.isMaxAdmins()) {
			return new CommandResponse("demote.maxAdmins");
		}

		team.demotePlayer(demotePlayer);

		OfflinePlayer offlineDemotePlayer = demotePlayer.getPlayer();
		if (offlineDemotePlayer.isOnline()) {
			MessageManager.sendMessage(offlineDemotePlayer.getPlayer(), "demote.notify");
		}

		return new CommandResponse(true, "demote.success");

	}

	@Override
	public String getCommand() {
		return "demote";
	}

	@Override
	public int getMinimumArguments() {
		return 1;
	}

	@Override
	public String getNode() {
		return "demote";
	}

	@Override
	public String getHelp() {
		return "Demote the specified player within your team";
	}

	@Override
	public String getArguments() {
		return "<player>";
	}

	@Override
	public int getMaximumArguments() {
		return 1;
	}

	@Override
	public void onTabComplete(List<String> options, CommandSender sender, String label, String[] args) {
		addPlayerStringList(options, (args.length == 0) ? "" : args[0]);
	}

	@Override
	public PlayerRank getDefaultRank() {
		return PlayerRank.OWNER;
	}
}
