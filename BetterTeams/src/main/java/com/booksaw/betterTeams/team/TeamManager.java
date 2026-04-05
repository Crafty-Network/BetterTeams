package com.booksaw.betterTeams.team;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.customEvents.CreateTeamEvent;
import com.booksaw.betterTeams.customEvents.PurgeEvent;
import com.booksaw.betterTeams.customEvents.post.PostCreateTeamEvent;
import com.booksaw.betterTeams.customEvents.post.PostPurgeEvent;
import com.booksaw.betterTeams.events.ChestManagement;
import com.booksaw.betterTeams.team.storage.team.TeamStorage;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class TeamManager {
	
	protected final ConcurrentHashMap<UUID, Team> loadedTeams;

	@Getter
	private final boolean logChat;

	protected TeamManager() {
		logChat = Main.plugin.getConfig().getBoolean("logTeamChat");

		loadedTeams = new ConcurrentHashMap<>();

	}

	public Map<UUID, Team> getLoadedTeamListClone() {
		return new HashMap<>(loadedTeams);
	}

	@Nullable
	@Contract(pure = true, value = "null -> null")
	public Team getTeam(@Nullable UUID uuid) {
		if (uuid == null) {
			return null;
		}

		if (loadedTeams.containsKey(uuid)) {
			return loadedTeams.get(uuid);
		}

		if (!isTeam(uuid)) {
			return null;
		}

		try {
			return new Team(uuid);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	@Nullable
	@Contract(pure = true, value = "null -> null")
	public Team getTeam(@Nullable String name) {
		if (name == null)
			return null;

		Team team = getTeamByName(name);
		if (team != null) {
			return team;
		}

		OfflinePlayer player = Bukkit.getPlayer(name);
		if (player == null) {
			return null;
		}
		return getTeam(player);

	}

	@Nullable
	@Contract(pure = true, value = "null -> null")
	public Team getTeam(@Nullable OfflinePlayer player) {
		if (player == null) {
			return null;
		}

		Optional<Team> possibleTeam = loadedTeams.values().stream().filter(team -> team.getMembers().contains(player)).findFirst();
		if (possibleTeam.isPresent()) {
			return possibleTeam.get();
		}

		if (!isInTeam(player)) {
			return null;
		}

		UUID uuid = getTeamUUID(player);
		if (uuid == null) {
			return null;
		}

		return getTeam(uuid);
	}

	@Nullable
	public Team getTeamByName(@NotNull String name) {
		if (!isTeam(name)) {
			return null;
		}

		UUID uuid = getTeamUUID(name);

		if (uuid == null) {
			return null;
		}

		return getTeam(uuid);
	}

	public Team createNewTeam(String name, Player owner) {

		UUID id = UUID.randomUUID();
		
		while (getTeam(id) != null) {
			id = UUID.randomUUID();
		}
		Team team = new Team(name, id, owner);

		CreateTeamEvent event = new CreateTeamEvent(team, owner);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled()) {
			throw new IllegalArgumentException("Creating team was cancelled by another plugin");
		}

		loadedTeams.put(id, team);
		registerNewTeam(team, owner);

		if (Main.plugin.teamManagement != null && owner != null) {
			Main.plugin.teamManagement.displayBelowName(owner);
		}

		Bukkit.getPluginManager().callEvent(new PostCreateTeamEvent(team, owner));

		return team;
	}

	public Team getClaimingTeam(Location location) {
		UUID claimingTeam = getClaimingTeamUUID(location);

		if (claimingTeam == null) {
			return null;
		}

		if (!isTeam(claimingTeam)) {
			return null;
		}

		return getTeam(claimingTeam);
	}

	public abstract UUID getClaimingTeamUUID(Location location);

	public Team getClaimingTeam(Block block) {
		
		if (block.getType() != Material.CHEST) return null; 

		Location location1 = block.getLocation();
		Location location2 = ChestManagement.getOtherSide(block);

		if (location2 == null) {
			return null;
		}

		if (ChestManagement.isSingleChest(location1, location2)) {
			return getClaimingTeam(location1);
		}

		Team claimedBy = getClaimingTeam(location1);
		if (claimedBy != null) return claimedBy;

		return getClaimingTeam(location2);
	}

	public Location getClaimingLocation(Block block) {
		
		if (block.getType() != Material.CHEST) return null;

		Location location1 = block.getLocation();
		Location location2 = ChestManagement.getOtherSide(block);

		if (location2 == null) {
			return null;
		}

		if (ChestManagement.isSingleChest(location1, location2)) {
			Team claimedBy = getClaimingTeam(location1);
			if (claimedBy != null) return location1;
		} else {
			Team claimedBy = getClaimingTeam(location1);
			if (claimedBy != null) return location1;

			claimedBy = getClaimingTeam(location2);
			if (claimedBy != null) return location2;
		}
		return null;
	}

	public boolean purgeTeams(boolean money, boolean score) {
		
		PurgeEvent event = new PurgeEvent();

		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return false;
		}

		if (score) {
			Main.plugin.getLogger().info("purging team score");
			purgeTeamScore();
		}
		if (money) {
			Main.plugin.getLogger().info("purging team score");
			purgeTeamMoney();
		}

		Bukkit.getPluginManager().callEvent(new PostPurgeEvent());
		return true;
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	@Contract(pure = true, value = "null -> false")
	public abstract boolean isTeam(@Nullable UUID uuid);

	@Contract(pure = true, value = "null -> false")
	public abstract boolean isTeam(@Nullable String name);

	public abstract boolean isInTeam(OfflinePlayer player);

	public abstract UUID getTeamUUID(OfflinePlayer player);

	public abstract UUID getTeamUUID(String name);

	public abstract void loadTeams();

	public boolean isLoaded(UUID teamUUID) {
		return loadedTeams.containsKey(teamUUID);
	}

	protected abstract void registerNewTeam(Team team, Player player);

	public void disbandTeam(Team team) {
		loadedTeams.remove(team.getID());

		if (team.getName() != null) {
			deleteTeamStorage(team);
		}
	}

	protected abstract void deleteTeamStorage(Team team);

	public abstract void teamNameChange(Team team, String newName);

	public abstract void playerJoinTeam(Team team, TeamPlayer player);

	public abstract void playerLeaveTeam(Team team, TeamPlayer player);

	public abstract TeamStorage createTeamStorage(Team team);

	public abstract TeamStorage createNewTeamStorage(Team team);

	public abstract String[] sortTeamsByScore();

	public abstract String[] sortTeamsByBalance();

	public abstract String[] sortTeamsByMembers();

	public abstract void purgeTeamScore();

	public abstract void purgeTeamMoney();

	public abstract List<String> getHoloDetails();

	public abstract void setHoloDetails(List<String> details);

	public abstract void addChestClaim(Team team, Location loc);

	public abstract void removeChestclaim(Location loc);

	public abstract void rebuildLookups();

	public void disable() {
	}
}