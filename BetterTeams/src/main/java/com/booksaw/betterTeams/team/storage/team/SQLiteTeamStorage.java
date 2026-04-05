package com.booksaw.betterTeams.team.storage.team;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.Team;
import com.booksaw.betterTeams.TeamPlayer;
import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Utils;
import com.booksaw.betterTeams.Warp;
import com.booksaw.betterTeams.database.SQLiteConnectionManager;
import com.booksaw.betterTeams.team.meta.TeamMeta;
import com.booksaw.betterTeams.team.storage.storageManager.SQLiteStorageManager;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class SQLiteTeamStorage extends TeamStorage {

    private final SQLiteConnectionManager db;
    private final SQLiteStorageManager storageManager;

    private static final Set<String> VALID_COLUMNS = Collections.unmodifiableSet(
        Arrays.stream(StoredTeamValue.values())
              .map(StoredTeamValue::getReference)
              .collect(Collectors.toCollection(() -> new HashSet<>(Arrays.asList("echest"))))
    );

    public SQLiteTeamStorage(SQLiteStorageManager storageManager, Team team) {
        super(team);
        this.storageManager = storageManager;
        this.db = storageManager.getDb();
    }

    private String teamId() {
        return team.getID().toString();
    }

    private static void checkColumn(String column) {
        if (!VALID_COLUMNS.contains(column))
            throw new IllegalArgumentException("Invalid column name: " + column);
    }

    private void invalidatePapiCache() {
        if (Main.placeholderAPI) {
            Main.plugin.getTeamPlaceholders().invalidateCache();
        }
    }

    @Override
    protected void setValue(String column, TeamStorageType storageType, Object value) {
        storageManager.markTeamDirty(team.getID());
    }

    @Override
    public String getString(String column) {
        checkColumn(column);
        return db.runWithAffinity(() -> {
            String sql = "SELECT " + column +
                         " FROM " + SQLiteStorageManager.T_TEAMS +
                         " WHERE teamID = ? LIMIT 1";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, teamId());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(column) : "";
                }
            }
        });
    }

    @Override
    public boolean getBoolean(String column) {
        checkColumn(column);
        return db.runWithAffinity(() -> {
            String sql = "SELECT " + column +
                         " FROM " + SQLiteStorageManager.T_TEAMS +
                         " WHERE teamID = ? LIMIT 1";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, teamId());
                
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(column) == 1;
                }
            }
        });
    }

    @Override
    public double getDouble(String column) {
        checkColumn(column);
        return db.runWithAffinity(() -> {
            String sql = "SELECT " + column +
                         " FROM " + SQLiteStorageManager.T_TEAMS +
                         " WHERE teamID = ? LIMIT 1";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, teamId());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getDouble(column) : 0.0;
                }
            }
        });
    }

    @Override
    public int getInt(String column) {
        checkColumn(column);
        return db.runWithAffinity(() -> {
            String sql = "SELECT " + column +
                         " FROM " + SQLiteStorageManager.T_TEAMS +
                         " WHERE teamID = ? LIMIT 1";
            try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
                ps.setString(1, teamId());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(column) : 0;
                }
            }
        });
    }

    @Override
    public List<TeamPlayer> getPlayerList() {
        return db.runWithAffinity(() -> {
            List<TeamPlayer> players = new ArrayList<>();
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "SELECT playerUUID, playerRank, title, anchor" +
                    " FROM " + SQLiteStorageManager.T_PLAYERS +
                    " WHERE teamID = ?")) {
                ps.setString(1, teamId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        players.add(new TeamPlayer(
                            Bukkit.getOfflinePlayer(UUID.fromString(rs.getString("playerUUID"))),
                            PlayerRank.getRank(rs.getInt("playerRank")),
                            rs.getString("title"),
                            rs.getInt("anchor") == 1
                        ));
                    }
                }
            }
            return players;
        });
    }

    @Override
    public void setPlayerList(List<String> players) {
        
    }

    @Override
    public List<UUID> getAnchoredPlayerList() {
        return db.runWithAffinity(() -> {
            List<UUID> anchored = new ArrayList<>();
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "SELECT playerUUID FROM " + SQLiteStorageManager.T_PLAYERS +
                    " WHERE teamID = ? AND anchor = 1")) {
                ps.setString(1, teamId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        anchored.add(UUID.fromString(rs.getString("playerUUID")));
                    }
                }
            }
            return anchored;
        });
    }

    @Override
    public void setAnchoredPlayerList(List<String> players) {
        
    }

    @Override
    public void setAnchor(TeamPlayer player, boolean anchor) {
        invalidatePapiCache();
        db.execute(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "UPDATE " + SQLiteStorageManager.T_PLAYERS +
                    " SET anchor = ? WHERE playerUUID = ?")) {
                ps.setInt(1, anchor ? 1 : 0);
                ps.setString(2, player.getPlayer().getUniqueId().toString());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public List<String> getBanList() {
        return db.runWithAffinity(() -> {
            List<String> bans = new ArrayList<>();
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "SELECT playerUUID FROM " + SQLiteStorageManager.T_BANS +
                    " WHERE teamID = ?")) {
                ps.setString(1, teamId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        bans.add(rs.getString("playerUUID"));
                    }
                }
            }
            return bans;
        });
    }

    @Override
    public void setBanList(List<String> players) {
        
    }

    @Override
    public void addBan(UUID playerUUID) {
        invalidatePapiCache();
        db.execute(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "INSERT OR IGNORE INTO " + SQLiteStorageManager.T_BANS +
                    " (playerUUID, teamID) VALUES (?, ?)")) {
                ps.setString(1, playerUUID.toString());
                ps.setString(2, teamId());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public void removeBan(UUID playerUUID) {
        invalidatePapiCache();
        db.execute(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "DELETE FROM " + SQLiteStorageManager.T_BANS +
                    " WHERE playerUUID = ? AND teamID = ?")) {
                ps.setString(1, playerUUID.toString());
                ps.setString(2, teamId());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public List<String> getAllyList() {
        return db.runWithAffinity(() -> {
            
            Set<String> allies = new LinkedHashSet<>();
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "SELECT team1ID, team2ID FROM " + SQLiteStorageManager.T_ALLIES +
                    " WHERE team1ID = ? OR team2ID = ?")) {
                ps.setString(1, teamId());
                ps.setString(2, teamId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String t1 = rs.getString("team1ID");
                        
                        String other = teamId().equals(t1)
                                ? rs.getString("team2ID")
                                : t1;
                        allies.add(other);
                    }
                }
            }
            return new ArrayList<>(allies);
        });
    }

    @Override
    public void setAllyList(List<String> allies) {
        
    }

    @Override
    public void addAlly(UUID allyTeamId) {
        invalidatePapiCache();
        db.execute(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "INSERT OR IGNORE INTO " + SQLiteStorageManager.T_ALLIES +
                    " (team1ID, team2ID) VALUES (?, ?)")) {
                ps.setString(1, teamId());
                ps.setString(2, allyTeamId.toString());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public void removeAlly(UUID allyTeamId) {
        invalidatePapiCache();
        db.execute(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "DELETE FROM " + SQLiteStorageManager.T_ALLIES +
                    " WHERE (team1ID = ? AND team2ID = ?)" +
                    "    OR (team1ID = ? AND team2ID = ?)")) {
                ps.setString(1, teamId());
                ps.setString(2, allyTeamId.toString());
                ps.setString(3, allyTeamId.toString());
                ps.setString(4, teamId());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public List<String> getAllyRequestList() {
        return db.runWithAffinity(() -> {
            List<String> requests = new ArrayList<>();
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "SELECT requestingTeamID FROM " + SQLiteStorageManager.T_ALLY_REQUESTS +
                    " WHERE receivingTeamID = ?")) {
                ps.setString(1, teamId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        requests.add(rs.getString("requestingTeamID"));
                    }
                }
            }
            return requests;
        });
    }

    @Override
    public void setAllyRequestList(List<String> requests) {
        
    }

    @Override
    public void addAllyRequest(UUID requestingTeamId) {
        invalidatePapiCache();
        db.execute(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "INSERT OR IGNORE INTO " + SQLiteStorageManager.T_ALLY_REQUESTS +
                    " (receivingTeamID, requestingTeamID) VALUES (?, ?)")) {
                ps.setString(1, teamId());
                ps.setString(2, requestingTeamId.toString());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public void removeAllyRequest(UUID requestingTeamId) {
        invalidatePapiCache();
        db.execute(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "DELETE FROM " + SQLiteStorageManager.T_ALLY_REQUESTS +
                    " WHERE receivingTeamID = ? AND requestingTeamID = ?")) {
                ps.setString(1, teamId());
                ps.setString(2, requestingTeamId.toString());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public void getEchestContents(Inventory inventory) {
        String raw = db.runWithAffinity(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "SELECT echest FROM " + SQLiteStorageManager.T_TEAMS +
                    " WHERE teamID = ? LIMIT 1")) {
                ps.setString(1, teamId());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString("echest") : null;
                }
            }
        });

        if (raw != null && !raw.isEmpty()) {
            Utils.deserializeIntoInventory(inventory, raw);
        }
    }

    @Override
    public void setEchestContents(Inventory inventory) {
        String serial = Utils.serializeInventory(inventory);
        invalidatePapiCache();
        db.execute(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "UPDATE " + SQLiteStorageManager.T_TEAMS +
                    " SET echest = ? WHERE teamID = ?")) {
                ps.setString(1, serial);
                ps.setString(2, teamId());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public List<String> getWarps() {
        return db.runWithAffinity(() -> {
            List<String> warps = new ArrayList<>();
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "SELECT warpInfo FROM " + SQLiteStorageManager.T_WARPS +
                    " WHERE teamID = ?")) {
                ps.setString(1, teamId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        warps.add(rs.getString("warpInfo"));
                    }
                }
            }
            return warps;
        });
    }

    @Override
    public void setWarps(List<String> warps) {
        
    }

    @Override
    public void addWarp(Warp warp) {
        invalidatePapiCache();
        db.execute(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "INSERT OR IGNORE INTO " + SQLiteStorageManager.T_WARPS +
                    " (teamID, warpInfo) VALUES (?, ?)")) {
                ps.setString(1, teamId());
                ps.setString(2, warp.toString());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public void removeWarp(Warp warp) {
        invalidatePapiCache();
        db.execute(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "DELETE FROM " + SQLiteStorageManager.T_WARPS +
                    " WHERE teamID = ? AND warpInfo = ?")) {
                ps.setString(1, teamId());
                ps.setString(2, warp.toString());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public List<String> getClaimedChests() {
        return db.runWithAffinity(() -> {
            List<String> claims = new ArrayList<>();
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "SELECT chestLoc FROM " + SQLiteStorageManager.T_CHEST_CLAIMS +
                    " WHERE teamID = ?")) {
                ps.setString(1, teamId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        claims.add(rs.getString("chestLoc"));
                    }
                }
            }
            return claims;
        });
    }

    @Override
    public void setClaimedChests(List<String> chests) {

    }

    @Override
    public void promotePlayer(TeamPlayer player) {
        updatePlayerRank(player);
    }

    @Override
    public void demotePlayer(TeamPlayer player) {
        updatePlayerRank(player);
    }

    private void updatePlayerRank(TeamPlayer player) {
        invalidatePapiCache();
        db.execute(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "UPDATE " + SQLiteStorageManager.T_PLAYERS +
                    " SET playerRank = ? WHERE playerUUID = ?")) {
                ps.setInt(1, player.getRank().value);
                ps.setString(2, player.getPlayer().getUniqueId().toString());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public void setTitle(TeamPlayer player) {
        invalidatePapiCache();
        db.execute(() -> {
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "UPDATE " + SQLiteStorageManager.T_PLAYERS +
                    " SET title = ? WHERE playerUUID = ?")) {
                ps.setString(1, player.getTitle() != null ? player.getTitle() : "");
                ps.setString(2, player.getPlayer().getUniqueId().toString());
                ps.executeUpdate();
            }
        });
    }

    @Override
    public Map<String, String> getRawMeta() {
        return db.runWithAffinity(() -> {
            Map<String, String> meta = new LinkedHashMap<>();
            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "SELECT metaKey, metaValue FROM " + SQLiteStorageManager.T_TEAM_META +
                    " WHERE teamID = ?")) {
                ps.setString(1, teamId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        meta.put(rs.getString("metaKey"), rs.getString("metaValue"));
                    }
                }
            }
            return meta;
        });
    }

    @Override
    public void saveMeta(TeamMeta meta) {
        Map<String, String> serialized = meta.getSerialized();
        db.execute(() -> {

            Connection conn = db.getConnection();
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM " + SQLiteStorageManager.T_TEAM_META +
                        " WHERE teamID = ?")) {
                    del.setString(1, teamId());
                    del.executeUpdate();
                }

                if (!serialized.isEmpty()) {
                    try (PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO " + SQLiteStorageManager.T_TEAM_META +
                            " (teamID, metaKey, metaValue) VALUES (?, ?, ?)")) {
                        for (Map.Entry<String, String> entry : serialized.entrySet()) {
                            ins.setString(1, teamId());
                            ins.setString(2, entry.getKey());
                            ins.setString(3, entry.getValue());
                            ins.addBatch();
                        }
                        ins.executeBatch();
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                try {
                    conn.rollback();
                } catch (SQLException rbEx) {
                    Main.plugin.getLogger().log(Level.SEVERE,
                            "[SQLite] Rollback failed for team " + teamId(), rbEx);
                }
                Main.plugin.getLogger().log(Level.SEVERE,
                        "[SQLite] Failed to save team meta for " + teamId(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        });
    }
}
