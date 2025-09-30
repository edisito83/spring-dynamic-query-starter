package com.latam.springdynamicquery.repository;

import java.util.List;
import java.util.Map;

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

	/**
	 * Obtiene la clase de entidad gestionada por este repository.
	 */
	protected Class<T> getEntityClass() {
		return entityClass;
	}
}