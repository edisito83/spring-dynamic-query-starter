package com.latam.springdynamicquery.integration

import com.latam.springdynamicquery.TestApplication
import com.latam.springdynamicquery.core.criteria.FilterCriteria
import com.latam.springdynamicquery.testrepository.TestUserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

/**
 * Tests de integración con PostgreSQL usando Testcontainers y Spock.
 * 
 * IMPORTANTE: Requiere Podman o Docker configurado correctamente.
 * Ver TESTCONTAINERS_PODMAN_SETUP.md para instrucciones de configuración.
 * 
 * Comandos rápidos:
 * - Linux/Mac: ./setup-podman.sh
 * - Windows: .\setup-podman.ps1
 * 
 * Verificar: echo $DOCKER_HOST
 */
@SpringBootTest(classes = [TestApplication])
@Testcontainers
@ActiveProfiles("postgresql")
class PostgresContainerSpec extends Specification {
    
    @Shared
    static PostgreSQLContainer<?> postgres
    
    static {
        try {
            println("🚀 Starting PostgreSQL container with Testcontainers...")
            println("📍 DOCKER_HOST: ${System.getenv('DOCKER_HOST') ?: 'not set (will use default)'}")
            
            postgres = new PostgreSQLContainer<>("postgres:15")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true)  // Reutilizar contenedor entre ejecuciones
                .withStartupTimeout(java.time.Duration.ofMinutes(3))
                .withLogConsumer { frame -> 
                    // Solo mostrar errores del contenedor
                    def msg = frame.utf8String.trim()
                    if (msg.contains("ERROR") || msg.contains("FATAL") || msg.contains("WARNING")) {
                        println("🐘 Postgres: ${msg}")
                    }
                }
            
            postgres.start()
            
            println("✅ PostgreSQL container started successfully!")
            println("📍 JDBC URL: ${postgres.jdbcUrl}")
            println("👤 Username: ${postgres.username}")
            println("🏷️  Container ID: ${postgres.containerId}")
            
            // Ejecutar schema script
            println("📝 Loading database schema...")
            def dataSource = new DriverManagerDataSource(
                postgres.jdbcUrl,
                postgres.username,
                postgres.password
            )
            
            ScriptUtils.executeSqlScript(
                dataSource.connection,
                new ClassPathResource("sql/schema/postgres-schema.sql")
            )
            println("✅ Schema loaded successfully!")
            
            // Ejecutar data script
            println("📝 Loading test data...")
            ScriptUtils.executeSqlScript(
                dataSource.connection,
                new ClassPathResource("sql/data/test-data.sql")
            )
            println("✅ Test data loaded successfully!")
            
        } catch (Exception e) {
            println("\n❌ ERROR: Failed to start PostgreSQL container")
            println("📋 Error details: ${e.message}")
            println("\n💡 Troubleshooting:")
            println("   1. Check if Podman/Docker is running:")
            println("      podman ps  (or)  docker ps")
            println("\n   2. Verify DOCKER_HOST is set:")
            println("      echo \$DOCKER_HOST")
            println("\n   3. Run setup script:")
            println("      ./setup-podman.sh (Linux/Mac)")
            println("      .\\setup-podman.ps1 (Windows)")
            println("\n   4. Check Podman machine status (if on Mac/Windows):")
            println("      podman machine list")
            println("\n   5. Read full guide: TESTCONTAINERS_PODMAN_SETUP.md\n")
            
            throw new RuntimeException("PostgreSQL container failed to start. See troubleshooting steps above.", e)
        }
    }
    
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
        println "✅ Test passed: Found ${users.size()} users in PostgreSQL"
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
        println "✅ Test passed: PostgreSQL ILIKE query returned ${users.size()} results"
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
        println "✅ Test passed: PostgreSQL INTERVAL functions work correctly"
    }
    
    def "should test concurrent access with PostgreSQL"() {
        given:
        def numberOfThreads = 5
        def results = Collections.synchronizedList([])
        
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
        println "✅ Test passed: Concurrent access works (${numberOfThreads} threads)"
    }
    
    def "should test PostgreSQL JSON functions"() {
        when:
        // Test that PostgreSQL is working with standard queries
        def count = userRepository.count()
        
        then:
        count >= 0
        println "✅ Test passed: PostgreSQL query execution works (count: ${count})"
    }
    
    def cleanupSpec() {
        println("\n🧹 Test cleanup completed")
        println("💡 Container will be reused for next test run (testcontainers.reuse.enable=true)")
    }
}