package org.photoshelf;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

public class ExpressionParser {

    /**
     * Parses a string expression into a list of tokens.
     * The expression is split by operators, keeping the operators as tokens.
     * For example, "(keyword1 & keyword2) | !keyword3" becomes
     * ["(", "keyword1", "&", "keyword2", ")", "|", "!", "keyword3"].
     *
     * @param expression The string expression to parse.
     * @return A list of string tokens.
     * @throws ParseException if the expression has a syntax error.
     */
    public List<String> parse(String expression) throws ParseException {
        if (expression == null || expression.isBlank()) {
            return List.of();
        }

        // This regex splits the string by operators, but keeps the operators as separate tokens.
        // It uses lookarounds to match the positions before or after the operators.
        String[] tokens = expression.split("(?<=[()&|!])|(?=[()&|!])");

        List<String> tokenList = Arrays.stream(tokens)
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toList());

        validate(tokenList);

        return tokenList;
    }

    /**
     * Validates the list of tokens for syntax errors.
     * Currently checks for balanced parentheses.
     *
     * @param tokens The list of tokens to validate.
     * @throws ParseException if a syntax error is found.
     */
    private void validate(List<String> tokens) throws ParseException {
        Deque<String> stack = new ArrayDeque<>();
        for (String token : tokens) {
            if (token.equals("(")) {
                stack.push(token);
            } else if (token.equals(")")) {
                if (stack.isEmpty()) {
                    throw new ParseException("Unbalanced parentheses: Extra closing parenthesis.");
                }
                stack.pop();
            }
        }
        if (!stack.isEmpty()) {
            throw new ParseException("Unbalanced parentheses: Missing closing parenthesis.");
        }
    }
}
