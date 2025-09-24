package org.photoshelf;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

public class KeywordAutoComplete {

    private final JTextComponent textComponent;
    private final KeywordSuggestion keywordSuggestion;
    private final JPopupMenu suggestionMenu;
    private final JList<String> suggestionList;
    private final DefaultListModel<String> suggestionListModel;
    private boolean isAcceptingSuggestion = false;

    public KeywordAutoComplete(JTextComponent textComponent, KeywordSuggestion keywordSuggestion) {
        this.textComponent = textComponent;
        this.keywordSuggestion = keywordSuggestion;

        suggestionMenu = new JPopupMenu();
        suggestionListModel = new DefaultListModel<>();
        suggestionList = new JList<>(suggestionListModel);
        suggestionMenu.add(new JScrollPane(suggestionList));
        suggestionMenu.setFocusable(false);
        suggestionList.setFocusable(false);

        textComponent.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (isAcceptingSuggestion) return;
                showSuggestions();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (isAcceptingSuggestion) return;
                showSuggestions();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Not used for plain text components
            }
        });

        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int index = suggestionList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        String selectedSuggestion = suggestionList.getModel().getElementAt(index);
                        acceptSuggestion(selectedSuggestion);
                    }
                }
            }
        });

        textComponent.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!suggestionMenu.isVisible()) {
                    return;
                }

                int keyCode = e.getKeyCode();
                switch (keyCode) {
                    case KeyEvent.VK_DOWN:
                        int selectedIndex = suggestionList.getSelectedIndex();
                        if (selectedIndex < suggestionListModel.getSize() - 1) {
                            suggestionList.setSelectedIndex(selectedIndex + 1);
                            suggestionList.ensureIndexIsVisible(selectedIndex + 1);
                        }
                        e.consume();
                        break;
                    case KeyEvent.VK_UP:
                        selectedIndex = suggestionList.getSelectedIndex();
                        if (selectedIndex > 0) {
                            suggestionList.setSelectedIndex(selectedIndex - 1);
                            suggestionList.ensureIndexIsVisible(selectedIndex - 1);
                        }
                        e.consume();
                        break;
                    case KeyEvent.VK_ENTER:
                    case KeyEvent.VK_TAB:
                        String selectedSuggestion = suggestionList.getSelectedValue();
                        if (selectedSuggestion != null) {
                            acceptSuggestion(selectedSuggestion);
                        }
                        e.consume();
                        break;
                    case KeyEvent.VK_ESCAPE:
                        suggestionMenu.setVisible(false);
                        e.consume();
                        break;
                }
            }
        });
    }

    private void showSuggestions() {
        SwingUtilities.invokeLater(() -> {
            int caretPosition = textComponent.getCaretPosition();
            String text;
            try {
                text = textComponent.getText(0, caretPosition);
            } catch (BadLocationException ex) {
                System.err.println("Error getting text: " + ex.getMessage());
                return;
            }

            int wordStart = findWordStart(text, caretPosition);
            String prefix = text.substring(wordStart, caretPosition);

            if (prefix.trim().isEmpty()) {
                suggestionMenu.setVisible(false);
                return;
            }

            Set<String> suggestions = keywordSuggestion.getSuggestions(prefix.trim());
            suggestionListModel.clear();
            if (suggestions.isEmpty() || (suggestions.size() == 1 && suggestions.iterator().next().equalsIgnoreCase(prefix.trim()))) {
                suggestionMenu.setVisible(false);
                return;
            }

            suggestions.forEach(suggestionListModel::addElement);
            suggestionList.setSelectedIndex(0);

            try {
                Rectangle rect = textComponent.modelToView(caretPosition);
                suggestionMenu.show(textComponent, rect.x, rect.y + rect.height);
            } catch (BadLocationException e) {
                suggestionMenu.setVisible(false);
            }
        });
    }

    private void acceptSuggestion(String suggestion) {
        isAcceptingSuggestion = true;
        try {
            int caretPosition = textComponent.getCaretPosition();
            String text = textComponent.getText(0, caretPosition);
            int wordStart = findWordStart(text, caretPosition);

            textComponent.getDocument().remove(wordStart, caretPosition - wordStart);
            textComponent.getDocument().insertString(wordStart, suggestion, null);
            suggestionMenu.setVisible(false);
        } catch (BadLocationException e) {
            System.err.println("Error accepting suggestion: " + e.getMessage());
        } finally {
            isAcceptingSuggestion = false;
        }
    }

    private int findWordStart(String text, int offset) {
        if (offset == 0) {
            return 0;
        }
        int commaIndex = text.lastIndexOf(',', offset - 1);
        int start = commaIndex + 1;
        // Skip leading whitespace after the comma
        while (start < offset && Character.isWhitespace(text.charAt(start))) {
            start++;
        }
        return start;
    }
}
