package com.booksaw.betterTeams.team.storage.convert;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.database.SQLiteConnectionManager;
import com.booksaw.betterTeams.database.api.Database;
import com.booksaw.betterTeams.team.storage.storageManager.SQLiteStorageManager;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.*;

public class SqlToSQLite extends Converter {

    @Override
    protected void convert() {

        File teamsFile = new File("plugins/BetterTeams/teams.yml");
        if (!teamsFile.exists()) {
            Main.plugin.saveResource("teams.yml", false);
        }
        YamlConfiguration teamStorage = YamlConfiguration.loadConfiguration(teamsFile);

        log("[SQLite Migration] Connecting to MySQL source database...");
        Database mysql = new Database();
        mysql.setupConnectionFromConfiguration(
                Main.plugin.getConfig().getConfigurationSection("database"));
        log("[SQLite Migration] MySQL connection established.");

        log("[SQLite Migration] Backing up teams.yml...");
        File backup = new File("plugins/BetterTeamsBACKUP_sql_to_sqlite.yml");
        try {
            Files.copy(teamsFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log("[SQLite Migration] Backup written to " + backup.getName());
        } catch (IOException e) {
            log("[SQLite Migration] Backup FAILED — aborting migration for data safety.");
            e.printStackTrace();
            mysql.closeConnection();
            return;
        }

        File dataFolder = new File("plugins/BetterTeams");
        SQLiteConnectionManager db = new SQLiteConnectionManager(dataFolder);
        try {
            db.open();
        } catch (Exception e) {
            log("[SQLite Migration] Failed to open SQLite database — aborting.");
            e.printStackTrace();
            mysql.closeConnection();
            return;
        }

        try {
            db.submit(() -> {
                try (Statement st = db.getConnection().createStatement()) {
                    createTables(st);
                    st.execute("PRAGMA foreign_keys=OFF");
                }
                return null;
            }).join();
        } catch (Exception e) {
            log("[SQLite Migration] Schema setup failed — aborting.");
            e.printStackTrace();
            db.close();
            mysql.closeConnection();
            return;
        }

        String prefix  = Main.plugin.getConfig().getString("database.tablePrefix", "");
        String tTeam   = prefix + "Team";
        String tPlayer = prefix + "Players";
        String tBan    = prefix + "Bans";
        String tAlly   = prefix + "Allies";
        String tAllyRq = prefix + "AllyRequests";
        String tWarp   = prefix + "warps";
        String tChest  = prefix + "ChestClaims";
        String tMeta   = prefix + "TeamMeta";

        int migrated = 0;
        int failed   = 0;
        try {
            Connection mysqlConn = mysql.getConnection();
            if (mysqlConn == null) {
                log("[SQLite Migration] Cannot obtain MySQL connection — aborting.");
                db.close();
                mysql.closeConnection();
                return;
            }

            List<Map<String, Object>> teams = readAll(mysqlConn,
                    "SELECT * FROM " + tTeam);
            int total = teams.size();
            log("[SQLite Migration] Found " + total + " team(s) in MySQL.");

            for (Map<String, Object> teamRow : teams) {
                String teamId = str(teamRow.get("teamid"));
                if (teamId == null || teamId.isEmpty()) {
                    failed++;
                    log("[SQLite Migration] WARNING: Team row with null/empty ID — skipped.");
                    continue;
                }
                try {
                    migrateTeam(db, mysqlConn, teamRow, teamId,
                            tPlayer, tBan, tAlly, tAllyRq, tWarp, tChest, tMeta);
                    migrated++;
                    log("[SQLite Migration] Team " + teamId
                            + " (" + migrated + "/" + total + ") OK");
                } catch (Exception e) {
                    failed++;
                    log("[SQLite Migration] WARNING: Team " + teamId
                            + " failed: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            List<String> holos = teamStorage.getStringList("holos");
            if (!holos.isEmpty()) {
                log("[SQLite Migration] Migrating hologram details...");
                String holoValue = String.join("\n", holos);
                try {
                    db.submit(() -> {
                        try (PreparedStatement ps = db.getConnection().prepareStatement(
                                "INSERT OR IGNORE INTO " + SQLiteStorageManager.T_TEAMS
                                + " (teamID, name) VALUES (?, '__hologram_storage__')")) {
                            ps.setString(1, SQLiteStorageManager.HOLO_TEAM_ID);
                            ps.executeUpdate();
                        }
                        try (PreparedStatement ps = db.getConnection().prepareStatement(
                                "INSERT OR REPLACE INTO " + SQLiteStorageManager.T_TEAM_META
                                + " (teamID, metaKey, metaValue) VALUES (?, ?, ?)")) {
                            ps.setString(1, SQLiteStorageManager.HOLO_TEAM_ID);
                            ps.setString(2, SQLiteStorageManager.HOLO_META_KEY);
                            ps.setString(3, holoValue);
                            ps.executeUpdate();
                        }
                        return null;
                    }).join();
                    log("[SQLite Migration] Hologram details migrated.");
                } catch (Exception e) {
                    log("[SQLite Migration] WARNING: Hologram migration failed: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            log("[SQLite Migration] Unexpected error during migration: " + e.getMessage());
            e.printStackTrace();
        }

        db.submit(() -> {
            try (Statement st = db.getConnection().createStatement()) {
                st.execute("PRAGMA foreign_keys=ON");
            }
            return null;
        }).join();
        db.close();
        mysql.closeConnection();

        teamStorage.set("storageType", "SQLITE");
        try {
            teamStorage.save(teamsFile);
            log("[SQLite Migration] teams.yml updated — storageType set to SQLITE.");
        } catch (IOException e) {
            log("[SQLite Migration] WARNING: Could not update teams.yml storageType. "
                    + "Migration will re-run on next start (safe — INSERT OR IGNORE prevents duplicates).");
            e.printStackTrace();
        }

        log("[SQLite Migration] ================================================");
        log("[SQLite Migration] MySQL → SQLite migration complete.");
        log("[SQLite Migration]   Teams migrated : " + migrated);
        if (failed > 0) {
            log("[SQLite Migration]   Teams FAILED   : " + failed + " (check logs above)");
        }
        log("[SQLite Migration] MySQL data is PRESERVED (not deleted).");
        log("[SQLite Migration] ================================================");
    }

    private void migrateTeam(
            SQLiteConnectionManager db,
            Connection mysql,
            Map<String, Object> teamRow,
            String teamId,
            String tPlayer, String tBan, String tAlly,
            String tAllyRq, String tWarp, String tChest, String tMeta)
            throws Exception {

        List<Map<String, Object>> players  = readAll(mysql,
                "SELECT * FROM " + tPlayer + " WHERE teamID = ?",   teamId);
        List<Map<String, Object>> bans     = readAll(mysql,
                "SELECT * FROM " + tBan    + " WHERE TeamID = ?",   teamId);
        
        List<Map<String, Object>> allies   = readAll(mysql,
                "SELECT * FROM " + tAlly   + " WHERE team1ID = ? OR team2ID = ?",
                teamId, teamId);
        List<Map<String, Object>> allyReqs = readAll(mysql,
                "SELECT * FROM " + tAllyRq + " WHERE receivingTeamID = ?", teamId);
        List<Map<String, Object>> warps    = readAll(mysql,
                "SELECT * FROM " + tWarp   + " WHERE TeamID = ?",   teamId);
        List<Map<String, Object>> chests   = readAll(mysql,
                "SELECT * FROM " + tChest  + " WHERE TeamID = ?",   teamId);
        List<Map<String, Object>> meta     = readAll(mysql,
                "SELECT * FROM " + tMeta   + " WHERE teamID = ?",   teamId);

        db.submit(() -> {
            Connection conn = db.getConnection();
            boolean prev = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO " + SQLiteStorageManager.T_TEAMS
                        + " (teamID,name,description,open,score,money,home,color,echest,level,tag,pvp,anchor)"
                        + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                    ps.setString(1,  teamId);
                    ps.setString(2,  safe(str(teamRow.get("name")),        teamId));
                    ps.setString(3,  safe(str(teamRow.get("description")), ""));
                    ps.setInt(4,     toBool(teamRow.get("open"))   ? 1 : 0);
                    ps.setInt(5,     toInt(teamRow.get("score")));
                    ps.setDouble(6,  toDouble(teamRow.get("money")));
                    ps.setString(7,  safe(str(teamRow.get("home")),  ""));
                    ps.setString(8,  safe(str(teamRow.get("color")), "6"));
                    ps.setString(9,  safe(str(teamRow.get("echest")), ""));
                    ps.setInt(10,    teamRow.get("level") != null ? toInt(teamRow.get("level")) : 1);
                    ps.setString(11, safe(str(teamRow.get("tag")), ""));
                    ps.setInt(12,    toBool(teamRow.get("pvp"))    ? 1 : 0);
                    ps.setInt(13,    toBool(teamRow.get("anchor")) ? 1 : 0);
                    ps.executeUpdate();
                }

                for (Map<String, Object> p : players) {
                    String uuid = str(p.get("playeruuid"));
                    if (uuid == null || uuid.isEmpty()) continue;
                    try {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT OR IGNORE INTO " + SQLiteStorageManager.T_PLAYERS
                                + " (playerUUID,teamID,playerRank,title,anchor) VALUES (?,?,?,?,?)")) {
                            ps.setString(1, uuid);
                            ps.setString(2, teamId);
                            ps.setInt(3,    toInt(p.get("playerrank")));
                            ps.setString(4, safe(str(p.get("title")), ""));
                            ps.setInt(5,    toBool(p.get("anchor")) ? 1 : 0);
                            ps.executeUpdate();
                        }
                    } catch (Exception e) {
                        Main.plugin.getLogger().warning(
                                "[SQLite Migration] Bad player " + uuid + " in team " + teamId + ": " + e.getMessage());
                    }
                }

                for (Map<String, Object> b : bans) {
                    String uuid = str(b.get("playeruuid"));
                    if (uuid == null || uuid.isEmpty()) continue;
                    try {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT OR IGNORE INTO " + SQLiteStorageManager.T_BANS
                                + " (playerUUID,teamID) VALUES (?,?)")) {
                            ps.setString(1, uuid);
                            ps.setString(2, teamId);
                            ps.executeUpdate();
                        }
                    } catch (Exception e) {
                        Main.plugin.getLogger().warning(
                                "[SQLite Migration] Bad ban " + uuid + " in team " + teamId);
                    }
                }

                for (Map<String, Object> a : allies) {
                    String t1 = str(a.get("team1id"));
                    String t2 = str(a.get("team2id"));
                    if (t1 == null || t2 == null) continue;
                    try {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT OR IGNORE INTO " + SQLiteStorageManager.T_ALLIES
                                + " (team1ID,team2ID) VALUES (?,?)")) {
                            ps.setString(1, t1);
                            ps.setString(2, t2);
                            ps.executeUpdate();
                        }
                    } catch (Exception e) {
                        Main.plugin.getLogger().warning(
                                "[SQLite Migration] Bad ally (" + t1 + "," + t2 + "): " + e.getMessage());
                    }
                }

                for (Map<String, Object> r : allyReqs) {
                    String req = str(r.get("requestingteamid"));
                    if (req == null || req.isEmpty()) continue;
                    try {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT OR IGNORE INTO " + SQLiteStorageManager.T_ALLY_REQUESTS
                                + " (requestingTeamID,receivingTeamID) VALUES (?,?)")) {
                            ps.setString(1, req);
                            ps.setString(2, teamId);
                            ps.executeUpdate();
                        }
                    } catch (Exception e) {
                        Main.plugin.getLogger().warning(
                                "[SQLite Migration] Bad ally request " + req + " in team " + teamId);
                    }
                }

                for (Map<String, Object> w : warps) {
                    String info = str(w.get("warpinfo"));
                    if (info == null || info.isEmpty()) continue;
                    try {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT OR IGNORE INTO " + SQLiteStorageManager.T_WARPS
                                + " (teamID,warpInfo) VALUES (?,?)")) {
                            ps.setString(1, teamId);
                            ps.setString(2, info);
                            ps.executeUpdate();
                        }
                    } catch (Exception e) {
                        Main.plugin.getLogger().warning(
                                "[SQLite Migration] Bad warp in team " + teamId + ": " + e.getMessage());
                    }
                }

                for (Map<String, Object> c : chests) {
                    String loc = str(c.get("chestloc"));
                    if (loc == null || loc.isEmpty()) continue;
                    try {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT OR IGNORE INTO " + SQLiteStorageManager.T_CHEST_CLAIMS
                                + " (teamID,chestLoc) VALUES (?,?)")) {
                            ps.setString(1, teamId);
                            ps.setString(2, loc);
                            ps.executeUpdate();
                        }
                    } catch (Exception e) {
                        Main.plugin.getLogger().warning(
                                "[SQLite Migration] Bad chest claim in team " + teamId + ": " + e.getMessage());
                    }
                }

                for (Map<String, Object> m : meta) {
                    String key = str(m.get("metakey"));
                    String val = str(m.get("metavalue"));
                    if (key == null || key.isEmpty()) continue;
                    try {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT OR IGNORE INTO " + SQLiteStorageManager.T_TEAM_META
                                + " (teamID,metaKey,metaValue) VALUES (?,?,?)")) {
                            ps.setString(1, teamId);
                            ps.setString(2, key);
                            ps.setString(3, val != null ? val : "");
                            ps.executeUpdate();
                        }
                    } catch (Exception e) {
                        Main.plugin.getLogger().warning(
                                "[SQLite Migration] Bad meta key '" + key + "' in team " + teamId);
                    }
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(prev);
            }
            return null;
        }).join();
    }

    private List<Map<String, Object>> readAll(Connection conn, String sql, String... params)
            throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        row.put(md.getColumnName(i).toLowerCase(Locale.ROOT), rs.getObject(i));
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private void createTables(Statement st) throws SQLException {
        st.execute("CREATE TABLE IF NOT EXISTS " + SQLiteStorageManager.T_TEAMS
                + " (teamID TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL,"
                + " description TEXT NOT NULL DEFAULT '', open INTEGER NOT NULL DEFAULT 0,"
                + " score INTEGER NOT NULL DEFAULT 0, money REAL NOT NULL DEFAULT 0,"
                + " home TEXT NOT NULL DEFAULT '', color TEXT NOT NULL DEFAULT '6',"
                + " echest TEXT NOT NULL DEFAULT '', level INTEGER NOT NULL DEFAULT 1,"
                + " tag TEXT NOT NULL DEFAULT '', pvp INTEGER NOT NULL DEFAULT 0,"
                + " anchor INTEGER NOT NULL DEFAULT 0)");

        st.execute("CREATE TABLE IF NOT EXISTS " + SQLiteStorageManager.T_PLAYERS
                + " (playerUUID TEXT NOT NULL PRIMARY KEY, teamID TEXT NOT NULL,"
                + " playerRank INTEGER NOT NULL, title TEXT NOT NULL DEFAULT '',"
                + " anchor INTEGER NOT NULL DEFAULT 0,"
                + " FOREIGN KEY (teamID) REFERENCES " + SQLiteStorageManager.T_TEAMS
                + "(teamID) ON DELETE CASCADE)");

        st.execute("CREATE TABLE IF NOT EXISTS " + SQLiteStorageManager.T_ALLY_REQUESTS
                + " (requestingTeamID TEXT NOT NULL, receivingTeamID TEXT NOT NULL,"
                + " PRIMARY KEY (requestingTeamID, receivingTeamID),"
                + " FOREIGN KEY (requestingTeamID) REFERENCES " + SQLiteStorageManager.T_TEAMS
                + "(teamID) ON DELETE CASCADE,"
                + " FOREIGN KEY (receivingTeamID) REFERENCES " + SQLiteStorageManager.T_TEAMS
                + "(teamID) ON DELETE CASCADE)");

        st.execute("CREATE TABLE IF NOT EXISTS " + SQLiteStorageManager.T_WARPS
                + " (teamID TEXT NOT NULL, warpInfo TEXT NOT NULL,"
                + " PRIMARY KEY (teamID, warpInfo),"
                + " FOREIGN KEY (teamID) REFERENCES " + SQLiteStorageManager.T_TEAMS
                + "(teamID) ON DELETE CASCADE)");

        st.execute("CREATE TABLE IF NOT EXISTS " + SQLiteStorageManager.T_CHEST_CLAIMS
                + " (teamID TEXT NOT NULL, chestLoc TEXT NOT NULL,"
                + " PRIMARY KEY (teamID, chestLoc),"
                + " FOREIGN KEY (teamID) REFERENCES " + SQLiteStorageManager.T_TEAMS
                + "(teamID) ON DELETE CASCADE)");

        st.execute("CREATE TABLE IF NOT EXISTS " + SQLiteStorageManager.T_BANS
                + " (playerUUID TEXT NOT NULL, teamID TEXT NOT NULL,"
                + " PRIMARY KEY (playerUUID, teamID),"
                + " FOREIGN KEY (teamID) REFERENCES " + SQLiteStorageManager.T_TEAMS
                + "(teamID) ON DELETE CASCADE)");

        st.execute("CREATE TABLE IF NOT EXISTS " + SQLiteStorageManager.T_ALLIES
                + " (team1ID TEXT NOT NULL, team2ID TEXT NOT NULL,"
                + " PRIMARY KEY (team1ID, team2ID),"
                + " FOREIGN KEY (team1ID) REFERENCES " + SQLiteStorageManager.T_TEAMS
                + "(teamID) ON DELETE CASCADE,"
                + " FOREIGN KEY (team2ID) REFERENCES " + SQLiteStorageManager.T_TEAMS
                + "(teamID) ON DELETE CASCADE)");

        st.execute("CREATE TABLE IF NOT EXISTS " + SQLiteStorageManager.T_TEAM_META
                + " (teamID TEXT NOT NULL, metaKey TEXT NOT NULL, metaValue TEXT,"
                + " PRIMARY KEY (teamID, metaKey),"
                + " FOREIGN KEY (teamID) REFERENCES " + SQLiteStorageManager.T_TEAMS
                + "(teamID) ON DELETE CASCADE)");
    }

    private String safe(String v, String fallback) {
        return (v != null && !v.isEmpty()) ? v : fallback;
    }

    private String str(Object o) {
        return (o == null) ? null : o.toString();
    }

    private int toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return 0; }
    }

    private double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0.0; }
    }

    private boolean toBool(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean) return (Boolean) o;
        if (o instanceof Number) return ((Number) o).intValue() != 0;
        String s = o.toString().trim().toLowerCase(Locale.ROOT);
        return s.equals("1") || s.equals("true");
    }
}
