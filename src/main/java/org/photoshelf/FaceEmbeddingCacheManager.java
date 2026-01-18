package org.photoshelf;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class FaceEmbeddingCacheManager {
    private final DatabaseManager dbManager;

    public FaceEmbeddingCacheManager() {
        this.dbManager = new DatabaseManager();
    }

    public void saveCache() {
        // No-op
    }

    public float[] getEmbedding(File file) {
        if (file == null || !file.exists()) return null;
        
        String sql = "SELECT embedding, last_modified FROM face_embeddings WHERE file_path = ?";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, file.getAbsolutePath());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    long lastModified = rs.getLong("last_modified");
                    if (lastModified == file.lastModified()) {
                        Object[] objArray = (Object[]) rs.getArray("embedding").getArray();
                        float[] embedding = new float[objArray.length];
                        for (int i = 0; i < objArray.length; i++) {
                            embedding[i] = ((Number) objArray[i]).floatValue();
                        }
                        return embedding;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void putEmbedding(File file, float[] embedding) {
        if (file == null || !file.exists()) return;
        
        String sql = "MERGE INTO face_embeddings (file_path, embedding, last_modified) KEY(file_path) VALUES (?, ?, ?)";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, file.getAbsolutePath());
            
            // Convert float[] to Object[] for H2 ARRAY
            Object[] objArray = new Object[embedding.length];
            for (int i = 0; i < embedding.length; i++) {
                objArray[i] = embedding[i];
            }
            pstmt.setObject(2, objArray);
            
            pstmt.setLong(3, file.lastModified());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public boolean hasEmbedding(File file) {
        if (file == null || !file.exists()) return false;
        String sql = "SELECT last_modified FROM face_embeddings WHERE file_path = ?";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, file.getAbsolutePath());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("last_modified") == file.lastModified();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Map<String, float[]> getAllEmbeddings() {
        Map<String, float[]> result = new HashMap<>();
        String sql = "SELECT file_path, embedding, last_modified FROM face_embeddings";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String path = rs.getString("file_path");
                File file = new File(path);
                if (file.exists() && file.lastModified() == rs.getLong("last_modified")) {
                    Object[] objArray = (Object[]) rs.getArray("embedding").getArray();
                    float[] embedding = new float[objArray.length];
                    for (int i = 0; i < objArray.length; i++) {
                        embedding[i] = ((Number) objArray[i]).floatValue();
                    }
                    result.put(path, embedding);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }
}
