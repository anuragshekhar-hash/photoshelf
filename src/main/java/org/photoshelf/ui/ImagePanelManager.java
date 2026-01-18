package org.photoshelf.ui;

import org.photoshelf.PhotoShelfUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ImagePanelManager {
    private final SelectionPanel imagePanel;
    private final JScrollPane panel;
    private final PhotoShelfUI mainApp;
    private int thumbnailSize = 150;
    private final JSlider thumbnailSizeSlider;
    private final JPanel containerPanel;
    private JLabel directoryPathLabel;

    public ImagePanelManager(PhotoShelfUI mainApp) {
        this.mainApp = mainApp;

        // 1. Create the panel that holds the images and draws the selection box
        imagePanel = new SelectionPanel(mainApp);
        WrapLayout wrapLayout = new WrapLayout(FlowLayout.LEFT);
        wrapLayout.setHgap(16);
        wrapLayout.setVgap(16);
        imagePanel.setLayout(wrapLayout);
        imagePanel.setBackground(Color.WHITE);

        // 2. Create the scroll pane and add the image panel to it
        panel = new JScrollPane(imagePanel);
        panel.getVerticalScrollBar().setUnitIncrement(16);
        addSelectionListenersTo(panel.getViewport());

        // 3. Create the top menu with slider and path label
        thumbnailSizeSlider = new JSlider(JSlider.HORIZONTAL, 60, 240, 120);
        thumbnailSizeSlider.setMajorTickSpacing(60);
        thumbnailSizeSlider.setPaintTicks(true);
        thumbnailSizeSlider.setPaintLabels(true);
        thumbnailSizeSlider.setToolTipText("Adjust Thumbnail Size");
        thumbnailSizeSlider.addChangeListener(e -> {
            setThumbnailSize(thumbnailSizeSlider.getValue());
            mainApp.resizeView();
        });

        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.add(new JLabel("Thumbnail Size: "), BorderLayout.WEST);
        sliderPanel.add(thumbnailSizeSlider, BorderLayout.CENTER);

        directoryPathLabel = new JLabel();
        directoryPathLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JPanel menuPanel = new JPanel(new BorderLayout());
        menuPanel.add(directoryPathLabel, BorderLayout.EAST);
        menuPanel.add(sliderPanel, BorderLayout.WEST);

        // 4. Assemble the final container panel
        containerPanel = new JPanel(new BorderLayout());
        containerPanel.add(menuPanel, BorderLayout.NORTH);
        containerPanel.add(panel, BorderLayout.CENTER);
    }

    private void addSelectionListenersTo(JViewport viewport) {
        final Point[] startPoint = {null};
        final Rectangle selectionRect = new Rectangle();

        viewport.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Start selection only if clicking on the panel background
                if (e.getSource() == viewport) {
                    startPoint[0] = e.getPoint();
                    if (!e.isControlDown() && !e.isMetaDown()) {
                        imagePanel.getSelectionCallback().clearSelectionUI();
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                startPoint[0] = null;
                imagePanel.setSelectionRectangle(null);
                imagePanel.repaint();
            }
        });

        viewport.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (startPoint[0] != null) {
                    Point endPoint = e.getPoint();
                    selectionRect.setBounds(
                            Math.min(startPoint[0].x, endPoint.x),
                            Math.min(startPoint[0].y, endPoint.y),
                            Math.abs(startPoint[0].x - endPoint.x),
                            Math.abs(startPoint[0].y - endPoint.y)
                    );
                    imagePanel.setSelectionRectangle(selectionRect);

                    for (Component comp : imagePanel.getComponents()) {
                        if (comp instanceof JLabel && selectionRect.intersects(comp.getBounds())) {
                            imagePanel.getSelectionCallback().addToSelectionUI((JLabel) comp);
                        }
                    }
                    imagePanel.repaint();
                }
            }
        });
    }

    public JPanel getPanel() {
        return containerPanel;
    }

    public JPanel getImagePanel() {
        return imagePanel;
    }

    public void addImageLabel(JLabel label) {
        imagePanel.add(label);
        imagePanel.revalidate();
        imagePanel.repaint();
    }

    public void clearImagePanel() {
        imagePanel.removeAll();
        imagePanel.revalidate();
        imagePanel.repaint();
    }

    public int getThumbnailSize() {
        return thumbnailSize;
    }

    public void setThumbnailSize(int thumbnailSize) {
        this.thumbnailSize = thumbnailSize;
    }

    public void sortImages(Comparator<File> comparator) {
        List<JLabel> labels = new ArrayList<>();
        for (Component comp : imagePanel.getComponents()) {
            if (comp instanceof JLabel) {
                labels.add((JLabel) comp);
            }
        }

        labels.sort((l1, l2) -> {
            File f1 = (File) l1.getClientProperty("imageFile");
            File f2 = (File) l2.getClientProperty("imageFile");
            if (f1 == null || f2 == null) return 0;
            return comparator.compare(f1, f2);
        });

        imagePanel.removeAll();
        for (JLabel label : labels) {
            imagePanel.add(label);
        }
        imagePanel.revalidate();
        imagePanel.repaint();
    }
}
