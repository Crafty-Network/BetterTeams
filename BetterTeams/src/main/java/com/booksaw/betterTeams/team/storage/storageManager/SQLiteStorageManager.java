package com.booksaw.betterTeams.team.storage.storageManager;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.team.TeamManager;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.database.SQLiteConnectionManager;
import com.booksaw.betterTeams.text.LegacyTextUtils;
import com.booksaw.betterTeams.team.LocationSetComponent;
import com.booksaw.betterTeams.team.storage.team.SQLiteTeamStorage;
import com.booksaw.betterTeams.team.storage.team.TeamStorage;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SQLiteStorageManager extends TeamManager implements Listener {

    public static final String T_TEAMS         = "bt_teams";
    public static final String T_PLAYERS       = "bt_players";
    public static final String T_ALLY_REQUESTS = "bt_ally_requests";
    public static final String T_WARPS         = "bt_warps";
    public static final String T_CHEST_CLAIMS  = "bt_chest_claims";
    public static final String T_BANS          = "bt_bans";
    public static final String T_ALLIES        = "bt_allies";
    public static final String T_TEAM_META     = "bt_team_meta";

    public static final String HOLO_TEAM_ID = "00000000-0000-0000-0000-000000000000";
    public static final String HOLO_META_KEY = "__holoDetails__";

    @Getter
    private final SQLiteConnectionManager db;

    private final Map<String, UUID> chestClaimCache = new ConcurrentHashMap<>();
    private volatile boolean chestClaimCacheLoaded = false;

    private final Set<UUID> dirtyTeamIds = ConcurrentHashMap.newKeySet();

    private static final int DEFAULT_SAVE_INTERVAL_SECONDS = 60;

    private BukkitTask periodicSaveTask;

    public SQLiteStorageManager() {
        File dataFolder = new File("plugins/BetterTeams");
        db = new SQLiteConnectionManager(dataFolder);

        try {
            db.open();
        } catch (Exception e) {
            Main.plugin.getLogger().log(Level.SEVERE,
                    "[SQLite] Failed to open database – disabling plugin", e);
            Main.plugin.getServer().getPluginManager().disablePlugin(Main.plugin);
            return;
        }

        createTables();

        ensureChestClaimCacheLoaded();
        Main.plugin.getServer().getPluginManager().registerEvents(this, Main.plugin);
        startPeriodicSave();
        Main.plugin.getLogger().info("[SQLite] Storage manager ready.");
    }

    private void createTables() {
        db.submit(() -> {
            try (Statement st = db.getConnection().createStatement()) {
                
                st.execute(
                    "CREATE TABLE IF NOT EXISTS " + T_TEAMS + " (" +
                    "  teamID      TEXT    NOT NULL PRIMARY KEY," +
                    "  name        TEXT    NOT NULL," +
                    "  description TEXT    NOT NULL DEFAULT ''," +
                    "  open        INTEGER NOT NULL DEFAULT 0," +
                    "  score       INTEGER NOT NULL DEFAULT 0," +
                    "  money       REAL    NOT NULL DEFAULT 0," +
                    "  home        TEXT    NOT NULL DEFAULT ''," +
                    "  color       TEXT    NOT NULL DEFAULT '6'," +
                    "  echest      TEXT    NOT NULL DEFAULT ''," +
                    "  level       INTEGER NOT NULL DEFAULT 1," +
                    "  tag         TEXT    NOT NULL DEFAULT ''," +
                    "  pvp         INTEGER NOT NULL DEFAULT 0," +
                    "  anchor      INTEGER NOT NULL DEFAULT 0" +
                    ")"
                );

                st.execute(
                    "CREATE TABLE IF NOT EXISTS " + T_PLAYERS + " (" +
                    "  playerUUID TEXT    NOT NULL PRIMARY KEY," +
                    "  teamID     TEXT    NOT NULL," +
                    "  playerRank INTEGER NOT NULL," +
                    "  title      TEXT    NOT NULL DEFAULT ''," +
                    "  anchor     INTEGER NOT NULL DEFAULT 0," +
                    "  FOREIGN KEY (teamID) REFERENCES " + T_TEAMS +
                        "(teamID) ON DELETE CASCADE" +
                    ")"
                );

                st.execute(
                    "CREATE TABLE IF NOT EXISTS " + T_ALLY_REQUESTS + " (" +
                    "  requestingTeamID TEXT NOT NULL," +
                    "  receivingTeamID  TEXT NOT NULL," +
                    "  PRIMARY KEY (requestingTeamID, receivingTeamID)," +
                    "  FOREIGN KEY (requestingTeamID) REFERENCES " + T_TEAMS +
                        "(teamID) ON DELETE CASCADE," +
                    "  FOREIGN KEY (receivingTeamID)  REFERENCES " + T_TEAMS +
                        "(teamID) ON DELETE CASCADE" +
                    ")"
                );

                st.execute(
                    "CREATE TABLE IF NOT EXISTS " + T_WARPS + " (" +
                    "  teamID   TEXT NOT NULL," +
                    "  warpInfo TEXT NOT NULL," +
                    "  PRIMARY KEY (teamID, warpInfo)," +
                    "  FOREIGN KEY (teamID) REFERENCES " + T_TEAMS +
                        "(teamID) ON DELETE CASCADE" +
                    ")"
                );

                st.execute(
                    "CREATE TABLE IF NOT EXISTS " + T_CHEST_CLAIMS + " (" +
                    "  teamID   TEXT NOT NULL," +
                    "  chestLoc TEXT NOT NULL," +
                    "  PRIMARY KEY (teamID, chestLoc)," +
                    "  FOREIGN KEY (teamID) REFERENCES " + T_TEAMS +
                        "(teamID) ON DELETE CASCADE" +
                    ")"
                );

                st.execute(
                    "CREATE TABLE IF NOT EXISTS " + T_BANS + " (" +
                    "  playerUUID TEXT NOT NULL," +
                    "  teamID     TEXT NOT NULL," +
                    "  PRIMARY KEY (playerUUID, teamID)," +
                    "  FOREIGN KEY (teamID) REFERENCES " + T_TEAMS +
                        "(teamID) ON DELETE CASCADE" +
                    ")"
                );

                st.execute(
                    "CREATE TABLE IF NOT EXISTS " + T_ALLIES + " (" +
                    "  team1ID TEXT NOT NULL," +
                    "  team2ID TEXT NOT NULL," +
                    "  PRIMARY KEY (team1ID, team2ID)," +
                    "  FOREIGN KEY (team1ID) REFERENCES " + T_TEAMS +
                        "(teamID) ON DELETE CASCADE," +
                    "  FOREIGN KEY (team2ID) REFERENCES " + T_TEAMS +
                        "(teamID) ON DELETE CASCADE" +
                    ")"
                );

                st.execute(
                    "CREATE TABLE IF NOT EXISTS " + T_TEAM_META + " (" +
                    "  teamID    TEXT NOT NULL," +
                    "  metaKey   TEXT NOT NULL," +
                    "  metaValue TEXT," +
                    "  PRIMARY KEY (teamID, metaKey)," +
                    "  FOREIGN KEY (teamID) REFERENCES " + T_TEAMS +
                        "(teamID) ON DELETE CASCADE" +
                    ")"
                );

                st.execute("CREATE INDEX IF NOT EXISTS idx_teams_name_upper" +
                    " ON " + T_TEAMS + " (UPPER(name))");
                
                st.execute("CREATE INDEX IF NOT EXISTS idx_players_teamid" +
                    " ON " + T_PLAYERS + " (teamID)");
            }
            return null;
        }).join(); 
    }

    @Override
    public boolean isTeam(UUID uuid) {
        if (uuid == null) return false;
        return db.runWithAffinity(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "SELECT 1 FROM " + T_TEAMS + " WHERE teamID = ? LIMIT 1")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    @Override
    public boolean isTeam(String name) {
        if (name == null) return false;
        return db.runWithAffinity(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "SELECT 1 FROM " + T_TEAMS +
                    " WHERE UPPER(name) = UPPER(?) LIMIT 1")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    @Override
    public boolean isInTeam(OfflinePlayer player) {
        return db.runWithAffinity(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "SELECT 1 FROM " + T_PLAYERS +
                    " WHERE playerUUID = ? LIMIT 1")) {
                ps.setString(1, player.getUniqueId().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    @Override
    public UUID getTeamUUID(OfflinePlayer player) {
        return db.runWithAffinity(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "SELECT teamID FROM " + T_PLAYERS +
                    " WHERE playerUUID = ? LIMIT 1")) {
                ps.setString(1, player.getUniqueId().toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? UUID.fromString(rs.getString("teamID")) : null;
                }
            }
        });
    }

    @Override
    public UUID getTeamUUID(String name) {
        return db.runWithAffinity(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "SELECT teamID FROM " + T_TEAMS +
                    " WHERE UPPER(name) = UPPER(?) LIMIT 1")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? UUID.fromString(rs.getString("teamID")) : null;
                }
            }
        });
    }

    @Override
    public void loadTeams() {
        loadAllTeamsAsync().join();
        Main.plugin.getLogger().info("[SQLite] Loaded " + loadedTeams.size() + " teams into memory.");
    }

    public CompletableFuture<Void> loadAllTeamsAsync() {
        
        List<UUID> allIds = db.runWithAffinity(() -> {
            List<UUID> ids = new ArrayList<>();
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "SELECT teamID FROM " + T_TEAMS + " WHERE teamID != ?")) {
                ps.setString(1, HOLO_TEAM_ID);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    try {
                        ids.add(UUID.fromString(rs.getString("teamID")));
                    } catch (IllegalArgumentException ignored) {
                        
                    }
                }
            }
            return ids;
        });

        List<CompletableFuture<Void>> futures = new ArrayList<>(allIds.size());
        for (UUID id : allIds) {
            futures.add(loadTeamAsync(id));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public CompletableFuture<Void> loadTeamAsync(UUID uuid) {
        if (uuid == null || isLoaded(uuid)) {
            return CompletableFuture.completedFuture(null);
        }
        return db.submit(() -> {
            try {
                Team team = new Team(uuid);

                loadedTeams.put(uuid, team);
            } catch (IllegalArgumentException e) {
                Main.plugin.getLogger().log(Level.WARNING,
                        "[SQLite] Could not load team " + uuid, e);
            }
            return null;
        });
    }

    public void loadTeam(UUID uuid) {
        loadTeamAsync(uuid).join();
    }

    public void unloadTeam(UUID uuid) {
        Team team = loadedTeams.get(uuid);
        if (team != null && team.getScoreboardTeamOrNull() != null) {
            team.getScoreboardTeamOrNull().unregister();
        }
        
    }

    @Override
    protected void registerNewTeam(Team team, Player player) {
        db.execute(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "INSERT OR IGNORE INTO " + T_TEAMS +
                    " (teamID, name) VALUES (?, ?)")) {
                ps.setString(1, team.getID().toString());
                ps.setString(2, team.getName());
                ps.executeUpdate();
            }
        });
    }

    @Override
    protected void deleteTeamStorage(Team team) {
        
        db.execute(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "DELETE FROM " + T_TEAMS + " WHERE teamID = ?")) {
                ps.setString(1, team.getID().toString());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public void teamNameChange(Team team, String newName) {
        db.execute(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "UPDATE " + T_TEAMS + " SET name = ? WHERE teamID = ?")) {
                ps.setString(1, newName);
                ps.setString(2, team.getID().toString());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public void playerJoinTeam(Team team, TeamPlayer player) {
        db.execute(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "INSERT OR IGNORE INTO " + T_PLAYERS +
                    " (playerUUID, teamID, playerRank) VALUES (?, ?, ?)")) {
                ps.setString(1, player.getPlayer().getUniqueId().toString());
                ps.setString(2, team.getID().toString());
                ps.setInt(3, player.getRank().value);
                ps.executeUpdate();
            }
        });
    }

    @Override
    public void playerLeaveTeam(Team team, TeamPlayer player) {
        db.execute(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "DELETE FROM " + T_PLAYERS + " WHERE playerUUID = ?")) {
                ps.setString(1, player.getPlayer().getUniqueId().toString());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public TeamStorage createTeamStorage(Team team) {
        return new SQLiteTeamStorage(this, team);
    }

    @Override
    public TeamStorage createNewTeamStorage(Team team) {
        
        db.runWithAffinity(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "INSERT OR IGNORE INTO " + T_TEAMS +
                    " (teamID, name) VALUES (?, ?)")) {
                ps.setString(1, team.getID().toString());
                ps.setString(2, team.getName());
                ps.executeUpdate();
            }
            return null;
        });
        return new SQLiteTeamStorage(this, team);
    }

    @Override
    public String[] sortTeamsByScore() {
        return runSortQuery(
            "SELECT name FROM " + T_TEAMS + " ORDER BY score DESC");
    }

    @Override
    public String[] sortTeamsByBalance() {
        return runSortQuery(
            "SELECT name FROM " + T_TEAMS + " ORDER BY money DESC");
    }

    @Override
    public String[] sortTeamsByMembers() {
        return runSortQuery(
            "SELECT t.name FROM " + T_TEAMS + " t" +
            " INNER JOIN " + T_PLAYERS + " p ON t.teamID = p.teamID" +
            " GROUP BY t.teamID, t.name" +
            " ORDER BY COUNT(p.playerUUID) DESC"
        );
    }

    private String[] runSortQuery(String sql) {
        return db.runWithAffinity(() -> {
            List<String> names = new ArrayList<>();
            try (Statement st = db.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    names.add(rs.getString("name"));
                }
            }
            return names.toArray(new String[0]);
        });
    }

    @Override
    public void purgeTeamScore() {
        
        List<Team> snapshot = new ArrayList<>(loadedTeams.values());
        snapshot.forEach(t -> t.setScore(0));

        db.execute(() -> {
            try (Statement st = db.getConnection().createStatement()) {
                st.execute("UPDATE " + T_TEAMS + " SET score = 0");
            }
        }).whenComplete((v, ex) -> {
            if (ex == null) snapshot.forEach(Team::clearDirty);
        });
    }

    @Override
    public void purgeTeamMoney() {
        List<Team> snapshot = new ArrayList<>(loadedTeams.values());
        snapshot.forEach(t -> t.setMoney(0));
        db.execute(() -> {
            try (Statement st = db.getConnection().createStatement()) {
                st.execute("UPDATE " + T_TEAMS + " SET money = 0");
            }
        }).whenComplete((v, ex) -> {
            if (ex == null) snapshot.forEach(Team::clearDirty);
        });
    }

    @Override
    public UUID getClaimingTeamUUID(Location location) {
        ensureChestClaimCacheLoaded();
        return chestClaimCache.get(LocationSetComponent.getString(location));
    }

    private void ensureChestClaimCacheLoaded() {
        if (chestClaimCacheLoaded) return;
        synchronized (this) {
            if (chestClaimCacheLoaded) return;
            db.runWithAffinity(() -> {
                try (Statement st = db.getConnection().createStatement();
                     ResultSet rs = st.executeQuery(
                         "SELECT teamID, chestLoc FROM " + T_CHEST_CLAIMS)) {
                    while (rs.next()) {
                        chestClaimCache.put(
                            rs.getString("chestLoc"),
                            UUID.fromString(rs.getString("teamID")));
                    }
                }
                return null;
            });
            chestClaimCacheLoaded = true;
        }
    }

    @Override
    public void addChestClaim(Team team, Location loc) {
        String locStr = LocationSetComponent.getString(loc);
        chestClaimCache.put(locStr, team.getID());
        db.execute(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "INSERT OR IGNORE INTO " + T_CHEST_CLAIMS +
                    " (teamID, chestLoc) VALUES (?, ?)")) {
                ps.setString(1, team.getID().toString());
                ps.setString(2, locStr);
                ps.executeUpdate();
            }
        });
    }

    @Override
    public void removeChestclaim(Location loc) {
        String locStr = LocationSetComponent.getString(loc);
        chestClaimCache.remove(locStr);
        db.execute(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "DELETE FROM " + T_CHEST_CLAIMS + " WHERE chestLoc = ?")) {
                ps.setString(1, locStr);
                ps.executeUpdate();
            }
        });
    }

    @Override
    public List<String> getHoloDetails() {
        return db.runWithAffinity(() -> {
            ensureHoloSentinelExists();
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "SELECT metaValue FROM " + T_TEAM_META +
                    " WHERE teamID = ? AND metaKey = ?")) {
                ps.setString(1, HOLO_TEAM_ID);
                ps.setString(2, HOLO_META_KEY);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String raw = rs.getString("metaValue");
                        if (raw != null && !raw.isEmpty()) {
                            return Arrays.asList(raw.split("\n", -1));
                        }
                    }
                }
            }
            return Collections.emptyList();
        });
    }

    @Override
    public void setHoloDetails(List<String> details) {
        db.execute(() -> {
            ensureHoloSentinelExists();
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "INSERT OR REPLACE INTO " + T_TEAM_META +
                    " (teamID, metaKey, metaValue) VALUES (?, ?, ?)")) {
                ps.setString(1, HOLO_TEAM_ID);
                ps.setString(2, HOLO_META_KEY);
                ps.setString(3, String.join("\n", details));
                ps.executeUpdate();
            }
        });
    }

    private void ensureHoloSentinelExists() throws SQLException {
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "INSERT OR IGNORE INTO " + T_TEAMS +
                " (teamID, name) VALUES (?, '__hologram_storage__')")) {
            ps.setString(1, HOLO_TEAM_ID);
            ps.executeUpdate();
        }
    }

    @Override
    public void rebuildLookups() {
        
    }

    @Override
    public void disable() {
        if (periodicSaveTask != null && !periodicSaveTask.isCancelled()) {
            periodicSaveTask.cancel();
        }
        flushDirtyTeams();
        db.close();
        Main.plugin.getLogger().info("[SQLite] Connection closed.");
    }

    public void markTeamDirty(UUID teamId) {
        dirtyTeamIds.add(teamId);
    }

    private void startPeriodicSave() {
        int intervalSeconds = Main.plugin.getConfig()
                .getInt("sqliteSaveInterval", DEFAULT_SAVE_INTERVAL_SECONDS);
        long intervalTicks = (long) intervalSeconds * 20L;

        periodicSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(Main.plugin, () -> {
            if (dirtyTeamIds.isEmpty()) return;

            Set<UUID> toSave = new HashSet<>(dirtyTeamIds);
            dirtyTeamIds.removeAll(toSave);

            int saved = 0;
            for (UUID id : toSave) {
                Team team = loadedTeams.get(id);
                if (team == null) continue; 
                try {
                    saveTeam(team).join(); 
                    team.clearDirty();
                    saved++;
                } catch (Exception e) {
                    Main.plugin.getLogger().log(Level.WARNING,
                            "[SQLite] Periodic save failed for team " + id, e);
                    
                    dirtyTeamIds.add(id);
                }
            }

            if (saved > 0) {
                Main.plugin.getLogger().fine("[SQLite] Periodic save: flushed " + saved + " team(s).");
            }
        }, intervalTicks, intervalTicks);
    }

    private void flushDirtyTeams() {
        if (dirtyTeamIds.isEmpty()) return;

        Set<UUID> toSave = new HashSet<>(dirtyTeamIds);
        dirtyTeamIds.clear();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (UUID id : toSave) {
            Team team = loadedTeams.get(id);
            if (team == null) continue;
            futures.add(saveTeam(team).whenComplete((v, ex) -> {
                if (ex != null) {
                    Main.plugin.getLogger().log(Level.SEVERE,
                            "[SQLite] Shutdown save failed for team " + id, ex);
                } else {
                    team.clearDirty();
                }
            }));
        }

        if (!futures.isEmpty()) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            Main.plugin.getLogger().info("[SQLite] Flushed " + futures.size() + " dirty team(s) on shutdown.");
        }
    }

    public CompletableFuture<Void> saveTeam(TeamSnapshot snapshot) {
        return db.execute(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "UPDATE " + T_TEAMS + " SET" +
                    "  name = ?, description = ?, open = ?, score = ?," +
                    "  money = ?, home = ?, color = ?, level = ?," +
                    "  tag = ?, pvp = ?, anchor = ?" +
                    " WHERE teamID = ?")) {
                ps.setString(1,  snapshot.name());
                ps.setString(2,  snapshot.description());
                ps.setInt(3,     snapshot.open() ? 1 : 0);
                ps.setInt(4,     snapshot.score());
                ps.setDouble(5,  snapshot.money());
                ps.setString(6,  snapshot.home());
                ps.setString(7,  snapshot.color());
                ps.setInt(8,     snapshot.level());
                ps.setString(9,  snapshot.tag());
                ps.setInt(10,    snapshot.pvp() ? 1 : 0);
                ps.setInt(11,    snapshot.anchor() ? 1 : 0);
                ps.setString(12, snapshot.teamId());
                ps.executeUpdate();
            }
        });
    }

    public CompletableFuture<Void> saveTeam(Team team) {
        
        TeamSnapshot snapshot = TeamSnapshot.from(team);
        return saveTeam(snapshot);
    }

    public CompletableFuture<Void> deleteTeam(UUID teamId) {
        
        chestClaimCache.entrySet().removeIf(e -> e.getValue().equals(teamId));

        return db.execute(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "DELETE FROM " + T_TEAMS + " WHERE teamID = ?")) {
                ps.setString(1, teamId.toString());
                ps.executeUpdate();
                
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        UUID teamUUID = getTeamUUID(e.getPlayer());
        if (teamUUID != null && !isLoaded(teamUUID)) {
            loadTeamAsync(teamUUID);
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {

        UUID teamUUID = getTeamUUID(e.getPlayer());
        if (teamUUID == null) return;
        Team team = loadedTeams.get(teamUUID);
        if (team == null) return;

        TeamPlayer tp = team.getTeamPlayer(e.getPlayer());
        if (tp != null) tp.setTeamChat(false);

        if (team.getOnlineMembers().size() <= 1) {
            unloadTeam(team.getID());
        }
    }

    public record TeamSnapshot(
            String teamId,
            String name,
            String description,
            boolean open,
            int score,
            double money,
            String home,
            String color,
            int level,
            String tag,
            boolean pvp,
            boolean anchor
    ) {
        public static TeamSnapshot from(Team team) {
            return new TeamSnapshot(
                team.getID().toString(),
                team.getName() != null ? team.getName() : "",
                team.getDescription() != null ? team.getDescription() : "",
                team.isOpen(),
                team.getScore(),
                team.getMoney(),
                team.getStorage().getString("home"),
                String.valueOf(team.getColor() != null ? LegacyTextUtils.namedColorToChar(team.getColor()) : '6'),
                team.getLevel(),
                team.getTag() != null ? team.getTag() : "",
                team.isPvp(),
                team.getStorage().getBoolean("anchor")
            );
        }
    }
}
