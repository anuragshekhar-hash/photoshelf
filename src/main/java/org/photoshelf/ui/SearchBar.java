package org.photoshelf.ui;

import org.photoshelf.ParseException;
import org.photoshelf.PhotoShelfUI;
import org.photoshelf.SearchParams;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
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

    class SearchAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
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
                }
                catch (ParseException pe) {
                    JOptionPane.showMessageDialog(mainApp, pe.getMessage(), "Parsing Error", JOptionPane.ERROR_MESSAGE);
                }
            }
            String pattern = getSearchQuery();
            if (pattern != null && !pattern.isBlank()) {
                searchParams.setSearchString(pattern.trim());
            }
            mainApp.executeSearch(searchParams, filterCurrentViewCheckBox.isSelected());
        }
    }

    public SearchBar(PhotoShelfUI mainApp) {
        this.mainApp = mainApp;
        setFloatable(true);

        SearchAction searchAction = new SearchAction();

        // --- Components ---
        searchAllDirsCheckBox = new JCheckBox("Recursive");
        searchAllDirsCheckBox.setToolTipText("Search in all subdirectories");

        searchQueryField = new JTextField(15);
        searchQueryField.setToolTipText("Enter a search pattern for file names and press Enter");
        searchQueryField.addActionListener(searchAction);

        keywordField = new ExpressionTextBox(15, mainApp.getKeywordManager());
        keywordField.setToolTipText("Enter comma-separated keywords and press Enter");
        keywordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume(); // Prevent newline in text area
                    searchAction.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, null));
                }
            }
        });

        noKeywordsCheckBox = new JCheckBox("No Keywords");
        noKeywordsCheckBox.setToolTipText("Find images that have no keywords");

        filterCurrentViewCheckBox = new JCheckBox("Filter current view");
        filterCurrentViewCheckBox.setToolTipText("Apply search criteria to the images currently shown in the grid");

        JButton expressionBuilderButton = new JButton("Expression...");
        expressionLabel = new JLabel("");
        expressionBuilderButton.addActionListener(e -> {
            VisualKeywordExpressionBuilderDialog dialog = new VisualKeywordExpressionBuilderDialog(mainApp, mainApp.getKeywordManager(), keywordExpression);
            dialog.setVisible(true);
            String newExpression = dialog.getExpression();
            if (newExpression != null) {
                keywordExpression = newExpression;
                expressionLabel.setText(" " + keywordExpression);
            }
        });

        noKeywordsCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    keywordField.setEnabled(false);
                    expressionBuilderButton.setEnabled(false);
                }
                else {
                    keywordField.setEnabled(true);
                    expressionBuilderButton.setEnabled(true);
                }
            }
        });

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(searchAction);

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
            searchAllDirsCheckBox.setSelected(false);
            searchQueryField.setText("");
            noKeywordsCheckBox.setSelected(false);
            keywordField.clear();
            filterCurrentViewCheckBox.setSelected(false);
            keywordExpression = null;
            expressionLabel.setText("");
            mainApp.displayImages(mainApp.getCurrentDirectory()); // Refresh view
        });

        // --- Layout ---
        setLayout(new FlowLayout(FlowLayout.LEFT));
        add(new JLabel("Scope:"));
        add(searchAllDirsCheckBox);
        addSeparator();

        add(new JLabel("File Name:"));
        add(searchQueryField);
        addSeparator();

        add(new JLabel("Keywords:"));
        add(keywordField);
        add(noKeywordsCheckBox);
        add(expressionBuilderButton);
        add(expressionLabel);
        addSeparator();

        add(filterCurrentViewCheckBox);
        add(searchButton);
        add(clearButton);
    }

    public String getSearchQuery() {
        return searchQueryField.getText();
    }

    public boolean isRecursive() {
        return searchAllDirsCheckBox.isSelected();
    }
}
