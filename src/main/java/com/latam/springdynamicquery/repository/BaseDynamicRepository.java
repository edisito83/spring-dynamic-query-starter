package com.latam.springdynamicquery.repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.latam.springdynamicquery.core.criteria.FilterCriteria;
import com.latam.springdynamicquery.core.executor.DynamicQueryExecutor;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * Implementación base para repositories dinámicos. Proporciona implementaciones
 * por defecto para todos los métodos de DynamicRepository.
 */
@Transactional(readOnly = true)
public class BaseDynamicRepository<T, ID> extends SimpleJpaRepository<T, ID> implements DynamicRepository<T, ID> {

	protected final DynamicQueryExecutor queryExecutor;

	protected final EntityManager entityManager;

	protected final Class<T> entityClass;

	public BaseDynamicRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager,
			DynamicQueryExecutor queryExecutor) {
		super(entityInformation, entityManager);
		this.entityClass = entityInformation.getJavaType();
		this.entityManager = entityManager;
		this.queryExecutor = queryExecutor;
	}

	public BaseDynamicRepository(Class<T> entityClass, EntityManager entityManager,
			DynamicQueryExecutor queryExecutor) {
		super(entityClass, entityManager);
		this.entityClass = entityClass;
		this.entityManager = entityManager;
		this.queryExecutor = queryExecutor;
	}
	
	// ==================== Métodos con FilterCriteria (filtros dinámicos) ====================

	@Override
	public List<T> executeNamedQuery(String queryName, Map<String, FilterCriteria> filters) {
		return queryExecutor.executeNamedQuery(queryName, entityClass, filters);
	}

	@Override
	public List<T> executeDynamicQuery(String baseSql, Map<String, FilterCriteria> filters) {
		return queryExecutor.executeQuery(baseSql, entityClass, filters);
	}

	@Override
	public List<Object[]> executeRawQuery(String queryName, Map<String, FilterCriteria> filters) {
		return queryExecutor.executeRawQuery(queryName, filters);
	}

	@Override
	public <R> R executeSingleResult(String queryName, Map<String, FilterCriteria> filters, Class<R> resultType) {
		return queryExecutor.executeSingleResult(queryName, filters, resultType);
	}

	@Override
	public List<T> executeNativeQueryWithFilters(String nativeQuery, Map<String, FilterCriteria> filters) {
		return queryExecutor.executeQuery(nativeQuery, entityClass, filters);
	}

	@Override
	public List<T> executeJpaNamedQuery(String namedQueryName, Map<String, Object> parameters) {
		TypedQuery<T> query = entityManager.createNamedQuery(namedQueryName, entityClass);

		if (parameters != null) {
			parameters.forEach(query::setParameter);
		}

		return query.getResultList();
	}
	
	// ==================== Métodos con parámetros fijos (sin filtros dinámicos) ====================

    /**
     * Ejecuta una query nombrada con parámetros fijos.
     * Usar cuando la query tiene :param definidos y NO necesitas agregar condiciones dinámicas.
     * 
     * Ejemplo:
     * SQL: SELECT * FROM users WHERE id = :id
     * Uso: executeNamedQueryWithParams("findUser", Map.of("id", 123L))
     */
    public List<T> executeNamedQueryWithParams(String queryName, Map<String, Object> parameters) {
        return queryExecutor.executeNamedQueryWithParameters(queryName, entityClass, parameters);
    }

    /**
     * Ejecuta una query nombrada con parámetros fijos y devuelve un resultado único.
     */
    public Optional<T> executeSingleResultWithParams(String queryName, Map<String, Object> parameters) {
        List<T> results = executeNamedQueryWithParams(queryName, parameters);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Ejecuta una query nombrada sin parámetros.
     */
    public List<T> executeNamedQuery(String queryName) {
        return executeNamedQuery(queryName, Collections.emptyMap());
    }

    /**
     * Ejecuta una query nombrada sin parámetros y devuelve un resultado único.
     */
    public Optional<T> executeSingleResult(String queryName) {
        List<T> results = executeNamedQuery(queryName);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

	/**
	 * Obtiene la clase de entidad gestionada por este repository.
	 */
	protected Class<T> getEntityClass() {
		return entityClass;
	}
}