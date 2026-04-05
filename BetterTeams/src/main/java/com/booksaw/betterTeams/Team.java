package com.booksaw.betterTeams;

import com.booksaw.betterTeams.customEvents.*;
import com.booksaw.betterTeams.customEvents.post.*;
import com.booksaw.betterTeams.exceptions.CancelledEventException;
import com.booksaw.betterTeams.message.Message;
import com.booksaw.betterTeams.message.MessageManager;
import com.booksaw.betterTeams.message.ReferencedFormatMessage;
import com.booksaw.betterTeams.team.*;
import com.booksaw.betterTeams.team.AnchoredPlayerUUIDSetComponent.AnchorResult;
import com.booksaw.betterTeams.team.controller.TeamMessageController;
import com.booksaw.betterTeams.team.level.LevelManager;
import com.booksaw.betterTeams.team.level.TeamLevel;
import com.booksaw.betterTeams.team.storage.StorageType;
import com.booksaw.betterTeams.team.storage.team.StoredTeamValue;
import com.booksaw.betterTeams.team.storage.team.TeamStorage;
import com.booksaw.betterTeams.text.LegacyTextUtils;
import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

public class Team {

	private static TeamManager TEAMMANAGER;

	public static void setupTeamManager(StorageType storageType) {
		if (TEAMMANAGER != null) {
			throw new IllegalArgumentException("The team manager has already been setup");
		}

		TEAMMANAGER = storageType.getNewTeamManager();

		if (Main.plugin.getConfig().getBoolean("rebuildLookups")) {

			TEAMMANAGER.rebuildLookups();

			Main.plugin.getConfig().set("rebuildLookups", false);
			Main.plugin.saveConfig();
		}

	}

	public static void disable() {
		TEAMMANAGER.disable();
		TEAMMANAGER = null;
	}

	public static TeamManager getTeamManager() {
		return TEAMMANAGER;
	}

	@Contract(pure = true, value = "null -> null")
	public static @Nullable Team getTeam(@Nullable UUID uuid) {
		return TEAMMANAGER.getTeam(uuid);
	}

	@Contract(pure = true, value = "null -> null")
	public static @Nullable Team getTeam(@Nullable String name) {
		return TEAMMANAGER.getTeam(name);
	}

	@Contract(pure = true, value = "null -> null")
	public static @Nullable Team getTeam(@Nullable OfflinePlayer player) {
		return TEAMMANAGER.getTeam(player);
	}

	public static Team getTeamByName(String name) {
		return TEAMMANAGER.getTeamByName(name);
	}

	public static Team getClaimingTeam(Block block) {
		return TEAMMANAGER.getClaimingTeam(block);
	}

	public static Team getClaimingTeam(Location location) {
		return TEAMMANAGER.getClaimingTeam(location);
	}

	public static Location getClaimingLocation(Block block) {
		if (block.getType() != Material.CHEST) {
			return null;
		}
		return TEAMMANAGER.getClaimingLocation(block);
	}

	public static boolean canOpenAllyChests() {
		return Main.plugin.getConfig().getBoolean("allowAllyChests");
	}

	@Contract("null -> false")
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean isValidTeamName(@Nullable String name) {
		if (name == null) {
			return false;
		}

		for (String temp : Main.plugin.getConfig().getStringList("blacklist")) {
			if (temp.equalsIgnoreCase(name.toLowerCase())) {
				return false;
			}
		}

		String chars = Main.plugin.getConfig().getString("bannedChars");
		if (chars != null) {
			for (char temp : chars.toCharArray()) {
				if (name.contains(Character.toString(temp))) {
					return false;
				}
			}
		}

		if (!name.equals(LEGACY_COLOR_CODE_PATTERN.matcher(name).replaceAll(""))) {
			return false;
		}

		if (name.contains("&") || name.contains(":")) {
			return false;
		}

		String allowed = Main.plugin.getConfig().getString("allowedChars");

		if (allowed != null && !allowed.isEmpty()) {
			for (char temp : name.toCharArray()) {
				if (!allowed.contains(Character.toString(temp))) {
					return false;
				}
			}
		}

		return true;
	}

	@Getter
	private final TeamStorage storage;

	private volatile boolean dirty = false;

	public void markDirty() { this.dirty = true; }
	public boolean isDirty() { return dirty; }
	public void clearDirty() { this.dirty = false; }

	private final UUID id;

	@Getter
	private volatile String name;

