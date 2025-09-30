package com.latam.springdynamicquery.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.latam.springdynamicquery.core.criteria.FilterCriteria;

/**
 * Interface principal para repositories que soportan consultas dinámicas.
 * <p>
 * Extiende {@link JpaRepository} para proporcionar toda la funcionalidad estándar de Spring Data JPA,
 * además de capacidades avanzadas para ejecutar consultas dinámicas basadas en filtros,
 * parámetros y SQL nativo.
 * </p>
 *
 * @param <T>  Tipo de la entidad administrada por el repositorio
 * @param <ID> Tipo del identificador de la entidad
 */
@NoRepositoryBean
public interface DynamicRepository<T, ID> extends JpaRepository<T, ID> {
    
    /**
     * Ejecuta una consulta dinámica usando un nombre de query preconfigurado.
     *
     * @param queryName Nombre de la consulta (formato: namespace.queryId o solo queryId)
     * @param filters   Mapa de criterios de filtrado dinámicos
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
     * Ejecuta una consulta que devuelve resultados genéricos en forma de {@code Object[]}.
     * <p>
     * Útil para joins, proyecciones personalizadas o cuando el resultado no es una entidad.
     * </p>
     *
     * @param queryName Nombre de la consulta
     * @param filters   Mapa de criterios de filtrado dinámicos
     * @return Lista de arreglos de objetos con los resultados
     */
    List<Object[]> executeRawQuery(String queryName, Map<String, FilterCriteria> filters);
    
    /**
     * Ejecuta una consulta de agregación que devuelve un valor único.
     *
     * @param <R>        Tipo del resultado esperado
     * @param queryName  Nombre de la consulta
     * @param filters    Mapa de criterios de filtrado dinámicos
     * @param resultType Clase del tipo de resultado esperado
     * @return Valor único del resultado, o {@code null} si no hay coincidencias
     */
    <R> R executeSingleResult(String queryName, Map<String, FilterCriteria> filters, Class<R> resultType);
    
    /**
     * Ejecuta una consulta nativa con parámetros dinámicos.
     *
     * @param nativeQuery Consulta SQL nativa
     * @param filters     Mapa de criterios de filtrado dinámicos
     * @return Lista de entidades que coinciden con los filtros
     */
    List<T> executeNativeQueryWithFilters(String nativeQuery, Map<String, FilterCriteria> filters);
    
    /**
     * Ejecuta una named query JPA con parámetros dinámicos.
     *
     * @param namedQueryName Nombre de la named query JPA
     * @param parameters     Mapa de parámetros estáticos
     * @return Lista de entidades que coinciden con los parámetros
     */
    List<T> executeJpaNamedQuery(String namedQueryName, Map<String, Object> parameters);
    
    /**
     * Ejecuta una query nombrada con parámetros fijos.
     *
     * @param queryName  Nombre de la consulta
     * @param parameters Mapa de parámetros estáticos
     * @return Lista de entidades que coinciden con los parámetros
     */
    List<T> executeNamedQueryWithParams(String queryName, Map<String, Object> parameters);

    /**
     * Ejecuta una query nombrada con parámetros fijos y devuelve un resultado único.
     *
     * @param queryName  Nombre de la consulta
     * @param parameters Mapa de parámetros estáticos
     * @return Resultado único envuelto en {@link Optional}, vacío si no se encuentra coincidencia
     */
    Optional<T> executeSingleResultWithParams(String queryName, Map<String, Object> parameters);

    /**
     * Ejecuta una query nombrada sin parámetros.
     *
     * @param queryName Nombre de la consulta
     * @return Lista de entidades que coinciden con la consulta
     */
    List<T> executeNamedQuery(String queryName);

    /**
     * Ejecuta una query nombrada sin parámetros y devuelve un resultado único.
     *
     * @param queryName Nombre de la consulta
     * @return Resultado único envuelto en {@link Optional}, vacío si no se encuentra coincidencia
     */
    Optional<T> executeSingleResult(String queryName);
}
