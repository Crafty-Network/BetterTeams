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

/**
* This class is used to store all the information about a user in a team
*
* @author booksaw
*/
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

 /**
 * Used to create a new player
 *
 * @param player the player that is associated with this object
 * @param rank   the rank that the player has
 */
	public TeamPlayer(@NotNull OfflinePlayer player, @Nullable PlayerRank rank) {
		Objects.requireNonNull(player, "player cannot be null");
		this.playerUUID = player.getUniqueId();
		this.rank = rank != null ? rank : PlayerRank.DEFAULT;
	}

 /**
 * Used to load player information relating to that player
 *
 * @param data the data for the player (uuid,rank,title)
 */
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

 /**
 * @return The player which is associated with this object
 */
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

 /**
 * @return the player prefix for messages that the player has sent
 */
	public String getPlayerPrefix() {
		if (title == null || title.isEmpty()) {
			return rank.getPrefix();
		} else {
			return rank.getPrefix() + title + " ";
		}
	}

 /**
 * Checks if the player associated with this TeamPlayer is currently online.
 *
 * @return true if the player is online, false otherwise
 */
	public boolean isOnline() {
		return getPlayer().isOnline();
	}
}
