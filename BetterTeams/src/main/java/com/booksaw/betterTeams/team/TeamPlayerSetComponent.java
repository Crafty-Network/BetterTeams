package com.booksaw.betterTeams.team;

import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.message.Message;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class TeamPlayerSetComponent extends SetTeamComponent<TeamPlayer> {

	public List<Player> getOnlinePlayers() {
		return getClone().stream()
				.map(TeamPlayer::getPlayer)

				.map(OfflinePlayer::getPlayer)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	public List<OfflinePlayer> getOfflinePlayers() {
		return getClone().stream()
				.map(TeamPlayer::getPlayer)
				.filter(p -> !p.isOnline())
				.collect(Collectors.toList());
	}

	public List<TeamPlayer> getOnlineTeamPlayers() {
		return getClone().stream()
				.filter(TeamPlayer::isOnline)
				.collect(Collectors.toList());
	}

	@Nullable
	public TeamPlayer getTeamPlayer(@NotNull OfflinePlayer p) {
		return getClone().stream()
				.filter(teamPlayer -> p.getUniqueId().equals(teamPlayer.getPlayer().getUniqueId()))
				.findFirst()
				.orElse(null);
	}

	public List<TeamPlayer> getRank(PlayerRank rank) {
		return getClone().stream()
				.filter(player -> player.getRank() == rank)
				.collect(Collectors.toList());
	}

	public void broadcastMessage(@NotNull Message message) {
		message.sendMessage(getOnlinePlayers());
	}

	public void broadcastTitle(@NotNull Message message) {
		message.sendTitle(getOnlinePlayers());
	}

	@Override
	public TeamPlayer fromString(String str) {
		return new TeamPlayer(str);
	}

	@Override
	public String toString(@NotNull TeamPlayer component) {
		return component.toString();
	}

	@Override
	public boolean contains(@NotNull TeamPlayer component) {
		return contains(component.getPlayer());
	}

	public boolean contains(OfflinePlayer player) {
		return getTeamPlayer(player) != null;
	}

	private String getPlayersString(@NotNull List<? extends OfflinePlayer> players) {
		return players.stream().map(p -> p.getName()).collect(Collectors.joining(", "));
	}

	public String getOnlinePlayersString() {
		return getPlayersString(getOnlinePlayers());
	}

	public String getOfflinePlayersString() {
		return getPlayersString(getOfflinePlayers());
	}
}