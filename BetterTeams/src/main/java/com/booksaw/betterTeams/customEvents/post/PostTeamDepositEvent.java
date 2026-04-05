package com.booksaw.betterTeams.customEvents.post;

import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.customEvents.TeamDepositEvent;
import com.booksaw.betterTeams.customEvents.TeamPlayerEvent;
import lombok.Getter;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public final class PostTeamDepositEvent extends TeamPlayerEvent implements PostTeamMoneyEvent {
	private static final HandlerList HANDLERS = new HandlerList();

	private final double amount;

	public PostTeamDepositEvent(final Team team, final TeamPlayer teamPlayer, final double amount) {
		super(team, teamPlayer, false);

		this.amount = amount;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}
}
