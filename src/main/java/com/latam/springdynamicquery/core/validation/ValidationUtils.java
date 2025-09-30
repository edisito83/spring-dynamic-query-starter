package com.latam.springdynamicquery.core.validation;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Utilidades de validación usando Java 17 pattern matching.
 * Proporciona métodos para validar diferentes tipos de valores en consultas dinámicas.
 */
public final class ValidationUtils {
    
    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Valida si un valor es válido para usar en consultas dinámicas.
     * Utiliza pattern matching de Java 17 para instanceof.
     * 
     * @param value El valor a validar
     * @return true si el valor es válido, false en caso contrario
     */
    public static boolean isValidValue(Object value) {
        return switch (value) {
            case null -> false;
            case String s -> !s.trim().isEmpty();
            case Collection<?> c -> !c.isEmpty();
            case Object[] array -> array.length > 0;
            default -> true;
        };
    }
    
    /**
     * Verifica si un valor numérico es válido (no nulo y mayor que 0).
     * 
     * @param value El valor numérico a validar
     * @return true si el valor es numérico válido, false en caso contrario
     */
    public static boolean isValidNumericValue(Object value) {
        return switch (value) {
            case null -> false;
            case Integer i -> i > 0;
            case Long l -> l > 0L;
            case Double d -> d > 0.0;
            case Float f -> f > 0.0f;
            case BigDecimal bd -> bd.compareTo(BigDecimal.ZERO) > 0;
            default -> false;
        };
    }
    
    /**
     * Verifica si una cadena no es nula ni vacía.
     * 
     * @param value La cadena a validar
     * @return true si la cadena es válida, false en caso contrario
     */
    public static boolean isValidString(String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    /**
     * Verifica si una colección no es nula ni vacía.
     * 
     * @param collection La colección a validar
     * @return true si la colección es válida, false en caso contrario
     */
    public static boolean isValidCollection(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }
}