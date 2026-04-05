package com.booksaw.betterTeams.customEvents.post;

import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.customEvents.TeamPlayerEvent;
import com.booksaw.betterTeams.team.controller.TeamMessageController;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Set;

@Getter
public class PostTeamSendMessageEvent extends TeamPlayerEvent {

	@NotNull
	private final String formattedMessage;
	
	@Unmodifiable
	private final Set<TeamPlayer> recipients;

	private final TeamMessageController.TeamMessageType messageType;

	public PostTeamSendMessageEvent(@NotNull Team team,
									@NotNull TeamPlayer sender,
									@NotNull String formattedMessage,
									@NotNull Collection<TeamPlayer> recipients,
									TeamMessageController.TeamMessageType messageType) {
		super(team, sender, true);

		this.formattedMessage = formattedMessage;
		this.recipients = ImmutableSet.copyOf(recipients);
		this.messageType = messageType;
	}

	public TeamPlayer getSender() {
		return getTeamPlayer();
	}

	private static final HandlerList HANDLERS = new HandlerList();

	@SuppressWarnings("unused")
	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

}
