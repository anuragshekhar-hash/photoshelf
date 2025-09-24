package org.photoshelf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * A class that evaluates a boolean keyword expression against a set of keywords.
 * The expression is parsed once at initialization for efficient re-evaluation.
 */
public class KeywordExpressionEvaluator {

    private final List<String> postfixExpression;

    /**
     * Constructs an evaluator for a given keyword expression.
     * The expression is parsed and converted to postfix notation immediately.
     *
     * @param expression The boolean keyword expression (e.g., "(cat & dog) | !animal").
     */
    public KeywordExpressionEvaluator(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            this.postfixExpression = new ArrayList<>();
        } else {
            this.postfixExpression = infixToPostfix(expression);
        }
    }

    /**
     * Evaluates the pre-compiled expression against a given set of keywords.
     *
     * @param keywords The set of keywords to test against.
     * @return {@code true} if the keywords satisfy the expression, {@code false} otherwise.
     */
    public boolean evaluate(Set<String> keywords) {
        if (postfixExpression.isEmpty()) {
            return true; // An empty expression is always true.
        }

        Stack<Boolean> values = new Stack<>();
        for (String token : postfixExpression) {
            if (isOperator(token)) {
                try {
                    if (token.equals("!")) {
                        values.push(!values.pop());
                    } else {
                        boolean right = values.pop();
                        boolean left = values.pop();
                        if (token.equals("&")) {
                            values.push(left && right);
                        } else if (token.equals("|")) {
                            values.push(left || right);
                        }
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid expression format.", e);
                }
            } else { // Operand (keyword)
                values.push(keywords.contains(token.toLowerCase()));
            }
        }

        if (values.size() != 1) {
            throw new IllegalArgumentException("Malformed expression resulted in an invalid stack state.");
        }
        return values.pop();
    }

    /**
     * Converts an infix expression string to a postfix token list using the Shunting-yard algorithm.
     */
    private List<String> infixToPostfix(String expression) {
        List<String> postfix = new ArrayList<>();
        Stack<String> operators = new Stack<>();
        // Regex to split by operators/parentheses, keeping them, and by whitespace.
        String[] tokens = expression.split("(?<=[()&|!])|(?=[()&|!])|\\s+");

        for (String token : tokens) {
            token = token.trim();
            if (token.isEmpty()) {
                continue;
            }

            if (isOperator(token)) {
                while (!operators.isEmpty() && precedence(operators.peek()) >= precedence(token)) {
                    postfix.add(operators.pop());
                }
                operators.push(token);
            } else if (token.equals("(")) {
                operators.push(token);
            } else if (token.equals(")")) {
                while (!operators.isEmpty() && !operators.peek().equals("(")) {
                    postfix.add(operators.pop());
                }
                if (operators.isEmpty()) {
                    throw new IllegalArgumentException("Mismatched parentheses.");
                }
                operators.pop(); // Pop the '('
            } else { // Operand
                postfix.add(token);
            }
        }

        while (!operators.isEmpty()) {
            String op = operators.pop();
            if (op.equals("(")) {
                throw new IllegalArgumentException("Mismatched parentheses.");
            }
            postfix.add(op);
        }

        return postfix;
    }

    private boolean isOperator(String token) {
        return token.equals("&") || token.equals("|") || token.equals("!");
    }

    private int precedence(String operator) {
        switch (operator) {
            case "!":
                return 3;
            case "&":
                return 2;
            case "|":
                return 1;
            default:
                return 0; // For parentheses
        }
    }
}
