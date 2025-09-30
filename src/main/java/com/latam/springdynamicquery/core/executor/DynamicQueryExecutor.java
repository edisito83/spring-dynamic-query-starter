package com.latam.springdynamicquery.core.executor;

import java.util.List;
import java.util.Map;

import com.latam.springdynamicquery.autoconfigure.DynamicQueryProperties;
import com.latam.springdynamicquery.core.criteria.FilterCriteria;
import com.latam.springdynamicquery.core.loader.SqlQueryLoader;

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
    private final DynamicQueryProperties properties; // ✅ AGREGADO
    
    // ✅ Constructor actualizado con properties
    public DynamicQueryExecutor(SqlQueryLoader queryLoader, 
                               EntityManager entityManager,
                               DynamicQueryProperties properties) {
        this.queryLoader = queryLoader;
        this.entityManager = entityManager;
        this.properties = properties;
    }
    
    /**
     * Ejecuta una consulta nombrada con filtros dinámicos
     */
    public <T> List<T> executeNamedQuery(String queryName, Class<T> resultClass, 
                                        Map<String, FilterCriteria> filters) {
        long startTime = System.currentTimeMillis();
        
        try {
            String baseSql = queryLoader.getQuery(queryName);
            return executeQuery(baseSql, resultClass, filters, queryName);
        } finally {
            // ✅ USA properties.getLogging().isLogExecutionTime()
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
     * Método interno que ejecuta la consulta
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> executeQuery(String baseSql, Class<T> resultClass, 
                                     Map<String, FilterCriteria> filters, String queryName) {
        String finalSql = buildDynamicQuery(baseSql, filters);
        
        Query query = entityManager.createNativeQuery(finalSql, resultClass);
        setParameters(query, filters, queryName);
        
        // ✅ USA properties.getLogging().isEnabled() y isLogGeneratedSql()
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
            
            // ✅ USA properties.getLogging()
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
            
            // ✅ USA properties.getLogging()
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
        
        String whereClause = determineWhereClause(baseSql);
        sqlBuilder.append(whereClause);
        
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
     * Determina si usar WHERE o AND basado en la consulta existente
     */
    private String determineWhereClause(String sql) {
        String upperSql = sql.toUpperCase();
        
        boolean hasWhere = false;
        int parenLevel = 0;
        String[] tokens = upperSql.split("\\s+");
        
        for (String token : tokens) {
            for (char c : token.toCharArray()) {
                if (c == '(') parenLevel++;
                else if (c == ')') parenLevel--;
            }
            
            if (parenLevel == 0 && "WHERE".equals(token)) {
                hasWhere = true;
                break;
            }
        }
        
        return hasWhere ? " AND " : " WHERE ";
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
                    
                    // ✅ USA properties.getLogging().isLogParameters()
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
        
        // ✅ USA properties.getLogging().isLogGeneratedSql()
        log.debug("Executing query '{}': {}", queryName, 
                properties.getLogging().isLogGeneratedSql() ? sql : "[SQL hidden]");
        
        // ✅ USA properties.getLogging().isLogParameters()
        if (properties.getLogging().isLogParameters() && filters != null) {
            filters.entrySet().stream()
                    .filter(entry -> entry.getValue().shouldApply())
                    .forEach(entry -> log.debug("  Parameter '{}' = {}", 
                            entry.getKey(), entry.getValue().getValue()));
        }
    }
}