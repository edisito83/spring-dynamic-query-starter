package com.latam.springdynamicquery.integration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

import com.latam.springdynamicquery.TestApplication
import com.latam.springdynamicquery.core.criteria.FilterCriteria
import com.latam.springdynamicquery.testrepository.TestUserRepository

import spock.lang.Specification

/**
 * Tests de cumplimiento y validación para el sistema de consultas dinámicas.
 */
@SpringBootTest(classes = [TestApplication])
class ComplianceSpec extends Specification {
    
    @Autowired
    TestUserRepository userRepository
    
    def "should ensure all standard JPA Repository methods work"() {
        when:
        def allMethods = [
            { -> userRepository.findAll() },
            { -> userRepository.count() },
            { -> userRepository.existsById(1L) },
            { -> userRepository.findById(1L) }
        ]
        
        then:
        allMethods.each { method ->
            def result = method.call()
            assert result != null
        }
    }
    
    def "should ensure all custom query methods work"() {
        when:
        def customMethods = [
            { -> userRepository.findByActiveTrue() },
            { -> userRepository.findByNameContaining("Test") },
            { -> userRepository.findByEmail("test@example.com") }
        ]
        
        then:
        customMethods.each { method ->
            def result = method.call()
            assert result != null
        }
    }
    
    def "should ensure all @Query methods work"() {
        when:
        def queryMethods = [
            { -> userRepository.findActiveUsersByNameJpql("Test") },
            { -> userRepository.findUsersByStatusNative("ACTIVE") }
        ]
        
        then:
        queryMethods.each { method ->
            def result = method.call()
            assert result != null
        }
    }
    
    def "should ensure all dynamic query methods work"() {
        when:
        def dynamicMethods = [
            { -> userRepository.findUserByIdFromYaml(1L) },
            { -> userRepository.findAllActiveUsersFromYaml() },
            { -> userRepository.findUsersWithComplexSearch("Test", null, [1L], 25) },
            { -> userRepository.countUsersByStatus("ACTIVE") }
        ]
        
        then:
        dynamicMethods.each { method ->
            def result = method.call()
            assert result != null
        }
    }
    
    def "should validate parameter binding security"() {
        given:
        def testCases = [
            "normal value",
            "'; DROP TABLE users; --",
            "1' OR '1'='1",
            "<script>alert('xss')</script>",
            "NULL",
            "",
            "very long string that might cause buffer overflow " * 100
        ]
        
        expect:
        testCases.each { testValue ->
            def filters = ["name": FilterCriteria.when("u.name = :name", testValue)]
            def result = userRepository.executeNamedQuery("UserMapper.findUsersWithDynamicFilters", filters)
            assert result != null
        }
    }
    
    def "should validate filter criteria behavior consistency"() {
        given:
        def testCases = [
            [value: null, shouldApply: false],
            [value: "", shouldApply: false],
            [value: "   ", shouldApply: false],
            [value: "valid", shouldApply: true],
            [value: [], shouldApply: false],
            [value: [1, 2], shouldApply: true]
        ]
        
        expect:
        testCases.each { testCase ->
            def criteria = FilterCriteria.when("test = :test", testCase.value)
            assert criteria.shouldApply() == testCase.shouldApply
        }
    }
    
    def "should ensure thread safety"() {
        given:
        def numberOfThreads = 20
        def results = Collections.synchronizedList([])
        def errors = Collections.synchronizedList([])
        
        when:
        def threads = (1..numberOfThreads).collect { threadNum ->
            Thread.start {
                try {
                    (1..10).each { iteration ->
                        def users = userRepository.findByActiveTrue()
                        results.add("Thread ${threadNum}, Iteration ${iteration}: ${users.size()}")
                    }
                } catch (Exception e) {
                    errors.add("Thread ${threadNum}: ${e.message}")
                }
            }
        }
        
        threads.each { it.join(10000) } // Wait up to 10 seconds
        
        then:
        errors.isEmpty()
        results.size() == numberOfThreads * 10
    }
}