package com.latam.springdynamicquery.repository;

import com.latam.springdynamicquery.core.criteria.FilterCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Map;

/**
 * Interface principal para repositories que soportan consultas dinámicas.
 * Extiende JpaRepository para proporcionar toda la funcionalidad estándar de Spring Data JPA
 * además de las capacidades de consulta dinámica.
 */
@NoRepositoryBean
public interface DynamicRepository<T, ID> extends JpaRepository<T, ID> {
    
    /**
     * Ejecuta una consulta dinámica usando un nombre de query preconfigurado.
     * 
     * @param queryName Nombre de la consulta (formato: namespace.queryId o solo queryId)
     * @param filters Mapa de criterios de filtrado dinámicos
     * @return Lista de entidades que coinciden con los filtros
     */
    List<T> executeNamedQuery(String queryName, Map<String, FilterCriteria> filters);
    
    /**
     * Ejecuta una consulta dinámica directamente con SQL.
     * 
     * @param baseSql Consulta SQL base
     * @param filters Mapa de criterios de filtrado dinámicos
     * @return Lista de entidades que coinciden con los filtros
     */
    List<T> executeDynamicQuery(String baseSql, Map<String, FilterCriteria> filters);
    
    /**
     * Ejecuta una consulta que devuelve Object[] (útil para joins y proyecciones).
     * 
     * @param queryName Nombre de la consulta
     * @param filters Mapa de criterios de filtrado dinámicos
     * @return Lista de Object[] con los resultados
     */
    List<Object[]> executeRawQuery(String queryName, Map<String, FilterCriteria> filters);
    
    /**
     * Ejecuta una consulta de agregación que devuelve un valor único.
     * 
     * @param queryName Nombre de la consulta
     * @param filters Mapa de criterios de filtrado dinámicos
     * @param resultType Clase del tipo de resultado esperado
     * @return Valor único del resultado
     */
    <R> R executeSingleResult(String queryName, Map<String, FilterCriteria> filters, Class<R> resultType);
    
    /**
     * Ejecuta una consulta nativa con parámetros dinámicos.
     * 
     * @param nativeQuery Consulta SQL nativa
     * @param filters Mapa de criterios de filtrado dinámicos
     * @return Lista de entidades que coinciden con los filtros
     */
    List<T> executeNativeQueryWithFilters(String nativeQuery, Map<String, FilterCriteria> filters);
    
    /**
     * Ejecuta una named query JPA con parámetros dinámicos.
     * 
     * @param namedQueryName Nombre de la named query JPA
     * @param parameters Mapa de parámetros
     * @return Lista de entidades que coinciden con los parámetros
     */
    List<T> executeJpaNamedQuery(String namedQueryName, Map<String, Object> parameters);
}
