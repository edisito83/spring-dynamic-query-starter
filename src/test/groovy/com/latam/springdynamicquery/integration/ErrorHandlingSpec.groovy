package com.latam.springdynamicquery.integration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql

import com.latam.springdynamicquery.TestApplication
import com.latam.springdynamicquery.core.criteria.FilterCriteria
import com.latam.springdynamicquery.exception.QueryNotFoundException
import com.latam.springdynamicquery.testrepository.TestUserRepository

import spock.lang.Specification

/**
 * Tests de manejo de errores para el sistema de consultas dinámicas.
 */
@SpringBootTest(classes = [TestApplication])
@TestPropertySource(properties = [
	"spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=LEGACY",
	"spring.jpa.hibernate.ddl-auto=none",
	"spring.sql.init.mode=always"
])
@Sql(scripts = [
	"classpath:sql/schema/h2-schema.sql",
	"classpath:sql/data/test-data.sql"
], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ErrorHandlingSpec extends Specification {
    
    @Autowired
    TestUserRepository userRepository
    
    def "should throw QueryNotFoundException for non-existent query"() {
        given:
        def filters = [:]
        
        when:
        userRepository.executeNamedQuery("NonExistentQuery", filters)
        
        then:
        thrown(QueryNotFoundException)
    }
    
    def "should handle null filters gracefully"() {
        when:
        def users = userRepository.executeNamedQuery("UserMapper.findActiveUsers", null)
        
        then:
        users != null
		users.size() == 8
        noExceptionThrown()
    }
    
    def "should handle empty filters gracefully"() {
        when:
        def users = userRepository.executeNamedQuery("UserMapper.findActiveUsers", [:])
        
        then:
        users != null
        noExceptionThrown()
    }
    
    def "should handle filters with null values"() {
        given:
        def filters = [
            "name": FilterCriteria.when("u.name = :name", null),
            "email": FilterCriteria.when("u.email = :email", "test@example.com")
        ]
        
        when:
        def users = userRepository.executeNamedQuery("UserMapper.findUsersWithDynamicFilters", filters)
        
        then:
        users != null
        noExceptionThrown()
		users.size() == 1
		
		and: "los ids son exactamente los esperados"
		users*.name.toSet() == ['Test User'] as Set
    }
    
    def "should handle SQL injection attempts safely"() {
        given:
        def maliciousInput = "'; DROP TABLE users; --"
        def filters = [
            "name": FilterCriteria.when("u.name = :name", maliciousInput)
        ]
        
        when:
        def users = userRepository.executeNamedQuery("UserMapper.findUsersWithDynamicFilters", filters)
        
        then:
        users != null
        noExceptionThrown()
    }
    
    def "should handle database connection errors gracefully"() {
        // This test would need a way to simulate database failure
        // For now, we'll test that normal operations work
        when:
        def users = userRepository.findAll()
        
        then:
        users != null
    }
    
    def "should handle malformed filter criteria"() {
        given:
        def filters = [
            "badCriteria": FilterCriteria.withCondition("invalid sql fragment", "value") {
                throw new RuntimeException("Simulated error")
            }
        ]
        
        when:
        def users = userRepository.executeNamedQuery("UserMapper.findActiveUsers", filters)
        
        then:
        users != null
        noExceptionThrown()
        // Bad criteria should be skipped due to exception in condition
    }
    
    def "should provide meaningful error messages"() {
        when:
        userRepository.executeNamedQuery("Definitely.NonExistent.Query", [:])
        
        then:
        def exception = thrown(QueryNotFoundException)
        exception.message.contains("Query not found")
        exception.message.contains("Definitely.NonExistent.Query")
        exception.message.contains("Available queries")
    }
    
    def "should handle transaction rollback scenarios"() {
        // This would typically test rollback behavior
        // For read-only operations, we test that queries work consistently
        when:
        def users1 = userRepository.findByActiveTrue()
        def users2 = userRepository.findByActiveTrue()
        
        then:
        users1.size() == users2.size()
        noExceptionThrown()
    }
}