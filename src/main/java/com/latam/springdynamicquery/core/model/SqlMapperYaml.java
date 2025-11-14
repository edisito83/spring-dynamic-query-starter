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
		
		/**
         * Indica si la query acepta filtros dinámicos (FilterCriteria).
         * - true: Query puede usar construcción dinámica con FilterCriteria
         * - false: Query estática, solo acepta parámetros fijos
         */
        private boolean dynamic = true;
        
        /**
         * Clase de resultado para la query.
         * Ejemplos:
         * - com.company.entity.User (Entity JPA)
         * - com.company.dto.UserDTO (DTO con constructor)
         * - java.lang.Long (tipo primitivo/wrapper)
         * - java.util.Map (Map genérico)
         * - jakarta.persistence.Tuple (Tuple JPA)
         * - java.lang.Object[] (array de objetos)
         * 
         * Requerido para SELECT. Opcional para INSERT/UPDATE/DELETE.
         */
        private String resultType;
        
        /**
         * Alias legacy de resultType (compatibilidad con iBatis).
         * Si ambos están presentes, se usa resultType.
         */
        private String resultClass;
        
        /**
         * Nombre del @SqlResultSetMapping para mapeos complejos.
         * Solo necesario cuando el mapeo automático no es suficiente.
         */
        private String resultMapping;
        
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
		
		/**
         * Obtiene el resultType efectivo.
         * resultClass es un alias legacy de resultType (compatibilidad iBatis).
         * 
         * @return resultType si está definido, sino resultClass como fallback
         */
        public String getEffectiveResultType() {
            return resultType != null ? resultType : resultClass;
        }
        
        /**
         * Verifica si tiene resultType o resultClass definido.
         */
        public boolean hasResultType() {
            return resultType != null || resultClass != null;
        }
        
        /**
         * Verifica si usa mapeo complejo (resultMapping).
         * El resultMapping es independiente del resultType.
         * 
         * @return true si tiene resultMapping definido
         */
        public boolean hasResultMapping() {
            return resultMapping != null && !resultMapping.trim().isEmpty();
        }
        
        /**
         * Verifica si tiene al menos resultType/resultClass O resultMapping definido.
         * En MyBatis: tienes resultType O resultMap, no necesariamente ambos.
         * 
         * @return true si tiene al menos uno definido
         */
        public boolean hasResultTypeOrMapping() {
            return hasResultType() || hasResultMapping();
        }
        
        /**
         * Verifica si esta query requiere resultType.
         * Solo SELECT y procedimientos almacenados lo requieren.
         * INSERT, UPDATE, DELETE no necesitan resultType.
         */
        public boolean requiresResultType() {
            if (sql == null || sql.trim().isEmpty()) {
                return false;
            }
            
            String trimmed = sql.trim().toUpperCase();
            return trimmed.startsWith("SELECT") || 
                   trimmed.startsWith("WITH") ||
                   trimmed.startsWith("CALL") ||
                   trimmed.startsWith("EXEC");
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
