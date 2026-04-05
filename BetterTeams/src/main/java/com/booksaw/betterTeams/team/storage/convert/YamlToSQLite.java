package com.booksaw.betterTeams.team.storage.convert;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.PlayerRank;
import com.booksaw.betterTeams.Utils;
import com.booksaw.betterTeams.database.SQLiteConnectionManager;
import com.booksaw.betterTeams.team.storage.storageManager.SQLiteStorageManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.*;

public class YamlToSQLite extends Converter {

    @Override
    protected void convert() {

        File teamsFile = new File("plugins/BetterTeams/teams.yml");
        if (!teamsFile.exists()) {
            Main.plugin.saveResource("teams.yml", false);
        }
        YamlConfiguration teamStorage = YamlConfiguration.loadConfiguration(teamsFile);

        log("[SQLite Migration] Backing up /BetterTeams/ folder...");
        try {
            pack("plugins/BetterTeams/", "plugins/BetterTeamsBACKUP_to_sqlite.zip");
            log("[SQLite Migration] Backup written to plugins/BetterTeamsBACKUP_to_sqlite.zip");
        } catch (IOException e) {
            log("[SQLite Migration] Backup FAILED — aborting migration for data safety. Fix the error and restart.");
            e.printStackTrace();
            return;
        }

        File dataFolder = new File("plugins/BetterTeams");
        SQLiteConnectionManager db = new SQLiteConnectionManager(dataFolder);
        try {
            db.open();
        } catch (Exception e) {
            log("[SQLite Migration] Failed to open SQLite database — aborting.");
            e.printStackTrace();
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
            return;
        }

        File teamInfoDir = new File("plugins/BetterTeams/teamInfo");
        File[] files = teamInfoDir.isDirectory() ? teamInfoDir.listFiles() : new File[0];
        if (files == null) files = new File[0];

        int total = 0;
        for (File f : files) {
            if (f.getName().endsWith(".yml")) total++;
        }
        log("[SQLite Migration] Found " + total + " team file(s) to migrate.");

        int migrated = 0;
        int failed   = 0;
        for (File f : files) {
            if (!f.getName().endsWith(".yml")) continue;

            String uuidStr = f.getName().replace(".yml", "");
            try {
                UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                log("[SQLite Migration] Skipping non-UUID filename: " + f.getName());
                continue;
            }

            try {
                migrateTeamFile(db, f, uuidStr);
                migrated++;
                log("[SQLite Migration] Team " + uuidStr + " (" + migrated + "/" + total + ") OK");
            } catch (Exception e) {
                failed++;
                log("[SQLite Migration] WARNING: Team " + uuidStr
                        + " failed (" + migrated + "/" + total + "): " + e.getMessage());
                e.printStackTrace();
            }
        }

        log("[SQLite Migration] Migrating chest claims...");
        int claimsFailed = 0;
        for (String entry : teamStorage.getStringList("chestClaims")) {
            
            String[] parts = entry.split(";", 2);
            if (parts.length != 2) {
                log("[SQLite Migration] WARNING: Invalid chest-claim entry (skipped): " + entry);
                claimsFailed++;
                continue;
            }
            String locStr  = parts[0];
            String teamId  = parts[1];
            try {
                UUID.fromString(teamId);
                db.submit(() -> {
                    try (PreparedStatement ps = db.getConnection().prepareStatement(
                            "INSERT OR IGNORE INTO " + SQLiteStorageManager.T_CHEST_CLAIMS
                            + " (teamID, chestLoc) VALUES (?, ?)")) {
                        ps.setString(1, teamId);
                        ps.setString(2, locStr);
                        ps.executeUpdate();
                    }
                    return null;
                }).join();
            } catch (Exception e) {
                claimsFailed++;
                log("[SQLite Migration] WARNING: Chest claim failed (" + entry + "): " + e.getMessage());
            }
        }
        log("[SQLite Migration] Chest claims done (" + claimsFailed + " failed).");

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

        db.submit(() -> {
            try (Statement st = db.getConnection().createStatement()) {
                st.execute("PRAGMA foreign_keys=ON");
            }
            return null;
        }).join();
        db.close();

        teamStorage.set("storageType", "SQLITE");
        teamStorage.set("chestClaims",    null);
        teamStorage.set("playerLookup",   null);
        teamStorage.set("teamNameLookup", null);
        try {
            teamStorage.save(teamsFile);
            log("[SQLite Migration] teams.yml updated — storageType set to SQLITE.");
        } catch (IOException e) {
            log("[SQLite Migration] WARNING: Could not update teams.yml storageType. "
                    + "Migration will re-run on next start (safe — INSERT OR IGNORE prevents duplicates).");
            e.printStackTrace();
        }

        log("[SQLite Migration] ================================================");
        log("[SQLite Migration] YAML → SQLite migration complete.");
        log("[SQLite Migration]   Teams migrated : " + migrated + " / " + total);
        if (failed > 0) {
            log("[SQLite Migration]   Teams FAILED   : " + failed + " (check logs above)");
        }
        log("[SQLite Migration]   Chest claims   : " + claimsFailed + " failed");
        log("[SQLite Migration] Old YAML files are preserved in teamInfo/ and");
        log("[SQLite Migration]   plugins/BetterTeamsBACKUP_to_sqlite.zip");
        log("[SQLite Migration] ================================================");
    }

