package com.booksaw.betterTeams.cost;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
import lombok.Getter;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;

@Getter
public class CommandCost {

	private final double cost;
	private final String command;

	public CommandCost(String command, double cost) {
		this.command = command;
		this.cost = cost;
	}

	public boolean runCommand(Player player) {
		if (player.hasPermission("betterteams.cost.bypass")) {
			return true;
		}

		if (Main.econ == null) {
			Main.plugin.getLogger().warning("Could not detect vault, command running with no cost");
			return true;
		}
		double cost = this.cost;
		if (CostManager.costFromTeam) {
			Team team = Team.getTeam(player);

			if (team != null) {
				if (team.getMoney() >= cost) {
					team.setMoney(team.getMoney() - cost);
					return true;
				} else if (team.getMoney() > 0) {
					cost -= team.getMoney();
					team.setMoney(0);
				}
			}

		}

		EconomyResponse response = Main.econ.withdrawPlayer(player, cost);
		return response.transactionSuccess();
	}

	public boolean hasBalance(Player player) {
		if (player.hasPermission("betterteams.cost.bypass")) {
			return true;
		}

		if (Main.econ == null) {
			Main.plugin.getLogger().warning("Could not detect vault, command running with no cost");
			return true;
		}

		double cost = this.cost;
		if (CostManager.costFromTeam) {
			Team team = Team.getTeam(player);

			if (team != null) {
				if (team.getMoney() >= cost) {
					return true;
				} else if (team.getMoney() > 0) {
					cost -= team.getMoney();
				}
			}
		}

		return Main.econ.has(player, cost);
	}

}
