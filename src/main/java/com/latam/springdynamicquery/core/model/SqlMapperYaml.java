package com.latam.springdynamicquery.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa la estructura de un archivo YAML de mapper SQL. Equivalente a la
 * estructura XML de MyBatis pero en formato YAML.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SqlMapperYaml {

	private String namespace;
	private String description;
	private List<QueryDefinition> queries = new ArrayList<>();

	/**
	 * Representa una definición de consulta individual dentro del mapper.
	 */
	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class QueryDefinition {
		private String id;
		private String description;
		private String resultType;
		private String resultClass;
		private String sql;
		private boolean cacheable = true;
		private List<ParameterMapping> parameters = new ArrayList<>();

		/**
		 * Representa el mapeo de un parámetro en la consulta.
		 */
		@Data
		@JsonIgnoreProperties(ignoreUnknown = true)
		public static class ParameterMapping {
			private String name;
			private String type;
			private String jdbcType;
			private boolean required = false;
			private String description;
		}

		/**
		 * Verifica si esta consulta tiene parámetros definidos.
		 */
		public boolean hasParameters() {
			return parameters != null && !parameters.isEmpty();
		}

		/**
		 * Obtiene un parámetro por nombre.
		 */
		public ParameterMapping getParameter(String name) {
			if (parameters == null)
				return null;
			return parameters.stream().filter(param -> name.equals(param.getName())).findFirst().orElse(null);
		}

		/**
		 * Verifica si un parámetro es requerido.
		 */
		public boolean isParameterRequired(String name) {
			ParameterMapping param = getParameter(name);
			return param != null && param.isRequired();
		}
	}

	/**
	 * Genera la clave única para una consulta: namespace.id
	 */
	public String getQueryKey(String queryId) {
		if (namespace == null || namespace.trim().isEmpty()) {
			return queryId;
		}
		return namespace + "." + queryId;
	}

	/**
	 * Obtiene una consulta por su ID.
	 */
	public QueryDefinition getQuery(String queryId) {
		if (queries == null)
			return null;
		return queries.stream().filter(query -> queryId.equals(query.getId())).findFirst().orElse(null);
	}

	/**
	 * Verifica si este mapper tiene consultas definidas.
	 */
	public boolean hasQueries() {
		return queries != null && !queries.isEmpty();
	}

	/**
	 * Obtiene el número total de consultas en este mapper.
	 */
	public int getQueryCount() {
		return queries != null ? queries.size() : 0;
	}

	/**
	 * Verifica si el namespace está definido.
	 */
	public boolean hasNamespace() {
		return namespace != null && !namespace.trim().isEmpty();
	}
}
