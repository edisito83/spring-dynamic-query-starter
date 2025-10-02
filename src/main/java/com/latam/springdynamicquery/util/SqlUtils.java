package com.latam.springdynamicquery.util;

import org.springframework.util.StringUtils;

import com.latam.springdynamicquery.exception.InvalidQueryException;

import java.util.regex.Pattern;

/**
 * Utilidades para procesamiento de SQL.
 */
public final class SqlUtils {

	private static final Pattern COMMENTS_PATTERN = Pattern.compile("--[^\r\n]*|/\\*(?:(?!\\*/).)*\\*/",
			Pattern.MULTILINE | Pattern.DOTALL);
	private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
	private static final String SQL_KEYWORDS_REGEX = "^(SELECT|INSERT|UPDATE|DELETE|WITH|CREATE|DROP|ALTER)\\s+.*";

	// Patrones para detectar valores hardcodeados peligrosos (solo para
	// FilterCriteria)
	private static final Pattern HARDCODED_STRING_PATTERN = Pattern
			.compile("(WHERE|AND|OR)\\s+\\w+\\.?\\w*\\s*=\\s*'[^:][^']*'", Pattern.CASE_INSENSITIVE);

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

	public record WhereInfo(boolean exists, int position, String wherePrefix) {
	}

	private SqlUtils() {
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Limpia y normaliza una consulta SQL.
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
	 * Validación LIGERA para SQL en archivos YAML. Asume que el SQL es código
	 * confiable versionado en Git. Solo valida sintaxis básica, NO valores
	 * hardcodeados.
	 * 
	 * Casos de uso: - Queries definidas en archivos YAML - SQL estático en
	 * configuración - Código revisado en PRs
	 */
	public static void validateYamlQuerySyntax(String queryKey, String sql) {
		if (!StringUtils.hasText(sql)) {
			throw new InvalidQueryException(queryKey, "SQL must not be empty or null");
		}

		String trimmed = sql.trim().toUpperCase();

		// Validar palabra clave inicial
		if (!trimmed.matches(SQL_KEYWORDS_REGEX)) {
			throw new InvalidQueryException(queryKey,
					"SQL must start with SELECT, INSERT, UPDATE, DELETE, WITH, CREATE, DROP, or ALTER");
		}

		// Validar balance de paréntesis
		if (!areParenthesesBalanced(sql)) {
			throw new InvalidQueryException(queryKey, "Unbalanced parentheses in SQL");
		}
	}

	/**
	 * Validación ESTRICTA para FilterCriteria creados en runtime. Asume que el SQL
	 * puede contener input de usuario no confiable. Valida TODO: sintaxis,
	 * hardcoded values, patrones de inyección.
	 * 
	 * Casos de uso: - FilterCriteria.when() en código de aplicación - SQL
	 * construido dinámicamente en runtime - Cualquier fragmento que pueda recibir
	 * input de usuario
	 */
	public static void validateFilterCriteriaSafety(String sqlFragment, Object value) {
		if (!StringUtils.hasText(sqlFragment)) {
			throw new InvalidQueryException("FilterCriteria", "SQL fragment cannot be null or empty");
		}

		// Si hay un valor proporcionado, DEBE haber un parámetro nombrado
		if (value != null) {
			java.util.Set<String> params = extractParameterNames(sqlFragment);

			if (params.isEmpty()) {
				throw new InvalidQueryException("FilterCriteria",
						"SQL fragment must use named parameters (:param) when a value is provided. " + "Fragment: '"
								+ sqlFragment + "'. " + "This prevents SQL injection vulnerabilities.");
			}
		}

		// Detectar patrones de SQL injection en el fragmento
		if (SQL_INJECTION_PATTERNS.matcher(sqlFragment).find()) {
			throw new InvalidQueryException("FilterCriteria",
					"SQL fragment contains potential SQL injection patterns. " + "Fragment: '" + sqlFragment + "'. "
							+ "Ensure you're using parameterized queries with named parameters.");
		}

		// Detectar valores hardcodeados en WHERE/AND/OR (patrón completo)
		if (HARDCODED_STRING_PATTERN.matcher(sqlFragment).find()) {
			throw new InvalidQueryException("FilterCriteria",
					"SQL fragment contains hardcoded string value in WHERE/AND/OR clause. " + "Fragment: '"
							+ sqlFragment + "'. " + "Use named parameters (:param) instead.");
		}

		// Detectar string concatenation peligrosa
		if (sqlFragment.matches(".*=\\s*'[^:][^']*'.*")) {
			throw new InvalidQueryException("FilterCriteria",
					"SQL fragment contains hardcoded string value without parameter binding. " + "Fragment: '"
							+ sqlFragment + "'. " + "Use named parameters (:param) instead.");
		}
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
		SELECT, INSERT, UPDATE, DELETE, DDL, UNKNOWN
	}
}