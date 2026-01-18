package org.photoshelf;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class KeywordManager {
    private final DatabaseManager dbManager;

    public KeywordManager() {
        this.dbManager = new DatabaseManager();
        // Trigger migration if needed
        dbManager.migrateFromLegacyCache();
    }

    public void addKeyword(File imageFile, String keyword) {
        String sql = "MERGE INTO keywords (file_path, keyword) KEY(file_path, keyword) VALUES (?, ?)";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, imageFile.getAbsolutePath());
            pstmt.setString(2, keyword.toLowerCase());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeKeyword(File imageFile, String keyword) {
        String sql = "DELETE FROM keywords WHERE file_path = ? AND keyword = ?";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, imageFile.getAbsolutePath());
            pstmt.setString(2, keyword.toLowerCase());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void renameKeyword(String oldKeyword, String newKeyword) {
        String sql = "UPDATE keywords SET keyword = ? WHERE keyword = ?";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newKeyword.toLowerCase());
            pstmt.setString(2, oldKeyword.toLowerCase());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Set<String> getKeywords(File imageFile) {
        Set<String> keywords = new HashSet<>();
        String sql = "SELECT keyword FROM keywords WHERE file_path = ?";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, imageFile.getAbsolutePath());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    keywords.add(rs.getString("keyword"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return keywords;
    }

    public boolean hasKeyword(File imageFile, String keyword) {
        String sql = "SELECT 1 FROM keywords WHERE file_path = ? AND keyword = ? LIMIT 1";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, imageFile.getAbsolutePath());
            pstmt.setString(2, keyword.toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void renameFile(File oldFile, File newFile) {
        String sql = "UPDATE keywords SET file_path = ? WHERE file_path = ?";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newFile.getAbsolutePath());
            pstmt.setString(2, oldFile.getAbsolutePath());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteFile(File file) {
        String sql = "DELETE FROM keywords WHERE file_path = ?";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, file.getAbsolutePath());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void copyKeywords(File source, File destination) {
        Set<String> keywords = getKeywords(source);
        if (!keywords.isEmpty()) {
            addKeywords(destination, new ArrayList<>(keywords));
        }
    }

    public Set<String> getAllKeywords() {
        Set<String> keywords = new HashSet<>();
        String sql = "SELECT DISTINCT keyword FROM keywords";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                keywords.add(rs.getString("keyword"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return keywords;
    }

    public int cleanup() {
        int removedCount = 0;
        String selectSql = "SELECT DISTINCT file_path FROM keywords";
        String deleteSql = "DELETE FROM keywords WHERE file_path = ?";
        
        Connection conn = dbManager.getConnection();
        try (PreparedStatement selectStmt = conn.prepareStatement(selectSql);
             PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
             ResultSet rs = selectStmt.executeQuery()) {
            
            while (rs.next()) {
                String path = rs.getString("file_path");
                if (!new File(path).exists()) {
                    deleteStmt.setString(1, path);
                    deleteStmt.addBatch();
                    removedCount++;
                }
            }
            if (removedCount > 0) {
                deleteStmt.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return removedCount;
    }

    public void shutdown() {
        dbManager.close();
    }

    public void addKeywords(File newFile, ArrayList<String> strings) {
        String sql = "MERGE INTO keywords (file_path, keyword) KEY(file_path, keyword) VALUES (?, ?)";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (String s : strings) {
                pstmt.setString(1, newFile.getAbsolutePath());
                pstmt.setString(2, s.toLowerCase());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
