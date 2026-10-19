package org.facenet.service.scale.engine.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;

/** Small, dependency-free arithmetic parser for formulas using x/value/raw. */
final class NumericFormulaEvaluator {
    private final String expression;
    private final BigDecimal value;
    private final Map<String, BigDecimal> variables;
    private int position;

    private NumericFormulaEvaluator(String expression, BigDecimal value, Map<String, BigDecimal> variables) {
        this.expression = expression.replace("${value}", "value").replace("${raw}", "raw");
        this.value = value;
        this.variables = variables == null ? Map.of() : variables;
    }

    static BigDecimal evaluate(String expression, BigDecimal value) {
        return evaluate(expression, value, Map.of());
    }

    static BigDecimal evaluate(String expression, BigDecimal value, Map<String, BigDecimal> variables) {
        NumericFormulaEvaluator parser = new NumericFormulaEvaluator(expression, value, variables);
        BigDecimal result = parser.parseExpression();
        parser.skipWhitespace();
        if (parser.position != parser.expression.length()) {
            throw new IllegalArgumentException("Invalid formula near: " + parser.expression.substring(parser.position));
        }
        return result;
    }

    private BigDecimal parseExpression() {
        BigDecimal result = parseTerm();
        while (true) {
            skipWhitespace();
            if (consume('+')) result = result.add(parseTerm());
            else if (consume('-')) result = result.subtract(parseTerm());
            else return result;
        }
    }

    private BigDecimal parseTerm() {
        BigDecimal result = parseFactor();
        while (true) {
            skipWhitespace();
            if (consume('*')) result = result.multiply(parseFactor());
            else if (consume('/')) result = result.divide(parseFactor(), MathContext.DECIMAL64);
            else if (consume('%')) result = result.remainder(parseFactor());
            else return result;
        }
    }

    private BigDecimal parseFactor() {
        skipWhitespace();
        if (consume('+')) return parseFactor();
        if (consume('-')) return parseFactor().negate();
        if (consume('(')) {
            BigDecimal nested = parseExpression();
            skipWhitespace();
            if (!consume(')')) throw new IllegalArgumentException("Missing ')' in formula");
            return nested;
        }
        if (position < expression.length() && Character.isLetter(expression.charAt(position))) {
            int start = position;
            while (position < expression.length() && Character.isLetterOrDigit(expression.charAt(position))) position++;
            String variable = expression.substring(start, position);
            if (variable.equalsIgnoreCase("x") || variable.equalsIgnoreCase("value")
                    || variable.equalsIgnoreCase("raw")) return value;
            BigDecimal registerValue = variables.get(variable.toUpperCase());
            if (registerValue != null) return registerValue;
            throw new IllegalArgumentException("Unsupported formula variable: " + variable);
        }
        int start = position;
        while (position < expression.length()) {
            char current = expression.charAt(position);
            if (!Character.isDigit(current) && current != '.') break;
            position++;
        }
        if (start == position) throw new IllegalArgumentException("Expected a number or value in formula");
        return new BigDecimal(expression.substring(start, position));
    }

    private void skipWhitespace() {
        while (position < expression.length() && Character.isWhitespace(expression.charAt(position))) position++;
    }

    private boolean consume(char expected) {
        if (position < expression.length() && expression.charAt(position) == expected) {
            position++;
            return true;
        }
        return false;
    }
}
