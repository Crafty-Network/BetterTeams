package com.booksaw.betterTeams.team.storage;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.team.TeamManager;
import com.booksaw.betterTeams.team.storage.storageManager.SQLiteStorageManager;

public enum StorageType {

    FLATFILE,

    YAML,

    SQL,

    SQLITE;

    public TeamManager getNewTeamManager() {
        if (this != SQLITE) {
            Main.plugin.getLogger().warning(
                    "[BetterTeams] Storage type '" + this.name() + "' is no longer supported " +
                    "as a runtime storage back-end. Using SQLite instead. " +
                    "Set storageType=SQLITE in config.yml and ensure migration has run.");
        }
        return new SQLiteStorageManager();
    }

    public static StorageType getStorageType(String str) {
        if (str == null) return SQLITE;
        switch (str.trim().toUpperCase()) {
            case "FLATFILE": return FLATFILE;
            case "YAML":     return YAML;
            case "SQL":      return SQL;
            case "SQLITE":   return SQLITE;
            default:         return SQLITE;
        }
    }
}
