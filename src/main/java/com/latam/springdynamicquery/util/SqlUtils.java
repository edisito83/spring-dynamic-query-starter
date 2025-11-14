package com.latam.springdynamicquery.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

/**
 * Utilidades para procesamiento de SQL.
 */
public final class SqlUtils {

	private static final Pattern COMMENTS_PATTERN = Pattern.compile("--[^\r\n]*|/\\*(?:(?!\\*/).)*\\*/",
			Pattern.MULTILINE | Pattern.DOTALL);
	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

	public record WhereInfo(boolean exists, int position, String wherePrefix) {
	}

	private SqlUtils() {
		throw new UnsupportedOperationException("Utility class");
	}

	/**
    * Limpia y normaliza una consulta SQL.
    * Elimina comentarios y normaliza espacios en blanco.
    */
	public static String cleanSql(String sql) {
		if (sql == null || sql.trim().isEmpty()) {
			return "";
		}

		// Eliminar comentarios de línea y de bloque
		String cleaned = COMMENTS_PATTERN.matcher(sql).replaceAll("");

		// Normalizar espacios y saltos de línea
		cleaned = WHITESPACE_PATTERN.matcher(cleaned).replaceAll(" ");

		return cleaned.trim();
	}

    /**
     * Analiza una consulta SQL para determinar si tiene cláusula WHERE
     * y dónde agregar nuevas condiciones.
     */
	public static WhereInfo analyzeWhereClause(String sql) {
		if (sql == null || sql.trim().isEmpty()) {
			return new WhereInfo(false, -1, " WHERE ");
		}

		String upperSql = sql.toUpperCase();
		String[] tokens = upperSql.split("\\s+");

		int parenLevel = 0;
		int wherePos = -1;

		for (int i = 0; i < tokens.length; i++) {
			String token = tokens[i];

			for (char c : token.toCharArray()) {
				if (c == '(')
					parenLevel++;
				else if (c == ')')
					parenLevel--;
			}

			if (parenLevel == 0 && "WHERE".equals(token)) {
				wherePos = i;
				break;
			}
		}

		boolean exists = wherePos != -1;
		String wherePrefix = exists ? " AND " : " WHERE ";

		return new WhereInfo(exists, wherePos, wherePrefix);
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
		} else if (trimmed.startsWith("CALL") || 
                trimmed.startsWith("EXEC")) {
         return SqlType.PROCEDURE;
        }

		return SqlType.UNKNOWN;
	}

	/**
     * Tipos de consultas SQL.
     */
    public enum SqlType {
        SELECT,      // Consultas de lectura
        INSERT,      // Inserciones
        UPDATE,      // Actualizaciones
        DELETE,      // Eliminaciones
        DDL,         // Data Definition Language (CREATE, DROP, ALTER)
        PROCEDURE,   // Llamadas a procedimientos almacenados
        UNKNOWN      // Tipo no identificado
    }
    
    /**
     * Verifica si un tipo SQL requiere resultType.
     */
    public static boolean requiresResultType(SqlType type) {
        return type == SqlType.SELECT || type == SqlType.PROCEDURE;
    }
    
    /**
     * Extrae el nombre de tabla principal de una query simple.
     * Útil para logging y debugging.
     */
    public static String extractTableName(String sql) {
        if (!StringUtils.hasText(sql)) {
            return "unknown";
        }
        
        String cleaned = cleanSql(sql).toUpperCase();
        
        // Para SELECT: extraer de FROM
        if (cleaned.startsWith("SELECT")) {
            Pattern pattern = Pattern.compile("FROM\\s+([\\w.]+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(cleaned);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        
        // Para INSERT: extraer de INTO
        if (cleaned.startsWith("INSERT")) {
            Pattern pattern = Pattern.compile("INTO\\s+([\\w.]+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(cleaned);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        
        // Para UPDATE/DELETE: extraer directamente después del comando
        if (cleaned.startsWith("UPDATE") || cleaned.startsWith("DELETE")) {
            Pattern pattern = Pattern.compile("(?:UPDATE|DELETE FROM)\\s+([\\w.]+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(cleaned);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        
        return "unknown";
    }
}