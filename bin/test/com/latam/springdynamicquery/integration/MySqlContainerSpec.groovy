package com.latam.springdynamicquery.integration

import com.latam.springdynamicquery.TestApplication
import com.latam.springdynamicquery.core.criteria.FilterCriteria
import com.latam.springdynamicquery.testrepository.TestGuiaRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

/**
 * Tests de integración con MySQL usando Testcontainers y Spock.
 */
@SpringBootTest(classes = [TestApplication])
@Testcontainers
class MySqlContainerSpec extends Specification {
    
    @Shared
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("sql/schema/mysql-schema.sql")
    
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
        println "MySQL container is running on: ${mysql.jdbcUrl}"
    }
    
    def "should execute your specific OR + EXISTS case in MySQL"() {
        when:
        def guias = guiaRepository.findGuiasByNumero("12345")
        
        then:
        guias != null
        guias.size() > 0
        // Should find guías where numero_guia contains "12345" OR AWB numero contains "12345"
    }
    
    def "should handle MySQL specific functions and syntax"() {
        given:
        def filters = [
            "numeroGuia": FilterCriteria.whenNotEmpty(
                "(gd.numero_guia LIKE :numeroGuia OR EXISTS(SELECT 1 FROM numeroawb NA WHERE NA.guia_id = gd.id AND NA.numero LIKE :numeroGuia))",
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
        // MySQL specific date functions should work
    }
    
    def "should test MySQL transaction handling"() {
        given:
        def originalCount = guiaRepository.count()
        
        when:
        // This should work within a transaction
        def guias = guiaRepository.findGuiasByNumero("TEST")
        def newCount = guiaRepository.count()
        
        then:
        guias != null
        newCount == originalCount // No new records added, just querying
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
        // MySQL collation should work correctly
    }
}