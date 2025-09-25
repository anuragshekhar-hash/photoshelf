package org.photoshelf.ui;

import org.photoshelf.KeywordAutoComplete;
import org.photoshelf.KeywordManager;
import org.photoshelf.KeywordSuggestion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VisualKeywordExpressionBuilderDialog extends JDialog {

    private final List<String> expressionTokens = new ArrayList<>();
    private final JPanel expressionCanvas;
    private String finalExpression;
    JTextArea manualExpressionArea;
    private boolean responce;

    public VisualKeywordExpressionBuilderDialog(Frame owner, KeywordManager keywordManager, String initialExpression) {
        super(owner, "Visual Keyword Expression Builder", true);
        setLayout(new BorderLayout(10, 10));

        // --- Top Panel: Manual Entry & GUI Controls ---
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));

        // 1. Manual Entry with Suggestions
        JPanel manualEntryPanel = new JPanel(new BorderLayout(5, 5));
        manualEntryPanel.setBorder(BorderFactory.createTitledBorder("Manual Expression Entry"));
        manualExpressionArea = new JTextArea(3, 30);
        manualExpressionArea.setLineWrap(true);
        manualExpressionArea.setWrapStyleWord(true);
        KeywordSuggestion keywordSuggestion = new KeywordSuggestion(keywordManager);
        new KeywordAutoComplete(manualExpressionArea, keywordSuggestion);

        manualExpressionArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_ENTER){
                    parseExpression();
                }
            }
        });

        JButton parseButton = new JButton("Parse");
        parseButton.setToolTipText("Parse the manual expression and populate the grid below");
        parseButton.addActionListener(e -> {
                parseExpression();
            });
        manualEntryPanel.add(new JScrollPane(manualExpressionArea), BorderLayout.CENTER);
        manualEntryPanel.add(parseButton, BorderLayout.EAST);

        // 2. GUI Control Panel (for adding tokens)
        JPanel controlPanel = new JPanel(new BorderLayout(10, 10));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Add to Expression via GUI"));

        JPanel operatorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        String[] operators = {"(", ")", "&", "|", "!"};
        for (String op : operators) {
            JButton button = new JButton(op);
            button.addActionListener(e -> addToken(op));
            operatorPanel.add(button);
        }

        JPanel keywordPanel = new JPanel(new BorderLayout(5, 5));
        KeywordEntryField keywordEntryField = new KeywordEntryField(keywordManager);
        JButton addKeywordButton = new JButton("Add Keyword");
        addKeywordButton.addActionListener(e -> {
            List<String> keywords = keywordEntryField.getKeywords();
            if (!keywords.isEmpty()) {
                addToken(keywords.get(0));
                keywordEntryField.setKeywords(List.of());
            }
        });
        keywordEntryField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_ENTER){
                    addKeywordButton.doClick();
                }
            }
        });
        keywordPanel.add(keywordEntryField, BorderLayout.CENTER);
        keywordPanel.add(addKeywordButton, BorderLayout.EAST);

        controlPanel.add(operatorPanel, BorderLayout.NORTH);
        controlPanel.add(keywordPanel, BorderLayout.CENTER);

        topPanel.add(manualEntryPanel, BorderLayout.NORTH);
        topPanel.add(controlPanel, BorderLayout.CENTER);

        // --- Center: Expression Canvas ---
        expressionCanvas = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        expressionCanvas.setBorder(BorderFactory.createTitledBorder("Current Expression (Click token to remove)"));
        JScrollPane scrollPane = new JScrollPane(expressionCanvas);
        scrollPane.setPreferredSize(new Dimension(400, 100));

        // --- Bottom: Dialog Buttons ---
        JPanel dialogButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            finalExpression = expressionTokens.stream().collect(Collectors.joining(" "));
            responce = true;
            dispose();
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            finalExpression = null;
            responce = false;
            dispose();
        });

        JButton clearButton = new JButton("Clear All");
        clearButton.addActionListener(e -> {
            expressionTokens.clear();
            manualExpressionArea.setText("");
            redrawExpressionCanvas();
        });

        dialogButtonPanel.add(clearButton);
        dialogButtonPanel.add(cancelButton);
        dialogButtonPanel.add(okButton);

        // --- Main Layout ---
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(dialogButtonPanel, BorderLayout.SOUTH);

        add(mainPanel);
        pack();
        setLocationRelativeTo(owner);

        if (initialExpression != null && !initialExpression.isBlank()) {
            manualExpressionArea.setText(initialExpression);
            parseButton.doClick();
        }
    }

    private void parseExpression() {
        String expression = manualExpressionArea.getText();
        if (expression != null && !expression.isBlank()) {
            String[] tokens = expression.split("(?<=[()&|!])|(?=[()&|!])");
            expressionTokens.clear();
            Arrays.stream(tokens)
                    .map(String::trim)
                    .filter(token -> !token.isEmpty())
                    .forEach(expressionTokens::add);
            redrawExpressionCanvas();
        }
    }

    private void addToken(String token) {
        expressionTokens.add(token);
        redrawExpressionCanvas();
    }

    private void removeToken(int index) {
        if (index >= 0 && index < expressionTokens.size()) {
            expressionTokens.remove(index);
            redrawExpressionCanvas();
        }
    }

    private void redrawExpressionCanvas() {
        expressionCanvas.removeAll();
        for (int i = 0; i < expressionTokens.size(); i++) {
            expressionCanvas.add(createTokenComponent(expressionTokens.get(i), i));
        }
        expressionCanvas.revalidate();
        expressionCanvas.repaint();
    }

    private JComponent createTokenComponent(String token, int index) {
        JPanel panel = new JPanel(new BorderLayout(3, 0));
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JLabel label = new JLabel(token);
        label.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        JButton removeButton = new JButton("x");
        removeButton.setMargin(new Insets(0, 2, 0, 2));
        removeButton.setFocusPainted(false);
        removeButton.setBorderPainted(false);
        removeButton.setContentAreaFilled(false);
        removeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        removeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                panel.setBackground(new Color(255, 200, 200));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                panel.setBackground(null);
            }
        });
        removeButton.addActionListener(e -> removeToken(index));

        panel.add(label, BorderLayout.CENTER);
        panel.add(removeButton, BorderLayout.EAST);
        return panel;
    }

    public String getExpression() {
        return finalExpression;
    }

    public boolean getResponce() {
        return responce;
    }
}
