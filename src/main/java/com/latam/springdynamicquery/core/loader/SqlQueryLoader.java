package com.latam.springdynamicquery.core.loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.latam.springdynamicquery.autoconfigure.DynamicQueryProperties;
import com.latam.springdynamicquery.core.model.SqlMapperYaml;
import com.latam.springdynamicquery.exception.InvalidQueryException;
import com.latam.springdynamicquery.exception.QueryNotFoundException;

import lombok.extern.slf4j.Slf4j;

/**
 * Cargador de consultas SQL desde archivos YAML. Maneja la carga, caché y
 * acceso a consultas SQL definidas en archivos YAML.
 */
@Slf4j
public class SqlQueryLoader implements InitializingBean {

	private final DynamicQueryProperties properties;
	private final Map<String, String> queryCache = new ConcurrentHashMap<>();
	private final Map<String, SqlMapperYaml.QueryDefinition> queryMetadata = new ConcurrentHashMap<>();
	private final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
	private final ObjectMapper yamlMapper;

	public SqlQueryLoader(DynamicQueryProperties properties) {
		this.properties = properties;
		this.yamlMapper = createYamlMapper();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (properties.isPreloadEnabled()) {
			loadAllQueriesFromYaml();
			log.info("Loaded {} SQL queries from {} YAML files into cache", queryCache.size(),
					getLoadedNamespaceCount());

			if (log.isDebugEnabled()) {
				logLoadedQueries();
			}

			if (properties.getValidation().isValidateAtStartup()) {
				validateAllQueries();
			}
		}
	}

	/**
	 * Carga todas las consultas desde archivos YAML al arrancar la aplicación.
	 */
	private void loadAllQueriesFromYaml() throws IOException {
		int loadedFiles = 0;
		int totalQueries = 0;

		for (String scanPackage : properties.getScanPackages()) {
			String locationPattern = "classpath:" + scanPackage;
			Resource[] resources = resourceResolver.getResources(locationPattern);

			log.debug("Scanning pattern: {} found {} resources", locationPattern, resources.length);

			for (Resource resource : resources) {
				if (resource.isReadable() && isYamlFile(resource)) {
					int queriesInFile = loadQueriesFromYamlResource(resource);
					totalQueries += queriesInFile;
					loadedFiles++;
					log.debug("Loaded {} queries from {}", queriesInFile, resource.getFilename());
				}
			}
		}

		log.info("Successfully processed {} YAML files with {} total queries", loadedFiles, totalQueries);

		if (totalQueries == 0) {
			log.warn("No SQL queries were loaded. Check your scan-packages configuration: {}",
					properties.getScanPackages());
		}
	}

	/**
	 * Carga consultas desde un archivo YAML específico.
	 */
	private int loadQueriesFromYamlResource(Resource resource) throws IOException {
		try (InputStream inputStream = resource.getInputStream()) {
			SqlMapperYaml sqlMapper = yamlMapper.readValue(inputStream, SqlMapperYaml.class);

			// Validar estructura básica
			if (sqlMapper.getQueries() == null || sqlMapper.getQueries().isEmpty()) {
				log.warn("No queries found in YAML file: {}", resource.getFilename());
				return 0;
			}

			// Si no hay namespace, usar el nombre del archivo sin extensión
			if (!sqlMapper.hasNamespace()) {
				sqlMapper.setNamespace(extractNamespaceFromResource(resource));
			}

			int queriesLoaded = 0;
			for (SqlMapperYaml.QueryDefinition query : sqlMapper.getQueries()) {
				if (isValidQueryDefinition(query, resource.getFilename())) {
					String queryKey = sqlMapper.getQueryKey(query.getId());
					String cleanSql = cleanSql(query.getSql());
					
					log.debug("Query key {}", queryKey);

					// Verificar duplicados
					if (queryCache.containsKey(queryKey)) {
						log.warn("Duplicate query key found: {} in file: {}. Previous definition will be overwritten.",
								queryKey, resource.getFilename());
					}

					queryCache.put(queryKey, cleanSql);
					queryMetadata.put(queryKey, query);
					queriesLoaded++;

					log.trace("Loaded query: {} -> {}", queryKey,
							cleanSql.substring(0, Math.min(50, cleanSql.length())) + "...");
				}
			}

			log.debug("Successfully loaded {} queries from namespace: {} (file: {})", queriesLoaded,
					sqlMapper.getNamespace(), resource.getFilename());

			return queriesLoaded;

		} catch (Exception e) {
			log.error("Error loading queries from YAML file: {}", resource.getFilename(), e);
			throw new IOException("Failed to load queries from: " + resource.getFilename(), e);
		}
	}

