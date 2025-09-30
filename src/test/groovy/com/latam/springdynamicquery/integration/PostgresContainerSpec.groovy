package com.latam.springdynamicquery.integration

import com.latam.springdynamicquery.TestApplication
import com.latam.springdynamicquery.core.criteria.FilterCriteria
import com.latam.springdynamicquery.testrepository.TestUserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

/**
 * Tests de integración con PostgreSQL usando Testcontainers y Spock.
 */
@SpringBootTest(classes = [TestApplication])
@Testcontainers
class PostgresContainerSpec extends Specification {
    
    @Shared
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("sql/schema/postgres-schema.sql")
    
    @Autowired
    TestUserRepository userRepository
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl)
        registry.add("spring.datasource.username", postgres::getUsername)
        registry.add("spring.datasource.password", postgres::getPassword)
        registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
    }
    
    def "should work with PostgreSQL container"() {
        given:
        postgres.isRunning()
        
        when:
        def users = userRepository.findAll()
        
        then:
        users != null
        println "PostgreSQL container is running on: ${postgres.jdbcUrl}"
    }
    
    def "should execute complex queries in PostgreSQL"() {
        given:
        def filters = [
            "name": FilterCriteria.whenNotEmpty("u.name ILIKE :name", "%test%"), // PostgreSQL ILIKE
            "minAge": FilterCriteria.whenNumericPositive("u.age >= :minAge", 20)
        ]
        
        when:
        def users = userRepository.executeNamedQuery("UserMapper.findUsersWithDynamicFilters", filters)
        
        then:
        users != null
        // PostgreSQL specific features should work
    }
    
    def "should handle PostgreSQL specific data types and functions"() {
        given:
        def filters = [
            "searchTerm": FilterCriteria.whenNotEmpty(
                "u.name ILIKE :searchTerm OR u.email ILIKE :searchTerm", 
                "%integration%"
            ),
            "dateRange": FilterCriteria.always(
                "u.created_date >= NOW() - INTERVAL '1 month'" // PostgreSQL interval
            )
        ]
        
        when:
        def users = userRepository.executeNamedQuery("UserMapper.findUsersWithComplexConditions", filters)
        
        then:
        users != null
        // PostgreSQL interval and ILIKE should work correctly
    }
    
    def "should test concurrent access with PostgreSQL"() {
        given:
        def numberOfThreads = 5
        def results = []
        
        when:
        def threads = (1..numberOfThreads).collect { threadNum ->
            Thread.start {
                def users = userRepository.findByActiveTrue()
                synchronized(results) {
                    results.add("Thread ${threadNum}: ${users.size()} users")
                }
            }
        }
        
        threads.each { it.join() }
        
        then:
        results.size() == numberOfThreads
        results.every { it.contains("users") }
    }
}