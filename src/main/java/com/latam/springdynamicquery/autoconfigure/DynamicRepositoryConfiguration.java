package com.latam.springdynamicquery.autoconfigure;

import com.latam.springdynamicquery.repository.factory.DynamicRepositoryFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuración para habilitar repositories dinámicos con factory personalizado.
 */
@Configuration
@ConditionalOnProperty(value = "app.dynamic-query.enabled", havingValue = "true", matchIfMissing = true)
@EnableJpaRepositories(
    repositoryFactoryBeanClass = DynamicRepositoryFactoryBean.class,
    basePackages = "${app.dynamic-query.repository.base-packages:}"
)
public class DynamicRepositoryConfiguration {
}