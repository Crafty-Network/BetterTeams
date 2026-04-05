package com.booksaw.betterTeams.customEvents.post;

import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.customEvents.TeamColorChangeEvent;
import com.booksaw.betterTeams.customEvents.TeamEvent;
import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class PostTeamColorChangeEvent extends TeamEvent {
	private final NamedTextColor oldTeamColor;
	private final NamedTextColor newTeamColor;

	public PostTeamColorChangeEvent(@NotNull Team team,
									@NotNull NamedTextColor oldTeamColor,
									@NotNull NamedTextColor newTeamColor) {
		super(team, true);

		this.oldTeamColor = oldTeamColor;
		this.newTeamColor = newTeamColor;
	}

	private static final HandlerList HANDLERS = new HandlerList();

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

}
