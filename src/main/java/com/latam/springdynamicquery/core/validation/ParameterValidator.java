package com.latam.springdynamicquery.core.validation;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Validador de parámetros para consultas dinámicas.
 * Proporciona métodos para validar diferentes tipos de valores que se usan
 * en FilterCriteria y construcción de queries.
 */
public final class ParameterValidator {
    
    // Pattern para detectar concatenación de null en strings
    private static final Pattern NULL_CONCATENATION_PATTERN = 
        Pattern.compile("^[%_]*null[%_]*$", Pattern.CASE_INSENSITIVE);
    
    private static final int MAX_LENGTH_FOR_NULL_CHECK = 20;
    
    private ParameterValidator() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Valida si un valor es válido para usar en consultas dinámicas.
     * Detecta nulls, strings vacíos, strings con "null" literal, colecciones vacías.
     * 
     * @param value El valor a validar
     * @return true si el valor es válido, false en caso contrario
     */
    public static boolean isValidValue(Object value) {
        return switch (value) {
            case null -> false;
            case String s -> isValidStringValue(s);
            case Collection<?> c -> !c.isEmpty();
            case Object[] array -> array.length > 0;
            default -> true;
        };
    }
    
    /**
     * Valida que un string no sea null, vacío, o resultado de concatenación con null.
     * Detecta casos como "null", "%null%", "undefined", espacios en blanco.
     * 
     * @param value El string a validar
     * @return true si el string es válido, false en caso contrario
     */
    private static boolean isValidStringValue(String value) {
        if (value == null) {
            return false;
        }
        
        // Detectar strings solo con espacios en blanco (incluyendo tabs, newlines, etc.)
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        
        // Detectar strings literales problemáticos
        if (trimmed.equals("null") || 
            trimmed.equals("undefined") || 
            trimmed.equals("NULL") ||
            trimmed.equals("Null")) {
            return false;
        }
        
        // Detectar concatenación de null en patrones LIKE: %null%, _null_, null%, etc.
        // Solo validar en strings cortos para evitar falsos positivos en búsquedas legítimas
        if (trimmed.length() <= MAX_LENGTH_FOR_NULL_CHECK && 
            NULL_CONCATENATION_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Verifica si un valor numérico es válido.
     * Acepta cero, positivos y negativos. Rechaza null, NaN e Infinity.
     * 
     * @param value El valor numérico a validar
     * @return true si el valor es numérico válido, false en caso contrario
     */
    public static boolean isValidNumericValue(Object value) {
        return switch (value) {
            case null -> false;
            case Integer i -> true;  // Acepta cualquier int (positivo, negativo, cero)
            case Long l -> true;
            case Double d -> !d.isNaN() && !d.isInfinite();
            case Float f -> !f.isNaN() && !f.isInfinite();
            case BigDecimal bd -> true;
            default -> false;
        };
    }
    
    /**
     * Verifica si un valor numérico es positivo (mayor que cero).
     * Útil para IDs, cantidades, etc.
     * 
     * @param value El valor numérico a validar
     * @return true si el valor es positivo, false en caso contrario
     */
    public static boolean isPositiveNumber(Object value) {
        return switch (value) {
            case null -> false;
            case Integer i -> i > 0;
            case Long l -> l > 0L;
            case Double d -> !d.isNaN() && !d.isInfinite() && d > 0.0;
            case Float f -> !f.isNaN() && !f.isInfinite() && f > 0.0f;
            case BigDecimal bd -> bd.compareTo(BigDecimal.ZERO) > 0;
            default -> false;
        };
    }
    
    /**
     * Verifica si una cadena no es nula ni vacía (después de trim).
     * 
     * @param value La cadena a validar
     * @return true si la cadena es válida, false en caso contrario
     */
    public static boolean isValidString(String value) {
        return isValidStringValue(value);
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
    
    /**
     * Validación específica para valores usados en LIKE patterns.
     * Más estricta que isValidValue, detecta patrones problemáticos como "%%".
     * 
     * @param pattern El patrón LIKE a validar
     * @return true si el patrón es válido, false en caso contrario
     */
    public static boolean isValidLikePattern(String pattern) {
        if (!isValidStringValue(pattern)) {
            return false;
        }
        
        String trimmed = pattern.trim();
        
        // Detectar patrón que busca todo (solo wildcards)
        if (trimmed.equals("%%") || 
            trimmed.equals("%") || 
            trimmed.equals("_") ||
            trimmed.equals("__")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Valida que un email tenga formato básico válido.
     * Validación simple, no exhaustiva.
     * 
     * @param email El email a validar
     * @return true si el email tiene formato básico válido
     */
    public static boolean isValidEmail(String email) {
        if (!isValidStringValue(email)) {
            return false;
        }
        
        String trimmed = email.trim();
        
        // Validación básica: debe contener @ y un punto después del @
        int atIndex = trimmed.indexOf('@');
        if (atIndex <= 0) {
            return false;
        }
        
        int dotIndex = trimmed.lastIndexOf('.');
        if (dotIndex <= atIndex + 1 || dotIndex >= trimmed.length() - 1) {
            return false;
        }
        
        // Rechazar emails problemáticos comunes
        if (trimmed.equals("@") || 
            trimmed.startsWith("@") || 
            trimmed.endsWith("@") ||
            trimmed.contains("..")) {
            return false;
        }
        
        return true;
    }
}