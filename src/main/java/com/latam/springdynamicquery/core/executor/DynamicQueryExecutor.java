package com.latam.springdynamicquery.core.executor;

import java.util.List;
import java.util.Map;

import com.latam.springdynamicquery.autoconfigure.DynamicQueryProperties;
import com.latam.springdynamicquery.core.criteria.FilterCriteria;
import com.latam.springdynamicquery.core.loader.SqlQueryLoader;
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
     * Ejecuta una consulta nombrada con PARÁMETROS FIJOS (sin filtros dinámicos)
     * Usar cuando la query ya tiene :param definidos y solo necesitas pasar valores
     */
    public <T> List<T> executeNamedQueryWithParameters(String queryName, Class<T> resultClass, 
                                                       Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        
        try {
            String sql = queryLoader.getQuery(queryName);
            
            Query query = entityManager.createNativeQuery(sql, resultClass);
            
            // Establecer parámetros directamente sin construir SQL dinámico
            if (parameters != null) {
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
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
     * Ejecuta una consulta nombrada con FILTROS DINÁMICOS (condiciones opcionales)
     */
    public <T> List<T> executeNamedQuery(String queryName, Class<T> resultClass, 
                                        Map<String, FilterCriteria> filters) {
        long startTime = System.currentTimeMillis();
        
        try {
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
            String baseSql = queryLoader.getQuery(queryName);
            String finalSql = buildDynamicQuery(baseSql, filters);
            
            Query query = entityManager.createNativeQuery(finalSql);
            setParameters(query, filters, queryName);
            
            if (properties.getLogging().isEnabled()) {
                logQueryExecution(queryName, finalSql, filters);
            }
            
            Object result = query.getSingleResult();
            
            return convertSingleResult(result, resultType);
        } finally {
            if (properties.getLogging().isLogExecutionTime()) {
                long executionTime = System.currentTimeMillis() - startTime;
                log.debug("Single result query '{}' executed in {} ms", queryName, executionTime);
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
}