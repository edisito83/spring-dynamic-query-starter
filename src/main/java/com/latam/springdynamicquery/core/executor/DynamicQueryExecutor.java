package com.latam.springdynamicquery.core.executor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.latam.springdynamicquery.autoconfigure.DynamicQueryProperties;
import com.latam.springdynamicquery.core.criteria.FilterCriteria;
import com.latam.springdynamicquery.core.loader.SqlQueryLoader;
import com.latam.springdynamicquery.core.model.SqlMapperYaml;
import com.latam.springdynamicquery.core.validation.QueryValidator;
import com.latam.springdynamicquery.exception.InvalidQueryException;
import com.latam.springdynamicquery.util.SqlUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;

/**
 * Ejecutor de consultas dinámicas.
 * Maneja la construcción y ejecución de consultas SQL dinámicas con filtros.
 */
@Slf4j
public class DynamicQueryExecutor {
    
    private final EntityManager entityManager;
    private final SqlQueryLoader queryLoader;
    private final DynamicQueryProperties properties;
    
    public DynamicQueryExecutor(SqlQueryLoader queryLoader, 
                               EntityManager entityManager,
                               DynamicQueryProperties properties) {
        this.queryLoader = queryLoader;
        this.entityManager = entityManager;
        this.properties = properties;
    }
    
    /**
     * Ejecuta una consulta SELECT nombrada con PARÁMETROS FIJOS (sin filtros dinámicos).
     * Obtiene el resultClass automáticamente desde la metadata de la query.
     * Solo para queries SELECT que retornan datos.
     * 
     * @param queryName Nombre de la query
     * @param parameters Parámetros de la query
     * @return Lista de resultados
     * @throws InvalidQueryException si la query no es SELECT o no tiene resultType
     */
    public <T> List<T> executeNamedQueryWithParameters(String queryName, 
                                                       Map<String, Object> parameters) {
        SqlMapperYaml.QueryDefinition queryDef = queryLoader.getQueryMetadata(queryName);
        
        if (queryDef == null) {
            throw new InvalidQueryException(queryName, 
                "Query metadata not found. Check if query exists in YAML.");
        }
        
        if (!queryDef.requiresResultType()) {
            throw new InvalidQueryException(queryName,
                "This method is for SELECT queries. Use executeDML() for INSERT/UPDATE/DELETE.");
        }
        
        try {
            @SuppressWarnings("unchecked")
            Class<T> resultClass = (Class<T>) Class.forName(queryDef.getEffectiveResultType());
            return executeNamedQueryWithParameters(queryName, resultClass, parameters);
        } catch (ClassNotFoundException e) {
            throw new InvalidQueryException(queryName, 
                "ResultType class not found: " + queryDef.getEffectiveResultType(), e);
        }
    }
    
    /**
    * Ejecuta una consulta nombrada con PARÁMETROS FIJOS (sin filtros dinámicos).
    * Usar cuando la query ya tiene :param definidos y solo necesitas pasar valores.
    * Ideal para queries estáticas (dynamic: false).
    */
    @SuppressWarnings("unchecked")
	public <T> List<T> executeNamedQueryWithParameters(String queryName, Class<T> resultClass, 
                                                       Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        
        try {
            String sql = queryLoader.getQuery(queryName);
            SqlMapperYaml.QueryDefinition queryDef = queryLoader.getQueryMetadata(queryName);
            
            // Validar que no sea una query dinamica si se esta usando este metodo
            if (queryDef != null && queryDef.isDynamic()) {
                log.debug("Query '{}' is marked as dynamic but being executed with fixed parameters", queryName);
            }
            
            // Si tiene resultMapping, usar createNativeQuery con el mapping
            Query query;
            if (queryDef != null && queryDef.hasResultMapping()) {
                log.debug("Using SqlResultSetMapping '{}' for query '{}'", 
                    queryDef.getResultMapping(), queryName);
                query = entityManager.createNativeQuery(sql, queryDef.getResultMapping());
            } else {
                query = entityManager.createNativeQuery(sql, resultClass);
            }
            
            Map<String, Object> completeParams = buildCompleteParameterMap(
                queryDef,
                parameters != null ? parameters : Collections.emptyMap(),
                queryName
            );
            
            // Establecer parámetros directamente sin construir SQL dinámico
            if (!completeParams.isEmpty()) {
                for (Map.Entry<String, Object> entry : completeParams.entrySet()) {
                    try {
                        query.setParameter(entry.getKey(), entry.getValue());
                        
                        if (properties.getLogging().isLogParameters()) {
                            log.debug("Set parameter '{}' = {} for query '{}'", 
                                    entry.getKey(), entry.getValue(), queryName);
                        }
                    } catch (IllegalArgumentException e) {
                        log.warn("Parameter '{}' not found in query '{}', skipping", 
                                entry.getKey(), queryName);
                    }
                }
            }
            
            if (properties.getLogging().isEnabled()) {
                if (properties.getLogging().isLogGeneratedSql()) {
                    log.debug("Executing query '{}': {}", queryName, sql);
                } else {
                    log.debug("Executing query '{}'", queryName);
                }
            }
            
            return query.getResultList();
            
        } finally {
            if (properties.getLogging().isLogExecutionTime()) {
                long executionTime = System.currentTimeMillis() - startTime;
                log.debug("Query '{}' executed in {} ms", queryName, executionTime);
            }
        }
    }
    