    private void migrateTeamFile(SQLiteConnectionManager db, File f, String uuidStr) throws Exception {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);

        final String  name        = safe(cfg.getString("name"),        uuidStr);
        final String  description = safe(cfg.getString("description"), "");
        final boolean open        = cfg.getBoolean("open",  false);
        final int     score       = cfg.getInt("score",     0);
        final double  money       = cfg.getDouble("money",  0.0);
        final String  home        = safe(cfg.getString("home"),  "");
        final String  color       = safe(cfg.getString("color"), "6");
        final int     level       = cfg.getInt("level",     1);
        final String  tag         = safe(cfg.getString("tag"), "");
        final boolean pvp         = cfg.getBoolean("pvp",   false);
        final boolean anchor      = cfg.getBoolean("anchor",false);

        Inventory inv = Bukkit.createInventory(null, 27);
        for (int i = 0; i < 27; i++) {
            ItemStack is = cfg.getItemStack("echest." + i);
            if (is != null) inv.setItem(i, is);
        }
        final String echest = safe(Utils.serializeInventory(inv), "");

        final List<String> players         = cfg.getStringList("players");
        final List<String> anchoredPlayers = cfg.getStringList("anchoredPlayers");
        final List<String> bans            = cfg.getStringList("bans");
        final List<String> allies          = cfg.getStringList("allies");
        final List<String> allyRequests    = cfg.getStringList("allyrequests");
        final List<String> warps           = cfg.getStringList("warps");

        final Map<String, String> meta = new LinkedHashMap<>();
        ConfigurationSection metaSection = cfg.getConfigurationSection("meta");
        if (metaSection != null) {
            for (String key : metaSection.getKeys(false)) {
                String val = metaSection.getString(key);
                if (val != null) meta.put(key, val);
            }
        }

