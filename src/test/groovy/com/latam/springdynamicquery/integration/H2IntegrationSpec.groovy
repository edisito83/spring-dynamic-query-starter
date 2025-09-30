package com.latam.springdynamicquery.integration

import com.latam.springdynamicquery.TestApplication
import com.latam.springdynamicquery.core.criteria.FilterCriteria
import com.latam.springdynamicquery.testmodel.GuiaDespacho
import com.latam.springdynamicquery.testrepository.TestGuiaRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification

/**
 * Tests de integración con H2 usando Spock.
 */
@DataJpaTest
@ContextConfiguration(classes = [TestApplication])
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
])
class H2IntegrationSpec extends Specification {
    
    @Autowired
    TestGuiaRepository guiaRepository
    
    def "should work with H2 database"() {
        when:
        def guias = guiaRepository.findAll()
        
        then:
        guias != null
    }
    
    def "should execute complex OR + EXISTS query in H2"() {
        when:
        def guias = guiaRepository.findGuiasByNumero("12345")
        
        then:
        guias != null
        guias.size() > 0
        // Should find guías that match either numero_guia or AWB numero
    }
    
    def "should handle advanced search in H2"() {
        given:
        def fechaInicio = java.time.LocalDate.of(2024, 1, 1)
        def fechaFin = java.time.LocalDate.of(2024, 12, 31)
        
        when:
        def guias = guiaRepository.findGuiasAdvanced("TEST", "CREADA", 3L, fechaInicio, fechaFin)
        
        then:
        guias != null
        // Should execute the complex query successfully
    }
    
    def "should validate SQL execution with H2 dialect"() {
        given:
        def filters = [
            "numeroGuia": FilterCriteria.whenNotEmpty(
                "(gd.numero_guia LIKE :numeroGuia OR EXISTS(SELECT 1 FROM numeroawb NA WHERE NA.guia_id = gd.id AND NA.numero LIKE :numeroGuia))",
                "%COMPLEX%"
            )
        ]
        
        when:
        def guias = guiaRepository.executeNamedQuery("GuiaMapper.findGuiasWithAWBSearch", filters)
        
        then:
        guias != null
        // Complex query should execute without SQL syntax errors
    }
}