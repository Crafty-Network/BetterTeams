package com.booksaw.betterTeams.customEvents;

import com.booksaw.betterTeams.Team;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public final class TeamDisallowedPvPEvent extends TeamEvent {
	private static final HandlerList HANDLERS = new HandlerList();

	private final Player source;
	private final Team damagerTeam;

	private final boolean isProtected; 

	public TeamDisallowedPvPEvent(final Team victimTeam, final Player source, final Team damagerTeam, final boolean isProtected) {
		super(victimTeam, false);

		this.source = source;
		this.damagerTeam = damagerTeam;
		this.isProtected = isProtected;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}
}