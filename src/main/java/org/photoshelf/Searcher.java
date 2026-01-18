package org.photoshelf;

import org.photoshelf.ui.ImagePanelManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Searcher extends SwingWorker<Void, JLabel> {
    private final File searchRoot;
    private final PhotoShelfUI mainApp;
    private final List<String> supportedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp");
    private final AtomicInteger filesFound = new AtomicInteger(0);
    private final SearchParams searchParam;
    private final String Filter;
    private final String sortCriteria;
    private final boolean descending;

    public Searcher(PhotoShelfUI mainAppm, File searchRoot, SearchParams params, String filter, String sortCriteria, boolean descending) {
        this.Filter = filter;
        this.mainApp = mainAppm;
        this.searchRoot = searchRoot;
        this.searchParam = params;
        this.sortCriteria = sortCriteria;
        this.descending = descending;
    }

    @Override
    protected Void doInBackground() throws IOException {
        int maxDepth = searchParam.isRecursive() ? Integer.MAX_VALUE : 1;
        List<File> foundFiles = new ArrayList<>();

        Files.walkFileTree(searchRoot.toPath(), EnumSet.noneOf(FileVisitOption.class), maxDepth, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (Files.isHidden(dir) || dir.getFileName().toString().startsWith(".")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                if (isCancelled()) {
                    return FileVisitResult.TERMINATE;
                }

                File file = path.toFile();

                if (!isSupported(file.getName().toLowerCase())) {
                    return FileVisitResult.CONTINUE;
                }

                if (Filter != null && !Filter.isBlank() && !file.getName().toLowerCase().endsWith(Filter.toLowerCase()))
                    return FileVisitResult.CONTINUE;

                boolean matches = true;
                if (searchParam.hasSearchString()) {
                    matches = file.getName().toLowerCase().contains(searchParam.getSearchString());
                }

                if (matches && searchParam.hasKeyword()) {
                    Set<String> keywords = mainApp.getKeywordManager().getKeywords(file);
                    if (searchParam.isNoKeywords()) {
                        matches = keywords.isEmpty();
                    } else {
                        matches = searchParam.evaluate(keywords);
                    }
                }

                if (matches) {
                    foundFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                System.err.println("Failed to access file: " + file + " - " + exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });

        if (isCancelled()) return null;

        // Sort the collected files
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
        foundFiles.sort(comparator);

        // Publish sorted files
        for (File file : foundFiles) {
            if (isCancelled()) break;
            filesFound.incrementAndGet();
            publish(createImageLabel(file));
        }

        return null;
    }

    private JLabel createImageLabel(File imgFile) throws IOException {
        ImagePanelManager imagePanelManager = mainApp.getImagePanelManager();
        int thumbnailSize = imagePanelManager.getThumbnailSize();
        ImageIcon icon = mainApp.createDisplayIcon(imgFile, thumbnailSize, thumbnailSize);
        String name = imgFile.getName();
        String shortName = name.length() > 20 ? name.substring(0, 17) + "..." : name;
        JLabel label = new JLabel(shortName, icon, JLabel.CENTER);
        label.setHorizontalTextPosition(JLabel.CENTER);
        label.setVerticalTextPosition(JLabel.BOTTOM);
        label.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.putClientProperty("imageFile", imgFile);
        label.setPreferredSize(new java.awt.Dimension(thumbnailSize + 8, thumbnailSize + 40));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.addMouseListener(mainApp.createImageMouseListener());
        return label;
    }

    private boolean isSupported(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            String ext = fileName.substring(lastDot + 1);
            return supportedExtensions.contains(ext);
        }
        return false;
    }

    @Override
    protected void process(List<JLabel> chunks) {
        ImagePanelManager imagePanelManager = mainApp.getImagePanelManager();
        for (JLabel label : chunks) {
            if (isCancelled()) break;
            imagePanelManager.addImageLabel(label);
        }
        mainApp.updateTotalFile(filesFound.get());
    }

    @Override
    protected void done() {
        if (!isCancelled()) {
            mainApp.setSearchStatus("Found " + filesFound.get() + " matching files");
        }
    }
}
