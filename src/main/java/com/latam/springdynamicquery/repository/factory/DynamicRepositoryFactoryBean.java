package com.latam.springdynamicquery.repository.factory;

import com.latam.springdynamicquery.core.executor.DynamicQueryExecutor;
import com.latam.springdynamicquery.repository.BaseDynamicRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

public class DynamicRepositoryFactoryBean<R extends Repository<T, I>, T, I> extends JpaRepositoryFactoryBean<R, T, I> {

	private DynamicQueryExecutor queryExecutor;

	public DynamicRepositoryFactoryBean(Class<? extends R> repositoryInterface) {
		super(repositoryInterface);
	}

	@Autowired
	public void setQueryExecutor(DynamicQueryExecutor queryExecutor) {
		this.queryExecutor = queryExecutor;
	}

	@Override
	protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
		return new DynamicRepositoryFactory(entityManager, queryExecutor);
	}

	private static class DynamicRepositoryFactory extends JpaRepositoryFactory {

//		private final EntityManager entityManager;
		private final DynamicQueryExecutor queryExecutor;

		public DynamicRepositoryFactory(EntityManager entityManager, DynamicQueryExecutor queryExecutor) {
			super(entityManager);
//			this.entityManager = entityManager;
			this.queryExecutor = queryExecutor;
		}

		@Override
		protected JpaRepositoryImplementation<?, ?> getTargetRepository(RepositoryInformation information,
				EntityManager entityManager) {
			JpaEntityInformation<?, Object> entityInformation = getEntityInformation(information.getDomainType());
			Object repository = getTargetRepositoryViaReflection(information, entityInformation, entityManager, queryExecutor);

			return (JpaRepositoryImplementation<?, ?>) repository;
		}

		@Override
		protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
			return BaseDynamicRepository.class;
		}
	}
}