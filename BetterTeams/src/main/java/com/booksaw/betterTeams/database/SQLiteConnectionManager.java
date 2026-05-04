package com.booksaw.betterTeams.database;

import com.booksaw.betterTeams.Main;
import lombok.Getter;

import java.io.File;
import java.sql.*;
import java.util.concurrent.*;
import java.util.logging.Level;

public class SQLiteConnectionManager {

    private static final String DB_THREAD_NAME = "BetterTeams-SQLite";

    private Thread dbThread;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        dbThread = new Thread(r, DB_THREAD_NAME);
        dbThread.setDaemon(true);
        return dbThread;
    });

    @Getter
    private Connection connection;

    private final File databaseFile;

    public SQLiteConnectionManager(File dataFolder) {
        this.databaseFile = new File(dataFolder, "betterteams.db");
    }

    public void open() throws Exception {
        submit(() -> {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + databaseFile.getAbsolutePath());
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=DELETE");
                st.execute("PRAGMA foreign_keys=ON");
                st.execute("PRAGMA synchronous=NORMAL");
            }
            return null;
        }).get(); 
    }

    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                Main.plugin.getLogger().warning(
                        "[SQLite] Executor did not terminate in 10 s – forcing shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            Main.plugin.getLogger().log(Level.SEVERE,
                    "[SQLite] Error closing connection", e);
        }
    }

    public <T> CompletableFuture<T> submit(ThrowingCallable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                future.complete(task.call());
            } catch (Throwable ex) {
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    public CompletableFuture<Void> execute(ThrowingRunnable task) {
        return submit((ThrowingCallable<Void>) () -> { task.run(); return null; })
            .whenComplete((v, ex) -> {
                if (ex != null)
                    Main.plugin.getLogger().log(Level.SEVERE,
                            "[SQLite] Async write failed", ex);
            });
    }

    public <T> T runWithAffinity(ThrowingCallable<T> task) {
        if (Thread.currentThread() == dbThread) {
            
            try {
                return task.call();
            } catch (Exception ex) {
                throw new RuntimeException("[SQLite] Direct execution failed", ex);
            }
        }
        
        return submit(task).join();
    }

    @FunctionalInterface
    public interface ThrowingCallable<T> {
        T call() throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
