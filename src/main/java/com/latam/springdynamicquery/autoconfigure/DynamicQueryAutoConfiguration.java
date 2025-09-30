package com.latam.springdynamicquery.autoconfigure;

import com.latam.springdynamicquery.core.executor.DynamicQueryExecutor;
import com.latam.springdynamicquery.core.loader.SqlQueryLoader;
//import com.latam.springdynamicquery.repository.BaseDynamicRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
//import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import jakarta.persistence.EntityManager;

/**
 * Auto-configuración principal para Spring Dynamic Query Starter.
 * Se activa automáticamente cuando JPA está presente y la propiedad está habilitada.
 */
@Slf4j
@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)
@ConditionalOnClass({EntityManager.class})
@ConditionalOnProperty(value = "app.dynamic-query.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DynamicQueryProperties.class)
//@EnableJpaRepositories(repositoryBaseClass = BaseDynamicRepository.class)
public class DynamicQueryAutoConfiguration {
    
    /**
     * Crea el bean SqlQueryLoader si no existe uno personalizado.
     */
    @Bean
    @ConditionalOnMissingBean
    public SqlQueryLoader sqlQueryLoader(DynamicQueryProperties properties) {
        log.info("Initializing SqlQueryLoader with properties: preload={}, scanPackages={}", 
                properties.isPreloadEnabled(), properties.getScanPackages());
        return new SqlQueryLoader(properties);
    }
    
    /**
     * Crea el bean DynamicQueryExecutor si no existe uno personalizado.
     */
    @Bean
    @ConditionalOnMissingBean
    public DynamicQueryExecutor dynamicQueryExecutor(SqlQueryLoader sqlQueryLoader, 
                                                     EntityManager entityManager,
                                                     DynamicQueryProperties properties) {
        log.info("Initializing DynamicQueryExecutor with logging enabled: {}", 
                properties.getLogging().isEnabled());
        return new DynamicQueryExecutor(sqlQueryLoader, entityManager, properties);
    }
    
    /**
     * Configuración adicional para desarrollo y debugging.
     */
    @Configuration
    @ConditionalOnProperty(value = "app.dynamic-query.logging.enabled", havingValue = "true")
    public static class LoggingConfiguration {
        
        public LoggingConfiguration() {
            log.info("Dynamic Query logging is enabled. This may impact performance in production.");
        }
    }
    
    /**
     * Configuración para modo estricto de validación.
     */
    @Configuration
    @ConditionalOnProperty(value = "app.dynamic-query.validation.strict-mode", havingValue = "true")
    public static class StrictValidationConfiguration {
        
        public StrictValidationConfiguration() {
            log.info("Dynamic Query strict validation mode is enabled. All queries will be validated at startup.");
        }
    }
}
