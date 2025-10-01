package org.photoshelf.ui;

import org.photoshelf.ExpressionParser;
import org.photoshelf.KeywordAutoComplete;
import org.photoshelf.KeywordManager;
import org.photoshelf.KeywordSuggestion;
import org.photoshelf.ParseException;

import javax.swing.*;
import java.util.List;

public class ExpressionTextBox extends JTextField {

    private final ExpressionParser parser;

    public ExpressionTextBox(int columns, KeywordManager keywordManager) {
        super(columns);
        this.parser = new ExpressionParser();
        // Setup autocomplete and suggestions
        KeywordSuggestion keywordSuggestion = new KeywordSuggestion(keywordManager);
        new KeywordAutoComplete(this, keywordSuggestion);
    }

    /**
     * Parses the current text in the text box into expression tokens.
     * @return A list of string tokens.
     * @throws ParseException if the expression is invalid.
     */
    public List<String> getParsedTokens() throws ParseException {
        return parser.parse(super.getText());
    }

    public void clear() {
        super.setText("");
    }
}
