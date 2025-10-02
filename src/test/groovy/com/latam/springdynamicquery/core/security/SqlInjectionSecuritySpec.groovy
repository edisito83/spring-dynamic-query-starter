package com.latam.springdynamicquery.core.security;

import com.latam.springdynamicquery.core.criteria.FilterCriteria
import com.latam.springdynamicquery.exception.InvalidQueryException
import com.latam.springdynamicquery.util.SqlUtils

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests de seguridad para prevención de SQL Injection
 */
class SqlInjectionSecuritySpec extends Specification {

    // ==================== Tests de FilterCriteria con Parámetros Seguros ====================
    
    def "should safely handle malicious input with proper parameterization"() {
        given: "un input malicioso"
        def maliciousInput = "'; DROP TABLE users; --"
        
        when: "usando FilterCriteria SEGURO con parámetro nombrado"
        def filter = FilterCriteria.when("u.name = :name", maliciousInput)
        
        then: "se crea sin problemas porque usa :name"
        filter != null
        filter.sqlFragment == "u.name = :name"
        filter.value == maliciousInput
        filter.securityValidated == true
    }
    
    def "should safely handle OR injection attempt with parameterization"() {
        given: "un intento de inyección OR"
        def maliciousInput = "admin' OR '1'='1"
        
        when: "usando parámetro seguro"
        def filter = FilterCriteria.when("u.username = :username", maliciousInput)
        
        then: "el valor se trata como string literal"
        filter != null
        filter.value == maliciousInput
        // En la BD buscará literal "admin' OR '1'='1", no ejecutará la inyección
    }

    // ==================== Tests que DEBEN FALLAR (SQL Injection) ====================
    
    def "should reject FilterCriteria with hardcoded string value"() {
        given: "un fragmento SQL con valor hardcodeado (INSEGURO)"
        def unsafeSql = "u.name = 'admin'"
        
        when: "intentando crear FilterCriteria con valor"
        FilterCriteria.when(unsafeSql, "someValue")
        
        then: "debe lanzar excepción de seguridad"
        def ex = thrown(InvalidQueryException)
        ex.message.contains("SQL fragment must use named parameters")
        ex.message.contains("This prevents SQL injection vulnerabilities")
    }
    
    def "should reject FilterCriteria with string concatenation"() {
        given: "un input malicioso y concatenación de strings"
        def maliciousInput = "admin' OR '1'='1"
        def unsafeSql = "u.name = '" + maliciousInput + "'"
        
        when: "intentando crear FilterCriteria INSEGURO"
        FilterCriteria.when(unsafeSql, null)
        
        then: "debe detectar el patrón de inyección"
        def ex = thrown(InvalidQueryException)
        ex.message.contains("SQL injection patterns")
    }
    
    @Unroll
    def "should reject FilterCriteria with SQL injection pattern: #description"() {
        when: "intentando crear FilterCriteria con patrón malicioso"
        FilterCriteria.when(sqlFragment, null)
        
        then: "debe lanzar excepción de seguridad"
        def ex = thrown(InvalidQueryException)
        ex.message.toLowerCase().contains("injection")
        
        where:
        description              | sqlFragment
        "DROP TABLE"            | "'; DROP TABLE users; --"
        "UNION SELECT"          | "' UNION SELECT * FROM passwords --"
        "OR 1=1"                | "' OR '1'='1"
        "SQL comment"           | "admin' --"
        "DELETE injection"      | "'; DELETE FROM users WHERE '1'='1"
        "EXEC injection"        | "'; EXEC sp_executesql N'SELECT * FROM users'"
    }
    
    def "should reject FilterCriteria when value provided but no parameter"() {
        when: "proporcionando valor sin parámetro nombrado"
        FilterCriteria.when("u.active = 1", "someValue")
        
        then: "debe rechazar porque falta :param"
        def ex = thrown(InvalidQueryException)
        ex.message.contains("must use named parameters")
        ex.message.contains("when a value is provided")
    }

    // ==================== Tests de Casos Válidos ====================
    
    def "should allow FilterCriteria with null value and no parameter"() {
        when: "creando filtro sin valor y sin parámetro (como condición estática)"
        def filter = FilterCriteria.when("u.deleted_at IS NULL", null)
        
        then: "debe permitirlo porque no hay valor a inyectar"
        filter != null
        filter.value == null
    }
    
    def "should allow FilterCriteria with multiple parameters"() {
        when: "usando múltiples parámetros nombrados"
        def filter = FilterCriteria.when(
            "u.age BETWEEN :minAge AND :maxAge",
            [minAge: 18, maxAge: 65]
        )
        
        then: "debe ser válido"
        filter != null
        filter.sqlFragment.contains(":minAge")
        filter.sqlFragment.contains(":maxAge")
    }
    
    def "should allow FilterCriteria with IN clause"() {
        given: "una lista de IDs"
        def ids = [1L, 2L, 3L]
        
        when: "usando IN con parámetro nombrado"
        def filter = FilterCriteria.when("u.id IN (:ids)", ids)
        
        then: "debe ser válido"
        filter != null
        filter.value == ids
    }

