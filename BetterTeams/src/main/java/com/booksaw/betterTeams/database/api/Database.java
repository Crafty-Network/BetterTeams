package com.booksaw.betterTeams.database.api;

import com.booksaw.betterTeams.Main;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.*;

/**
* API Class to managing databases
*
* @author booksaw
*/
public class Database {

    /**
    * Stores the information to create a connection to the database
    */
    private String host;
    private int port;
    private String database;
    private String user;
    private String password;
    private String additionalOptions = "";

    Connection connection;

    /**
    * Used to setup a connection from the provided data
    *
    * @param section The configuration section which contains the database
    *                information
    */
    public void setupConnectionFromConfiguration(ConfigurationSection section) {
        host     = section.getString("host",     "localhost");
        port     = section.getInt("port",        3306);
        database = section.getString("database", "spigot");
        user     = section.getString("user",     "root");
        password = section.getString("password", "password");

        StringBuilder sb = new StringBuilder();
        for (String property : section.getStringList("storageProperties")) {
            sb.append("&").append(property);
        }
        additionalOptions = sb.toString();

        /**
        * Used to setup a connection from the provided data
        */
        setupConnection();
    }

    public void setupConnection() {
        Main.plugin.getLogger().info("Attempting to connect to MySQL database for migration");
        try {
            connection = DriverManager.getConnection(
                    "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?autoReconnect=true" + additionalOptions,
                    user, password);
            if (connection == null || connection.isClosed()) {
                throw new SQLException("Connection object is null or already closed");
            }
            Main.plugin.getLogger().info("MySQL connection established for migration");
        } catch (Exception e) {
            Main.plugin.getLogger().severe(
                    "Could not connect to MySQL — migration aborted. " +
                    "Check database credentials in config.yml.");
            e.printStackTrace();
            Main.plugin.getServer().getPluginManager().disablePlugin(Main.plugin);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    /**
    * Used to close the connection to the database
    */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            Main.plugin.getLogger().severe("Error closing MySQL connection: " + e.getMessage());
        }
    }
}