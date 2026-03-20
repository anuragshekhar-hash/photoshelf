package org.photoshelf.ui;

import org.photoshelf.KeywordManager;
import org.photoshelf.PhotoShelfUI;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class AllKeywordsDialog extends JDialog {
    private final KeywordManager keywordManager;
    private final DefaultListModel<String> listModel;
    private final JList<String> keywordList;
    private final SearchBar searchBar;

    public AllKeywordsDialog(PhotoShelfUI mainApp, SearchBar searchBar) {
        super(mainApp, "All Keywords", false);
        this.keywordManager = mainApp.getKeywordManager();
        this.searchBar = searchBar;
        this.listModel = new DefaultListModel<>();
        this.keywordList = new JList<>(listModel);

        setLayout(new BorderLayout(10, 10));
        setSize(500, 600);
        setLocationRelativeTo(mainApp);

        refreshList();

        keywordList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scrollPane = new JScrollPane(keywordList);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("All Keywords"));
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton renameButton = new JButton("Rename Selected");
        renameButton.addActionListener(e -> renameSelected());
        
        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.addActionListener(e -> deleteSelected());
        
        actionPanel.add(renameButton);
        actionPanel.add(deleteButton);
        
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search Options"));

        JButton searchAnyButton = new JButton("Search Selected (Match Any)");
        searchAnyButton.addActionListener(e -> searchSelected(false));

        JButton searchAllButton = new JButton("Search Selected (Match All)");
        searchAllButton.addActionListener(e -> searchSelected(true));

        searchPanel.add(searchAnyButton);
        searchPanel.add(searchAllButton);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(actionPanel, BorderLayout.NORTH);
        bottomPanel.add(searchPanel, BorderLayout.SOUTH);
        
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void refreshList() {
        listModel.clear();
        Set<String> keywords = new TreeSet<>(keywordManager.getAllKeywords());
        for (String kw : keywords) {
            listModel.addElement(kw);
        }
    }
    
    private void renameSelected() {
        String selected = keywordList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a single keyword to rename.", "Rename Keyword", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (keywordList.getSelectedIndices().length > 1) {
            JOptionPane.showMessageDialog(this, "Please select only one keyword to rename.", "Rename Keyword", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String newName = JOptionPane.showInputDialog(this, "Rename keyword '" + selected + "' to:", selected);
        if (newName != null && !newName.trim().isEmpty() && !newName.equals(selected)) {
            if (newName.matches(".*[\\,\\&\\|\\!\\(\\)].*")) {
                JOptionPane.showMessageDialog(this, "Keyword cannot contain ',', '&', '|', '!', '(', ')'", "Invalid Keyword", JOptionPane.ERROR_MESSAGE);
                return;
            }
            keywordManager.renameKeyword(selected, newName.trim());
            refreshList();
            keywordList.setSelectedValue(newName.trim(), true);
        }
    }
    
    private void deleteSelected() {
        List<String> selected = keywordList.getSelectedValuesList();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select keywords to delete.", "Delete Keyword", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to permanently delete the selected " + selected.size() + " keyword(s) from all images?", "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            for (String kw : selected) {
                keywordManager.deleteKeywordGlobally(kw);
            }
            refreshList();
        }
    }
    
    private void searchSelected(boolean matchAll) {
        List<String> selected = keywordList.getSelectedValuesList();
        if (selected.isEmpty()) {
             JOptionPane.showMessageDialog(this, "Please select keywords to search for.", "Search", JOptionPane.INFORMATION_MESSAGE);
             return;
        }
        
        String expression = String.join(matchAll ? " & " : " | ", selected);
        searchBar.setKeywordExpression(expression);
        searchBar.performSearchPublic();
        dispose();
    }
}