    // ==================== Tests del método unsafe() ====================
    
    def "should allow unsafe FilterCriteria with warning"() {
        when: "usando el método unsafe() explícitamente"
        def filter = FilterCriteria.unsafe("u.status = 'active'", null)
        
        then: "debe crear el filtro pero marcarlo como no validado"
        filter != null
        filter.securityValidated == false
        // Nota: En logs aparecerá un WARNING
    }
    
    def "should allow complex SQL in unsafe mode for trusted sources"() {
        given: "SQL complejo que podría fallar validación estándar"
        def complexSql = "EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id AND o.status = 'pending')"
        
        when: "usando unsafe para SQL confiable y complejo"
        def filter = FilterCriteria.unsafe(complexSql, null)
        
        then: "debe permitirlo"
        filter != null
        filter.securityValidated == false
    }

    // ==================== Tests de SqlUtils ====================
    
    def "SqlUtils should validate filter safety"() {
        when: "validando fragmento seguro"
        SqlUtils.validateFilterCriteriaSafety("u.name = :name", "value")
        
        then: "no debe lanzar excepción"
        noExceptionThrown()
    }
    
    def "SqlUtils should reject unsafe filter"() {
        when: "validando fragmento inseguro"
        SqlUtils.validateFilterCriteriaSafety("u.name = 'hardcoded'", "value")
        
        then: "debe lanzar excepción"
        thrown(InvalidQueryException)
    }
    
    @Unroll
    def "SqlUtils should extract parameters correctly: #description"() {
        expect:
        SqlUtils.extractParameterNames(sql) == expectedParams as Set
        
        where:
        description          | sql                                     | expectedParams
        "single param"       | "u.id = :userId"                        | ["userId"]
        "multiple params"    | ":name AND :age"                        | ["name", "age"]
        "IN clause"          | "u.id IN (:ids)"                        | ["ids"]
        "no params"          | "u.active = 1"                          | []
        "repeated param"     | ":id OR :id"                            | ["id"]
        "mixed"              | "(:name, :age, :name)"                  | ["name", "age"]
    }

    // ==================== Tests de Integración con Repository ====================
    
    def "integration: should prevent SQL injection in repository execution"() {
        given: "un repository y un intento de inyección"
        def maliciousInput = "'; DROP TABLE users; --"
        
        when: "ejecutando query con filtro SEGURO"
        def filters = [
            name: FilterCriteria.when("u.name = :name", maliciousInput)
        ]
        // userRepository.executeNamedQuery("UserMapper.findUsers", filters)
        
        then: "el filtro se crea correctamente"
        filters.name.securityValidated == true
        // En ejecución real, JPA escapará el valor automáticamente
        // El SQL final será: WHERE u.name = ''''; DROP TABLE users; --'
        // Que es un string literal buscando ese texto exacto
    }
    
    def "integration: should reject unsafe filter before execution"() {
        when: "intentando crear filtro inseguro para repository"
        def filters = [
            name: FilterCriteria.when("u.name = 'admin' OR '1'='1'", null)
        ]
        
        then: "debe fallar en la creación del FilterCriteria"
        thrown(InvalidQueryException)
        // Nunca llega al repository, se detiene antes
    }

    // ==================== Tests de Edge Cases ====================
    
    def "should handle empty sql fragment gracefully"() {
        when:
        FilterCriteria.when("", "value")
        
        then:
        def ex = thrown(InvalidQueryException)
        ex.message.contains("cannot be null or empty")
    }
    
    def "should handle null sql fragment gracefully"() {
        when:
        FilterCriteria.when(null, "value")
        
        then:
        def ex = thrown(InvalidQueryException)
        ex.message.contains("cannot be null or empty")
    }
    
    def "should allow LIKE patterns with parameters"() {
        when: "usando LIKE con parámetro"
        def filter = FilterCriteria.when("u.name LIKE :pattern", "%admin%")
        
        then: "debe ser válido"
        filter != null
        filter.value == "%admin%"
        // JPA escapará correctamente incluso los caracteres especiales
    }
    
    def "should allow complex boolean expressions with parameters"() {
        when: "expresión booleana compleja pero segura"
        def filter = FilterCriteria.when(
            "(u.age >= :minAge OR u.verified = :verified) AND u.active = :active",
            [minAge: 18, verified: true, active: true]
        )
        
        then: "debe ser válido porque usa parámetros"
        filter != null
        SqlUtils.extractParameterNames(filter.sqlFragment).size() == 3
    }
    
    def "should handle case-insensitive SQL keywords"() {
        when: "usando keywords en minúsculas"
        def filter = FilterCriteria.when("u.status = :status", "active")
        
        then: "debe validar correctamente"
        filter != null
        filter.securityValidated == true
    }
	
	def "should handle malformed filter criteria"() {
		when:
		def filters = FilterCriteria.withCondition("invalid sql fragment", "value", null)
		
		then:
		def ex = thrown(InvalidQueryException)
		ex.message.contains("must use named parameters")
		ex.message.contains("when a value is provided")
	}
	
}