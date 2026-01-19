package org.photoshelf.service;

import org.photoshelf.ImageSupportChecker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PhotoService {
    private final PluginManager pluginManager;
    private final ExecutorService executor;

    public PhotoService() {
        this.pluginManager = PluginManager.getInstance();
        // Use a thread pool for parallel processing
        this.executor = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
    }

    /**
     * Scans a directory and processes all images using registered plugins.
     * @param directory The directory to scan.
     * @param onProgress Callback for progress updates (processed count).
     */
    public void scanDirectory(File directory, Consumer<Integer> onProgress) {
        if (directory == null || !directory.exists()) return;

        List<File> images = new ArrayList<>();
        collectImages(directory, images);

        int total = images.size();
        int processed = 0;

        for (File image : images) {
            executor.submit(() -> pluginManager.processImage(image));
            processed++;
            if (onProgress != null && processed % 10 == 0) {
                onProgress.accept(processed);
            }
        }
    }

    private void collectImages(File dir, List<File> imageList) {
        if (dir.isHidden() || dir.getName().startsWith(".")) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                collectImages(file, imageList);
            } else if (ImageSupportChecker.isImage(file)) {
                imageList.add(file);
            }
        }
    }
    
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        pluginManager.shutdown();
    }
}
