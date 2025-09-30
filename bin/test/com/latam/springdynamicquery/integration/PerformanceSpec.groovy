package com.latam.springdynamicquery.integration

import com.latam.springdynamicquery.TestApplication
import com.latam.springdynamicquery.core.criteria.FilterCriteria
import com.latam.springdynamicquery.core.loader.SqlQueryLoader
import com.latam.springdynamicquery.testrepository.TestUserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification
import spock.lang.Timeout

/**
 * Tests de rendimiento para el sistema de consultas dinámicas.
 */
@SpringBootTest(classes = [TestApplication])
@TestPropertySource(properties = [
    "app.dynamic-query.cache.enabled=true",
    "app.dynamic-query.logging.enabled=false"
])
class PerformanceSpec extends Specification {
    
    @Autowired
    TestUserRepository userRepository
    
    @Autowired
    SqlQueryLoader sqlQueryLoader
    
    @Timeout(10) // 10 seconds max
    def "should load queries quickly at startup"() {
        when:
        def stats = sqlQueryLoader.getStats()
        
        then:
        stats.totalQueries > 0
        stats.totalNamespaces > 0
        // Should complete within timeout
    }
    
    @Timeout(5)
    def "should execute simple queries quickly"() {
        when:
        def startTime = System.currentTimeMillis()
        def users = userRepository.findByActiveTrue()
        def endTime = System.currentTimeMillis()
        def executionTime = endTime - startTime
        
        then:
        users != null
        executionTime < 1000 // Should complete in less than 1 second
    }
    
    @Timeout(5)
    def "should execute dynamic queries quickly"() {
        given:
        def filters = [
            "name": FilterCriteria.whenNotEmpty("u.name LIKE :name", "%Test%"),
            "active": FilterCriteria.always("u.active = true")
        ]
        
        when:
        def startTime = System.currentTimeMillis()
        def users = userRepository.executeNamedQuery("UserMapper.findUsersWithDynamicFilters", filters)
        def endTime = System.currentTimeMillis()
        def executionTime = endTime - startTime
        
        then:
        users != null
        executionTime < 2000 // Should complete in less than 2 seconds
    }
    
    def "should handle multiple concurrent requests"() {
        given:
        def numberOfThreads = 10
        def results = Collections.synchronizedList([])
        def errors = Collections.synchronizedList([])
        
        when:
        def threads = (1..numberOfThreads).collect { threadNum ->
            Thread.start {
                try {
                    def users = userRepository.findByActiveTrue()
                    results.add("Thread ${threadNum}: ${users.size()} users")
                } catch (Exception e) {
                    errors.add("Thread ${threadNum}: ${e.message}")
                }
            }
        }
        
        threads.each { it.join(5000) } // Wait max 5 seconds per thread
        
        then:
        errors.isEmpty()
        results.size() == numberOfThreads
        results.every { it.contains("users") }
    }
    
    def "should cache query lookups efficiently"() {
        given:
        def queryName = "UserMapper.findActiveUsers"
        
        when:
        def startTime = System.currentTimeMillis()
        
        // Execute multiple lookups
        (1..100).each {
            sqlQueryLoader.getQuery(queryName)
        }
        
        def endTime = System.currentTimeMillis()
        def totalTime = endTime - startTime
        
        then:
        totalTime < 100 // 100 lookups should complete in less than 100ms
    }
    
    def "should handle large filter maps efficiently"() {
        given:
        def largeFilterMap = [:]
        (1..50).each { i ->
            largeFilterMap["filter${i}"] = FilterCriteria.when("u.field${i} = :filter${i}", null)
        }
        
        when:
        def startTime = System.currentTimeMillis()
        def users = userRepository.executeNamedQuery("UserMapper.findActiveUsers", largeFilterMap)
        def endTime = System.currentTimeMillis()
        def executionTime = endTime - startTime
        
        then:
        users != null
        executionTime < 3000 // Should handle large filter map efficiently
    }
}