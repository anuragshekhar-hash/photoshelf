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
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Set;

public class PreviewPanelManager {
    private final JLabel previewLabel;
    private final JScrollPane previewScroll;
    private BufferedImage originalImage;
    private double scale = 1.0;
    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 5.0;
    private final JSplitPane mainPanel;
    private final JPanel keywordGridPanel;
    private final KeywordManager keywordManager;
    private File currentFile;

    public PreviewPanelManager(KeywordManager keywordManager) {
        this.keywordManager = keywordManager;

        previewLabel = new JLabel("Select an image to preview", JLabel.CENTER);
        previewLabel.setHorizontalAlignment(JLabel.CENTER);
        previewLabel.setVerticalAlignment(JLabel.CENTER);
        previewScroll = new JScrollPane(previewLabel);
        previewScroll.setPreferredSize(new Dimension(400, 300));

        previewScroll.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.isControlDown()) {
                    if (originalImage == null) return; // Don't zoom if no image or if it's a GIF

                    int rotation = e.getWheelRotation();
                    if (rotation < 0) { // Zoom in
                        scale *= 1.1;
                        if (scale > MAX_SCALE) scale = MAX_SCALE;
                    } else { // Zoom out
                        scale /= 1.1;
                        if (scale < MIN_SCALE) scale = MIN_SCALE;
                    }
                    updatePreview();
                    e.consume(); // Prevent the scroll pane from scrolling
                }
            }
        });

        keywordGridPanel = new JPanel(new GridLayout(0, 1, 0, 2));
        keywordGridPanel.setBackground(Color.WHITE);

        // Wrapper panel to prevent grid from stretching
        JPanel keywordGridWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        keywordGridWrapper.setBackground(Color.WHITE);
        keywordGridWrapper.add(keywordGridPanel);

        JScrollPane keywordScroll = new JScrollPane(keywordGridWrapper);
        keywordScroll.setBorder(BorderFactory.createTitledBorder("Keywords"));
        keywordScroll.getViewport().setBackground(Color.WHITE);

        JButton addKeywordButton = new JButton("Add");
        addKeywordButton.addActionListener(e -> addKeyword());

        JPanel keywordButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
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
            scale = 1;
            if (imgFile.getName().toLowerCase().endsWith(".gif")) {
                this.originalImage = null;
                previewLabel.setIcon(new ImageIcon(imgFile.getAbsolutePath()));
                previewLabel.setText("");
            } else {
                this.originalImage = ImageIO.read(imgFile);
                if (this.originalImage == null) {
                    previewLabel.setIcon(null);
                    previewLabel.setText("Cannot preview this file");
                    return;
                }
                updatePreview();
            }
            updateKeywordGrid();
        } catch (Exception ex) {
            this.originalImage = null;
            previewLabel.setIcon(null);
            previewLabel.setText("Could not open image");
            ex.printStackTrace();
        }
    }

    private void updatePreview() {
        if (originalImage == null) return;

        int maxW = previewScroll.getViewport().getWidth();
        int maxH = previewScroll.getViewport().getHeight();
        int imgW = originalImage.getWidth();
        int imgH = originalImage.getHeight();

        if (scale == 1.0 && (imgW > maxW || imgH > maxH)) {
            scale = Math.min((double) maxW / imgW, (double) maxH / imgH);
        }

        int newW = (int) (imgW * scale);
        int newH = (int) (imgH * scale);

        BufferedImage scaledImage = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, newW, newH, null);
        g2d.dispose();

        previewLabel.setIcon(new ImageIcon(scaledImage));
        previewLabel.setText("");
        previewLabel.revalidate();
    }

    private void updateKeywordGrid() {
        keywordGridPanel.removeAll();
        if (currentFile != null) {
            Set<String> keywords = keywordManager.getKeywords(currentFile);
            if (keywords.isEmpty()) {
                JPanel emptyPanel = new JPanel();
                emptyPanel.setBackground(Color.WHITE);
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
        panel.setBackground(Color.WHITE);
        Border matteBorder = BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220));
        Border emptyBorder = BorderFactory.createEmptyBorder(2, 5, 2, 5);
        panel.setBorder(BorderFactory.createCompoundBorder(matteBorder, emptyBorder));

        JLabel label = new JLabel(keyword);

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
        buttonsPanel.setBackground(Color.WHITE);
        buttonsPanel.add(editButton);
        buttonsPanel.add(removeButton);

        panel.add(label, BorderLayout.CENTER);
        panel.add(buttonsPanel, BorderLayout.EAST);
        return panel;
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
                button.setForeground(Color.BLACK);
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
