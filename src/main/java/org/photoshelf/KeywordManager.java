package org.photoshelf;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class KeywordManager {
    private final Map<String, Set<String>> keywordMap;
    private final Path storagePath;
    private final Path tempPath;
    private final Path backupPath;

    public KeywordManager() {
        Path baseDir = Path.of(System.getProperty("user.home"), ".photoshelf_cache");
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            System.err.println("Could not create cache directory: " + e.getMessage());
        }
        this.storagePath = baseDir.resolve("keywords.ser");
        this.tempPath = baseDir.resolve("keywords.ser.tmp");
        this.backupPath = baseDir.resolve("keywords.ser.bak");
        this.keywordMap = loadKeywords();
    }

    public void addKeyword(File imageFile, String keyword) {
        String path = imageFile.getAbsolutePath();
        keywordMap.computeIfAbsent(path, k -> new HashSet<>()).add(keyword.toLowerCase());
        saveKeywords();
    }

    public void removeKeyword(File imageFile, String keyword) {
        String path = imageFile.getAbsolutePath();
        if (keywordMap.containsKey(path)) {
            keywordMap.get(path).remove(keyword.toLowerCase());
            if (keywordMap.get(path).isEmpty()) {
                keywordMap.remove(path);
            }
            saveKeywords();
        }
    }

    public void renameKeyword(String oldKeyword, String newKeyword) {
        String oldKeywordLower = oldKeyword.toLowerCase();
        String newKeywordLower = newKeyword.toLowerCase();
        for (Set<String> keywords : keywordMap.values()) {
            if (keywords.contains(oldKeywordLower)) {
                keywords.remove(oldKeywordLower);
                keywords.add(newKeywordLower);
            }
        }
        saveKeywords();
    }

    public Set<String> getKeywords(File imageFile) {
        return keywordMap.getOrDefault(imageFile.getAbsolutePath(), Collections.emptySet());
    }

    public boolean hasKeyword(File imageFile, String keyword) {
        return getKeywords(imageFile).contains(keyword.toLowerCase());
    }

    public void renameFile(File oldFile, File newFile) {
        String oldPath = oldFile.getAbsolutePath();
        if (keywordMap.containsKey(oldPath)) {
            Set<String> keywords = keywordMap.remove(oldPath);
            keywordMap.put(newFile.getAbsolutePath(), keywords);
            saveKeywords();
        }
    }

    public void deleteFile(File file) {
        if (keywordMap.remove(file.getAbsolutePath()) != null) {
            saveKeywords();
        }
    }

    public void copyKeywords(File source, File destination) {
        String sourcePath = source.getAbsolutePath();
        if (keywordMap.containsKey(sourcePath)) {
            Set<String> keywordsToCopy = new HashSet<>(keywordMap.get(sourcePath));
            String destPath = destination.getAbsolutePath();
            keywordMap.computeIfAbsent(destPath, k -> new HashSet<>()).addAll(keywordsToCopy);
            saveKeywords();
        }
    }

    public Set<String> getAllKeywords() {
        return keywordMap.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    public int cleanup() {
        int removedCount = 0;
        var iterator = keywordMap.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            String path = entry.getKey();
            if (!Files.exists(Path.of(path))) {
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            saveKeywords();
        }
        return removedCount;
    }

    public void shutdown() {
        // No-op, added for symmetry with other managers that have background threads.
    }

    @SuppressWarnings("unchecked")
    private Map<String, Set<String>> loadKeywords() {
        if (Files.exists(storagePath)) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(storagePath.toFile()))) {
                return (Map<String, Set<String>>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error loading keywords from main file: " + e.getMessage() + ". Attempting to load from backup.");
                if (Files.exists(backupPath)) {
                    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(backupPath.toFile()))) {
                        return (Map<String, Set<String>>) ois.readObject();
                    } catch (IOException | ClassNotFoundException backupEx) {
                        System.err.println("Error loading keywords from backup file: " + backupEx.getMessage());
                    }
                }
            }
        }
        return new HashMap<>();
    }

    private void saveKeywords() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempPath.toFile()))) {
            oos.writeObject(keywordMap);
        } catch (IOException e) {
            System.err.println("Error writing keywords to temporary file: " + e.getMessage());
            return; // Abort save
        }

        try {
            if (Files.exists(storagePath)) {
                Files.copy(storagePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tempPath, storagePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("Error saving keywords: " + e.getMessage());
        }
    }
}
