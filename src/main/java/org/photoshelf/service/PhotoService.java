package org.photoshelf.service;

import org.photoshelf.DuplicateImageFinder;
import org.photoshelf.ImageSupportChecker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PhotoService {
    private final PluginManager pluginManager;
    private final ExecutorService executor;

    public PhotoService() {
        this.pluginManager = PluginManager.getInstance();
        this.executor = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
    }

    public List<File> listFiles(File directory, String filterText, String sortCriteria, boolean descending) {
        if (directory == null || !directory.exists()) return Collections.emptyList();

        Set<String> supportedExtensions = pluginManager.getAllSupportedExtensions();
        String filter = filterText.trim().toLowerCase();

        List<File> files;
        try (Stream<Path> stream = Files.list(directory.toPath())) {
            files = stream
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        if (fileName.startsWith(".")) return false;

                        String lower = fileName.toLowerCase();
                        if (lower.lastIndexOf('.') == -1) return false;
                        String extension = lower.substring(lower.lastIndexOf('.') + 1);
                        return supportedExtensions.contains(extension) && (filter.isEmpty() || lower.contains(filter));
                    })
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }

        sortFiles(files, sortCriteria, descending);
        return files;
    }

    public void sortFiles(List<File> files, String sortCriteria, boolean descending) {
        Comparator<File> comparator = switch (sortCriteria) {
            case "Date Created" -> Comparator.comparing(file -> {
                try {
                    return Files.readAttributes(file.toPath(), BasicFileAttributes.class).creationTime();
                } catch (IOException e) {
                    return null;
                }
            }, Comparator.nullsLast(Comparator.naturalOrder()));
            case "Size" -> Comparator.comparingLong(File::length);
            case "Type" -> Comparator.comparing(file -> {
                String name = file.getName();
                int lastDot = name.lastIndexOf('.');
                return (lastDot > 0 && lastDot < name.length() - 1) ? name.substring(lastDot + 1).toLowerCase() : "";
            });
            default -> Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER);
        };

        if (descending) {
            comparator = comparator.reversed();
        }
        files.sort(comparator);
    }

    public Set<File> findDuplicates(List<File> files) {
        // In a full plugin system, this would call a DuplicateFinderPlugin.
        // For now, we use the existing class but wrapped here.
        DuplicateImageFinder finder = new DuplicateImageFinder();
        return finder.findDuplicates(files);
    }

    public void scanDirectory(File directory, Consumer<Integer> onProgress) {
        if (directory == null || !directory.exists()) return;

        List<File> images = new ArrayList<>();
        collectImages(directory, images);

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
