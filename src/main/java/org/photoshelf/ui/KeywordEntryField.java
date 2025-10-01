package org.photoshelf.ui;

import org.photoshelf.KeywordAutoComplete;
import org.photoshelf.KeywordManager;
import org.photoshelf.KeywordSuggestion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class KeywordEntryField extends JPanel {

    private final JTextField textField;

    public KeywordEntryField(KeywordManager keywordManager) {
        setLayout(new BorderLayout());

        textField = new JTextField();

        JScrollPane scrollPane = new JScrollPane(textField);
        add(scrollPane, BorderLayout.CENTER);

        KeywordSuggestion keywordSuggestion = new KeywordSuggestion(keywordManager);
        KeywordAutoComplete autoComplete = new KeywordAutoComplete(textField, keywordSuggestion);
    }

    public void addActionListener(ActionListener l) {
        textField.addActionListener(l);
    }

    public void setColumn(int col) {
        textField.setColumns(col);
    }

    public void setEnable(boolean enable) {
        textField.setEnabled(enable);
    }

    public List<String> getKeywords() {
        String text = textField.getText();
        if (text.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public void setKeywords(List<String> keywords) {
        String text = String.join(", ", keywords);
        textField.setText(text);
    }

    public JTextField getTextField() {
        return textField;
    }
}