	/**
	 * Obtiene una consulta del caché usando namespace.id o solo id.
	 */
	public String getQuery(String queryKey) {
		log.debug("Searching query key {}", queryKey);
		
		if (!properties.isPreloadEnabled()) {
			return loadQueryDynamically(queryKey);
		}
		
		String query = queryCache.get(queryKey);
		log.debug("Query from cache is {}", query);
		
		if (query == null) {
			// Intentar búsqueda flexible sin namespace completo
			query = findQueryWithoutFullNamespace(queryKey);
			log.debug("Query from cache without full namespace is {}", query);
		}

		if (query == null) {
			throw new QueryNotFoundException(queryKey, String.join(", ", getAvailableQueryKeys()));
		}

		log.trace("Retrieved query: {} -> {}", queryKey, query.substring(0, Math.min(50, query.length())) + "...");
		return query;
	}

	/**
	 * Obtiene metadata de una consulta.
	 */
	public SqlMapperYaml.QueryDefinition getQueryMetadata(String queryKey) {
		SqlMapperYaml.QueryDefinition metadata = queryMetadata.get(queryKey);
		if (metadata == null) {
			metadata = findQueryMetadataWithoutFullNamespace(queryKey);
		}
		return metadata;
	}

	/**
	 * Verifica si una consulta existe en el caché.
	 */
	public boolean hasQuery(String queryKey) {
		if (!properties.isPreloadEnabled()) {
			try {
				loadQueryDynamically(queryKey);
				return true;
			} catch (Exception e) {
				return false;
			}
		}

		return queryCache.containsKey(queryKey) || findQueryWithoutFullNamespace(queryKey) != null;
	}

	/**
	 * Obtiene todas las consultas disponibles agrupadas por namespace.
	 */
	public Map<String, List<String>> getAvailableQueriesByNamespace() {
		return queryCache.keySet().stream().collect(Collectors.groupingBy(this::extractNamespaceFromKey,
				Collectors.mapping(this::extractQueryIdFromKey, Collectors.toList())));
	}

	/**
	 * Obtiene todas las consultas de un namespace específico.
	 */
	public Set<String> getQueriesForNamespace(String namespace) {
		return queryCache.keySet().stream().filter(key -> extractNamespaceFromKey(key).equals(namespace))
				.map(this::extractQueryIdFromKey).collect(Collectors.toSet());
	}

	/**
	 * Obtiene todas las claves de consultas disponibles.
	 */
	public Set<String> getAvailableQueryKeys() {
		return Collections.unmodifiableSet(queryCache.keySet());
	}

	/**
	 * Obtiene estadísticas del loader.
	 */
	public LoaderStats getStats() {
		return new LoaderStats(queryCache.size(), getLoadedNamespaceCount(), queryMetadata.values().stream()
				.mapToInt(q -> q.getParameters() != null ? q.getParameters().size() : 0).sum());
	}

	// ==================== Métodos auxiliares privados ====================

	private ObjectMapper createYamlMapper() {
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
		return mapper;
	}

	private boolean isYamlFile(Resource resource) {
		String filename = resource.getFilename();
		return filename != null && (filename.endsWith(".yml") || filename.endsWith(".yaml"));
	}

	private String extractNamespaceFromResource(Resource resource) {
		String filename = resource.getFilename();
		if (filename != null && (filename.endsWith(".yml") || filename.endsWith(".yaml"))) {
			return filename.substring(0, filename.lastIndexOf("."));
		}
		return filename != null ? filename : "unknown";
	}

