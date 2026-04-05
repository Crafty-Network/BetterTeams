package com.booksaw.betterTeams.customEvents;

import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.team.controller.TeamMessageController;
import com.booksaw.betterTeams.util.StringUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
public class TeamSendMessageEvent extends TeamPlayerEvent {

	private String rawMessage;
	
	private String format;
	
	private String senderNamePrefix;

	private final TeamMessageController.TeamMessageType messageType;

	private final Set<TeamPlayer> recipients = new HashSet<>();

	public TeamSendMessageEvent(@NotNull Team team,
								@NotNull TeamPlayer sender,
								@NotNull String rawMessage,
								@NotNull String proposedFormat,
								@NotNull String senderNamePrefix,
								@NotNull Collection<TeamPlayer> recipients,
								TeamMessageController.TeamMessageType messageType) {
		super(team, sender, true);
		this.rawMessage = rawMessage;
		this.format = proposedFormat;
		this.senderNamePrefix = senderNamePrefix;
		this.recipients.addAll(recipients);
		this.messageType = messageType;
	}

	public String getFormattedMessage() {
		return StringUtil.setPlaceholders(getFormat(), getFormattedSenderName(), getRawMessage());
	}

	public String getFormattedSenderName() {
		return getSenderNamePrefix() + Objects.requireNonNull(getSender().getPlayer().getPlayer()).getDisplayName();
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
