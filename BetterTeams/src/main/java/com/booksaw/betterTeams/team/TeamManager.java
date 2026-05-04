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

/**
* Used to create a new teamManager
*/
public abstract class TeamManager {
	
 /**
 * A list of all teams
 */
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

  /**
  * Used to get the uuid of the team that the specified player is in
  *
  * @param player the plyaer to check for
  * @return The team uuid
  */
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

  /**
  * Used to get the team uuid from the team name
  *
  * @param name The name of the team
  * @return The UUID of the specified team
  */
		UUID uuid = getTeamUUID(name);

		if (uuid == null) {
			return null;
		}

		return getTeam(uuid);
	}

 /**
 * This method is used to create a new team with the specified name
 * <p>
 * Checks are not carried out to ensure that the name is available, so that
 * should be done before this method is called
 * </p>
 *
 * @param name  the name of the new team
 * @param owner the owner of the new team (the player who ran /team create)
 * @return The created team
 */
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
  /**
  * Called when a new team is registered, this can be used to register it in any
  * full team trackers The team file will be fully prepared with the members
  * within the team
  *
  * @param team   The new team
  * @param player The player that created the team
  */
		registerNewTeam(team, owner);

		if (Main.plugin.teamManagement != null && owner != null) {
			Main.plugin.teamManagement.displayBelowName(owner);
		}

		Bukkit.getPluginManager().callEvent(new PostCreateTeamEvent(team, owner));

		return team;
	}

 /**
 * Used to get the team which has claimed the provided chest, will return null
 * if that location is not claimed
 *
 * @param location the location of the chest - must already be normalised
 * @return The team which has claimed that chest
 */
	public Team getClaimingTeam(Location location) {
  /**
  * Used to get the UUID of the team which has claimed the provided chest, will
  * return null if that location is not claimed
  *
  * @param location The location of the chest - must already be normalised
  * @return the team which has claimed that chest
  */
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

 /**
 * Used to get the claiming team of a chest, will check both parts of a double
 * chest, it is assumed that the provided block is known to be a chest
 *
 * @param block The block being checked
 * @return The team which has claimed that block
 */
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

 /**
 * Used to get the claiming location, will check both parts of a double chest,
 * it is assumed that the provided block is known to be a chest
 *
 * @param block Part of the chest
 * @return The location of the claim
 */
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

 /**
 * Used to reset all teams scores to 0
 *
 * @return If the teams were purged or not
 */
	public boolean purgeTeams(boolean money, boolean score) {
		
		PurgeEvent event = new PurgeEvent();

		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return false;
		}

		if (score) {
			Main.plugin.getLogger().info("purging team score");
   /**
   * Used to reset the score of all teams
   */
			purgeTeamScore();
		}
		if (money) {
			Main.plugin.getLogger().info("purging team score");
   /**
   * Used to reset the balance of all teams
   */
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

 /**
 * Used to check if the specified player is in a team
 *
 * @param player The player to check
 * @return If they are in a team
 */
	public abstract boolean isInTeam(OfflinePlayer player);

	public abstract UUID getTeamUUID(OfflinePlayer player);

	public abstract UUID getTeamUUID(String name);

 /**
 * Used to load the stored values into the storage manager
 */
	public abstract void loadTeams();

	public boolean isLoaded(UUID teamUUID) {
		return loadedTeams.containsKey(teamUUID);
	}

	protected abstract void registerNewTeam(Team team, Player player);

 /**
 * Used to disband a team
 *
 * @param team The team that is being disbanded
 */
	public void disbandTeam(Team team) {
		loadedTeams.remove(team.getID());

		if (team.getName() != null) {
   /**
   * Used when a team is disbanded, can be used to remove it from any team
   * trackers
   *
   * @param team The team that is being disbanded
   */
			deleteTeamStorage(team);
		}
	}

	protected abstract void deleteTeamStorage(Team team);

 /**
 * Called when a team changes its name as this will effect the getTeam(String
 * teamName) method
 *
 * @param team    The new team
 * @param newName The name the team has changed to
 */
	public abstract void teamNameChange(Team team, String newName);

 /**
 * Called when a player joins a team, this can be used to track the players
 * location
 *
 * @param team   The team that the player has joined
 * @param player The player that has joined the team
 */
	public abstract void playerJoinTeam(Team team, TeamPlayer player);

 /**
 * Called when a player leaves a team
 *
 * @param team   The team that the player has left
 * @param player The team that the player has left
 */
	public abstract void playerLeaveTeam(Team team, TeamPlayer player);

 /**
 * Called when a team needs a storage manager to manage all information, this is
 * called for preexisting teams
 *
 * @param team The team instance
 * @return The created team storage
 */
	public abstract TeamStorage createTeamStorage(Team team);

 /**
 * Called when a new team is made
 *
 * @param team The team
 * @return The created team storage
 */
	public abstract TeamStorage createNewTeamStorage(Team team);

 /**
 * This method is used to sort all the teams into an array ranking from highest
 * score to lowest
 *
 * @return the array of teams in order of their rank
 */
	public abstract String[] sortTeamsByScore();

 /**
 * This method is used to sort all the team names into an array ranking from
 * highest to lowest
 *
 * @return The sorted array
 */
	public abstract String[] sortTeamsByBalance();

 /**
 * Used to sort all members from largest to smallest by number of members
 *
 * @return the sorted array
 */
	public abstract String[] sortTeamsByMembers();

	public abstract void purgeTeamScore();

	public abstract void purgeTeamMoney();

 /**
 * @return The stored hologram details
 */
	public abstract List<String> getHoloDetails();

 /**
 * Used to store and save the updated hologram details
 *
 * @param details the details to save
 */
	public abstract void setHoloDetails(List<String> details);

	public abstract void addChestClaim(Team team, Location loc);

	public abstract void removeChestclaim(Location loc);

 /**
 * Can be called by a config option if the server is having difficulties. Do not
 * call from anywhere else as it may cause problems depending on the storage
 * type
 */
	public abstract void rebuildLookups();

 /**
 * this can be overritten if any code needs to be run when onDisable is called
 */
	public void disable() {
	}
}