package org.photoshelf;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;

public class CacheValidationWorker extends SwingWorker<Void, String> {

    private final File rootDirectory;
    private final PHashCacheManager pHashCacheManager;
    private final PhotoShelfUI mainApp;
    private final AtomicInteger fileCount = new AtomicInteger(0);

    public CacheValidationWorker(PhotoShelfUI mainApp, File rootDirectory, PHashCacheManager pHashCacheManager) {
        this.mainApp = mainApp;
        this.rootDirectory = rootDirectory;
        this.pHashCacheManager = pHashCacheManager;
    }

    @Override
    protected Void doInBackground() throws Exception {
        publish("Starting recursive scan of " + rootDirectory.getAbsolutePath());

        Files.walkFileTree(rootDirectory.toPath(), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (isCancelled()) {
                    return FileVisitResult.TERMINATE;
                }

                File currentFile = file.toFile();
                if (ImageSupportChecker.isImage(currentFile)) {
                    try {
                        pHashCacheManager.getHash(currentFile);
                        int count = fileCount.incrementAndGet();
                        if (count % 50 == 0) {
                            publish("Scanned " + count + " images...");
                        }
                    } catch (IOException e) {
                        publish("Error processing " + currentFile.getName() + ": " + e.getMessage());
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                publish("Could not access: " + file.toString());
                return FileVisitResult.CONTINUE;
            }
        });

        if (isCancelled()) {
            publish("Scan cancelled.");
            return null;
        }

        publish("Validating cache: removing entries for deleted files...");
        int removedCount = pHashCacheManager.validateCache();
        publish("Cache validation complete. Removed " + removedCount + " stale entries.");

        return null;
    }

    @Override
    protected void process(java.util.List<String> chunks) {
        for (String status : chunks) {
            mainApp.setSearchStatus(status);
        }
    }

    @Override
    protected void done() {
        if (!isCancelled()) {
            mainApp.setSearchStatus("Recursive scan and cache validation complete. Scanned " + fileCount.get() + " files.");
        }
    }
}