    /**
     * Ejecuta una consulta nombrada con FILTROS DINÁMICOS (condiciones opcionales).
     * Usar cuando la query es dinámica (dynamic: true).
     */
    public <T> List<T> executeNamedQuery(String queryName, Class<T> resultClass, 
                                        Map<String, FilterCriteria> filters) {
        long startTime = System.currentTimeMillis();
        
        try {
        	// Validar que la query permita filtros dinámicos
            SqlMapperYaml.QueryDefinition queryDef = queryLoader.getQueryMetadata(queryName);
            
            if (queryDef != null) {
                boolean hasFilters = filters != null && !filters.isEmpty();
                QueryValidator.validateStaticQueryUsage(queryName, queryDef.isDynamic(), hasFilters);
            }
            
            String baseSql = queryLoader.getQuery(queryName);
            return executeQuery(baseSql, resultClass, filters, queryName);
        } finally {
            if (properties.getLogging().isLogExecutionTime()) {
                long executionTime = System.currentTimeMillis() - startTime;
                log.debug("Query '{}' executed in {} ms", queryName, executionTime);
            }
        }
    }
    
    /**
     * Ejecuta una consulta directa con filtros dinámicos
     */
    public <T> List<T> executeQuery(String baseSql, Class<T> resultClass, 
                                   Map<String, FilterCriteria> filters) {
        return executeQuery(baseSql, resultClass, filters, "direct-query");
    }
    