	private boolean isValidQueryDefinition(SqlMapperYaml.QueryDefinition query, String filename) {
		if (query.getId() == null || query.getId().trim().isEmpty()) {
			log.warn("Skipping query with missing ID in file: {}", filename);
			return false;
		}

		if (query.getSql() == null || query.getSql().trim().isEmpty()) {
			log.warn("Skipping query '{}' with missing SQL in file: {}", query.getId(), filename);
			return false;
		}

		return true;
	}

	private String cleanSql(String sql) {
		if (sql == null)
			return "";

		// Limpiar el SQL: mantener estructura pero normalizar espacios
		return sql.trim().replaceAll("\\s+", " ") // Múltiples espacios por uno solo
				.replaceAll("\\s*\\n\\s*", " ") // Saltos de línea por espacios
				.trim();
	}

	private String findQueryWithoutFullNamespace(String queryKey) {
		// Si ya contiene punto, buscar exact match
		if (queryKey.contains(".")) {
			return queryCache.get(queryKey);
		}

		// Si no contiene punto, buscar consultas que terminen con .queryKey
		List<String> matches = queryCache.entrySet().stream().filter(entry -> entry.getKey().endsWith("." + queryKey))
				.map(Map.Entry::getValue).toList();

		if (matches.isEmpty()) {
			return null;
		}

		if (matches.size() > 1) {
			log.warn("Multiple queries found for key '{}'. Using first match. Consider using full namespace.id",
					queryKey);
		}

		return matches.get(0);
	}

	private SqlMapperYaml.QueryDefinition findQueryMetadataWithoutFullNamespace(String queryKey) {
		if (queryKey.contains(".")) {
			return queryMetadata.get(queryKey);
		}

		List<SqlMapperYaml.QueryDefinition> matches = queryMetadata.entrySet().stream()
				.filter(entry -> entry.getKey().endsWith("." + queryKey)).map(Map.Entry::getValue).toList();

		return matches.isEmpty() ? null : matches.get(0);
	}

	private String extractNamespaceFromKey(String key) {
		int lastDot = key.lastIndexOf(".");
		return lastDot > 0 ? key.substring(0, lastDot) : "default";
	}

	private String extractQueryIdFromKey(String key) {
		int lastDot = key.lastIndexOf(".");
		return lastDot > 0 ? key.substring(lastDot + 1) : key;
	}

	private long getLoadedNamespaceCount() {
		return queryCache.keySet().stream().map(this::extractNamespaceFromKey).distinct().count();
	}

	private void logLoadedQueries() {
		queryCache.forEach((key, sql) -> log.debug("Loaded query: {} -> {}", key,
				sql.substring(0, Math.min(100, sql.length())) + "..."));
	}

	private void validateAllQueries() {
		log.info("Validating {} loaded queries...", queryCache.size());

		int validQueries = 0;
		int invalidQueries = 0;

		for (Map.Entry<String, SqlMapperYaml.QueryDefinition> entry : queryMetadata.entrySet()) {
			String queryKey = entry.getKey();
			SqlMapperYaml.QueryDefinition query = entry.getValue();

			try {
				validateQuery(queryKey, query);
				validQueries++;
			} catch (InvalidQueryException e) {
				log.error("Validation failed for query '{}': {}", queryKey, e.getReason());
				invalidQueries++;

				if (properties.getValidation().isStrictMode()) {
					throw e;
				}
			}
		}

		log.info("Query validation completed. Valid: {}, Invalid: {}", validQueries, invalidQueries);

		if (invalidQueries > 0 && properties.getValidation().isStrictMode()) {
			throw new InvalidQueryException("validation",
					"Query validation failed in strict mode. " + invalidQueries + " invalid queries found.");
		}
	}

