package org.photoshelf;

import org.photoshelf.ui.KeywordEntryField;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PreviewPanelManager {
    private final JPanel previewCanvas; // Changed from JLabel to JPanel for custom painting
    private final JScrollPane previewScroll;
    private Image currentImage; // Changed from BufferedImage to Image to support Toolkit images (GIFs)
    private double scale = 1.0;
    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 5.0;
    private final JSplitPane mainPanel;
    private final JPanel keywordGridPanel;
    private final KeywordManager keywordManager;
    private File currentFile;
    private final PhotoShelfUI mainApp;

    public PreviewPanelManager(PhotoShelfUI mainApp, KeywordManager keywordManager) {
        this.mainApp = mainApp;
        this.keywordManager = keywordManager;

        // Custom panel to paint the image scaled while preserving animation
        previewCanvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (currentImage != null) {
                    int panelWidth = getWidth();
                    int panelHeight = getHeight();
                    int imgWidth = currentImage.getWidth(this);
                    int imgHeight = currentImage.getHeight(this);

                    if (imgWidth > 0 && imgHeight > 0) {
                        // Calculate scaled dimensions
                        int newW = (int) (imgWidth * scale);
                        int newH = (int) (imgHeight * scale);

                        // Center the image
                        int x = (panelWidth - newW) / 2;
                        int y = (panelHeight - newH) / 2;

                        // Draw the image scaled. Passing 'this' as observer enables animation for GIFs.
                        g.drawImage(currentImage, x, y, newW, newH, this);
                    }
                } else {
                    // Draw placeholder text if no image
                    String text = (currentFile == null) ? "Select an image to preview" : "Cannot preview this file";
                    FontMetrics fm = g.getFontMetrics();
                    int x = (getWidth() - fm.stringWidth(text)) / 2;
                    int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                    g.drawString(text, x, y);
                }
            }
        };
        previewCanvas.setBackground(UIManager.getColor("Panel.background"));
        
        previewScroll = new JScrollPane(previewCanvas);
        previewScroll.setPreferredSize(new Dimension(400, 300));
        previewScroll.setBorder(BorderFactory.createEmptyBorder());
        previewScroll.getViewport().setBackground(UIManager.getColor("Panel.background"));

        previewScroll.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isControlDown()) {
                    if (currentImage == null) return;

                    int rotation = e.getWheelRotation();
                    if (rotation < 0) { // Zoom in
                        scale *= 1.1;
                        if (scale > MAX_SCALE) scale = MAX_SCALE;
                    } else { // Zoom out
                        scale /= 1.1;
                        if (scale < MIN_SCALE) scale = MIN_SCALE;
                    }
                    previewCanvas.revalidate();
                    previewCanvas.repaint();
                    e.consume();
                }
            }
        });

        keywordGridPanel = new JPanel(new GridLayout(0, 1, 0, 2));

        // Wrapper panel to prevent grid from stretching
        JPanel keywordGridWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        keywordGridWrapper.add(keywordGridPanel);

        JScrollPane keywordScroll = new JScrollPane(keywordGridWrapper);
        keywordScroll.setBorder(BorderFactory.createTitledBorder("Keywords"));

        JButton searchButton = new JButton("Search");
        searchButton.setToolTipText("Find images with selected keywords");
        searchButton.addActionListener(e -> searchSelectedKeywords());

        JButton addKeywordButton = new JButton("Add");
        addKeywordButton.addActionListener(e -> addKeyword());

        JPanel keywordButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        keywordButtonPanel.add(searchButton);
        keywordButtonPanel.add(addKeywordButton);

        JPanel keywordPanel = new JPanel(new BorderLayout());
        keywordPanel.add(keywordScroll, BorderLayout.CENTER);
        keywordPanel.add(keywordButtonPanel, BorderLayout.SOUTH);

        mainPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, previewScroll, keywordPanel);
        mainPanel.setResizeWeight(0.7);
    }

    public JComponent getPreviewScroll() {
        return mainPanel;
    }

    public void showImagePreview(File imgFile) {
        this.currentFile = imgFile;
        try {
            scale = 1.0; // Reset scale for new image
            if (imgFile.getName().toLowerCase().endsWith(".gif")) {
                // Load GIF using Toolkit to preserve animation frames
                this.currentImage = Toolkit.getDefaultToolkit().createImage(imgFile.getAbsolutePath());
                
                // Wait for image to load to get dimensions (needed for initial scaling)
                MediaTracker tracker = new MediaTracker(previewCanvas);
                tracker.addImage(this.currentImage, 0);
                try {
                    tracker.waitForID(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                this.currentImage = ImageIO.read(imgFile);
            }
            
            if (this.currentImage != null) {
                calculateInitialScale();
            }
            
            previewCanvas.revalidate();
            previewCanvas.repaint();
            updateKeywordGrid();
        } catch (Exception ex) {
            this.currentImage = null;
            previewCanvas.repaint();
            ex.printStackTrace();
        }
    }

    private void calculateInitialScale() {
        if (currentImage == null) return;

        int maxW = previewScroll.getViewport().getWidth();
        int maxH = previewScroll.getViewport().getHeight();
        int imgW = currentImage.getWidth(previewCanvas);
        int imgH = currentImage.getHeight(previewCanvas);

        if (imgW > 0 && imgH > 0 && (imgW > maxW || imgH > maxH)) {
            scale = Math.min((double) maxW / imgW, (double) maxH / imgH);
        } else {
            scale = 1.0;
        }
    }

    private void updateKeywordGrid() {
        keywordGridPanel.removeAll();
        if (currentFile != null) {
            Set<String> keywords = keywordManager.getKeywords(currentFile);
            if (keywords.isEmpty()) {
                JPanel emptyPanel = new JPanel();
                JLabel noKeywordsLabel = new JLabel("No keywords added.");
                noKeywordsLabel.setHorizontalAlignment(SwingConstants.CENTER);
                noKeywordsLabel.setForeground(Color.GRAY);
                emptyPanel.add(noKeywordsLabel);
                keywordGridPanel.add(emptyPanel);
            } else {
                for (String keyword : keywords) {
                    keywordGridPanel.add(createKeywordComponent(keyword));
                }
            }
        }
        keywordGridPanel.revalidate();
        keywordGridPanel.repaint();
    }

    private JComponent createKeywordComponent(String keyword) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        Border matteBorder = BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground"));
        Border emptyBorder = BorderFactory.createEmptyBorder(2, 5, 2, 5);
        panel.setBorder(BorderFactory.createCompoundBorder(matteBorder, emptyBorder));

        JCheckBox checkBox = new JCheckBox(keyword);
        // Removed immediate search listener to allow multiple selection

        JButton editButton = createStyledButton("\u270E"); // Pencil icon
        editButton.setToolTipText("Rename keyword");
        editButton.addActionListener(e -> editKeyword(keyword));

        JButton removeButton = createStyledButton("x");
        removeButton.setToolTipText("Remove keyword");
        removeButton.addActionListener(e -> {
            keywordManager.removeKeyword(currentFile, keyword);
            updateKeywordGrid();
        });

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonsPanel.add(editButton);
        buttonsPanel.add(removeButton);

        panel.add(checkBox, BorderLayout.CENTER);
        panel.add(buttonsPanel, BorderLayout.EAST);
        return panel;
    }

    private void searchSelectedKeywords() {
        List<String> selectedKeywords = new ArrayList<>();
        for (Component comp : keywordGridPanel.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                for (Component innerComp : panel.getComponents()) {
                    if (innerComp instanceof JCheckBox) {
                        JCheckBox checkBox = (JCheckBox) innerComp;
                        if (checkBox.isSelected()) {
                            selectedKeywords.add(checkBox.getText());
                        }
                        break;
                    }
                }
            }
        }

        if (!selectedKeywords.isEmpty()) {
            SearchParams params = new SearchParams();
            StringBuilder sb = new StringBuilder();
            for (String kw : selectedKeywords) {
                if (sb.length() > 0) sb.append(" & ");
                sb.append(kw);
            }
            params.setExpression(sb.toString());
            params.setRecursive(true);
            mainApp.executeSearch(params, false);
        } else {
            JOptionPane.showMessageDialog(mainPanel, "Please select at least one keyword to search.", "No Keywords Selected", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setForeground(Color.GRAY);
        button.setMargin(new Insets(0, 4, 0, 4));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setForeground(UIManager.getColor("Label.foreground"));
            }
            public void mouseExited(MouseEvent evt) {
                button.setForeground(Color.GRAY);
            }
        });
        return button;
    }

    private void editKeyword(String oldKeyword) {
        String newKeyword = JOptionPane.showInputDialog(mainPanel, "Enter new name for '" + oldKeyword + "':", oldKeyword);
        if (newKeyword != null && !newKeyword.trim().isEmpty() && !newKeyword.equals(oldKeyword)) {
            if (isValidKeyword(newKeyword)) {
                int choice = JOptionPane.showConfirmDialog(mainPanel, 
                    "Do you want to rename this keyword for all images?", 
                    "Global Rename", 
                    JOptionPane.YES_NO_OPTION);
                
                if (choice == JOptionPane.YES_OPTION) {
                    keywordManager.renameKeyword(oldKeyword, newKeyword);
                } else {
                    keywordManager.removeKeyword(currentFile, oldKeyword);
                    keywordManager.addKeyword(currentFile, newKeyword);
                }
                updateKeywordGrid();
            } else {
                JOptionPane.showMessageDialog(mainPanel, "Invalid keyword format. Keywords cannot contain special characters like ',', '&', '|', etc.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void addKeyword() {
        if (currentFile == null) return;

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(mainPanel), "Add Keywords", true);
        dialog.setLayout(new BorderLayout(0, 10));
        ((JPanel) dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        KeywordEntryField keywordEntryField = new KeywordEntryField(keywordManager);
        keywordEntryField.setColumn(25);
        dialog.add(keywordEntryField, BorderLayout.CENTER);

        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> {
            List<String> newKeywords = keywordEntryField.getKeywords();
            if (!newKeywords.isEmpty()) {
                for (String newKeyword : newKeywords) {
                    if (isValidKeyword(newKeyword)) {
                        keywordManager.addKeyword(currentFile, newKeyword);
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Keyword '" + newKeyword + "' is invalid.", "Invalid Keyword", JOptionPane.WARNING_MESSAGE);
                    }
                }
                updateKeywordGrid();
            }
            dialog.dispose();
        });

        keywordEntryField.addActionListener(e -> addButton.doClick());

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);

        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(mainPanel);
        dialog.setVisible(true);
    }

    private boolean isValidKeyword(String keyword) {
        return !keyword.matches(".*[,&|!()].*");
    }
}
