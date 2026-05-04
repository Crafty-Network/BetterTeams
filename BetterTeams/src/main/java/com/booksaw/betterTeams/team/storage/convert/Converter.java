package com.booksaw.betterTeams.team.storage.convert;

import com.booksaw.betterTeams.Main;
import com.booksaw.betterTeams.team.storage.StorageType;

public abstract class Converter {

    public static Converter getConverter(StorageType from, StorageType to) {
        if (from == to) {
            return null;
        }
        if (to == StorageType.SQLITE) {
            if (from == StorageType.YAML) {
                return new YamlToSQLite();
            }
            if (from == StorageType.SQL) {
                return new SqlToSQLite();
            }
            if (from == StorageType.FLATFILE) {
                Main.plugin.getLogger().warning(
                        "[BetterTeams] Direct FLATFILE → SQLITE migration is not supported. " +
                        "No data has been migrated. Starting with an empty SQLite database.");
            }
        }
        return null;
    }

    protected void log(String message) {
        Main.plugin.getLogger().info(message);
    }

    public void convertStorage() {
        log("Starting storage conversion — this may take a while…");
        convert();
        log("Storage conversion complete.");
    }

    protected abstract void convert();
}
