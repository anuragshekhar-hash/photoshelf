package org.photoshelf;

import javax.swing.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class DuplicateScanner extends SwingWorker<Map<File, List<File>>, File> {

    private final PhotoShelfUI mainApp;
    private final PHashCacheManager pHashCacheManager;
    private final Map<File, List<File>> duplicateGroups = new LinkedHashMap<>();

    public DuplicateScanner(PhotoShelfUI mainApp, PHashCacheManager pHashCacheManager) {
        this.mainApp = mainApp;
        this.pHashCacheManager = pHashCacheManager;
    }

    @Override
    protected Map<File, List<File>> doInBackground() throws Exception {
        Map<String, String> allHashes = pHashCacheManager.getAllHashes();
        List<Map.Entry<String, String>> hashEntries = new ArrayList<>(allHashes.entrySet());
        int totalFiles = hashEntries.size();

        // Pre-convert hashes to long for faster comparison
        long[] hashValues = new long[totalFiles];
        String[] filePaths = new String[totalFiles];
        
        for (int i = 0; i < totalFiles; i++) {
            Map.Entry<String, String> entry = hashEntries.get(i);
            filePaths[i] = entry.getKey();
            String hashStr = entry.getValue();
            try {
                if (hashStr.length() > 64) hashStr = hashStr.substring(0, 64);
                hashValues[i] = Long.parseUnsignedLong(hashStr, 2);
            } catch (NumberFormatException e) {
                hashValues[i] = 0;
            }
        }

        // Use Disjoint Set Union (DSU) for efficient grouping
        DSU dsu = new DSU(totalFiles);
        AtomicInteger processedCount = new AtomicInteger(0);

        // Parallel processing of comparisons
        IntStream.range(0, totalFiles).parallel().forEach(i -> {
            if (isCancelled()) return;

            // Update progress occasionally
            int current = processedCount.incrementAndGet();
            if (current % 100 == 0) {
                SwingUtilities.invokeLater(() -> 
                    mainApp.setSearchStatus(String.format("Scanning... (%d/%d)", current, totalFiles))
                );
            }

            long hash1 = hashValues[i];

            for (int j = i + 1; j < totalFiles; j++) {
                long hash2 = hashValues[j];
                
                // Hamming distance using XOR and bit count
                int distance = Long.bitCount(hash1 ^ hash2);

                if (distance <= 5) {
                    dsu.union(i, j);
                }
            }
        });

        if (isCancelled()) return null;

        SwingUtilities.invokeLater(() -> mainApp.setSearchStatus("Processing results..."));

        // Collect groups
        Map<Integer, List<File>> groups = new HashMap<>();
        for (int i = 0; i < totalFiles; i++) {
            int root = dsu.find(i);
            groups.computeIfAbsent(root, k -> new ArrayList<>()).add(new File(filePaths[i]));
        }

        // Publish groups
        for (List<File> group : groups.values()) {
            if (isCancelled()) return null;
            if (group.size() > 1) {
                group.sort(Comparator.comparing(File::getName));
                File representative = group.get(0);
                duplicateGroups.put(representative, group);
                publish(representative);
            }
        }

        return duplicateGroups;
    }

    @Override
    protected void process(List<File> chunks) {
        // This is called on the EDT
        for (File representative : chunks) {
            if (isCancelled()) break;
            List<File> group = duplicateGroups.get(representative);
            if (group != null) {
                mainApp.addDuplicateSet(representative, group);
            }
        }
    }

    @Override
    protected void done() {
        try {
            Map<File, List<File>> result = get();
            if (result != null) {
                mainApp.duplicateScanComplete(result);
            } else {
                 mainApp.duplicateScanComplete(Collections.emptyMap());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            mainApp.duplicateScanComplete(Collections.emptyMap());
        }
    }

    // Thread-safe Disjoint Set Union
    private static class DSU {
        private final int[] parent;

        public DSU(int size) {
            parent = new int[size];
            for (int i = 0; i < size; i++) {
                parent[i] = i;
            }
        }

        public synchronized void union(int i, int j) {
            int rootI = find(i);
            int rootJ = find(j);
            if (rootI != rootJ) {
                parent[rootI] = rootJ;
            }
        }

        public int find(int i) {
            int root = i;
            while (root != parent[root]) {
                root = parent[root];
            }
            // Path compression
            int curr = i;
            while (curr != root) {
                int next = parent[curr];
                parent[curr] = root;
                curr = next;
            }
            return root;
        }
    }
}
