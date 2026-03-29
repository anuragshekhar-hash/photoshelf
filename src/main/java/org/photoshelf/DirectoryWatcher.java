package org.photoshelf;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

public class DirectoryWatcher implements Runnable {
    private final File directory;
    private final PhotoShelfUI mainApp;
    private final Map<WatchKey, Path> keys;
    private final boolean recursive;

    public DirectoryWatcher(PhotoShelfUI mainApp, File directory, boolean recursive) {
        this.mainApp = mainApp;
        this.directory = directory;
        this.keys = new HashMap<>();
        this.recursive = recursive;
    }

    public DirectoryWatcher(PhotoShelfUI mainApp, File directory) {
        this(mainApp, directory, false); // Default to non-recursive for backward compatibility
    }

    private void registerDirectory(Path dir, WatchService watchService) throws IOException {
        WatchKey key = dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        keys.put(key, dir);
    }

    private void registerAllDirectories(final Path start, final WatchService watchService) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                registerDirectory(dir, watchService);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            
            if (recursive) {
                registerAllDirectories(directory.toPath(), watchService);
            } else {
                registerDirectory(directory.toPath(), watchService);
            }

            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException x) {
                    return;
                }

                Path dir = keys.get(key);
                if (dir == null) {
                    System.err.println("WatchKey not recognized!!");
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path name = ev.context();
                    Path child = dir.resolve(name);

                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        File newFile = child.toFile();
                        
                        // If recursive and a new directory is created, register it
                        if (recursive && newFile.isDirectory()) {
                            try {
                                registerAllDirectories(child, watchService);
                            } catch (IOException e) {
                                // Ignore registration errors
                            }
                        } else if (!newFile.isDirectory() && !newFile.isHidden() && !newFile.getName().startsWith(".")) {
                            SwingUtilities.invokeLater(() -> mainApp.addNewImage(newFile));
                        }
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    keys.remove(key);
                    if (keys.isEmpty()) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }
}