        db.submit(() -> {
            Connection conn = db.getConnection();
            boolean prevAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT OR IGNORE INTO " + SQLiteStorageManager.T_TEAMS
                        + " (teamID,name,description,open,score,money,home,color,echest,level,tag,pvp,anchor)"
                        + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                    ps.setString(1,  uuidStr);
                    ps.setString(2,  name);
                    ps.setString(3,  description);
                    ps.setInt(4,     open   ? 1 : 0);
                    ps.setInt(5,     score);
                    ps.setDouble(6,  money);
                    ps.setString(7,  home);
                    ps.setString(8,  color);
                    ps.setString(9,  echest);
                    ps.setInt(10,    level);
                    ps.setString(11, tag);
                    ps.setInt(12,    pvp    ? 1 : 0);
                    ps.setInt(13,    anchor ? 1 : 0);
                    ps.executeUpdate();
                }

                for (String entry : players) {
                    try {
                        String[] p = entry.split(",", 3);
                        if (p.length < 2) continue;
                        String playerUUID = p[0].trim();
                        PlayerRank rank   = PlayerRank.getRank(p[1].trim());
                        String title      = (p.length >= 3) ? p[2].trim() : "";
                        boolean pAnchor   = anchoredPlayers.contains(playerUUID);
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT OR IGNORE INTO " + SQLiteStorageManager.T_PLAYERS
                                + " (playerUUID,teamID,playerRank,title,anchor) VALUES (?,?,?,?,?)")) {
                            ps.setString(1, playerUUID);
                            ps.setString(2, uuidStr);
                            ps.setInt(3,    rank.value);
                            ps.setString(4, title);
                            ps.setInt(5,    pAnchor ? 1 : 0);
                            ps.executeUpdate();
                        }
                    } catch (Exception e) {
                        Main.plugin.getLogger().warning(
                                "[SQLite Migration] Bad player entry '" + entry
                                + "' in team " + uuidStr + ": " + e.getMessage());
                    }
                }

                for (String ban : bans) {
                    try {
                        UUID.fromString(ban);
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT OR IGNORE INTO " + SQLiteStorageManager.T_BANS
                                + " (playerUUID,teamID) VALUES (?,?)")) {
                            ps.setString(1, ban);
                            ps.setString(2, uuidStr);
                            ps.executeUpdate();
                        }
                    } catch (Exception e) {
                        Main.plugin.getLogger().warning(
                                "[SQLite Migration] Bad ban entry '" + ban
                                + "' in team " + uuidStr);
                    }
                }

                for (String ally : allies) {
                    try {
                        UUID.fromString(ally);
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT OR IGNORE INTO " + SQLiteStorageManager.T_ALLIES
                                + " (team1ID,team2ID) VALUES (?,?)")) {
                            ps.setString(1, uuidStr);
                            ps.setString(2, ally);
                            ps.executeUpdate();
                        }
                    } catch (Exception e) {
                        Main.plugin.getLogger().warning(
                                "[SQLite Migration] Bad ally entry '" + ally
                                + "' in team " + uuidStr);
                    }
                }

                for (String req : allyRequests) {
                    try {
                        UUID.fromString(req);
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT OR IGNORE INTO " + SQLiteStorageManager.T_ALLY_REQUESTS
                                + " (requestingTeamID,receivingTeamID) VALUES (?,?)")) {
                            ps.setString(1, req);
                            ps.setString(2, uuidStr);
                            ps.executeUpdate();
                        }
                    } catch (Exception e) {
                        Main.plugin.getLogger().warning(
                                "[SQLite Migration] Bad ally-request entry '" + req
                                + "' in team " + uuidStr);
                    }
                }

                for (String warp : warps) {
                    if (warp == null || warp.trim().isEmpty()) continue;
                    try {
                        try (PreparedStatement ps = conn.prepareStatement(
                                "INSERT OR IGNORE INTO " + SQLiteStorageManager.T_WARPS
                                + " (teamID,warpInfo) VALUES (?,?)")) {
                            ps.setString(1, uuidStr);
                            ps.setString(2, warp);
                            ps.executeUpdate();
                        }
                    } catch (Exception e) {
                        Main.plugin.getLogger().warning(
                                "[SQLite Migration] Bad warp entry '" + warp
                                + "' in team " + uuidStr);
                    }
                }

                if (!meta.isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT OR IGNORE INTO " + SQLiteStorageManager.T_TEAM_META
                            + " (teamID,metaKey,metaValue) VALUES (?,?,?)")) {
                        for (Map.Entry<String, String> e : meta.entrySet()) {
                            ps.setString(1, uuidStr);
                            ps.setString(2, e.getKey());
                            ps.setString(3, e.getValue());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(prevAutoCommit);
            }
            return null;
        }).join();
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

    private static void pack(String sourceDir, String destZip) throws IOException {
        File source = new File(sourceDir);
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destZip))) {
            packDir(source, source, zos);
        }
    }

    private static void packDir(File root, File current, ZipOutputStream zos) throws IOException {
        File[] files = current.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                packDir(root, f, zos);
            } else {
                String entryName = root.toURI().relativize(f.toURI()).getPath();
                zos.putNextEntry(new ZipEntry(entryName));
                try (FileInputStream fis = new FileInputStream(f)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = fis.read(buf)) > 0) {
                        zos.write(buf, 0, len);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    private String safe(String value, String fallback) {
        return (value != null && !value.isEmpty()) ? value : fallback;
    }
}
