package org.photoshelf;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {
    private static final String DB_NAME = "photoshelf_db";
    private static DatabaseManager instance;
    private Connection connection;
    private final String dbPath;

    private DatabaseManager() {
        String userHome = System.getProperty("user.home");
        File cacheDir = new File(userHome, ".photoshelf_cache");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        this.dbPath = new File(cacheDir, DB_NAME).getAbsolutePath();
        
        try {
            initializeDatabase();
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializeDatabase() throws SQLException {
        // Removed AUTO_SERVER=TRUE to avoid conflict with DB_CLOSE_ON_EXIT=FALSE
        // Kept DB_CLOSE_ON_EXIT=FALSE to prevent premature closing during shutdown
        connection = DriverManager.getConnection("jdbc:h2:" + dbPath + ";DB_CLOSE_ON_EXIT=FALSE", "sa", "");
        
        try (Statement stmt = connection.createStatement()) {
            // Keywords table
            stmt.execute("CREATE TABLE IF NOT EXISTS keywords (" +
                    "file_path VARCHAR(1024) NOT NULL, " +
                    "keyword VARCHAR(255) NOT NULL, " +
                    "PRIMARY KEY (file_path, keyword))");
            
            // Image Hashes table (pHash)
            stmt.execute("CREATE TABLE IF NOT EXISTS image_hashes (" +
                    "file_path VARCHAR(1024) PRIMARY KEY, " +
                    "hash VARCHAR(64) NOT NULL, " +
                    "last_modified BIGINT NOT NULL)");
            
            // Index for faster lookups
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_keywords_path ON keywords(file_path)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_hashes_path ON image_hashes(file_path)");
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        // Since it's a singleton, we might not want individual managers closing it.
        // But we can provide a method for the main app shutdown.
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- Migration Logic ---

    public void migrateFromLegacyCache() {
        String userHome = System.getProperty("user.home");
        File cacheDir = new File(userHome, ".photoshelf_cache");

        migrateKeywords(new File(cacheDir, "keywords.ser"));
        // Hashes and Embeddings are rebuilt automatically, so we skip complex migration for them to avoid issues.
    }

    @SuppressWarnings("unchecked")
    private void migrateKeywords(File file) {
        if (!file.exists()) return;
        System.out.println("Migrating keywords from " + file.getName() + "...");
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Map<String, Set<String>> keywordMap = (Map<String, Set<String>>) ois.readObject();
            
            String sql = "MERGE INTO keywords (file_path, keyword) KEY(file_path, keyword) VALUES (?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                connection.setAutoCommit(false);
                for (Map.Entry<String, Set<String>> entry : keywordMap.entrySet()) {
                    String path = entry.getKey();
                    for (String keyword : entry.getValue()) {
                        pstmt.setString(1, path);
                        pstmt.setString(2, keyword);
                        pstmt.addBatch();
                    }
                }
                pstmt.executeBatch();
                connection.commit();
                connection.setAutoCommit(true);
            }
            System.out.println("Keywords migration complete.");
            // Rename old file to .bak
            file.renameTo(new File(file.getAbsolutePath() + ".migrated"));
        } catch (Exception e) {
            System.err.println("Error migrating keywords: " + e.getMessage());
        }
    }
}
