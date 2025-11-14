package com.latam.springdynamicquery.core.validation;

import com.latam.springdynamicquery.core.model.SqlMapperYaml;
import com.latam.springdynamicquery.exception.InvalidQueryException;
import com.latam.springdynamicquery.util.SqlUtils;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validador de consultas SQL para el sistema de queries dinámicas.
 * Proporciona validaciones de sintaxis, seguridad y estructura de queries.
 */
public final class QueryValidator {
    
    private static final String SQL_KEYWORDS_REGEX = 
        "^(SELECT|INSERT|UPDATE|DELETE|WITH|CREATE|DROP|ALTER|CALL|EXEC)\\s+.*";
    
    // Patrones de SQL injection comunes
    private static final Pattern SQL_INJECTION_PATTERNS = Pattern.compile(
        // ' OR '1'='1 variations
        "'\\s*(?:OR|AND)\\s*'?\\d*'?\\s*=\\s*'?\\d*'?" +
        "|" +
        // ; DROP TABLE / DELETE / etc
        ";\\s*(?:DROP|DELETE|UPDATE|INSERT|ALTER)\\s+" +
        "|" +
        // SQL comment: --
        "--" +
        "|" +
        // Multi-line comment: /* ... */
        "/\\*.*?\\*/" +
        "|" +
        // UNION attacks
        "UNION\\s+SELECT" +
        "|" +
        // EXEC sp_name or EXECUTE sp_name
        "EXEC(?:UTE)?\\s+\\w+" +
        "|" +
        // EXEC(...) or EXECUTE(...)
        "EXEC(?:UTE)?\\s*\\(",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Patrón para detectar valores hardcodeados en WHERE/AND/OR
    private static final Pattern HARDCODED_STRING_PATTERN = Pattern
        .compile("(WHERE|AND|OR)\\s+\\w+\\.?\\w*\\s*=\\s*'[^:][^']*'", Pattern.CASE_INSENSITIVE);
    
    private QueryValidator() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Validación LIGERA para SQL en archivos YAML.
     * Asume que el SQL es código confiable versionado en Git.
     * Solo valida sintaxis básica, NO valores hardcodeados.
     * 
     * Casos de uso:
     * - Queries definidas en archivos YAML
     * - SQL estático en configuración
     * - Código revisado en PRs
     * 
     * @param queryKey Identificador de la query
     * @param sql SQL a validar
     * @throws InvalidQueryException si la sintaxis es inválida
     */
    public static void validateYamlQuerySyntax(String queryKey, String sql) {
        if (!StringUtils.hasText(sql)) {
            throw new InvalidQueryException(queryKey, "SQL must not be empty or null");
        }
        
        String trimmed = sql.trim().toUpperCase();
        
        // Validar palabra clave inicial
        if (!trimmed.matches(SQL_KEYWORDS_REGEX)) {
            throw new InvalidQueryException(queryKey,
                "SQL must start with SELECT, INSERT, UPDATE, DELETE, WITH, CREATE, DROP, ALTER, CALL, or EXEC");
        }
        
        // Validar balance de paréntesis
        if (!SqlUtils.areParenthesesBalanced(sql)) {
            throw new InvalidQueryException(queryKey, "Unbalanced parentheses in SQL");
        }
    }
    
    /**
     * Validación ESTRICTA para FilterCriteria creados en runtime.
     * Asume que el SQL puede contener input de usuario no confiable.
     * Valida TODO: sintaxis, hardcoded values, patrones de inyección.
     * 
     * Casos de uso:
     * - FilterCriteria.when() en código de aplicación
     * - SQL construido dinámicamente en runtime
     * - Cualquier fragmento que pueda recibir input de usuario
     * 
     * @param sqlFragment Fragmento SQL a validar
     * @param value Valor asociado al fragmento
     * @throws InvalidQueryException si se detectan problemas de seguridad
     */
    public static void validateFilterCriteriaSafety(String sqlFragment, Object value) {
        if (!StringUtils.hasText(sqlFragment)) {
            throw new InvalidQueryException("FilterCriteria", 
                "SQL fragment cannot be null or empty");
        }
        
        // Si hay un valor proporcionado, DEBE haber un parámetro nombrado
        if (value != null) {
            Set<String> params = SqlUtils.extractParameterNames(sqlFragment);
            
            if (params.isEmpty()) {
                throw new InvalidQueryException("FilterCriteria",
                    "SQL fragment must use named parameters (:param) when a value is provided. " +
                    "Fragment: '" + sqlFragment + "'. " +
                    "This prevents SQL injection vulnerabilities.");
            }
        }
        
        // Detectar patrones de SQL injection en el fragmento
        if (SQL_INJECTION_PATTERNS.matcher(sqlFragment).find()) {
            throw new InvalidQueryException("FilterCriteria",
                "SQL fragment contains potential SQL injection patterns. " +
                "Fragment: '" + sqlFragment + "'. " +
                "Ensure you're using parameterized queries with named parameters.");
        }
        
        // Detectar valores hardcodeados en WHERE/AND/OR (patrón completo)
        if (HARDCODED_STRING_PATTERN.matcher(sqlFragment).find()) {
            throw new InvalidQueryException("FilterCriteria",
                "SQL fragment contains hardcoded string value in WHERE/AND/OR clause. " +
                "Fragment: '" + sqlFragment + "'. " +
                "Use named parameters (:param) instead.");
        }
        
        // Detectar string concatenation peligrosa
        if (sqlFragment.matches(".*=\\s*'[^:][^']*'.*")) {
            throw new InvalidQueryException("FilterCriteria",
                "SQL fragment contains hardcoded string value without parameter binding. " +
                "Fragment: '" + sqlFragment + "'. " +
                "Use named parameters (:param) instead.");
        }
    }
    
    /**
     * Valida que una query estática no esté usando filtros dinámicos.
     * 
     * @param queryKey Identificador de la query
     * @param isDynamic Si la query es dinámica
     * @param hasFilters Si se están intentando usar filtros
     * @throws IllegalArgumentException si query estática recibe filtros
     */
    public static void validateStaticQueryUsage(String queryKey, boolean isDynamic, boolean hasFilters) {
        if (!isDynamic && hasFilters) {
            throw new IllegalArgumentException(
                "Query '" + queryKey + "' is marked as static (dynamic: false) " +
                "and does not accept dynamic filters (FilterCriteria). " +
                "Use executeNamedQueryWithParams() for static queries with parameters.");
        }
    }
    
//    /**
//     * Valida que una query que requiere resultType lo tenga especificado.
//     * 
//     * @param queryKey Identificador de la query
//     * @param sqlType Tipo de query SQL
//     * @param resultType Tipo de resultado especificado
//     * @throws InvalidQueryException si falta resultType en SELECT
//     */
//    public static void validateResultType(String queryKey, SqlUtils.SqlType sqlType, String resultType) {
//        // Solo SELECT y queries que retornan datos necesitan resultType
//        if ((sqlType == SqlUtils.SqlType.SELECT || sqlType == SqlUtils.SqlType.UNKNOWN) 
//            && !StringUtils.hasText(resultType)) {
//            throw new InvalidQueryException(queryKey,
//                "SELECT queries must specify 'resultType' or 'resultClass'. " +
//                "Example: resultType: com.company.dto.UserDTO");
//        }
//        
//        // INSERT, UPDATE, DELETE no deberían tener resultType
//        if ((sqlType == SqlUtils.SqlType.INSERT || 
//             sqlType == SqlUtils.SqlType.UPDATE || 
//             sqlType == SqlUtils.SqlType.DELETE) 
//            && StringUtils.hasText(resultType)) {
//            // Solo warning, no error
//            // Algunas DBs permiten RETURNING clause
//        }
//    }
    
    /**
     * Valida que una query que requiere resultType lo tenga especificado.
     * 
     * @param queryKey Identificador de la query
     * @param queryDef Definición de la query
     * @throws InvalidQueryException si falta resultType en SELECT
     */
    public static void validateResultType(String queryKey, SqlMapperYaml.QueryDefinition queryDef) {
        if (queryDef == null) {
        	throw new InvalidQueryException(queryKey,
                    "Query definition not found in YAML.");
        }
        
        // --- Validaciones principales ---
        // Verificar si la query requiere resultType basado en su SQL
        if (queryDef.requiresResultType()) {
            // Solo SELECT/CALL/EXEC pueden definir resultType o resultMapping
            if (!queryDef.hasResultTypeOrMapping()) {
                throw new InvalidQueryException(queryKey,
                        "Query must define either 'resultType' (or 'resultClass') OR 'resultMapping'.");
            }

            if (queryDef.hasResultType() && queryDef.hasResultMapping()) {
                throw new InvalidQueryException(queryKey,
                        "Query cannot define both 'resultType'/'resultClass' and 'resultMapping'.");
            }
        } else {
            // INSERT/UPDATE/DELETE no deben definir tipo de resultado
            if (queryDef.hasResultTypeOrMapping()) {
                throw new InvalidQueryException(queryKey,
                        "Non-select queries (INSERT/UPDATE/DELETE) must not define 'resultType' or 'resultMapping'.");
            }
        }
    }
    
}