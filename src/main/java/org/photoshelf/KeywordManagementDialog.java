package org.photoshelf;

import org.photoshelf.ui.KeywordEntryField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class KeywordManagementDialog extends JDialog {
    private final KeywordManager keywordManager;
    private final List<File> files;
    private final DefaultListModel<String> listModel;
    private final KeywordEntryField keywordEntryField;

    public KeywordManagementDialog(Frame owner, KeywordManager keywordManager, List<File> files) {
        super(owner, "Manage Keywords", true);
        this.keywordManager = keywordManager;
        this.files = files;
        this.listModel = new DefaultListModel<>();

        setLayout(new BorderLayout(10, 10));
        setSize(400, 500);
        setLocationRelativeTo(owner);

        // All keywords in selection
        Set<String> allKeywords = new TreeSet<>();
        for (File file : files) {
            allKeywords.addAll(keywordManager.getKeywords(file));
        }
        allKeywords.forEach(listModel::addElement);

        JList<String> keywordList = new JList<>(listModel);
        JScrollPane listScrollPane = new JScrollPane(keywordList);
        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBorder(BorderFactory.createTitledBorder("All Keywords in Selection"));
        listPanel.add(listScrollPane, BorderLayout.CENTER);

        JButton removeButton = new JButton("Remove Selected from All");
        removeButton.addActionListener(e -> removeSelectedKeyword(keywordList.getSelectedValue()));

        JButton applyButton = new JButton("Apply Selected to All");
        applyButton.addActionListener(e -> applySelectedKeyword(keywordList.getSelectedValuesList()));

        JPanel selectedKeywordActionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        selectedKeywordActionsPanel.add(applyButton);
        selectedKeywordActionsPanel.add(removeButton);
        listPanel.add(selectedKeywordActionsPanel, BorderLayout.SOUTH);

        // Add new keyword(s)
        JPanel addPanel = new JPanel(new BorderLayout(5, 5));
        addPanel.setBorder(BorderFactory.createTitledBorder("Add New Keywords (comma-separated)"));
        keywordEntryField = new KeywordEntryField(keywordManager);
        keywordEntryField.setColumn(25);
        JButton addButton = new JButton("Add to All");
        addPanel.add(keywordEntryField, BorderLayout.CENTER);
        addPanel.add(addButton, BorderLayout.EAST);

        keywordEntryField.addActionListener(e -> addButton.doClick());

        addButton.addActionListener(e -> {
            List<String> newKeywords = keywordEntryField.getKeywords();
            if (newKeywords.isEmpty()) {
                return;
            }

            List<String> invalidKeywords = newKeywords.stream()
                    .filter(kw -> !isValidKeyword(kw))
                    .collect(Collectors.toList());

            if (!invalidKeywords.isEmpty()) {
                JOptionPane.showMessageDialog(this, "The following keywords are invalid and were not added:\n" + String.join(", ", invalidKeywords) + "\n\nKeywords cannot contain ',', '&', '|', '!', '(', ')'", "Invalid Keywords", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int keywordsAddedCount = 0;
            for (String newKeyword : newKeywords) {
                for (File file : files) {
                    keywordManager.addKeyword(file, newKeyword);
                }
                if (!listModel.contains(newKeyword)) {
                    listModel.addElement(newKeyword);
                }
                keywordsAddedCount++;
            }
            keywordEntryField.setKeywords(List.of()); // Clear the field
            if (keywordsAddedCount > 0) {
                JOptionPane.showMessageDialog(this, keywordsAddedCount + " keyword(s) added to all " + files.size() + " images.");
            }
        });

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closePanel.add(closeButton);

        add(addPanel, BorderLayout.NORTH);
        add(listPanel, BorderLayout.CENTER);
        add(closePanel, BorderLayout.SOUTH);
    }

    private boolean isValidKeyword(String keyword) {
        return !keyword.matches(".*[\\,\\&\\|\\!\\(\\)].*");
    }

    private void removeSelectedKeyword(String keyword) {
        if (keyword != null) {
            int count = 0;
            for (File file : files) {
                if (keywordManager.hasKeyword(file, keyword)) {
                    keywordManager.removeKeyword(file, keyword);
                    count++;
                }
            }
            listModel.removeElement(keyword);
            JOptionPane.showMessageDialog(this, "Keyword '" + keyword + "' removed from " + count + " images.");
        }
    }

    private void applySelectedKeyword(List<String> keywords) {
        if (keywords != null) {
            for (File file : files) {
                for (String keyword : keywords) {
                    keywordManager.addKeyword(file, keyword);
                }
            }
            JOptionPane.showMessageDialog(this, "Keyword '" + keywords + "' applied to all " + files.size() + " images.");
        }
    }
}
