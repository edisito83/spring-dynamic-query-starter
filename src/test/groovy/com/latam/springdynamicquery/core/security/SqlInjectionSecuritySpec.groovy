package com.latam.springdynamicquery.core.security;

import com.latam.springdynamicquery.core.criteria.FilterCriteria
import com.latam.springdynamicquery.exception.InvalidQueryException

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests de integración para seguridad y prevención de SQL Injection.
 * Enfocado en casos de uso completos con FilterCriteria.
 * 
 * Para tests unitarios específicos ver:
 * - SqlUtilsSpec: Utilidades de procesamiento SQL (extractParameterNames, cleanSql, etc.)
 * - QueryValidatorSpec: Validaciones de seguridad (validateFilterCriteriaSafety, etc.)
 */
class SqlInjectionSecuritySpec extends Specification {

    // ==================== Tests de Escenarios Completos de Seguridad ====================
    
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
        filter.securityValidated
        // En la BD buscará literal "admin' OR '1'='1", no ejecutará la inyección
    }

    // ==================== Tests de Rechazo de Patrones Inseguros ====================
    
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
        filter.securityValidated
    }
    
    def "should allow FilterCriteria with multiple parameters"() {
        when: "usando múltiples parámetros nombrados"
        def filter = FilterCriteria.when(
            "u.age BETWEEN :minAge AND :maxAge",
            [minAge: 18, maxAge: 65]
        )
        
        then: "debe ser válido"
        filter != null
        filter.securityValidated
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
        filter.securityValidated
        filter.value == ids
    }

    // ==================== Tests del Modo Unsafe ====================
    
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

    // ==================== Tests de Integración Completos ====================
    
    def "integration: complete workflow with safe FilterCriteria"() {
        given: "múltiples filtros seguros para una query compleja"
        def maliciousName = "'; DROP TABLE users; --"
        def maliciousEmail = "admin' OR '1'='1"
        
        when: "creando filtros seguros con parámetros"
        def filters = [
            name: FilterCriteria.when("u.name = :name", maliciousName),
            email: FilterCriteria.when("u.email = :email", maliciousEmail),
            age: FilterCriteria.whenNumericPositive("u.age >= :age", 18),
            status: FilterCriteria.when("u.status IN (:status)", ["ACTIVE", "PENDING"])
        ]
        
        then: "todos los filtros se crean correctamente"
        filters.size() == 4
        filters.every { key, value -> value.securityValidated }
        
        and: "los valores maliciosos están parametrizados de forma segura"
        filters.name.value == maliciousName
        filters.email.value == maliciousEmail
    }
    
    def "integration: complex search with OR and EXISTS safely"() {
        given: "query compleja con OR y EXISTS usando parámetros"
        def searchTerm = "admin' OR '1'='1"
        
        when: "construyendo filtros complejos de forma segura"
        def filters = [
            search: FilterCriteria.when(
                "(u.name LIKE :search OR u.email LIKE :search)",
                "%${searchTerm}%"
            ),
            hasOrders: FilterCriteria.when(
                "EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id AND o.status = :orderStatus)",
                "COMPLETED"
            )
        ]
        
        then: "filtros se crean correctamente"
        filters.size() == 2
        filters.search.securityValidated
        filters.hasOrders.securityValidated
        
        and: "el término de búsqueda malicioso está parametrizado"
        filters.search.value.contains(searchTerm)
    }
    
    def "integration: should prevent SQL injection in repository execution"() {
        given: "un repository y un intento de inyección"
        def maliciousInput = "'; DROP TABLE users; --"
        
        when: "ejecutando query con filtro SEGURO"
        def filters = [
            name: FilterCriteria.when("u.name = :name", maliciousInput)
        ]
        // En ejecución real: userRepository.executeNamedQuery("UserMapper.findUsers", filters)
        
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
    
    def "integration: combining multiple filter types safely"() {
        when: "combinando diferentes tipos de filtros"
        def filter1 = FilterCriteria.when("u.name = :name", "test")
        def filter2 = FilterCriteria.whenNotEmpty("u.email = :email", "test@example.com")
        def filter3 = FilterCriteria.whenNumericPositive("u.age >= :age", 25)
        def filter4 = FilterCriteria.whenNotNull("u.department_id = :deptId", 5L)
        def combined = filter1.and(filter2)
        
        then: "todos los filtros están validados"
        filter1.securityValidated
        filter2.securityValidated
        filter3.securityValidated
        filter4.securityValidated
        combined.securityValidated
    }
    
    def "integration: should prevent multiple attack vectors simultaneously"() {
        given: "múltiples vectores de ataque diferentes"
        def attacks = [
            dropTable: "'; DROP TABLE users; --",
            union: "' UNION SELECT * FROM passwords --",
            or11: "' OR '1'='1",
            comment: "admin' --",
            delete: "'; DELETE FROM users WHERE '1'='1"
        ]
        
        when: "intentando crear filtros con cada ataque"
        def results = attacks.collectEntries { key, attack ->
            try {
                def filter = FilterCriteria.when("u.field = :param", attack)
                [(key): "SAFE - ${filter.securityValidated}"]
            } catch (InvalidQueryException e) {
                [(key): "BLOCKED - ${e.message.contains('injection')}"]
            }
        }
        
        then: "todos los ataques están bloqueados o parametrizados de forma segura"
        results.every { key, value -> 
            value.startsWith("SAFE - true") || value.startsWith("BLOCKED - true")
        }
    }
    
    def "integration: real-world scenario with user input"() {
        given: "simulando input de usuario en un formulario de búsqueda"
        def userInputName = "O'Brien" // Nombre legítimo con apóstrofe
        def userInputEmail = "user@company.com"
        def userInputStatus = ["ACTIVE", "PENDING"]
        def maliciousInput = "'; DELETE FROM users; --"
        
        when: "creando filtros con input mixto (legítimo y malicioso)"
        def filters = [
            name: FilterCriteria.whenNotEmpty("u.name LIKE :name", "%${userInputName}%"),
            email: FilterCriteria.whenNotEmpty("u.email = :email", userInputEmail),
            status: FilterCriteria.whenNotEmptyCollection("u.status IN (:status)", userInputStatus),
            notes: FilterCriteria.whenNotEmpty("u.notes LIKE :notes", "%${maliciousInput}%")
        ]
        
        then: "todos los filtros son seguros"
        filters.every { key, value -> value.securityValidated }
        
        and: "el input legítimo con apóstrofe está manejado correctamente"
        filters.name.value.contains("O'Brien")
        
        and: "el input malicioso está parametrizado de forma segura"
        filters.notes.value.contains(maliciousInput)
    }
    
    def "integration: unsafe mode for legitimate complex SQL"() {
        given: "SQL complejo pero confiable que podría fallar validación estándar"
        def trustedComplexSql = """
            EXISTS (
                SELECT 1 FROM orders o
                WHERE o.user_id = u.id
                AND o.status = 'COMPLETED'
                AND o.total > 1000
                AND o.created_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)
            )
        """
        
        when: "usando modo unsafe para SQL confiable"
        def filter = FilterCriteria.unsafe(trustedComplexSql, null)
        
        then: "el filtro se crea pero está marcado como no validado"
        filter != null
        !filter.securityValidated
        filter.sqlFragment.contains("EXISTS")
        filter.sqlFragment.contains("DATE_SUB")
    }

    // ==================== Tests de Edge Cases de Integración ====================
    
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
        filter.securityValidated
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
        filter.securityValidated
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
