package org.photoshelf;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PHashCacheManager {
    private final String cachePath = "phash_cache.ser";
    private Map<String, CacheEntry> cache;
    private boolean isDirty = false;

    private static class CacheEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        String hash;
        long lastModified;

        CacheEntry(String hash, long lastModified) {
            this.hash = hash;
            this.lastModified = lastModified;
        }
    }

    public PHashCacheManager() {
        loadCache();
    }

    @SuppressWarnings("unchecked")
    public void loadCache() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cachePath))) {
            cache = (Map<String, CacheEntry>) ois.readObject();
            System.out.println("pHash cache loaded with " + cache.size() + " entries.");
        } catch (FileNotFoundException e) {
            System.out.println("pHash cache not found. A new one will be created.");
            cache = new ConcurrentHashMap<>();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Could not load pHash cache: " + e.getMessage());
            cache = new ConcurrentHashMap<>();
        }
    }

    public void saveCache() {
        if (!isDirty) {
            System.out.println("pHash cache is clean. No need to save.");
            return;
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cachePath))) {
            oos.writeObject(cache);
            System.out.println("pHash cache saved with " + cache.size() + " entries.");
            isDirty = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getHash(File file) throws IOException {
        if (file == null || !file.exists()) {
            return null;
        }

        String filePath = file.getAbsolutePath();
        CacheEntry entry = cache.get(filePath);

        if (entry != null && entry.lastModified == file.lastModified()) {
            return entry.hash;
        }

        // File is new or has been modified, so we calculate a new hash.
        String hash = PHash.getHash(file);
        cache.put(filePath, new CacheEntry(hash, file.lastModified()));
        isDirty = true;
        return hash;
    }

    public Set<String> getAllFilePaths() {
        return cache.keySet();
    }

    public Map<String, String> getAllHashes() {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            result.put(entry.getKey(), entry.getValue().hash);
        }
        return result;
    }
}