    /**
     * Método interno que ejecuta la consulta con filtros dinámicos
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> executeQuery(String baseSql, Class<T> resultClass, 
                                     Map<String, FilterCriteria> filters, String queryName) {
        String finalSql = buildDynamicQuery(baseSql, filters);
        
        Query query = entityManager.createNativeQuery(finalSql, resultClass);
        setParameters(query, filters, queryName);
        
        if (properties.getLogging().isEnabled()) {
            if (properties.getLogging().isLogGeneratedSql()) {
                log.debug("Executing query '{}': {}", queryName, finalSql);
            } else {
                log.debug("Executing query '{}'", queryName);
            }
        }
        
        return query.getResultList();
    }
    
    /**
     * Ejecuta una consulta que devuelve Object[]
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> executeRawQuery(String queryName, Map<String, FilterCriteria> filters) {
        long startTime = System.currentTimeMillis();
        
        try {
        	 // Validar que la query permita filtros dinámicos
            SqlMapperYaml.QueryDefinition queryDef = queryLoader.getQueryMetadata(queryName);
            if (queryDef != null) {
                boolean hasFilters = filters != null && !filters.isEmpty();
                QueryValidator.validateStaticQueryUsage(queryName, queryDef.isDynamic(), hasFilters);
            }
            
            String baseSql = queryLoader.getQuery(queryName);
            String finalSql = buildDynamicQuery(baseSql, filters);
            
            Query query = entityManager.createNativeQuery(finalSql);
            setParameters(query, filters, queryName);
            
            if (properties.getLogging().isEnabled()) {
                logQueryExecution(queryName, finalSql, filters);
            }
            
            List<Object[]> result = query.getResultList();
            log.debug("Raw query '{}' returned {} rows", queryName, result.size());
            
            return result;
        } finally {
            if (properties.getLogging().isLogExecutionTime()) {
                long executionTime = System.currentTimeMillis() - startTime;
                log.debug("Raw query '{}' executed in {} ms", queryName, executionTime);
            }
        }
    }
    
    /**
     * Ejecuta una consulta de agregación
     */
    public <R> R executeSingleResult(String queryName, Map<String, FilterCriteria> filters, Class<R> resultType) {
        long startTime = System.currentTimeMillis();
        
        try {
        	// Validar que la query permita filtros dinámicos
            SqlMapperYaml.QueryDefinition queryDef = queryLoader.getQueryMetadata(queryName);
            if (queryDef != null) {
                boolean hasFilters = filters != null && !filters.isEmpty();
                QueryValidator.validateStaticQueryUsage(queryName, queryDef.isDynamic(), hasFilters);
            }
            
            String baseSql = queryLoader.getQuery(queryName);
            String finalSql = buildDynamicQuery(baseSql, filters);
            
            Query query = entityManager.createNativeQuery(finalSql);
            setParameters(query, filters, queryName);
            
            if (properties.getLogging().isEnabled()) {
                logQueryExecution(queryName, finalSql, filters);
            }
            
            List<?> results = query.getResultList();
            
            if (results.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Query '{}' returned no results, returning null", queryName);
                }
                return null;
            }
            
            if (results.size() > 1) {
                if (log.isWarnEnabled()) {
                    log.warn("Query '{}' expected single result but returned {} results. " +
                            "Returning first result. Consider adding LIMIT 1 or reviewing query logic.",
                            queryName, results.size());
                }
            }
            
            Object result = results.get(0);
            
            return convertSingleResult(result, resultType);
        } finally {
            if (properties.getLogging().isLogExecutionTime()) {
                long executionTime = System.currentTimeMillis() - startTime;
                log.debug("Single result query '{}' executed in {} ms", queryName, executionTime);
            }
        }
    }
    
    /**
    * Ejecuta una query DML (INSERT, UPDATE, DELETE) nombrada.
    * No retorna datos, solo el número de filas afectadas.
    * 
    * @param queryName Nombre de la query DML
    * @param parameters Parámetros de la query
    * @return Número de filas afectadas
    */
   public int executeDMLWithParameters(String queryName, Map<String, Object> parameters) {
       long startTime = System.currentTimeMillis();
       
       try {
           String sql = queryLoader.getQuery(queryName);
           SqlMapperYaml.QueryDefinition queryDef = queryLoader.getQueryMetadata(queryName);
           
           // CAMBIO: Validar que sea DML
           if (queryDef != null && queryDef.requiresResultType()) {
               throw new InvalidQueryException(queryName,
                   "This method is for DML queries (INSERT/UPDATE/DELETE). Use executeNamedQuery() for SELECT.");
           }
           
           // Validar que no sea una query dinámica
           if (queryDef != null && queryDef.isDynamic()) {
               log.debug("Query '{}' is marked as dynamic but being executed with fixed parameters", queryName);
           }
           
           if (properties.getLogging().isEnabled()) {
               if (properties.getLogging().isLogGeneratedSql()) {
                   log.debug("Executing DML query '{}': {}", queryName, sql);
               } else {
                   log.debug("Executing DML query '{}'", queryName);
               }
           }
           
           Query query = entityManager.createNativeQuery(sql);
           
           Map<String, Object> completeParams = buildCompleteParameterMap(
               queryDef,
               parameters != null ? parameters : Collections.emptyMap(),
               queryName
           );
           
           // Establecer parámetros
           for (Map.Entry<String, Object> entry : completeParams.entrySet()) {
               try {
                   query.setParameter(entry.getKey(), entry.getValue());
                   
                   if (properties.getLogging().isLogParameters()) {
                       log.debug("Set parameter '{}' = {} for DML query '{}'", 
                               entry.getKey(), entry.getValue(), queryName);
                   }
               } catch (IllegalArgumentException e) {
                   log.warn("Parameter '{}' not found in DML query '{}', skipping", 
                           entry.getKey(), queryName);
               }
           }
           
           // Ejecutar DML
           int affectedRows = query.executeUpdate();
           
           if (properties.getLogging().isEnabled()) {
               log.info("DML query '{}' affected {} rows", queryName, affectedRows);
           }
           
           return affectedRows;
           
       } finally {
           if (properties.getLogging().isLogExecutionTime()) {
               long executionTime = System.currentTimeMillis() - startTime;
               log.debug("DML query '{}' executed in {} ms", queryName, executionTime);
           }
       }
   }
   
   /**
    * Ejecuta una query DML dinámica (con filtros opcionales en WHERE).
    * Para casos como UPDATE/DELETE con condiciones dinámicas.
    * 
    * @param queryName Nombre de la query DML
    * @param filters Filtros dinámicos
    * @return Número de filas afectadas
    */
   public int executeDMLWithFilters(String queryName, Map<String, FilterCriteria> filters) {
       long startTime = System.currentTimeMillis();
       
       try {
           SqlMapperYaml.QueryDefinition queryDef = queryLoader.getQueryMetadata(queryName);
           
           // CAMBIO: Validar que sea DML
           if (queryDef != null && queryDef.requiresResultType()) {
               throw new InvalidQueryException(queryName,
                   "This method is for DML queries (INSERT/UPDATE/DELETE). Use executeNamedQuery() for SELECT.");
           }
           
           // Validar que permita filtros dinámicos
           if (queryDef != null) {
               boolean hasFilters = filters != null && !filters.isEmpty();
               QueryValidator.validateStaticQueryUsage(queryName, queryDef.isDynamic(), hasFilters);
           }
           
           String baseSql = queryLoader.getQuery(queryName);
           String finalSql = buildDynamicQuery(baseSql, filters);
           
           if (properties.getLogging().isEnabled()) {
               if (properties.getLogging().isLogGeneratedSql()) {
                   log.debug("Executing dynamic DML query '{}': {}", queryName, finalSql);
               } else {
                   log.debug("Executing dynamic DML query '{}'", queryName);
               }
           }
           
           Query query = entityManager.createNativeQuery(finalSql);
           setParameters(query, filters, queryName);
           
           // Ejecutar DML
           int affectedRows = query.executeUpdate();
           
           if (properties.getLogging().isEnabled()) {
               log.info("Dynamic DML query '{}' affected {} rows", queryName, affectedRows);
           }
           
           return affectedRows;
           
       } finally {
           if (properties.getLogging().isLogExecutionTime()) {
               long executionTime = System.currentTimeMillis() - startTime;
               log.debug("Dynamic DML query '{}' executed in {} ms", queryName, executionTime);
           }
       }
   }
    
    /**
     * Construye la consulta dinámica con los filtros
     */
    private String buildDynamicQuery(String baseSql, Map<String, FilterCriteria> filters) {
        if (filters == null || filters.isEmpty()) {
            log.trace("No filters provided, returning base SQL");
            return baseSql;
        }
        
        StringBuilder sqlBuilder = new StringBuilder(baseSql);
        List<FilterCriteria> appliedFilters = filters.values().stream()
            .filter(FilterCriteria::shouldApply)
            .toList();
        
        if (appliedFilters.isEmpty()) {
            log.trace("No filters matched conditions, returning base SQL");
            return baseSql;
        }
        
        SqlUtils.WhereInfo whereInfo = SqlUtils.analyzeWhereClause(baseSql);
        sqlBuilder.append(whereInfo.wherePrefix());
        
        for (int i = 0; i < appliedFilters.size(); i++) {
            if (i > 0) {
                sqlBuilder.append(" ").append(appliedFilters.get(i).getConnector()).append(" ");
            }
            sqlBuilder.append(appliedFilters.get(i).getSqlFragment());
        }
        
        String finalSql = sqlBuilder.toString();
        log.trace("Built dynamic SQL: {}", finalSql);
        
        return finalSql;
    }
    
    /**
     * Establece los parámetros en la consulta
     */
    private void setParameters(Query query, Map<String, FilterCriteria> filters, String queryName) {
        if (filters == null) {
            return;
        }
        
        int parametersSet = 0;
        
        for (Map.Entry<String, FilterCriteria> entry : filters.entrySet()) {
            FilterCriteria criteria = entry.getValue();
            
            if (criteria.shouldApply() && criteria.getValue() != null) {
                try {
                    query.setParameter(entry.getKey(), criteria.getValue());
                    parametersSet++;
                    
                    if (properties.getLogging().isLogParameters()) {
                        log.debug("Set parameter '{}' = {} for query '{}'", 
                                entry.getKey(), criteria.getValue(), queryName);
                    } else {
                        log.trace("Set parameter '{}' for query '{}'", entry.getKey(), queryName);
                    }
                    
                } catch (IllegalArgumentException e) {
                    log.debug("Parameter '{}' not found in query '{}', skipping", entry.getKey(), queryName);
                }
            }
        }
        
        log.debug("Set {} parameters for query '{}'", parametersSet, queryName);
    }
    
    /**
     * Convierte el resultado único al tipo solicitado
     */
    @SuppressWarnings("unchecked")
    private <R> R convertSingleResult(Object result, Class<R> resultType) {
        if (result == null) {
            return null;
        }
        
        return switch (result) {
            case Number n when resultType == Long.class -> (R) Long.valueOf(n.longValue());
            case Number n when resultType == Integer.class -> (R) Integer.valueOf(n.intValue());
            case Number n when resultType == Double.class -> (R) Double.valueOf(n.doubleValue());
            case Number n when resultType == Float.class -> (R) Float.valueOf(n.floatValue());
            default -> resultType.cast(result);
        };
    }
    
    /**
     * Registra la ejecución de la consulta para debugging
     */
    private void logQueryExecution(String queryName, String sql, Map<String, FilterCriteria> filters) {
        if (!log.isDebugEnabled()) {
            return;
        }
        
        log.debug("Executing query '{}': {}", queryName, 
                properties.getLogging().isLogGeneratedSql() ? sql : "[SQL hidden]");
        
        if (properties.getLogging().isLogParameters() && filters != null) {
            filters.entrySet().stream()
                    .filter(entry -> entry.getValue().shouldApply())
                    .forEach(entry -> log.debug("  Parameter '{}' = {}", 
                            entry.getKey(), entry.getValue().getValue()));
        }
    }
    
    /**
     * NUEVO MÉTODO: Construye un mapa completo de parámetros incluyendo NULL para parámetros opcionales.
     * 
     * Este método resuelve el problema donde Hibernate requiere que TODOS los parámetros nombrados
     * en el SQL tengan valores asignados, incluso si son opcionales según la metadata.
     * 
     * @param queryDef Definición de la query con metadata de parámetros
     * @param providedParams Parámetros provistos por el llamador
     * @param queryName Nombre de la query (para mensajes de error)
     * @return Mapa completo con todos los parámetros, usando NULL para opcionales faltantes
     * @throws InvalidQueryException si falta un parámetro requerido
     */
    private Map<String, Object> buildCompleteParameterMap(
            SqlMapperYaml.QueryDefinition queryDef,
            Map<String, Object> providedParams,
            String queryName) {
        
        // Si no hay metadata o no hay parámetros definidos, retornar lo provisto
        if (queryDef == null || !queryDef.hasParameters()) {
            return providedParams;
        }
        
        // Crear una copia del mapa provisto
        Map<String, Object> completeParams = new HashMap<>(providedParams);
        
        // Procesar cada parámetro definido en la metadata
        for (SqlMapperYaml.QueryDefinition.ParameterMapping paramDef : queryDef.getParameters()) {
            String paramName = paramDef.getName();
            
            if (!completeParams.containsKey(paramName)) {
                if (paramDef.isRequired()) {
                    // Parámetro requerido faltante - lanzar excepción
                    throw new InvalidQueryException(queryName,
                        String.format("Required parameter '%s' not provided. " +
                                "Required parameters must be included in the parameters map.", 
                                paramName));
                } else {
                    // Parámetro opcional faltante - agregar como NULL
                    completeParams.put(paramName, null);
                    
                    if (properties.getLogging().isLogParameters()) {
                        log.debug("Optional parameter '{}' not provided for query '{}', setting to NULL", 
                                paramName, queryName);
                    }
                }
            }
        }
        
        return completeParams;
    }
}