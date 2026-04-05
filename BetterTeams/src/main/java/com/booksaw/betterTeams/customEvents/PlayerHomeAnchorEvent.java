package com.booksaw.betterTeams.customEvents;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.NotNull;

import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;

import lombok.Getter;

public class PlayerHomeAnchorEvent extends TeamPlayerEvent {

	@Getter
	private final @NotNull Location location;

	@Getter
	private final @NotNull PlayerRespawnEvent parentEvent;

    public PlayerHomeAnchorEvent(
			@NotNull Team team,
			@NotNull TeamPlayer teamPlayer,
			@NotNull Location location,
			@NotNull PlayerRespawnEvent parentEvent
	) {
		super(team, teamPlayer, false);
		this.location = location;
		this.parentEvent = parentEvent;
	}

	private static final HandlerList HANDLERS = new HandlerList();

	@SuppressWarnings("unused")
	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}
}
