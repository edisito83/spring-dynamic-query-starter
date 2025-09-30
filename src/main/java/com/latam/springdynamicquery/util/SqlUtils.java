package com.latam.springdynamicquery.util;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Utilidades para procesamiento de SQL.
 */
public final class SqlUtils {
    
    private static final Pattern SQL_COMMENT_PATTERN = Pattern.compile("--.*$", Pattern.MULTILINE);
    private static final Pattern MULTI_LINE_COMMENT_PATTERN = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    
    private SqlUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Limpia y normaliza una consulta SQL.
     */
    public static String cleanSql(String sql) {
        if (!StringUtils.hasText(sql)) {
            return "";
        }
        
        // Eliminar comentarios de línea única
        String cleaned = SQL_COMMENT_PATTERN.matcher(sql).replaceAll("");
        
        // Eliminar comentarios multilínea
        cleaned = MULTI_LINE_COMMENT_PATTERN.matcher(cleaned).replaceAll("");
        
        // Normalizar espacios en blanco
        cleaned = WHITESPACE_PATTERN.matcher(cleaned).replaceAll(" ");
        
        return cleaned.trim();
    }
    
    /**
     * Verifica si una consulta SQL contiene una cláusula WHERE.
     */
    public static boolean hasWhereClause(String sql) {
        if (!StringUtils.hasText(sql)) {
            return false;
        }
        
        String upperSql = sql.toUpperCase();
        return findWhereClausePosition(upperSql) >= 0;
    }
    
    /**
     * Encuentra la posición de la cláusula WHERE principal (no en subconsultas).
     */
    public static int findWhereClausePosition(String sql) {
        if (!StringUtils.hasText(sql)) {
            return -1;
        }
        
        String upperSql = sql.toUpperCase();
        String[] tokens = upperSql.split("\\s+");
        
        int parenLevel = 0;
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            
            // Contar paréntesis para detectar subconsultas
            for (char c : token.toCharArray()) {
                if (c == '(') parenLevel++;
                else if (c == ')') parenLevel--;
            }
            
            // Solo considerar WHERE en el nivel principal
            if (parenLevel == 0 && "WHERE".equals(token)) {
                return i;
            }
        }
        
        return -1;
    }
    
    /**
     * Valida la sintaxis básica de SQL.
     */
    public static boolean isValidSqlSyntax(String sql) {
        if (!StringUtils.hasText(sql)) {
            return false;
        }
        
        String trimmed = sql.trim().toUpperCase();
        
        // Debe comenzar con una palabra clave SQL válida
        if (!trimmed.matches("^(SELECT|INSERT|UPDATE|DELETE|WITH|CREATE|DROP|ALTER)\\s+.*")) {
            return false;
        }
        
        // Verificar balance de paréntesis
        return areParenthesesBalanced(sql);
    }
    
    /**
     * Verifica si los paréntesis están balanceados en el SQL.
     */
    public static boolean areParenthesesBalanced(String sql) {
        int balance = 0;
        
        for (char c : sql.toCharArray()) {
            if (c == '(') {
                balance++;
            } else if (c == ')') {
                balance--;
                if (balance < 0) {
                    return false; // Más paréntesis de cierre que de apertura
                }
            }
        }
        
        return balance == 0;
    }
    
    /**
     * Extrae los nombres de parámetros de una consulta SQL (formato :paramName).
     */
    public static java.util.Set<String> extractParameterNames(String sql) {
        java.util.Set<String> parameters = new java.util.HashSet<>();
        
        if (!StringUtils.hasText(sql)) {
            return parameters;
        }
        
        Pattern paramPattern = Pattern.compile(":([a-zA-Z_][a-zA-Z0-9_]*)");
        java.util.regex.Matcher matcher = paramPattern.matcher(sql);
        
        while (matcher.find()) {
            parameters.add(matcher.group(1));
        }
        
        return parameters;
    }
    
    /**
     * Cuenta el número de parámetros únicos en una consulta SQL.
     */
    public static int countParameters(String sql) {
        return extractParameterNames(sql).size();
    }
    
    /**
     * Verifica si una consulta es de solo lectura (SELECT).
     */
    public static boolean isReadOnlyQuery(String sql) {
        if (!StringUtils.hasText(sql)) {
            return false;
        }
        
        String trimmed = sql.trim().toUpperCase();
        return trimmed.startsWith("SELECT") || trimmed.startsWith("WITH");
    }
    
    /**
     * Determina el tipo de consulta SQL.
     */
    public static SqlType getSqlType(String sql) {
        if (!StringUtils.hasText(sql)) {
            return SqlType.UNKNOWN;
        }
        
        String trimmed = sql.trim().toUpperCase();
        
        if (trimmed.startsWith("SELECT") || trimmed.startsWith("WITH")) {
            return SqlType.SELECT;
        } else if (trimmed.startsWith("INSERT")) {
            return SqlType.INSERT;
        } else if (trimmed.startsWith("UPDATE")) {
            return SqlType.UPDATE;
        } else if (trimmed.startsWith("DELETE")) {
            return SqlType.DELETE;
        } else if (trimmed.startsWith("CREATE")) {
            return SqlType.DDL;
        } else if (trimmed.startsWith("DROP")) {
            return SqlType.DDL;
        } else if (trimmed.startsWith("ALTER")) {
            return SqlType.DDL;
        }
        
        return SqlType.UNKNOWN;
    }
    
    /**
     * Tipos de consultas SQL.
     */
    public enum SqlType {
        SELECT,
        INSERT,
        UPDATE,
        DELETE,
        DDL,
        UNKNOWN
    }
}