	@Getter
	private volatile String description;

	@Getter
	private volatile boolean open;

	@Getter
	private Location teamHome = null;

	@Getter
	@NotNull
	private final MemberSetComponent members = new MemberSetComponent();

	@Getter
	private final AnchoredPlayerUUIDSetComponent anchoredPlayers = new AnchoredPlayerUUIDSetComponent();
	
	@Getter
	private final AllySetComponent allies = new AllySetComponent();

	@Getter
	private final List<UUID> invitedPlayers = new ArrayList<>();

	@Getter
	private final BanSetComponent bannedPlayers = new BanSetComponent();

	private final ChestClaimComponent claims = new ChestClaimComponent();

	private final ScoreComponent score = new ScoreComponent();

	private final MoneyComponent money = new MoneyComponent();

	@Getter
	private final TeamMessageController teamMessageController = new TeamMessageController(this);

	@Getter
	private volatile boolean pvp = false;

	private boolean useTeamHomeAsAnchor = false;

	@Getter
	private volatile NamedTextColor color = null;

	private static final Pattern LEGACY_COLOR_CODE_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");
	
	private int rank = -1;

	private int balRank;

	private final AllyRequestComponent allyRequests = new AllyRequestComponent();

	private final EChestComponent echest = new EChestComponent();

	@Getter
	private volatile int level;

	private volatile String tag;

	@Getter
	private final WarpSetComponent warps = new WarpSetComponent();

	private org.bukkit.scoreboard.Team team;

	@Getter
	private final MetaComponent meta = new MetaComponent();

	public Team(UUID id) {
		this.id = id;

		storage = TEAMMANAGER.createTeamStorage(this);

		name = storage.getString(StoredTeamValue.NAME);

		if (name == null) {
			
			getTeamManager().disbandTeam(this);

			throw new IllegalArgumentException(
					"The team that attempted loading is invalid, disbanding the team to avoid problems");
		}

		description = storage.getString(StoredTeamValue.DESCRIPTION);
		open = storage.getBoolean(StoredTeamValue.OPEN);
		pvp = storage.getBoolean(StoredTeamValue.PVP);
		useTeamHomeAsAnchor = storage.getBoolean(StoredTeamValue.ANCHOR);

		String colorStr = Optional.ofNullable(storage.getString(StoredTeamValue.COLOR)).orElse("6");

		if (colorStr.isEmpty()) {
			colorStr = "6";
		}

		color = Optional.ofNullable(LegacyTextUtils.namedColorByChar(colorStr.charAt(0))).orElse(NamedTextColor.GOLD);

		members.load(storage);
		anchoredPlayers.load(storage);
		allies.load(storage);
		score.load(storage);
		money.load(storage);
		echest.load(storage);
		bannedPlayers.load(storage);
		meta.load(storage);

		String teamHomeStr = storage.getString(StoredTeamValue.HOME);
		if (teamHomeStr != null && !teamHomeStr.isEmpty()) {
			teamHome = LocationSetComponent.getLocation(teamHomeStr);
		}
		allyRequests.load(storage);
		warps.load(storage);

		try {
			claims.load(storage);
		} catch (IllegalArgumentException e) {
			Main.plugin.getLogger().severe("Invalid location stored in the file for the team with the ID " + id + ", " + e.getMessage());
		}

		level = storage.getInt(StoredTeamValue.LEVEL);
		if (level < 1) {
			level = 1;
		}

		tag = Optional.ofNullable(storage.getString(StoredTeamValue.TAG)).orElse("");
	}

