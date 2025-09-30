package com.latam.springdynamicquery.core.criteria

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests para FilterCriteria usando Spock.
 */
class FilterCriteriaSpec extends Specification {
    
    def "should create basic FilterCriteria"() {
        given:
        def criteria = new FilterCriteria("u.name = :name", "John")
        
        expect:
        criteria.sqlFragment == "u.name = :name"
        criteria.value == "John"
        criteria.connector == "AND"
        criteria.shouldApply()
    }
    
    def "when factory method should work correctly"() {
        given:
        def criteria = FilterCriteria.when("u.age > :age", 25)
        
        expect:
        criteria.sqlFragment == "u.age > :age"
        criteria.value == 25
        criteria.shouldApply()
    }
    
    @Unroll
    def "whenNotEmpty should return #expected for value '#value'"() {
        given:
        def criteria = FilterCriteria.whenNotEmpty("u.name = :name", value)
        
        expect:
        criteria.shouldApply() == expected
        
        where:
        value       | expected
        null        | false
        ""          | false
        "   "       | false
        "John"      | true
        []          | false
        [1, 2]      | true
    }
    
    def "whenNotNull should work correctly"() {
        expect:
        FilterCriteria.whenNotNull("u.id = :id", value).shouldApply() == expected
        
        where:
        value   | expected
        null    | false
        0       | true
        ""      | true
        "test"  | true
    }
    
    def "whenNumericPositive should work correctly"() {
        expect:
        FilterCriteria.whenNumericPositive("u.age >= :minAge", value).shouldApply() == expected
		
		where:
		value << [1, 0, -1]
		expected << [true, false, false]
    }
}