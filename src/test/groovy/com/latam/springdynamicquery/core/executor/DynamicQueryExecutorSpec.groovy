package com.latam.springdynamicquery.core.executor

import com.latam.springdynamicquery.TestApplication
import com.latam.springdynamicquery.autoconfigure.DynamicQueryProperties
import com.latam.springdynamicquery.core.criteria.FilterCriteria
import com.latam.springdynamicquery.core.loader.SqlQueryLoader
import com.latam.springdynamicquery.testmodel.User
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import spock.lang.Specification

/**
 * Tests específicos para DynamicQueryExecutor cubriendo métodos sin cobertura.
 * Usa la estructura y datos existentes del proyecto.
 */
@SpringBootTest(classes = [TestApplication])
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=LEGACY",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.sql.init.mode=always",
    "app.dynamic-query.logging.enabled=true",
    "app.dynamic-query.logging.log-parameters=true"
])
@Sql(scripts = [
    "classpath:sql/schema/h2-schema.sql",
    "classpath:sql/data/test-data.sql"
], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DynamicQueryExecutorSpec extends Specification {

    @Autowired
    DynamicQueryExecutor executor

    @Autowired
    EntityManager entityManager

    @Autowired
    SqlQueryLoader queryLoader

    @Autowired
    DynamicQueryProperties properties

    // ==================== executeNamedQueryWithParameters (auto-detect resultType) Tests ====================

    def "should execute named query with parameters and auto-detect result class"() {
        given: "query name with auto result type detection"
        def queryName = "UserMapper.findUserById"
        def parameters = [id: 1L]

        when: "executing with auto-detect"
        def users = executor.executeNamedQueryWithParameters(queryName, parameters)

        then: "should return user list"
        users != null
        users.size() == 1
        users[0].id == 1L
        users[0].email == "john.doe@company.com"
    }

    def "should throw InvalidQueryException when metadata not found"() {
        given:
        def queryName = "NonExistent.query"
        def parameters = [id: 1L]

        when:
        executor.executeNamedQueryWithParameters(queryName, parameters)

        then:
        def ex = thrown(com.latam.springdynamicquery.exception.InvalidQueryException)
        ex.message.contains("Query metadata not found")
    }

    // ==================== executeRawQuery Tests ====================

    def "should execute raw query returning Object arrays"() {
        given: "a query that returns raw data"
        def queryName = "UserMapper.getUsersWithDepartmentInfo"
        def filters = [:]

        when:
        def results = executor.executeRawQuery(queryName, filters)

        then: "should return Object[] arrays"
        results != null
        results.size() > 0
        results[0] instanceof Object[]
    }

    def "should execute raw query with filters"() {
        given:
        def queryName = "UserMapper.getUsersWithDepartmentInfo"
        def filters = [
            active: FilterCriteria.when("u.active = :active", true)
        ]

        when:
        def results = executor.executeRawQuery(queryName, filters)

        then:
        results != null
        // Verificar que todos los usuarios retornados sean activos
        results.each { row ->
            def active = row[3] // assuming active is at index 3
            assert active == true
        }
    }

    def "should execute raw query with null filters"() {
        given:
        def queryName = "UserMapper.countAllUsers"

        when:
        def results = executor.executeRawQuery(queryName, null)

        then:
        results != null
        results.size() == 1
        results[0][0] == 7L // 7 users in test-data.sql
    }

    // ==================== executeDMLWithParameters Tests ====================

    def "should execute DML INSERT with parameters"() {
        given: "parameters for new user"
        def params = [
            name: "DML Test User",
            email: "dml.test@company.com",
            age: 40,
            status: "ACTIVE",
            active: true,
            departmentId: 1L,
            salary: 60000.00,
            employeeNumber: "DML001"
        ]

        when: "inserting new user"
        def rowsAffected = executor.executeDMLWithParameters("UserMapper.insertUser", params)

        then: "should insert one row"
        rowsAffected == 1

        and: "user should exist in database"
        def users = entityManager.createQuery(
            "SELECT u FROM User u WHERE u.email = :email", User.class
        ).setParameter("email", "dml.test@company.com").getResultList()
        users.size() == 1
        users[0].name == "DML Test User"
    }

    def "should execute DML UPDATE with parameters"() {
        given: "update params"
        def params = [
            id: 7L, // Test User from test-data.sql
            salary: 55000.00
        ]

        when: "updating user salary"
        def rowsAffected = executor.executeDMLWithParameters("UserMapper.updateUserSalary", params)

        then: "should update one row"
        rowsAffected == 1

        and: "salary should be updated"
        entityManager.clear()
        def user = entityManager.find(User.class, 7L)
        user.salary == 55000.00
    }

    def "should execute DML DELETE with parameters"() {
        given: "create a user to delete"
        def testUser = new User(
            name: "To Delete",
            email: "delete@test.com",
            age: 99,
            active: true
        )
        entityManager.persist(testUser)
        entityManager.flush()
        def userId = testUser.id

        when: "deleting user"
        def rowsAffected = executor.executeDMLWithParameters("UserMapper.deleteUser", [id: userId])

        then: "should delete one row"
        rowsAffected == 1

        and: "user should not exist"
        entityManager.clear()
        entityManager.find(User.class, userId) == null
    }

    def "should execute DML with null parameters"() {
        when:
        def rowsAffected = executor.executeDMLWithParameters("UserMapper.deactivateInactiveUsers", null)

        then:
        rowsAffected >= 0
        noExceptionThrown()
    }

    def "should execute DML with empty parameters map"() {
        when:
        def rowsAffected = executor.executeDMLWithParameters("UserMapper.deactivateInactiveUsers", [:])

        then:
        rowsAffected >= 0
        noExceptionThrown()
    }

    // ==================== executeDMLWithFilters Tests ====================

    def "should execute DML UPDATE with dynamic filters"() {
        given: "filters for updating users"
        def filters = [
            departmentId: FilterCriteria.when("department_id = :departmentId", 1L),
            minAge: FilterCriteria.whenNumericPositive("age < :minAge", 30)
        ]

        when: "updating salary for IT department users under 30"
        def rowsAffected = executor.executeDMLWithFilters("UserMapper.updateSalaryByFilters", filters)

        then: "should update matching users"
        rowsAffected > 0
    }

    def "should execute DML DELETE with dynamic filters"() {
        given: "create test users to delete"
        [1, 2, 3].each { i ->
            def testUser = new User(
                name: "Temp User $i",
                email: "temp$i@test.com",
                age: 20,
                active: false,
                departmentId: 1L
            )
            entityManager.persist(testUser)
        }
        entityManager.flush()

        and: "filters for deletion"
        def filters = [
            inactive: FilterCriteria.when("active = :active", false),
            email: FilterCriteria.whenValidString("email LIKE :email", "temp%@test.com")
        ]

        when: "deleting filtered users"
        def rowsAffected = executor.executeDMLWithFilters("UserMapper.deleteUsersByFilters", filters)

        then: "should delete temp users"
        rowsAffected == 3
    }

    def "should execute DML with filters where some don't apply"() {
        given:
        def filters = [
            active: FilterCriteria.whenNotNull("active = :active", true),
            name: FilterCriteria.whenNotEmpty("name LIKE :name", null) // Won't apply
        ]

        when:
        def rowsAffected = executor.executeDMLWithFilters("UserMapper.updateSalaryByFilters", filters)

        then:
        rowsAffected >= 0
        noExceptionThrown()
    }

    def "should execute DML with empty filters"() {
        when:
        def rowsAffected = executor.executeDMLWithFilters("UserMapper.updateSalaryByFilters", [:])

        then:
        rowsAffected >= 0
        noExceptionThrown()
    }

    def "should execute DML with null filters"() {
        when:
        def rowsAffected = executor.executeDMLWithFilters("UserMapper.updateSalaryByFilters", null)

        then:
        rowsAffected >= 0
        noExceptionThrown()
    }

    // ==================== execute Query(String, Class, Map) Tests ====================

    def "should execute direct query with result class"() {
        given:
        def baseSql = "SELECT * FROM users WHERE active = true"
        def filters = [:]

        when:
        def users = executor.executeQuery(baseSql, User.class, filters)

        then:
        users != null
        users.size() > 0
        users.every { it.active == true }
    }

    def "should execute direct query with filters"() {
        given:
        def baseSql = "SELECT * FROM users u WHERE 1=1"
        def filters = [
            departmentId: FilterCriteria.when("u.department_id = :departmentId", 1L)
        ]

        when:
        def users = executor.executeQuery(baseSql, User.class, filters)

        then:
        users != null
        users.every { it.departmentId == 1L }
    }

    // ==================== convertSingleResult Tests ====================

    def "should convert BigDecimal to Long"() {
        given:
        def queryName = "UserMapper.countAllUsers"

        when:
        def count = executor.executeSingleResult(queryName, [:], Long.class)

        then:
        count != null
        count instanceof Long
        count == 10L
    }

    def "should convert to Integer"() {
        given:
        def queryName = "UserMapper.getMaxAge"

        when:
        def maxAge = executor.executeSingleResult(queryName, [:], Integer.class)

        then:
        maxAge != null
        maxAge instanceof Integer
        maxAge == 35 // Bob Johnson es el mayor con 35 años
    }

    def "should convert to Double for aggregations"() {
        given:
        def queryName = "UserMapper.getAverageSalary"

        when:
        def avgSalary = executor.executeSingleResult(queryName, [:], Double.class)

        then:
        avgSalary != null
        avgSalary instanceof Double
        avgSalary > 0
    }

    def "should handle null result"() {
        given:
        def queryName = "UserMapper.findUserById"
        def filters = [id: FilterCriteria.when("id = :id", 9999L)]

        when:
        def result = executor.executeSingleResult(queryName, filters, User.class)

        then:
        result == null
    }

    // ==================== Edge Cases and Error Handling ====================

    def "should handle parameters with special characters"() {
        given:
        def params = [
            name: "Test's User",
            email: "test+special@example.com"
        ]

        when:
        executor.executeDMLWithParameters("UserMapper.insertUser", params)

        then:
        noExceptionThrown()
    }

    def "should handle very long parameter values"() {
        given:
        def longName = "A" * 99  // Just under 100 char limit
        def params = [
            name: longName,
            email: "long@test.com"
        ]

        when:
        executor.executeDMLWithParameters("UserMapper.insertUser", params)

        then:
        noExceptionThrown()
    }

    def "should handle DML affecting zero rows"() {
        given:
        def params = [id: 99999L] // Non-existent ID

        when:
        def rowsAffected = executor.executeDMLWithParameters("UserMapper.deleteUser", params)

        then:
        rowsAffected == 0
        noExceptionThrown()
    }

    def "should handle complex filter combinations"() {
        given:
        def filters = [
            active: FilterCriteria.when("u.active = :active", true),
            minAge: FilterCriteria.whenNumericPositive("u.age >= :minAge", 25),
            departmentId: FilterCriteria.when("u.department_id = :departmentId", 1L),
            email: FilterCriteria.whenValidString("u.email LIKE :email", "%company.com")
        ]

        when:
        def users = executor.executeNamedQuery("UserMapper.findUsersWithDynamicFilters", User.class, filters)

        then:
        users != null
        users.every {
            it.active && it.age >= 25 && it.departmentId == 1L && it.email.contains("company.com")
        }
    }

    def "should execute with logging enabled"() {
        given: "logging is enabled in properties"
        properties.getLogging().isEnabled()
        properties.getLogging().isLogParameters()

        when:
        def users = executor.executeNamedQueryWithParameters("UserMapper.findActiveUsers", [:])

        then:
        users != null
        noExceptionThrown()
    }

    // ==================== Integration with existing data ====================

    def "should work with existing test data"() {
        when: "querying existing users"
        def users = executor.executeNamedQueryWithParameters("UserMapper.findActiveUsers", [:])

        then: "should find active users from test-data.sql"
        users.size() == 6 // All except Alice Brown (inactive)
        users*.name.containsAll([
            "John Doe",
            "Jane Smith",
            "Bob Johnson",
            "Charlie Wilson",
            "Diana Davis",
            "Test User"
        ])
    }

    def "should query users by existing department"() {
        given:
        def filters = [
            departmentId: FilterCriteria.when("u.department_id = :departmentId", 1L) // IT department
        ]

        when:
        def users = executor.executeNamedQuery("UserMapper.findUsersWithDynamicFilters", User.class, filters)

        then: "should find IT department users"
        users.size() == 4 // John, Jane, Charlie, Test User
        users.every { it.departmentId == 1L }
    }

    def "should aggregate existing order data"() {
        given:
        def queryName = "UserMapper.countUsersWithOrders"

        when:
        def count = executor.executeSingleResult(queryName, [:], Long.class)

        then: "should count users that have orders from test-data.sql"
        count > 0
    }
}
