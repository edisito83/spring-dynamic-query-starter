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
        ValidationUtils.isValidValue(value) == expected
        
        where:
        value                    | expected
        null                     | false
        ""                       | false
        "   "                    | false
        "\t\n"                   | false
        "hello"                  | true
        "test"                   | true
        []                       | false
        [1, 2, 3]               | true
        new Object[0]            | false
        new String[]{"test"}     | true
        123                      | true
        true                     | true
    }
    
    @Unroll
    def "isValidNumericValue should return #expected for #value"() {
        expect:
        ValidationUtils.isValidNumericValue(value) == expected
        
        where:
        value                           | expected
        null                            | false
        0                               | false
        -1                              | false
        1                               | true
        100                             | true
        0L                              | false
        -5L                             | false
        10L                             | true
        0.0                             | false
        -1.5                            | false
        1.5                             | true
        0.0f                            | false
        1.0f                            | true
        new BigDecimal("0")             | false
        new BigDecimal("-1")            | false
        new BigDecimal("1.5")           | true
        "not a number"                  | false
    }
    
    def "isValidString should work correctly"() {
        expect:
        ValidationUtils.isValidString(value) == expected
        
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
        ValidationUtils.isValidCollection(value) == expected
        
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