package org.photoshelf;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Map;
import java.util.Set;

public class ImageLoader extends SwingWorker<Integer, JLabel> {
    private final File directory;
    private final String filterText;
    private final String sortCriteria;
    private final boolean descending;
    private final boolean showOnlyDuplicates;
    private final int thumbnailSize;
    public static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp");

    private final PhotoShelfUI ui;
    private final JPanel imagePanel;

    public ImageLoader(PhotoShelfUI ui, JPanel imagePanel, File directory, String filterText, String sortCriteria, boolean descending, boolean showOnlyDuplicates, int thumbnailSize) {
        this.ui = ui;
        this.imagePanel = imagePanel;
        this.directory = directory;
        this.filterText = filterText.trim().toLowerCase();
        this.sortCriteria = sortCriteria;
        this.descending = descending;
        this.showOnlyDuplicates = showOnlyDuplicates;
        this.thumbnailSize = thumbnailSize;
    }

    @Override
    protected Integer doInBackground() throws Exception {
        List<File> filesToDisplay;
        try (Stream<Path> stream = Files.list(directory.toPath())) {
            filesToDisplay = stream
                    .filter(path -> {
                        String lower = path.getFileName().toString().toLowerCase();
                        if (lower.lastIndexOf('.') == -1) return false;
                        String extension = lower.substring(lower.lastIndexOf('.') + 1);
                        return SUPPORTED_EXTENSIONS.contains(extension) && (filterText.isEmpty() || lower.contains(filterText));
                    })
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        }

        if (showOnlyDuplicates) {
           /* DuplicateImageFinder duplicateFinder = new DuplicateImageFinder();
            Map<String, Set<File>> duplicateGroups = duplicateFinder.findDuplicates(filesToDisplay);
            ui.setDuplicateFiles(duplicateGroups);
            filesToDisplay.clear();
            for (Set<File> group : duplicateGroups.values()) {
                filesToDisplay.addAll(group);
            }*/
        }

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
        filesToDisplay.sort(comparator);

        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<JLabel>> futures = new ArrayList<>();

        for (File imgFile : filesToDisplay) {
            if (isCancelled()) {
                break;
            }

            Callable<JLabel> task = () -> {
                try {
                    ImageIcon icon = ui.createDisplayIcon(imgFile, thumbnailSize, thumbnailSize);
                    if (icon == null) return null;
                    String name = imgFile.getName();
                    String shortName = name.length() > 20 ? name.substring(0, 17) + "..." : name;
                    JLabel label = new JLabel(shortName, icon, JLabel.CENTER);
                    label.setHorizontalTextPosition(JLabel.CENTER);
                    label.setVerticalTextPosition(JLabel.BOTTOM);
                    Color borderColor = ui.getDuplicateBorderColor(imgFile);
                    label.setBorder(borderColor != null ? BorderFactory.createLineBorder(borderColor, 2) : BorderFactory.createEmptyBorder(4, 4, 4, 4));
                    label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    label.putClientProperty("imageFile", imgFile);
                    label.setPreferredSize(new java.awt.Dimension(thumbnailSize + 8, thumbnailSize + 40));
                    label.setAlignmentX(Component.LEFT_ALIGNMENT);
                    label.addMouseListener(ui.createImageMouseListener());
                    return label;
                } catch (Exception e) {
                    System.err.println("Could not load thumbnail for " + imgFile.getName() + ": " + e.getMessage());
                    return null;
                }
            };
            futures.add(executor.submit(task));
        }

        int processedCount = 0;
        for (Future<JLabel> future : futures) {
            try {
                if (isCancelled()) {
                    break;
                }
                JLabel label = future.get();
                if (label != null) {
                    publish(label);
                    processedCount++;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException e) {
                System.err.println("Error processing image: " + e.getCause().getMessage());
            }
        }

        executor.shutdownNow();
        return processedCount;
    }

    @Override
    protected void process(List<JLabel> chunks) {
        for (JLabel label : chunks) {
            imagePanel.add(label);
        }
        imagePanel.revalidate();
        imagePanel.repaint();
    }
}
