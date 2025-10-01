package org.photoshelf.ui;

import org.photoshelf.ParseException;
import org.photoshelf.PhotoShelfUI;
import org.photoshelf.SearchParams;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;

public class SearchBar extends JToolBar {
    private final JTextField searchQueryField;
    private final ExpressionTextBox keywordField;
    private final JCheckBox searchAllDirsCheckBox;
    private final JCheckBox noKeywordsCheckBox;
    private final JCheckBox filterCurrentViewCheckBox;
    private final PhotoShelfUI mainApp;
    private String keywordExpression;
    private final JLabel expressionLabel;

    public SearchBar(PhotoShelfUI mainApp) {
        this.mainApp = mainApp;
        setFloatable(true);
        setLayout(new FlowLayout(FlowLayout.LEFT));

        // --- Components ---
        searchAllDirsCheckBox = new JCheckBox("Recursive");
        searchAllDirsCheckBox.setToolTipText("Search in all subdirectories");

        searchQueryField = new JTextField(15);
        searchQueryField.setToolTipText("Enter a search pattern for file names and press Enter");
        searchQueryField.addActionListener(e -> performSearch());

        keywordField = new ExpressionTextBox(15, mainApp.getKeywordManager());
        keywordField.setToolTipText("Enter a keyword expression and press Enter");
        keywordField.addActionListener(e -> performSearch());

        noKeywordsCheckBox = new JCheckBox("Without Keywords");
        noKeywordsCheckBox.setToolTipText("Find images that have no keywords");

        filterCurrentViewCheckBox = new JCheckBox("Filter view");
        filterCurrentViewCheckBox.setToolTipText("Apply search criteria to the images currently shown in the grid");

        JButton expressionBuilderButton = new JButton("...");
        expressionBuilderButton.setToolTipText("Open Visual Keyword Expression Builder");
        expressionLabel = new JLabel("");
        expressionBuilderButton.addActionListener(e -> {
            VisualKeywordExpressionBuilderDialog dialog = new VisualKeywordExpressionBuilderDialog(mainApp, mainApp.getKeywordManager(), keywordExpression);
            dialog.setVisible(true);
            if (dialog.getResponce()) {
                keywordExpression = dialog.getExpression();
                expressionLabel.setText(" " + keywordExpression);
            }
        });

        noKeywordsCheckBox.addItemListener(e -> {
            boolean isSelected = e.getStateChange() == ItemEvent.SELECTED;
            keywordField.setEnabled(!isSelected);
            expressionBuilderButton.setEnabled(!isSelected);
        });

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> performSearch());

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
            searchAllDirsCheckBox.setSelected(false);
            searchQueryField.setText("");
            noKeywordsCheckBox.setSelected(false);
            keywordField.setText("");
            filterCurrentViewCheckBox.setSelected(false);
            keywordExpression = null;
            expressionLabel.setText("");
            mainApp.displayImages(mainApp.getCurrentDirectory()); // Refresh view
        });

        // --- Layout ---
        add(new JLabel("File Name:"));
        add(searchQueryField);
        addSeparator();

        add(new JLabel("Keywords:"));
        add(noKeywordsCheckBox);
        add(keywordField);
        add(expressionBuilderButton);
        add(expressionLabel);
        addSeparator();

        add(searchAllDirsCheckBox);
        add(filterCurrentViewCheckBox);
        addSeparator();

        add(searchButton);
        add(clearButton);
    }

    private void performSearch() {
        SearchParams searchParams = new SearchParams();
        searchParams.setRecursive(isRecursive());
        if (noKeywordsCheckBox.isSelected()) {
            searchParams.setNoKeywords(true);
        } else if (keywordExpression != null && !keywordExpression.isBlank()) {
            searchParams.setExpression(keywordExpression);
        } else {
            try {
                List<String> keywords = keywordField.getParsedTokens();
                if (!keywords.isEmpty()) {
                    searchParams.setExpression(keywordField.getText());
                }
            } catch (ParseException pe) {
                JOptionPane.showMessageDialog(mainApp, pe.getMessage(), "Parsing Error", JOptionPane.ERROR_MESSAGE);
                return; // Stop the search if there's a parsing error
            }
        }
        String pattern = getSearchQuery();
        if (pattern != null && !pattern.isBlank()) {
            searchParams.setSearchString(pattern.trim());
        }
        mainApp.executeSearch(searchParams, filterCurrentViewCheckBox.isSelected());
    }

    public String getSearchQuery() {
        return searchQueryField.getText();
    }

    public boolean isRecursive() {
        return searchAllDirsCheckBox.isSelected();
    }
}
