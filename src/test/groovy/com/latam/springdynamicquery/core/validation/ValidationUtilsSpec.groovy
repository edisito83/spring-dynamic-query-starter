package com.latam.springdynamicquery.core.validation

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests para ValidationUtils usando Spock.
 */
class ValidationUtilsSpec extends Specification {
    
    @Unroll
    def "isValidValue should return #expected for #value"() {
        expect:
        ParameterValidator.isValidValue(value) == expected
        
        where:
        value                    | expected
        null                     | false
        ""                       | false
        "   "                    | false
        "\t\n"                   | false
        "hello"                  | true
        "test"                   | true
        []                       | false
        [1, 2, 3]                | true
        new Object[0]            | false
        ["test"]                 | true
        123                      | true
        true                     | true
    }
    
    @Unroll
    def "isValidNumericValue should return #expected for #value"() {
        expect:
        ParameterValidator.isValidNumericValue(value) == expected
        
        where:
        value                           | expected
        null                            | false
        0                               | true
        -1                              | true
        1                               | true
        100                             | true
        0L                              | true
        -5L                             | true
        10L                             | true
        0.0                             | true
        -1.5                            | true
        1.5                             | true
        0.0f                            | true
        1.0f                            | true
        new BigDecimal("0")             | true
        new BigDecimal("-1")            | true
        new BigDecimal("1.5")           | true
        "not a number"                  | false
    }
    
    def "isValidString should work correctly"() {
        expect:
        ParameterValidator.isValidString(value) == expected
        
        where:
        value       | expected
        null        | false
        ""          | false
        "   "       | false
        "hello"     | true
        "test"      | true
    }
    
    def "isValidCollection should work correctly"() {
        expect:
        ParameterValidator.isValidCollection(value) == expected
        
        where:
        value           | expected
        null            | false
        []              | false
        [1]             | true
        [1, 2, 3]       | true
        new HashSet()   | false
        [1, 2] as Set   | true
    }
}