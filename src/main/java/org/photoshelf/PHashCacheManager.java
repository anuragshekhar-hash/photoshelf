package org.photoshelf;

import org.photoshelf.service.PluginManager;

import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PHashCacheManager {
    private final DatabaseManager dbManager;

    public PHashCacheManager() {
        this.dbManager = DatabaseManager.getInstance();
        migrateLegacyCache();
    }

    @SuppressWarnings("unchecked")
    private void migrateLegacyCache() {
        File legacyFile = new File("phash_cache.ser");
        if (!legacyFile.exists()) return;

        System.out.println("Migrating pHash cache to database...");
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(legacyFile))) {
            System.out.println("Legacy cache found but skipping migration due to class structure change. Cache will be rebuilt.");
            legacyFile.renameTo(new File("phash_cache.ser.bak"));
        } catch (Exception e) {
            System.err.println("Error reading legacy cache: " + e.getMessage());
        }
    }

    public void saveCache() {
        // No-op, DB is auto-saved
    }

    public String getHash(File file) throws IOException {
        if (file == null || !file.exists()) {
            return null;
        }

        String filePath = file.getAbsolutePath();
        long currentModified = file.lastModified();

        Connection conn = dbManager.getConnection();
        if (conn == null) return null;

        // Check DB
        String sqlSelect = "SELECT hash, last_modified FROM image_hashes WHERE file_path = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sqlSelect)) {
            pstmt.setString(1, filePath);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String hash = rs.getString("hash");
                    long lastModified = rs.getLong("last_modified");
                    if (lastModified == currentModified) {
                        return hash;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Calculate new hash
        String hash;
        try {
            hash = PHash.getHash(file);
        } catch (IOException e) {
            // Try getting thumbnail from plugins (e.g. for video)
            BufferedImage thumb = PluginManager.getInstance().getThumbnail(file);
            if (thumb != null) {
                hash = PHash.getHash(thumb);
            } else {
                throw e;
            }
        }
        
        // Update DB
        String sqlMerge = "MERGE INTO image_hashes (file_path, hash, last_modified) KEY(file_path) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sqlMerge)) {
            pstmt.setString(1, filePath);
            pstmt.setString(2, hash);
            pstmt.setLong(3, currentModified);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return hash;
    }

    public Set<String> getAllFilePaths() {
        Set<String> paths = new HashSet<>();
        String sql = "SELECT file_path FROM image_hashes";
        Connection conn = dbManager.getConnection();
        if (conn == null) return paths;
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                paths.add(rs.getString("file_path"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return paths;
    }

    public Map<String, String> getAllHashes() {
        Map<String, String> result = new HashMap<>();
        String sql = "SELECT file_path, hash FROM image_hashes";
        Connection conn = dbManager.getConnection();
        if (conn == null) return result;
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("file_path"), rs.getString("hash"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
}
