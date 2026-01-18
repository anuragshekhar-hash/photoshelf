package org.photoshelf;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {
    private static final String DB_NAME = "photoshelf_db";
    private Connection connection;
    private final String dbPath;

    public DatabaseManager() {
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

    private void initializeDatabase() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:" + dbPath + ";AUTO_SERVER=TRUE", "sa", "");
        
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

            // Face Embeddings table
            // Storing float array as BLOB or serialized object
            stmt.execute("CREATE TABLE IF NOT EXISTS face_embeddings (" +
                    "file_path VARCHAR(1024) PRIMARY KEY, " +
                    "embedding ARRAY NOT NULL, " + // H2 supports ARRAY type
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
        migrateHashes(new File(cacheDir, "phash_cache.ser"));
        migrateEmbeddings(new File(cacheDir, "face_embeddings.ser"));
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

    @SuppressWarnings("unchecked")
    private void migrateHashes(File file) {
        if (!file.exists()) return;
        System.out.println("Migrating hashes from " + file.getName() + "...");

        // We need to access the private inner class CacheEntry structure from PHashCacheManager
        // Since we can't easily cast to it here without reflection or dependency, 
        // we'll assume the map is Map<String, PHashCacheManager.CacheEntry>
        // But CacheEntry is private. We might need to temporarily make it public or use reflection.
        // Or simpler: Just let the new system rebuild the cache over time, or try a best-effort read.
        
        // Actually, since we are refactoring PHashCacheManager anyway, we can just delete the old file
        // and let it rebuild. But for a smooth transition, let's try to read it.
        // The issue is deserializing a class that might not exist or has changed.
        // Given the complexity of deserializing private inner classes from a different context,
        // and that pHash calculation is fast, we might skip this or handle it in PHashCacheManager itself.
        
        // Strategy: We will handle migration INSIDE the new PHashCacheManager constructor 
        // before we switch it to use DB exclusively.
        // So this method is just a placeholder or we skip it here.
        System.out.println("Skipping hash migration here. Will be handled by manager.");
    }

    private void migrateEmbeddings(File file) {
        // Similar issue with private inner classes. 
        // We will handle this in the respective managers.
    }
}
