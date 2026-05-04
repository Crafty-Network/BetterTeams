package com.booksaw.betterTeams.team.storage;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.team.TeamManager;
import com.booksaw.betterTeams.team.storage.storageManager.SQLiteStorageManager;

public enum StorageType {

    /**
    * FLATFILE is the storage method pre 4.0 where all team data is stored within a
    * single file
    */
    FLATFILE,

    /**
    * YAML is where team data is all stored in individual team files along with
    * teams.yml to contain pointers to the correct file
    */
    YAML,

    SQL,

    SQLITE;

    /**
    * @return the teamStorageManager relevant to the storageType
    */
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
            /**
            * SQL is where team data is stored in a mySQL database
            */
            case "SQL":      return SQL;
            case "SQLITE":   return SQLITE;
            default:         return SQLITE;
        }
    }
}