	private void validateQuery(String queryKey, SqlMapperYaml.QueryDefinition query) {
		// Validar SQL no vacío
		if (query.getSql() == null || query.getSql().trim().isEmpty()) {
			throw new InvalidQueryException(queryKey, "SQL is empty or null");
		}

		// Validar sintaxis SQL básica si está habilitado
		if (properties.getValidation().isValidateSqlSyntax()) {
			validateBasicSqlSyntax(queryKey, query.getSql());
		}

		// Validar parámetros requeridos
		if (properties.getValidation().isValidateRequiredParameters() && query.hasParameters()) {
			validateRequiredParameters(queryKey, query);
		}
	}

	private void validateBasicSqlSyntax(String queryKey, String sql) {
		String upperSql = sql.toUpperCase().trim();

		// Validaciones básicas
		if (!upperSql.startsWith("SELECT") && !upperSql.startsWith("WITH") && !upperSql.startsWith("INSERT")
				&& !upperSql.startsWith("UPDATE") && !upperSql.startsWith("DELETE")) {
			throw new InvalidQueryException(queryKey, "SQL must start with SELECT, INSERT, UPDATE, DELETE, or WITH");
		}

		// Verificar balance de paréntesis
		long openParens = sql.chars().filter(ch -> ch == '(').count();
		long closeParens = sql.chars().filter(ch -> ch == ')').count();

		if (openParens != closeParens) {
			throw new InvalidQueryException(queryKey, "Unbalanced parentheses in SQL");
		}
	}

	private void validateRequiredParameters(String queryKey, SqlMapperYaml.QueryDefinition query) {
		for (SqlMapperYaml.QueryDefinition.ParameterMapping param : query.getParameters()) {
			if (param.isRequired() && !query.getSql().contains(":" + param.getName())) {
				throw new InvalidQueryException(queryKey,
						"Required parameter '" + param.getName() + "' not found in SQL");
			}
		}
	}

	private String loadQueryDynamically(String queryKey) {
		String[] parts = queryKey.split("\\.");
		if (parts.length < 2) {
			throw new IllegalArgumentException("Dynamic query loading requires namespace.queryId format");
		}

		String namespace = String.join(".", Arrays.copyOf(parts, parts.length - 1));
		String filename = extractFilenameFromNamespace(namespace);

		try {
			String resourcePath = "classpath:" + properties.getBasePath() + "/" + filename + "."
					+ properties.getYamlExtension();
			Resource resource = resourceResolver.getResource(resourcePath);

			if (!resource.exists()) {
				throw new QueryNotFoundException(queryKey, "Resource not found: " + resourcePath);
			}

			try (InputStream inputStream = resource.getInputStream()) {
				SqlMapperYaml sqlMapper = yamlMapper.readValue(inputStream, SqlMapperYaml.class);
				return sqlMapper.getQueries().stream().filter(q -> queryKey.endsWith("." + q.getId())).findFirst()
						.map(q -> cleanSql(q.getSql())).orElseThrow(() -> new QueryNotFoundException(queryKey));
			}
		} catch (IOException e) {
			throw new QueryNotFoundException(queryKey, "Failed to load query dynamically: " + e.getMessage());
		}
	}

	private String extractFilenameFromNamespace(String namespace) {
		// Extraer solo la última parte del namespace para usar como nombre de archivo
		int lastDot = namespace.lastIndexOf(".");
		return lastDot > 0 ? namespace.substring(lastDot + 1) : namespace;
	}

	/**
	 * Estadísticas del loader.
	 */
	public static class LoaderStats {
		private final int totalQueries;
		private final long totalNamespaces;
		private final int totalParameters;

		public LoaderStats(int totalQueries, long totalNamespaces, int totalParameters) {
			this.totalQueries = totalQueries;
			this.totalNamespaces = totalNamespaces;
			this.totalParameters = totalParameters;
		}

		public int getTotalQueries() {
			return totalQueries;
		}

		public long getTotalNamespaces() {
			return totalNamespaces;
		}

		public int getTotalParameters() {
			return totalParameters;
		}

		@Override
		public String toString() {
			return String.format("LoaderStats{queries=%d, namespaces=%d, parameters=%d}", totalQueries, totalNamespaces,
					totalParameters);
		}
	}
}