	public Team(String name, UUID id, Player owner) {
		this.id = id;

		if (name == null) {
			Main.plugin.getLogger()
					.warning("Provided team name was null, this should never occur. Team uuid = " + id);
			name = "invalidName";

			try {
				throw new IllegalArgumentException();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		this.name = name;

		storage = TEAMMANAGER.createNewTeamStorage(this);

		storage.set(StoredTeamValue.NAME, name);
		storage.set(StoredTeamValue.DESCRIPTION, "");

		this.description = "";

		storage.set(StoredTeamValue.OPEN, false);
		open = false;

		storage.set(StoredTeamValue.PVP, false);
		pvp = false;

		storage.set(StoredTeamValue.ANCHOR, false);
		useTeamHomeAsAnchor = false;

		storage.set(StoredTeamValue.HOME, "");
		rank = -1;

		String colorStr = Main.plugin.getConfig().getString("defaultColor", "6");
		if (colorStr.isEmpty()) colorStr = "6";
		color = Optional.ofNullable(LegacyTextUtils.namedColorByChar(colorStr.charAt(0))).orElse(NamedTextColor.GOLD);
		storage.set(StoredTeamValue.COLOR, LegacyTextUtils.namedColorToChar(color));

		claims.save(storage);
		if (owner != null) {
			members.add(this, new TeamPlayer(owner, PlayerRank.OWNER));
		}

		savePlayers();
		saveAnchoredPlayers();
		level = 1;
		storage.set(StoredTeamValue.LEVEL, 1);
		tag = "";
		storage.set(StoredTeamValue.TAG, "");
		
	}

	public void setName(String name, Player playerSource) {
		final String previousName = this.name;

		TeamNameChangeEvent event = new TeamNameChangeEvent(this, name, playerSource);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled()) {
			throw new IllegalArgumentException("Renaming was cancelled by another plugin");
		}
		name = event.getNewTeamName();

		TEAMMANAGER.teamNameChange(this, name);
		this.name = name;
		getStorage().set(StoredTeamValue.NAME, name);

		registerTeamName();

		Bukkit.getPluginManager().callEvent(new PostTeamNameChangeEvent(this, previousName, name, playerSource));
	}

	private void registerTeamName() {
		if (Main.plugin.teamManagement != null) {
			if (team != null) {
				for (TeamPlayer p : members.getClone()) {

					Player online = p.getPlayer().getPlayer();
					if (online != null) {
						team.removeEntry(online.getName());
					}
				}
				team.unregister();
			}

			team = null;

			for (TeamPlayer p : members.getClone()) {
				Player online = p.getPlayer().getPlayer();
				if (online != null) {
					Main.plugin.teamManagement.displayBelowName(online);
				}
			}
		}
	}

	public @NotNull String getOpenColor() {
		if (color == null) return "";
		return "<" + NamedTextColor.NAMES.key(color) + ">";
	}

	public @NotNull String getCloseColor() {
		if (color == null) return "";
		return "</" + NamedTextColor.NAMES.key(color) + ">";
	}

	public @NotNull String getAdventureDisplayName() {
		return getAdventureDisplayName(false);
	}

	public @NotNull String getAdventureDisplayName(boolean checkConfig) {
		boolean doColor = !checkConfig || Main.plugin.getConfig().getBoolean("colorTeamName", true);
		return (doColor ? getOpenColor() : "")
				+ getName()
				+ (doColor ? getCloseColor() : "");
	}

	public @NotNull String getDisplayName(@Nullable NamedTextColor resetTo) {
		return getDisplayName(resetTo, true);
	}

	public @NotNull String getDisplayName(@Nullable NamedTextColor resetTo, boolean asAdventure) {
		if (resetTo == null) {
			return name;
		} else if (asAdventure) {
			String resetTag = "<" + NamedTextColor.NAMES.key(resetTo) + ">";
			return getAdventureDisplayName(true) + resetTag;
		} else {
			String legacyCode = "§" + LegacyTextUtils.namedColorToChar(resetTo);
			return getDisplayName(false) + legacyCode;
		}
	}

	public @NotNull String getDisplayName() {
		return getDisplayName(true);
	}

	public @NotNull String getDisplayName(boolean asAdventure) {
		if (asAdventure) {
			return getAdventureDisplayName(true);
		} else {
			return (color != null && Main.plugin.getConfig().getBoolean("colorTeamName", true)
				? "§" + LegacyTextUtils.namedColorToChar(color) : "")
				+ name;
		}
	}

	public String getAdventureTag() {
		return getAdventureTag(false);
	}

	public String getAdventureTag(boolean checkConfig) {
		if (tag == null || tag.isEmpty()) return getAdventureDisplayName(checkConfig);
		boolean doColor = !checkConfig || Main.plugin.getConfig().getBoolean("colorTeamName", true);
		return (doColor ? getOpenColor() : "")
				+ tag
				+ (doColor ? getCloseColor() : "");
	}

	public String getTag() {
		return getTag(true);
	}

	public String getTag(boolean asAdventure) {
		if (asAdventure) return getAdventureTag(true);
		else return tag == null || tag.isEmpty() ? getDisplayName() :
				(color != null && Main.plugin.getConfig().getBoolean("colorTeamName", true)
						? "§" + LegacyTextUtils.namedColorToChar(color) : "")
						+ tag;
	}

	public String getTag(@Nullable NamedTextColor returnTo) {
		return getTag(returnTo, true);
	}

	public String getTag(@Nullable NamedTextColor returnTo, boolean asAdventure) {
		if (returnTo == null) {
			return getOriginalTag();
		} else if (tag == null || tag.isEmpty()) {
			return getDisplayName(asAdventure);
		} else
			if (asAdventure) {
				return getTag(true) + "<" + NamedTextColor.NAMES.key(returnTo) + ">";
			} else {
				return getTag(false) + "§" + LegacyTextUtils.namedColorToChar(returnTo);
			}
	}

	public String getOriginalTag() {
		return tag != null ? tag : "";
	}

	public void setTag(String tag) {
		final String oldTag = getTag();

		TeamTagChangeEvent event = new TeamTagChangeEvent(this, tag);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled()) {
			throw new IllegalArgumentException("Changing tag was cancelled by another plugin");
		}
		tag = Optional.ofNullable(event.getNewTeamTag()).orElse("");

		this.tag = tag;
		getStorage().set(StoredTeamValue.TAG, tag);

		registerTeamName();

		Bukkit.getPluginManager().callEvent(new PostTeamTagChangeEvent(this, oldTag, getTag(false)));
	}

