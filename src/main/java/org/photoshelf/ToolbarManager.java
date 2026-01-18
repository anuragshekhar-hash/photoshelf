package org.photoshelf;

import org.photoshelf.ui.SearchBar;

import javax.swing.*;
import java.awt.*;

public class ToolbarManager {
    private final JTextField filterField;
    private final JComboBox<String> sortComboBox;
    private final JCheckBox sortDescendingCheckBox;
    private final JCheckBox showDuplicatesCheckBox;
    private final JButton showSelectedButton;
    private boolean isFilteredToSelection = false;
    private final JPanel toolPanel;

    public ToolbarManager(PhotoShelfUI mainApp) {

        JToolBar toolBar = new JToolBar();
        toolBar.setLayout(new FlowLayout(FlowLayout.LEFT));
        toolBar.setFloatable(false);

        filterField = new JTextField(10);
        filterField.setToolTipText("Enter file extension (e.g. jpg, png, gif, bmp) or leave empty for all");
        filterField.addActionListener(e -> mainApp.displayImages(mainApp.getCurrentDirectory()));

        sortComboBox = new JComboBox<>(new String[]{"Name", "Date Created", "Size", "Type"});
        // Use sortCurrentView for client-side sorting
        sortComboBox.addActionListener(e -> mainApp.sortCurrentView());

        sortDescendingCheckBox = new JCheckBox("Descending", true);
        // Use sortCurrentView for client-side sorting
        sortDescendingCheckBox.addActionListener(e -> mainApp.sortCurrentView());

        showDuplicatesCheckBox = new JCheckBox("Show only duplicates");
        showDuplicatesCheckBox.addActionListener(e -> mainApp.displayImages(mainApp.getCurrentDirectory()));

        toolBar.add(new JLabel("Filter by ext:"));
        toolBar.add(filterField);
        toolBar.add(Box.createHorizontalStrut(10));
        toolBar.add(Box.createHorizontalStrut(10));
        toolBar.add(new JLabel("Sort by:"));
        toolBar.add(sortComboBox);
        toolBar.add(sortDescendingCheckBox);
        toolBar.add(showDuplicatesCheckBox);
        
        showSelectedButton = new JButton("Show Selected");
        showSelectedButton.addActionListener(e -> {
            if (isFilteredToSelection) {
                //mainApp.displayImages(mainApp.getCurrentDirectory());
                mainApp.filterToSelection(false);

            } else {
                mainApp.filterToSelection(true);
            }
            // The state is toggled in PhotoShelfUI after the action completes
        });
        toolBar.add(showSelectedButton);
        toolPanel = new JPanel(new BorderLayout());
        toolPanel.add(toolBar, BorderLayout.NORTH);
        SearchBar search = new SearchBar(mainApp);
        toolPanel.add(search, BorderLayout.CENTER);
    }

    public JPanel getToolPanel() {
        return toolPanel;
    }

    public String getFilterText() {
        return filterField.getText();
    }

    public String getSortCriteria() {
        return (String) sortComboBox.getSelectedItem();
    }

    public boolean isSortDescending() {
        return sortDescendingCheckBox.isSelected();
    }

    public boolean isShowDuplicates() {
        return showDuplicatesCheckBox.isSelected();
    }

    public void setFilteredToSelection(boolean filtered) {
        isFilteredToSelection = filtered;
        if (isFilteredToSelection) {
            showSelectedButton.setText("Show All");
        } else {
            showSelectedButton.setText("Show Selected");
        }
    }
}
