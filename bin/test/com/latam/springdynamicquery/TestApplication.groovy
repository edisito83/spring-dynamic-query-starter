package com.latam.springdynamicquery

import com.latam.springdynamicquery.autoconfigure.DynamicQueryProperties
import com.latam.springdynamicquery.repository.BaseDynamicRepository

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * Aplicación de prueba para tests.
 */
@SpringBootApplication
@EntityScan(basePackages = "com.latam.springdynamicquery.testmodel")
class TestApplication {
    
    static void main(String[] args) {
        SpringApplication.run(TestApplication, args)
    }
    
    @TestConfiguration
    static class TestConfig {
        
        @Bean
        @Primary
        DynamicQueryProperties testDynamicQueryProperties() {
            def properties = new DynamicQueryProperties()
            properties.enabled = true
            properties.preloadEnabled = true
            properties.scanPackages = ["sql/test/*.yml"]
            properties.logging.enabled = true
            properties.logging.logParameters = true
            properties.validation.strictMode = false
			properties.repository.customFactoryEnabled = true
			properties.repository.basePackages = ["com.latam.springdynamicquery.testrepository"]
            return properties
        }
    }
}