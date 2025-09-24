package org.photoshelf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class StatusPanelManager {
    private final JPanel statusPanel;
    private final JLabel totalFilesLabel;
    private final JLabel previewFileLabel;
    private final JLabel selectionCountLabel;
    private final JLabel selectionSizeLabel;
    private final JLabel searchStatusLabel;

    public StatusPanelManager() {
        statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                new EmptyBorder(5, 10, 5, 10)
        ));

        totalFilesLabel = new JLabel("Total Files: 0");
        previewFileLabel = new JLabel("Preview: None");
        selectionCountLabel = new JLabel("Selected: 0");
        selectionSizeLabel = new JLabel("Size: 0 KB");
        searchStatusLabel = new JLabel();

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        leftPanel.add(totalFilesLabel);
        leftPanel.add(previewFileLabel);

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        centerPanel.add(searchStatusLabel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        rightPanel.add(selectionCountLabel);
        rightPanel.add(selectionSizeLabel);

        statusPanel.add(leftPanel, BorderLayout.WEST);
        statusPanel.add(centerPanel, BorderLayout.CENTER);
        statusPanel.add(rightPanel, BorderLayout.EAST);
    }

    public JPanel getStatusPanel() {
        return statusPanel;
    }

    public void updateTotalFiles(int count) {
        totalFilesLabel.setText("Total Files: " + count);
    }

    public void updatePreviewFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            previewFileLabel.setText("Preview: None");
        } else {
            previewFileLabel.setText("Preview: " + fileName);
        }
    }

    public void updateSelectionCount(int count) {
        selectionCountLabel.setText("Selected: " + count);
    }

    public void updateSelectionSize(long totalSizeInBytes) {
        long totalSizeInKB = totalSizeInBytes / 1024;
        selectionSizeLabel.setText(String.format("Size: %,d KB", totalSizeInKB));
    }

    public void setSearchStatus(String status) {
        searchStatusLabel.setText(status);
    }
}
