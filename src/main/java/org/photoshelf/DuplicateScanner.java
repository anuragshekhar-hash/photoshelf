package org.photoshelf;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DuplicateScanner extends SwingWorker<Void, Map<String, Set<File>>> {
    private final File directory;
    private final PhotoShelfUI ui;

    public DuplicateScanner(PhotoShelfUI ui, File directory) {
        this.ui = ui;
        this.directory = directory;
    }

    @Override
    protected Void doInBackground() throws Exception {
        List<File> files = new ArrayList<>();
        scanDirectory(directory, files);

        Map<String, String> pHashMap = new ConcurrentHashMap<>();
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<Void>> futures = new ArrayList<>();

        for (File file : files) {
            futures.add(executor.submit(() -> {
                try {
                    String hash = PHash.getHash(file);
                    pHashMap.put(file.getAbsolutePath(), hash);
                } catch (IOException e) {
                    System.err.println("Could not process file: " + file.getName() + " - " + e.getMessage());
                }
                return null;
            }));
        }

        for (Future<Void> future : futures) {
            future.get();
        }

        executor.shutdown();

        Map<String, Set<File>> duplicateGroups = new HashMap<>();
        List<File> remainingFiles = new ArrayList<>(files);

        while (!remainingFiles.isEmpty()) {
            File currentFile = remainingFiles.remove(0);
            String currentHash = pHashMap.get(currentFile.getAbsolutePath());
            if (currentHash == null) {
                continue;
            }

            Set<File> group = new HashSet<>();
            group.add(currentFile);

            Iterator<File> iterator = remainingFiles.iterator();
            while (iterator.hasNext()) {
                File otherFile = iterator.next();
                String otherHash = pHashMap.get(otherFile.getAbsolutePath());
                if (otherHash != null) {
                    if (PHash.distance(currentHash, otherHash) <= 5) {
                        group.add(otherFile);
                        iterator.remove();
                    }
                }
            }

            if (group.size() > 1) {
                duplicateGroups.put(currentFile.getAbsolutePath(), group);
            }
        }

        if (!duplicateGroups.isEmpty()) {
            publish(duplicateGroups);
        }

        return null;
    }

    private void scanDirectory(File dir, List<File> files) {
        File[] fileList = dir.listFiles();
        if (fileList == null) {
            return;
        }
        for (File file : fileList) {
            if (file.isHidden()) {
                continue;
            }
            if (file.isDirectory()) {
                scanDirectory(file, files);
            } else if (ImageSupportChecker.isImage(file)) {
                files.add(file);
            }
        }
    }

    @Override
    protected void process(List<Map<String, Set<File>>> chunks) {
        for (Map<String, Set<File>> duplicateGroups : chunks) {
            ui.displayDuplicateGroups(duplicateGroups);
        }
    }
}
