package com.booksaw.betterTeams.commands.team;

import com.booksaw.betterTeams.*;
import com.booksaw.betterTeams.commands.presets.TeamSubCommand;
import com.booksaw.betterTeams.events.InventoryManagement;
import org.bukkit.command.CommandSender;

import java.util.List;

public class EchestCommand extends TeamSubCommand {

	@Override
	public CommandResponse onCommand(TeamPlayer player, String label, String[] args, Team team) {

		org.bukkit.entity.Player onlinePlayer = player.getPlayer().getPlayer();
		if (onlinePlayer == null) {
			return new CommandResponse(true);
		}

		InventoryManagement.adminViewers.put(onlinePlayer, team);
		if (team.getEchest() == null || team.getEchest().getSize() == 0) {
			Main.plugin.getLogger().warning("EnderChest was found to be null or empty " + team.getEchest()
					+ " this should never occur, report to booksaw");
		}

		Main.plugin.getFoliaLib().getScheduler().runAtEntity(onlinePlayer, task -> {
			
			if (onlinePlayer.isOnline()) {
				onlinePlayer.openInventory(team.getEchest());
			} else {
				InventoryManagement.adminViewers.remove(onlinePlayer);
			}
		});

		return new CommandResponse(true);
	}

	@Override
	public String getCommand() {
		return "echest";
	}

	@Override
	public String getNode() {
		return "echest";
	}

	@Override
	public String getHelp() {
		return "View your teams ender chest";
	}

	@Override
	public String getArguments() {
		return "";
	}

	@Override
	public int getMinimumArguments() {
		return 0;
	}

	@Override
	public int getMaximumArguments() {
		return 0;
	}

	@Override
	public void onTabComplete(List<String> options, CommandSender sender, String label, String[] args) {
	}

	@Override
	public PlayerRank getDefaultRank() {
		return PlayerRank.DEFAULT;
	}

}
