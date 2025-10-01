package org.photoshelf;

import org.photoshelf.ui.ImagePanelManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class Searcher extends SwingWorker<Void, JLabel> {
    private final File searchRoot;
    private final PhotoShelfUI mainApp;
    private final List<String> supportedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp");
    private final AtomicInteger filesFound = new AtomicInteger(0);
    private final SearchParams searchParam;

    public Searcher(PhotoShelfUI mainAppm, File searchRoot, SearchParams params) {
        this.mainApp = mainAppm;
        this.searchRoot = searchRoot;
        this.searchParam = params;
    }

    @Override
    protected Void doInBackground() throws IOException {
        int maxDepth = searchParam.isRecursive() ? Integer.MAX_VALUE : 1;
        try (Stream<Path> stream = Files.walk(searchRoot.toPath(), maxDepth)) {
            stream.parallel()
                    .filter(path -> !isCancelled())
                    .filter(path -> {
                        File file = path.toFile();
                        if (file.isDirectory()) {
                            return false;
                        }
                        if (!isSupported(file.getName().toLowerCase())) {
                            return false;
                        }
                        boolean matches = true;
                        if (matches && searchParam.hasSearchString()) {
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
                        return matches;
                    })
                    .forEach(path -> {
                        filesFound.incrementAndGet();
                        try {
                            publish(createImageLabel(path.toFile()));
                        } catch (IOException e) {
                            System.err.println("Could not create thumbnail for new file: " + e.getMessage());
                        }
                    });
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
