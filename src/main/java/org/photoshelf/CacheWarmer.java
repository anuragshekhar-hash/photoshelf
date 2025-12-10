package org.photoshelf;

import javax.swing.SwingWorker;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public class CacheWarmer extends SwingWorker<Void, Void> {
    private final PHashCacheManager pHashCacheManager;
    private final File directory;
    private final Consumer<File> onDone;

    public CacheWarmer(PHashCacheManager pHashCacheManager, File directory, Consumer<File> onDone) {
        this.pHashCacheManager = pHashCacheManager;
        this.directory = directory;
        this.onDone = onDone;
    }

    @Override
    protected Void doInBackground() throws Exception {
        // Set a lower priority to not interfere with the UI responsiveness
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        System.out.println("Starting background cache warming for: " + directory.getAbsolutePath());
        warmDirectory(directory);
        return null;
    }

    @Override
    protected void done() {
        System.out.println("Finished background cache warming for: " + directory.getAbsolutePath());
        onDone.accept(directory);
    }

    private void warmDirectory(File dir) {
        if (isCancelled() || dir.isHidden()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (isCancelled()) {
                break;
            }
            if (file.isDirectory()) {
                warmDirectory(file);
            } else if (ImageSupportChecker.isImage(file)) {
                try {
                    // This will calculate and cache the hash if it's new or updated
                    pHashCacheManager.getHash(file);
                } catch (IOException e) {
                    // Silently ignore files that can't be processed
                }
            }
        }
    }
}
