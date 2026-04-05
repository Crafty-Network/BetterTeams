package com.booksaw.betterTeams;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class TeamPlayer {

	@Getter
	private final UUID playerUUID;

	@Setter
	@Getter
	private PlayerRank rank;

	@Setter
	private boolean teamChat = false;

	@Setter
	private boolean allyChat = false;

	@Setter
	@Getter
	private String title;

	@Setter
	private boolean anchor = false;

	public TeamPlayer(@NotNull OfflinePlayer player, @Nullable PlayerRank rank) {
		Objects.requireNonNull(player, "player cannot be null");
		this.playerUUID = player.getUniqueId();
		this.rank = rank != null ? rank : PlayerRank.DEFAULT;
	}

	public TeamPlayer(@NotNull OfflinePlayer player, @Nullable PlayerRank rank, @Nullable String title) {
		this(player, rank);
		this.title = title != null ? title : "";
	}

	public TeamPlayer(@NotNull OfflinePlayer player, @Nullable PlayerRank rank, @Nullable String title, boolean anchor) {
		this(player, rank, title);
		this.anchor = anchor;
	}

	public TeamPlayer(@NotNull String data) {
		String[] split = data.split(",");
		playerUUID = UUID.fromString(split[0]);
		rank = PlayerRank.valueOf(split[1]);
		if (split.length > 2) {
			title = split[2];
		}
	}

	public OfflinePlayer getPlayer() {
		return Bukkit.getOfflinePlayer(playerUUID);
	}

	public Optional<Player> getOnlinePlayer() {
		return Optional.ofNullable(Bukkit.getPlayer(playerUUID));
	}

	@Override
	public String toString() {
		if (title == null || title.isEmpty()) {
			return playerUUID + "," + rank;
		}
		return playerUUID + "," + rank + "," + title;
	}

	public boolean isInTeamChat() {
		return teamChat;
	}

	public boolean isInAllyChat() {
		return allyChat;
	}

	public boolean isAnchored() {
		return anchor;
	}

	public String getPlayerPrefix() {
		if (title == null || title.isEmpty()) {
			return rank.getPrefix();
		} else {
			return rank.getPrefix() + title + " ";
		}
	}

	public boolean isOnline() {
		return getPlayer().isOnline();
	}
}
