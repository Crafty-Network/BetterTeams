package com.booksaw.betterTeams.commands.presets;

import com.booksaw.betterTeams.CommandResponse;
import com.booksaw.betterTeams.Team;
/**
* This class can be extended for any sub commands which require players to be
* in a team
*
* @author booksaw
*/
import com.booksaw.betterTeams.commands.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class NoTeamSubCommand extends SubCommand {

	@Override
 /**
 * This method is run if the player is not in a team
 *
 * @param player the player who is not in a team
 * @param label  the label for the command
 * @param args   the arguments for the command
 * @return the message reference to send to the user
 */
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
