package org.photoshelf;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class DirectoryWatcher implements Runnable {
    private final File directory;
    private final PhotoShelfUI mainApp;

    public DirectoryWatcher(PhotoShelfUI mainApp, File directory) {
        this.mainApp = mainApp;
        this.directory = directory;
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            Path path = directory.toPath();
            path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            WatchKey key;
            while ((key = watchService.take()) != null) {
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path newFilePath = (Path) event.context();
                        File newFile = new File(directory, newFilePath.toString());
                        SwingUtilities.invokeLater(() -> mainApp.addNewImage(newFile));
                    }
                }
                key.reset();
            }
        } catch (IOException | InterruptedException e) {
            //e.printStackTrace();
        }
    }
}