	public void setOpen(boolean open) {
		this.open = open;
		getStorage().set(StoredTeamValue.OPEN, open);
	}

	public void setDescription(String description) {
		this.description = description;
		getStorage().set(StoredTeamValue.DESCRIPTION, description);
	}

	public void setColor(NamedTextColor color) {
		TeamColorChangeEvent event = new TeamColorChangeEvent(this, color);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled()) {
			throw new IllegalArgumentException("Recoloring was cancelled by another plugin");
		}

		color = event.getNewTeamColor();

		final NamedTextColor oldColor = getColor();
		this.color = color;
		getStorage().set(StoredTeamValue.COLOR, LegacyTextUtils.namedColorToChar(color));

		registerTeamName();

		Bukkit.getPluginManager().callEvent(new PostTeamColorChangeEvent(this, oldColor, color));
	}

	private void savePlayers() {
		members.save(getStorage());
	}

	private void saveAnchoredPlayers() {
		anchoredPlayers.save(getStorage());
	}

	private void saveBans() {
		bannedPlayers.save(getStorage());
	}

	public boolean removePlayer(OfflinePlayer p) {
		return removePlayer(getTeamPlayer(p));
	}

	public boolean removePlayer(TeamPlayer p) {
		try {
			members.remove(this, p);
		} catch (CancelledEventException e) {
			return false;
		}

		savePlayers();
		if (p.isAnchored()) {
			anchoredPlayers.remove(this, p.getPlayerUUID());
			saveAnchoredPlayers();
		}

		if (team != null && p.getPlayer().isOnline()) {
			Main.plugin.teamManagement.remove(p.getPlayer().getPlayer());
		}

		return true;
	}

	public boolean isPlayerAnchored(OfflinePlayer p) {
		return isPlayerAnchored(getTeamPlayer(p));
	}

	public boolean isPlayerAnchored(TeamPlayer p) {
		return anchoredPlayers.getClone().contains(p.getPlayerUUID());
	}

	public AnchorResult setPlayerAnchor(OfflinePlayer p, boolean anchor) {
		return setPlayerAnchor(getTeamPlayer(p), anchor);
	}

	public AnchorResult setPlayerAnchor(TeamPlayer p, boolean anchor) {
		return anchor ? anchorPlayer(p) : unanchorPlayer(p);
	}

	public AnchorResult anchorPlayer(TeamPlayer p) {
		AnchorResult result = anchoredPlayers.add(this, p);
		if (result == AnchorResult.SUCCESS) {
			getStorage().setAnchor(p, true);
			saveAnchoredPlayers();
		}
		return result;
	}

	public AnchorResult unanchorPlayer(TeamPlayer p) {
		AnchorResult result = anchoredPlayers.remove(this, p);
		if (result == AnchorResult.SUCCESS) {
			getStorage().setAnchor(p, false);
			saveAnchoredPlayers();
		}
		return result;
	}

	@Nullable
	public TeamPlayer getTeamPlayer(OfflinePlayer player) {
		if (player == null) {
			return null;
		}
		return members.getTeamPlayer(player);
	}

	public List<TeamPlayer> getRank(PlayerRank rank) {
		return members.getRank(rank);
	}

	public void disband() {
		disband(null);
	}

	public void disband(Player player) {
		DisbandTeamEvent event = new DisbandTeamEvent(this, player);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled()) {
			throw new IllegalArgumentException("Disbanding was cancelled by another plugin");
		}

		final Set<UUID> alliesClone = allies.getClone();
		final Set<TeamPlayer> membersClone = members.getClone();

		alliesClone.forEach(uuid -> {
			Team team = Team.getTeam(uuid);
			if (team != null) team.becomeNeutral(this, false);
		});

		for (TeamPlayer teamPlayer : membersClone) {
			getTeamManager().playerLeaveTeam(this, teamPlayer);
		}

		getTeamManager().disbandTeam(this);

		if (Main.plugin.teamManagement != null) {
			for (TeamPlayer p : membersClone) {
				if (p.getPlayer().isOnline()) {
					Main.plugin.teamManagement.remove(p.getPlayer().getPlayer());
				}
			}

			if (team != null)
				team.unregister();
			team = null;
		}

		Bukkit.getPluginManager().callEvent(new PostDisbandTeamEvent(this, player, alliesClone, membersClone));
	}

	public boolean isInvited(UUID uuid) {
		for (UUID temp : invitedPlayers) {
			if (temp.compareTo(uuid) == 0) {
				return true;
			}
		}
		return false;
	}

	public void invite(UUID uniqueId) {
		invitedPlayers.add(uniqueId);

		int invite = Main.plugin.getConfig().getInt("invite");

		if (invite <= 0) {
			return;
		}

		Main.plugin.getFoliaLib().getScheduler().runLaterAsync(task -> {
			Player p = Bukkit.getPlayer(uniqueId);
			if (p == null || getTeamPlayer(p) != null) {
				return;
			}
			invitedPlayers.remove(uniqueId);
			MessageManager.sendMessage(p, "invite.expired", getName());
		}, invite * 20L);
	}

	public boolean join(Player p) {
		try {
			members.add(this, new TeamPlayer(p, PlayerRank.DEFAULT));
		} catch (CancelledEventException e) {
			return false;
		}
		savePlayers();
		return true;

	}

	public void promotePlayer(TeamPlayer promotePlayer) {
		PlayerRank newRank;
		if (promotePlayer.getRank() == PlayerRank.DEFAULT) {
			newRank = PlayerRank.ADMIN;
		} else {
			newRank = PlayerRank.OWNER;
		}
		promotePlayer(promotePlayer, newRank);
	}

	public void promotePlayerToOwner(TeamPlayer promotePlayer) {
		promotePlayer(promotePlayer, PlayerRank.OWNER);
	}

	private void promotePlayer(TeamPlayer promotePlayer, PlayerRank newRank) {
		PlayerRank oldRank = promotePlayer.getRank();

		final PromotePlayerEvent event = new PromotePlayerEvent(this, promotePlayer, oldRank, newRank);

		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled()) {
			return;
		}

		promotePlayer.setRank(event.getNewRank());
		storage.promotePlayer(promotePlayer);
		savePlayers();

		Bukkit.getPluginManager().callEvent(new PostPromotePlayerEvent(this, promotePlayer, oldRank, newRank));
	}

	public void demotePlayer(TeamPlayer demotePlayer) {

		PlayerRank oldRank = demotePlayer.getRank();
		PlayerRank newRank;
		if (oldRank == PlayerRank.ADMIN) {
			newRank = PlayerRank.DEFAULT;
		} else {
			newRank = PlayerRank.ADMIN;
		}
		final DemotePlayerEvent event = new DemotePlayerEvent(this, demotePlayer, oldRank, newRank);

		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled()) {
			return;
		}

		demotePlayer.setRank(event.getNewRank());
		storage.demotePlayer(demotePlayer);
		savePlayers();

		Bukkit.getPluginManager().callEvent(new PostDemotePlayerEvent(this, demotePlayer, oldRank, newRank));
	}

	public void setTeamHome(Location teamHome) {
		this.teamHome = teamHome;
		getStorage().set(StoredTeamValue.HOME, LocationSetComponent.getString(teamHome));
	}

	public void deleteTeamHome() {
		teamHome = null;
		getStorage().set(StoredTeamValue.HOME, "");
		if (useTeamHomeAsAnchor) setAnchored(false);
	}

	public void banPlayer(OfflinePlayer player) {
		bannedPlayers.add(this, player.getUniqueId());
		saveBans();
	}

	public void unbanPlayer(OfflinePlayer player) {
		bannedPlayers.remove(this, player.getUniqueId());
		saveBans();
	}

	public boolean isBanned(OfflinePlayer player) {
		return bannedPlayers.contains(player);
	}

	@Deprecated(forRemoval = true)
	public void sendMessage(TeamPlayer sender, String message) {
		teamMessageController.sendTeamChatMessage(sender, message);
	}

	@Deprecated
	public String getTeamChatSyntax(TeamPlayer sender) {
		return teamMessageController.getChatSyntax(sender, TeamMessageController.TeamMessageType.TEAM_CHAT_MESSAGE);
	}

	@Deprecated
	public String getAllyChatSyntax(TeamPlayer sender) {
		return teamMessageController.getChatSyntax(sender, TeamMessageController.TeamMessageType.ALLY_CHAT_MESSAGE);
	}

	@Deprecated
	public void sendAllyMessage(TeamPlayer sender, String message) {
		teamMessageController.sendAllyChatMessage(sender, message);
	}

	public int getScore() {
		return score.get();
	}

	public ScoreComponent getScoreComponent() {
		return score;
	}

	public void setScore(int score) {
		this.score.set(score);
		this.score.save(getStorage());
	}

	public double getMoney() {
		return money.get();
	}

	public MoneyComponent getMoneyComponent() {
		return money;
	}

	public void setMoney(double money) {
		this.money.set(money);
		this.money.save(getStorage());
	}

	public int getTeamRank() {
		return rank;
	}

	public void setTeamRank(int rank) {
		this.rank = rank;
	}

	public void setTeamBalRank(int rank) {
		this.balRank = rank;
	}

	public int getTeamBalRank() {
		return balRank;
	}

	public org.bukkit.scoreboard.Team getScoreboardTeam(Scoreboard board) {
		if (team != null) {
			return team;
		}

		String legacyColorCode = color != null ? "§" + LegacyTextUtils.namedColorToChar(color) : "";
		String name = legacyColorCode + LegacyTextUtils.parseAllAdventure(MessageManager.getMessage("nametag.syntax", getTag(null, false)));

		int attempt = 0;
		do {
			try {
				String attemptStr = ((attempt > 0) ? attempt + "" : "");
				String teamName = getName();

				while (teamName.length() + attemptStr.length() > 16) {
					teamName = teamName.substring(0, teamName.length() - 1);
				}

				if (board.getTeam(teamName + attemptStr) != null) {
					team = null;
					attempt++;
					continue;
				}
				team = board.registerNewTeam(teamName + attemptStr);

			} catch (Exception e) {
				team = null;
				attempt++;
			}

		} while (team == null && attempt < 100);

		if (team == null) {
			Main.plugin.getLogger().warning(
					"An avaliable team cannot be found, be prepared for a lot of errors. (this should never happen, and should always be reported to booksaw)");
			Main.plugin.getLogger().warning("This catch is merely here to stop the server crashing");
			return null;
		}

		Main.plugin.teamManagement.setupTeam(team, name);

		return team;

	}

	public org.bukkit.scoreboard.Team getScoreboardTeamOrNull() {
		return team;
	}

	public String getBalance() {
		return money.getStringFormatting();
	}

	public void setTitle(TeamPlayer player, String title) {
		player.setTitle(title);
		storage.setTitle(player);
		savePlayers();
	}

	private boolean callUserEvent(Team otherTeam, RelationType prevStatus, RelationType newStatus) {
		final RelationChangeTeamEvent event = new RelationChangeTeamEvent(this, otherTeam, prevStatus, newStatus);
		Bukkit.getPluginManager().callEvent(event);
		return event.isCancelled() || prevStatus == event.getNewRelation();
	}

	public void addAlly(UUID otherTeam, boolean sendPostEvent) {
		if (isAlly(otherTeam)) return;

		RelationType prevRelation = RelationType.NEUTRAL;
		final Team other = Team.getTeam(otherTeam);
		if (callUserEvent(other, prevRelation, RelationType.ALLY)) return;

		allies.add(this, otherTeam);
		saveAllies();

		List<String> channelsToUse = Main.plugin.getConfig().getStringList("onAllyMessageChannel");
		final String displayName = getTeam(otherTeam).getDisplayName();
		if (channelsToUse.isEmpty() || channelsToUse.contains("CHAT")) {
			Message message = new ReferencedFormatMessage("ally.ally", displayName);
			getMembers().broadcastMessage(message);
		}
		if (channelsToUse.isEmpty() || channelsToUse.contains("TITLE")) {
			Message message = new ReferencedFormatMessage("ally.ally_title", displayName);
			getMembers().broadcastTitle(message);
		}

		if (sendPostEvent)
			Bukkit.getPluginManager().callEvent(new PostRelationChangeTeamEvent(this, other, prevRelation, RelationType.ALLY));
	}

	public void addAlly(@Nullable Team ally, boolean sendPostEvent) {
		if (ally == null) return;

		addAlly(ally.getID(), sendPostEvent);
	}

	public void addAlly(@Nullable Team ally) {
		addAlly(ally, true);
	}

	public void addAlly(@Nullable UUID ally) {
		addAlly(ally, true);
	}

	public void becomeNeutral(UUID otherTeam, boolean sendPostEvent) {
		if (!isAlly(otherTeam)) return;

		final Team other = Team.getTeam(otherTeam);

		RelationType prevRelation = RelationType.ALLY;
		if (callUserEvent(other, prevRelation, RelationType.NEUTRAL)) return;

		allies.remove(this, otherTeam);
		saveAllies();

		List<String> channelsToUse;
		Message chatMessage, titleMessage;
		channelsToUse = Main.plugin.getConfig().getStringList("onAllyMessageChannel");
		chatMessage = new ReferencedFormatMessage("neutral.remove", other.getDisplayName());
		titleMessage = new ReferencedFormatMessage("neutral.remove_title", other.getDisplayName());

		if (channelsToUse.isEmpty() || channelsToUse.contains("CHAT")) {
			getMembers().broadcastMessage(chatMessage);
		}
		if (channelsToUse.isEmpty() || channelsToUse.contains("TITLE")) {
			getMembers().broadcastTitle(titleMessage);
		}

		if (sendPostEvent)
			Bukkit.getPluginManager().callEvent(new PostRelationChangeTeamEvent(this, other, prevRelation, RelationType.NEUTRAL));
	}

	public void becomeNeutral(Team otherTeam, boolean sendPostEvent) {
		if (otherTeam == null) return;
		becomeNeutral(otherTeam.getID(), sendPostEvent);
	}

	public boolean isAlly(UUID team) {
		return allies.contains(team);
	}

	public boolean isAlly(@Nullable Team team) {
		if (team == null) return false;

		return isAlly(team.getID());
	}

	public boolean isNeutral(UUID team) {
		return !allies.contains(team);
	}

	public boolean isNeutral(@Nullable Team team) {
		if (team == null) return true;

		return isNeutral(team.getID());
	}

	public void addAllyRequest(UUID team) {
		allyRequests.add(this, team);
		saveAllyRequests();
	}

	public void addAllyRequest(@Nullable Team team) {
		if (team == null) return;

		addAllyRequest(team.getID());
	}

	public void removeAllyRequest(UUID team) {
		allyRequests.remove(this, team);
		saveAllyRequests();
	}

	public void removeAllyRequest(@Nullable Team team) {
		if (team == null) return;

		removeAllyRequest(team.getID());
	}

	public boolean hasRequested(UUID team) {
		return allyRequests.contains(team);
	}

	public boolean hasRequested(@Nullable Team team) {
		if (team == null) return false;

		return hasRequested(team.getID());
	}

	public Set<UUID> getAllyRequests() {
		return allyRequests.get();
	}

	private void saveAllies() {
		allies.save(getStorage());
	}

	private void saveAllyRequests() {
		allyRequests.save(storage);
	}

	public UUID getID() {
		return id;
	}

	public boolean canDamage(Player player, Player source) {
		Team team = Team.getTeam(player);
		if (team == null) return true;
		return canDamage(team, source);
	}

	public boolean canDamage(Team team, Player source) {
		final boolean isProtected = team.isAlly(getID()) || team == this;

		boolean disallow;

		if (isProtected) {
			if (pvp && team.pvp) {
				disallow = false;
			} else if (Main.plugin.wgManagement != null) {
				disallow = !Main.plugin.wgManagement.canTeamPvp(source);
			} else
				disallow = true;

			if (disallow) {
				final TeamDisallowedPvPEvent event = new TeamDisallowedPvPEvent(team, source, this, true);

				Bukkit.getPluginManager().callEvent(event);
				if (event.isCancelled()) return true;
			}

			return !disallow;
		}

		return true;
	}

	public boolean canDamage(Player player) {
		Team team = Team.getTeam(player);
		if (team == null) return true;
		return canDamage(team);
	}

	public boolean canDamage(Team team) {
		if (team.isAlly(getID()) || team == this) {
			return pvp && team.pvp;
		}
		return true;
	}

	public boolean hasMaxAllies() {
		int limit = Main.plugin.getConfig().getInt("allyLimit");
		if (limit == -1) {
			return false;
		}

		return allies.size() >= limit;
	}

	public void saveWarps() {
		warps.save(storage);
	}

	public Warp getWarp(String name) {
		return warps.get(name);
	}

	public void addWarp(Warp warp) {
		warps.add(this, warp);
		saveWarps();
	}

	public void delWarp(String name) {
		warps.remove(this, getWarp(name));
		saveWarps();
	}

	public List<Player> getOnlineMembers() {
		return members.getOnlinePlayers();
	}

	public void addClaim(Location location) {
		claims.add(this, location);
		saveClaims();
	}

	public void removeClaim(Location location) {
		claims.remove(this, location);
		saveClaims();
	}

	public void clearClaims() {
		claims.clear();
		saveClaims();
	}

	public int getClaimCount() {
		return claims.size();
	}

	public boolean isClaimed(Location location) {
		return claims.contains(location);
	}

	private void saveClaims() {
		claims.save(getStorage());
	}

	public void saveEchest() {
		echest.save(getStorage());
	}

	public Inventory getEchest() {
		return echest.get();
	}

	public EChestComponent getEchestComponent() {
		return echest;
	}

	public int getMaxWarps() {
		return getLevelObject().getMaxWarps();
	}

	public int getMaxChests() {
		return getLevelObject().getMaxChests();
	}

	public void setLevel(int level) {
		this.level = level;
		getStorage().set(StoredTeamValue.LEVEL, level);

	}

	public void setPvp(boolean pvp) {
		this.pvp = pvp;
		getStorage().set(StoredTeamValue.PVP, pvp);

	}

	public boolean toggleAnchor() {
		return setAnchored(!useTeamHomeAsAnchor);
	}

	public boolean setAnchored(boolean anchor) {
		if (anchor && teamHome == null) return false;
		this.useTeamHomeAsAnchor = anchor;
		getStorage().set(StoredTeamValue.ANCHOR, anchor);
		return true;
	}

	public boolean isAnchored() {
		return useTeamHomeAsAnchor;
	}

	public double getMaxMoney() {
		return getLevelObject().getMaxBalance();
	}

	public int getTeamLimit() {
		if (!Main.plugin.getConfig().getBoolean("permissionLevels") || Main.perms == null) {
			return getLevelObject().getTeamLimit();
		} else {

			int limit = 1;

			for (TeamPlayer player : getRank(PlayerRank.OWNER)) {

				OfflinePlayer op = player.getPlayer();

				for (int i = 100; i > 0 && i > limit; i--) {
					if (Main.perms.playerHas(Bukkit.getWorlds().get(0).getName(), op, "betterteams.limit." + i)) {
						limit = i;
					}
				}

			}
			return limit;
		}
	}

	public boolean isTeamFull() {
		return getMembers().size() >= getTeamLimit();
	}

	public int getMaxAdmins() {
		return getLevelObject().getMaxAdmins();
	}

	public int getMaxOwners() {
		return getLevelObject().getMaxOwners();
	}

	public boolean isMaxAdmins() {
		int max = getMaxAdmins();
		if (max == -1) {
			return false;
		}
		return max <= getRank(PlayerRank.ADMIN).size();
	}

	public boolean isMaxOwners() {
		int max = getMaxOwners();
		if (max == -1) {
			return false;
		}
		return max <= getRank(PlayerRank.OWNER).size();

	}

	public void setAndSaveMeta(String key, String value) {
		getMeta().get().set(key, value);
		getStorage().saveMeta(getMeta().get());
	}

	public void removeAndSaveMeta(String key) {
		getMeta().get().remove(key);
		getStorage().saveMeta(getMeta().get());
	}

	public TeamLevel getLevelObject() {
		return LevelManager.getLevel(this.level);
	}
}