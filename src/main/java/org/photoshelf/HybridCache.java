package org.photoshelf;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A generic, two-tier hybrid cache that combines a fast, size-limited in-memory
 * (LRU) cache with a persistent on-disk cache.
 *
 * @param <K> The type of the keys.
 * @param <V> The type of the values, which must be Serializable.
 */
public class HybridCache<K, V extends Serializable> {

    private final Map<K, V> memoryCache;
    private final Path diskCacheDir;
    private final ExecutorService diskWriterService = Executors.newSingleThreadExecutor();

    /**
     * Creates a new HybridCache.
     *
     * @param cacheName        A unique name for the cache, used to create the disk directory.
     * @param maxMemoryEntries The maximum number of entries to keep in the in-memory cache.
     */
    public HybridCache(String cacheName, int maxMemoryEntries) {
        // Use ConcurrentHashMap for thread-safe memory access
        this.memoryCache = new ConcurrentHashMap<>(maxMemoryEntries);

        // Define the on-disk cache directory
        this.diskCacheDir = Path.of(System.getProperty("user.home"), ".photoshelf_cache", cacheName);

        // Create the directory if it doesn't exist
        try {
            Files.createDirectories(diskCacheDir);
        } catch (IOException e) {
            System.err.println("Failed to create disk cache directory: " + diskCacheDir);
            e.printStackTrace();
        }
    }

    /**
     * Retrieves an item from the cache.
     * It first checks the in-memory cache, then falls back to the on-disk cache.
     *
     * @param key The key of the item to retrieve.
     * @return The cached item, or null if it is not in either cache.
     */
    public V get(K key) {
        // 1. Check in-memory cache first (fastest)
        V value = memoryCache.get(key);
        if (value != null) {
            return value; // Hot cache hit
        }

        // 2. Fallback to on-disk cache
        value = readFromDisk(key);
        if (value != null) {
            memoryCache.put(key, value); // Warm cache hit, promote to memory
            return value;
        }

        return null; // Cache miss
    }

    /**
     * Adds an item to the cache. The item is stored in the in-memory cache and
     * asynchronously written to the on-disk cache.
     *
     * @param key   The key of the item.
     * @param value The value to cache.
     */
    public void put(K key, V value) {
        if (key == null || value == null) return;

        // Add to memory for immediate access
        memoryCache.put(key, value);

        // Asynchronously write to disk to avoid blocking
        diskWriterService.submit(() -> writeToDisk(key, value));
    }

    /**
     * Removes an item from both the in-memory and on-disk caches.
     *
     * @param key The key of the item to remove.
     */
    public void remove(K key) {
        memoryCache.remove(key);
        diskWriterService.submit(() -> {
            try {
                Files.deleteIfExists(getFileForKey(key));
            } catch (IOException e) {
                // Log error but don't crash
                System.err.println("Error deleting file for key: " + key);
                e.printStackTrace();
            }
        });
    }

    /**
     * Clears both the in-memory and on-disk caches completely.
     */
    public void clear() {
        memoryCache.clear();
        diskWriterService.submit(() -> {
            try (var paths = Files.walk(diskCacheDir)) {
                paths.filter(Files::isRegularFile).forEach(file -> {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        System.err.println("Failed to delete cache file: " + file);
                    }
                });
            } catch (IOException e) {
                System.err.println("Error clearing disk cache: " + diskCacheDir);
                e.printStackTrace();
            }
        });
    }

    /**
     * Shuts down the background thread pool used for disk writes.
     * This should be called when the application is closing.
     */
    public void shutdown() {
        diskWriterService.shutdown();
    }

    // --- Private Helper Methods ---

    private Path getFileForKey(K key) {
        // Sanitize the key to create a valid and safe filename
        String fileName = key.toString().replaceAll("[^a-zA-Z0-9.-]", "_") + ".cache";
        return diskCacheDir.resolve(fileName);
    }

    private void writeToDisk(K key, V value) {
        Path filePath = getFileForKey(key);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath.toFile()))) {
            oos.writeObject(value);
        } catch (IOException e) {
            System.err.println("Error writing to disk cache for key: " + key);
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private V readFromDisk(K key) {
        Path filePath = getFileForKey(key);
        if (!Files.exists(filePath)) {
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath.toFile()))) {
            return (V) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error reading from disk cache for key: " + key + ". Deleting corrupt file.");
            // The cached file might be corrupt or from an old version of the class.
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException deleteException) {
                // Ignore
            }
            return null;
        }
    }
}
