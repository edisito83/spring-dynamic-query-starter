package com.latam.springdynamicquery.repository

import com.latam.springdynamicquery.TestApplication
import com.latam.springdynamicquery.core.criteria.FilterCriteria
import com.latam.springdynamicquery.testmodel.User
import com.latam.springdynamicquery.testrepository.TestUserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

/**
 * Tests de integración para DynamicRepository usando Spock.
 */
@DataJpaTest
@ContextConfiguration(classes = [TestApplication])
class DynamicRepositoryIntegrationSpec extends Specification {
    
    @Autowired
    TestUserRepository userRepository
    
    def "should have all JpaRepository methods available"() {
        when:
        def allUsers = userRepository.findAll()
        
        then:
        allUsers != null
        allUsers.size() >= 0
    }
    
    def "should have custom query methods available"() {
        when:
        def activeUsers = userRepository.findByActiveTrue()
        
        then:
        activeUsers != null
        activeUsers.every { it.active == true }
    }
    
    def "should execute named queries from YAML"() {
        when:
        def user = userRepository.findUserByIdFromYaml(1L)
        
        then:
        user.isPresent()
        user.get().id == 1L
    }
    
    def "should execute dynamic queries with filters"() {
        given:
        def filters = [
            "name": FilterCriteria.whenNotEmpty("u.name LIKE :name", "%Test%"),
            "minAge": FilterCriteria.whenNumericPositive("u.age >= :minAge", 20)
        ]
        
        when:
        def users = userRepository.executeNamedQuery("UserMapper.findUsersWithDynamicFilters", filters)
        
        then:
        users != null
        users.every { it.name.contains("Test") && it.age >= 20 }
    }
    
    def "should handle empty filters gracefully"() {
        when:
        def users = userRepository.executeNamedQuery("UserMapper.findActiveUsers", [:])
        
        then:
        users != null
        users.every { it.active == true }
    }
    
    def "should execute complex search with multiple criteria"() {
        when:
        def users = userRepository.findUsersWithComplexSearch("Test", null, [1L, 2L], 25)
        
        then:
        users != null
        users.every { 
            it.name.contains("Test") && 
            it.age >= 25 && 
            it.departmentId in [1L, 2L]
        }
    }
    
    def "should execute aggregation queries"() {
        when:
        def count = userRepository.countUsersByStatus("ACTIVE")
        
        then:
        count != null
        count >= 0
    }
    
    def "should save and find users"() {
        given:
        def newUser = new User(
            name: "Integration Test User",
            email: "integration@test.com",
            age: 30,
            status: "ACTIVE",
            active: true
        )
        
        when:
        def savedUser = userRepository.save(newUser)
        def foundUser = userRepository.findUserByIdFromYaml(savedUser.id)
        
        then:
        savedUser.id != null
        foundUser.isPresent()
        foundUser.get().name == "Integration Test User"
        foundUser.get().email == "integration@test.com"
    }
}
