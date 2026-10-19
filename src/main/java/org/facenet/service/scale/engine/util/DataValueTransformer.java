package org.facenet.service.scale.engine.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/** Applies optional numeric scaling/formulas and produces a stable string value. */
public final class DataValueTransformer {

    private DataValueTransformer() {
    }

    public static String transform(Object rawValue, Map<String, Object> config) {
        return transform(rawValue, config, Map.of());
    }

    public static String transform(Object rawValue, Map<String, Object> config,
                                   Map<String, BigDecimal> variables) {
        if (rawValue == null) return null;

        BigDecimal numeric = rawValue instanceof Number ? new BigDecimal(rawValue.toString()) : null;
        if (numeric == null) {
            if (hasNumericTransform(config)) {
                throw new IllegalArgumentException("Formula/scaling can only be applied to numeric data");
            }
            return rawValue.toString();
        }

        BigDecimal result = numeric;
        if (config != null && config.get("multiplier") != null) {
            result = result.multiply(number(config.get("multiplier"), "multiplier"));
        } else if (config != null && config.get("scale") != null) {
            result = result.multiply(number(config.get("scale"), "scale"));
        }
        if (config != null && config.get("divisor") != null) {
            BigDecimal divisor = number(config.get("divisor"), "divisor");
            if (divisor.compareTo(BigDecimal.ZERO) == 0) throw new IllegalArgumentException("divisor must not be zero");
            result = result.divide(divisor, 16, RoundingMode.HALF_UP);
        }
        if (config != null && config.get("offset") != null) {
            result = result.add(number(config.get("offset"), "offset"));
        }
        if (config != null && config.get("formula") != null && !config.get("formula").toString().isBlank()) {
            result = NumericFormulaEvaluator.evaluate(config.get("formula").toString(), result, variables);
        }
        if (config != null && config.get("decimal_places") != null) {
            int places = number(config.get("decimal_places"), "decimal_places").intValueExact();
            if (places < 0 || places > 12) throw new IllegalArgumentException("decimal_places must be between 0 and 12");
            return result.setScale(places, RoundingMode.HALF_UP).toPlainString();
        }
        return result.stripTrailingZeros().toPlainString();
    }

    private static boolean hasNumericTransform(Map<String, Object> config) {
        return config != null && (config.get("formula") != null || config.get("multiplier") != null
                || config.get("scale") != null || config.get("divisor") != null
                || config.get("offset") != null || config.get("decimal_places") != null);
    }

    private static BigDecimal number(Object value, String field) {
        try {
            return new BigDecimal(value.toString().trim());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(field + " must be numeric", exception);
        }
    }
}
