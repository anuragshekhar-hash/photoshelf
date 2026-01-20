package org.photoshelf;

import org.photoshelf.service.PluginManager;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ImageLoader extends SwingWorker<Integer, JLabel> {
    private final List<File> filesToDisplay;
    private final int thumbnailSize;
    private final PhotoShelfUI ui;
    private final JPanel imagePanel;

    public ImageLoader(PhotoShelfUI ui, JPanel imagePanel, List<File> filesToDisplay, int thumbnailSize) {
        this.ui = ui;
        this.imagePanel = imagePanel;
        this.filesToDisplay = filesToDisplay;
        this.thumbnailSize = thumbnailSize;
    }

    @Override
    protected Integer doInBackground() throws Exception {
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<JLabel>> futures = new ArrayList<>();

        for (File file : filesToDisplay) {
            if (isCancelled()) break;

            Callable<JLabel> task = () -> {
                try {
                    // Use PluginManager to get thumbnail if available (e.g. for videos)
                    BufferedImage thumb = null;
                    try {
                        thumb = PluginManager.getInstance().getThumbnail(file);
                    } catch (Exception e) {
                        // Ignore plugin errors
                    }
                    
                    ImageIcon icon;
                    if (thumb != null) {
                        // Scale plugin thumbnail
                        int imgWidth = thumb.getWidth();
                        int imgHeight = thumb.getHeight();
                        if (thumbnailSize >= imgWidth && thumbnailSize >= imgHeight) {
                            icon = new ImageIcon(thumb);
                        } else {
                            double scale = Math.min((double) thumbnailSize / imgWidth, (double) thumbnailSize / imgHeight);
                            int newWidth = (int) (imgWidth * scale);
                            int newHeight = (int) (imgHeight * scale);
                            Image scaled = thumb.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                            icon = new ImageIcon(scaled);
                        }
                    } else {
                        // Fallback to standard ImageIO via UI helper
                        icon = ui.createDisplayIcon(file, thumbnailSize, thumbnailSize);
                    }

                    if (icon == null) return null;
                    
                    String name = file.getName();
                    String shortName = name.length() > 20 ? name.substring(0, 17) + "..." : name;
                    JLabel label = new JLabel(shortName, icon, JLabel.CENTER);
                    label.setHorizontalTextPosition(JLabel.CENTER);
                    label.setVerticalTextPosition(JLabel.BOTTOM);
                    label.setBorder(ui.isDuplicate(file) ? BorderFactory.createLineBorder(Color.RED, 2) : BorderFactory.createEmptyBorder(4, 4, 4, 4));
                    label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    label.putClientProperty("imageFile", file);
                    label.setPreferredSize(new Dimension(thumbnailSize + 8, thumbnailSize + 40));
                    label.setAlignmentX(Component.LEFT_ALIGNMENT);
                    label.addMouseListener(ui.createImageMouseListener());
                    return label;
                } catch (Exception e) {
                    System.err.println("Could not load thumbnail for " + file.getName() + ": " + e.getMessage());
                    return null;
                }
            };
            futures.add(executor.submit(task));
        }

        int processedCount = 0;
        for (Future<JLabel> future : futures) {
            try {
                if (isCancelled()) break;
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
            if (isCancelled()) break;
            imagePanel.add(label);
        }
        imagePanel.revalidate();
        imagePanel.repaint();
    }
}
