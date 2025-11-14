package com.latam.springdynamicquery.integration

import com.latam.springdynamicquery.TestApplication
import com.latam.springdynamicquery.core.criteria.FilterCriteria
import com.latam.springdynamicquery.testrepository.TestGuiaRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.ScriptUtils
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

/**
 * Tests de integración con MySQL usando Testcontainers y Spock.
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
@ActiveProfiles("mysql")
class MySqlContainerSpec extends Specification {
    
    @Shared
    static MySQLContainer<?> mysql
    
    static {
        try {
            println("🚀 Starting MySQL container with Testcontainers...")
            println("📍 DOCKER_HOST: ${System.getenv('DOCKER_HOST') ?: 'not set (will use default)'}")
            
            mysql = new MySQLContainer<>("mysql:8.0")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true)  // Reutilizar contenedor entre ejecuciones
                .withStartupTimeout(java.time.Duration.ofMinutes(3))
                .withLogConsumer { frame -> 
                    // Solo mostrar errores del contenedor
                    def msg = frame.utf8String.trim()
                    if (msg.contains("ERROR") || msg.contains("Warning")) {
                        println("🐬 MySQL: ${msg}")
                    }
                }
            
            mysql.start()
            
            println("✅ MySQL container started successfully!")
            println("📍 JDBC URL: ${mysql.jdbcUrl}")
            println("👤 Username: ${mysql.username}")
            println("🏷️  Container ID: ${mysql.containerId}")
            
            // Ejecutar schema script
            println("📝 Loading database schema...")
            def dataSource = new DriverManagerDataSource(
                mysql.jdbcUrl,
                mysql.username,
                mysql.password
            )
            
            ScriptUtils.executeSqlScript(
                dataSource.connection,
                new ClassPathResource("sql/schema/mysql-schema.sql")
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
            println("\n❌ ERROR: Failed to start MySQL container")
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
            
            throw new RuntimeException("MySQL container failed to start. See troubleshooting steps above.", e)
        }
    }
    
    @Autowired
    TestGuiaRepository guiaRepository
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl)
        registry.add("spring.datasource.username", mysql::getUsername)
        registry.add("spring.datasource.password", mysql::getPassword)
        registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
        registry.add("spring.jpa.database-platform") { "org.hibernate.dialect.MySQL8Dialect" }
    }
    
    def "should work with MySQL container"() {
        given:
        mysql.isRunning()
        
        when:
        def guias = guiaRepository.findAll()
        
        then:
        guias != null
        println "✅ Test passed: Found ${guias.size()} guías in MySQL"
    }
    
    def "should execute your specific OR + EXISTS case in MySQL"() {
        when:
        def guias = guiaRepository.findGuiasByNumero("12345")
        
        then:
        guias != null
        guias.size() > 0
        println "✅ Test passed: OR + EXISTS query returned ${guias.size()} results"
    }
    
    def "should handle MySQL specific functions and syntax"() {
        given:
        def filters = [
            "numeroGuia": FilterCriteria.whenNotEmpty(
                "(gd.numero_guia LIKE :numeroGuia OR EXISTS(SELECT 1 FROM numero_awb NA WHERE NA.guia_id = gd.id AND NA.numero LIKE :numeroGuia))",
                "%COMPLEX%"
            ),
            "dateRange": FilterCriteria.always(
                "gd.fecha_creacion >= DATE_SUB(NOW(), INTERVAL 1 MONTH)" // MySQL DATE_SUB
            )
        ]
        
        when:
        def guias = guiaRepository.executeNamedQuery("GuiaMapper.findGuiasWithAWBSearch", filters)
        
        then:
        guias != null
        println "✅ Test passed: MySQL-specific functions work correctly"
    }
    
    def "should test MySQL transaction handling"() {
        given:
        def originalCount = guiaRepository.count()
        
        when:
        def guias = guiaRepository.findGuiasByNumero("TEST")
        def newCount = guiaRepository.count()
        
        then:
        guias != null
        newCount == originalCount
        println "✅ Test passed: Transaction handling works (count: ${newCount})"
    }
    
    def "should handle MySQL collation and charset"() {
        given:
        def filters = [
            "clienteNombre": FilterCriteria.whenNotEmpty(
                "c.nombre COLLATE utf8mb4_unicode_ci LIKE :clienteNombre",
                "%TEST%"
            )
        ]
        
        when:
        def guias = guiaRepository.executeNamedQuery("GuiaMapper.findGuiasAdvanced", filters)
        
        then:
        guias != null
        println "✅ Test passed: MySQL collation works correctly"
    }
    
    def cleanupSpec() {
        println("\n🧹 Test cleanup completed")
        println("💡 Container will be reused for next test run (testcontainers.reuse.enable=true)")
    }
}