package com.latam.springdynamicquery.util

import com.latam.springdynamicquery.util.SqlUtils
import com.latam.springdynamicquery.util.SqlUtils.SqlType
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests para SqlUtils - Utilidades de procesamiento SQL.
 */
class SqlUtilsSpec extends Specification {

    // ==================== Tests de cleanSql ====================
    
    def "should clean SQL by removing comments"() {
        given:
        def sqlWithComments = """
            SELECT * FROM users -- This is a comment
            WHERE active = true /* Multi-line
            comment here */
            AND id > 0
        """
        
        when:
        def cleaned = SqlUtils.cleanSql(sqlWithComments)
        
        then:
        !cleaned.contains("--")
        !cleaned.contains("/*")
        !cleaned.contains("*/")
        cleaned.contains("SELECT")
        cleaned.contains("WHERE active = true")
    }
    
    def "should normalize whitespace in SQL"() {
        given:
        def messySql = "SELECT   *\n\n\nFROM\tusers\r\n  WHERE   id = 1"
        
        when:
        def cleaned = SqlUtils.cleanSql(messySql)
        
        then:
        cleaned == "SELECT * FROM users WHERE id = 1"
    }
    
    def "should handle empty or null SQL"() {
        expect:
        SqlUtils.cleanSql(null) == ""
        SqlUtils.cleanSql("") == ""
        SqlUtils.cleanSql("   ") == ""
    }

    // ==================== Tests de analyzeWhereClause ====================
    
    def "should detect existing WHERE clause"() {
        given:
        def sql = "SELECT * FROM users WHERE active = true"
        
        when:
        def whereInfo = SqlUtils.analyzeWhereClause(sql)
        
        then:
        whereInfo.exists()
        whereInfo.wherePrefix() == " AND "
    }
    
    def "should detect missing WHERE clause"() {
        given:
        def sql = "SELECT * FROM users"
        
        when:
        def whereInfo = SqlUtils.analyzeWhereClause(sql)
        
        then:
        !whereInfo.exists()
        whereInfo.wherePrefix() == " WHERE "
    }
    
    def "should handle WHERE inside subquery"() {
        given:
        def sql = "SELECT * FROM (SELECT * FROM users WHERE id > 0) u"
        
        when:
        def whereInfo = SqlUtils.analyzeWhereClause(sql)
        
        then:
        !whereInfo.exists() // WHERE dentro de paréntesis no cuenta
        whereInfo.wherePrefix() == " WHERE "
    }
    
    def "should handle null or empty SQL"() {
        expect:
        !SqlUtils.analyzeWhereClause(null).exists()
        !SqlUtils.analyzeWhereClause("").exists()
    }

    // ==================== Tests de areParenthesesBalanced ====================
    
    @Unroll
    def "should validate parentheses balance: #description"() {
        expect:
        SqlUtils.areParenthesesBalanced(sql) == expected
        
        where:
        description           | sql                                    | expected
        "balanced"           | "SELECT * FROM users WHERE (id > 0)"   | true
        "nested balanced"    | "((a) AND (b))"                        | true
        "unbalanced open"    | "SELECT * FROM (users"                 | false
        "unbalanced close"   | "SELECT * FROM users)"                 | false
        "multiple balanced"  | "(a) AND (b) OR (c)"                   | true
        "empty"              | ""                                     | true
        "no parentheses"     | "SELECT * FROM users"                  | true
    }

    // ==================== Tests de extractParameterNames ====================
    
    @Unroll
    def "should extract parameter names: #description"() {
        expect:
        SqlUtils.extractParameterNames(sql) == expectedParams as Set
        
        where:
        description          | sql                                     | expectedParams
        "single param"       | "WHERE id = :userId"                    | ["userId"]
        "multiple params"    | "WHERE name = :name AND age = :age"     | ["name", "age"]
        "IN clause"          | "WHERE id IN (:ids)"                    | ["ids"]
        "no params"          | "WHERE active = true"                   | []
        "repeated param"     | "WHERE :id = :id"                       | ["id"]
        "mixed"              | "VALUES (:name, :age, :name)"           | ["name", "age"]
        "with underscore"    | "WHERE user_id = :user_id"              | ["user_id"]
        "camelCase"          | "WHERE userId = :userId"                | ["userId"]
        "null or empty"      | null                                    | []
    }
    
    def "should handle complex parameter patterns"() {
        given:
        def sql = """
            SELECT * FROM users 
            WHERE name = :name 
            AND age BETWEEN :minAge AND :maxAge
            AND department_id IN (:departmentIds)
            AND created_date > :startDate
        """
        
        when:
        def params = SqlUtils.extractParameterNames(sql)
        
        then:
        params.size() == 5
        params.containsAll(["name", "minAge", "maxAge", "departmentIds", "startDate"])
    }

    // ==================== Tests de countParameters ====================
    
    def "should count unique parameters"() {
        given:
        def sql = "WHERE :id = :id AND :name = :name OR :id = :id"
        
        when:
        def count = SqlUtils.countParameters(sql)
        
        then:
        count == 2 // Solo 'id' y 'name', aunque se repitan
    }

    // ==================== Tests de isReadOnlyQuery ====================
    
    @Unroll
    def "should detect read-only queries: #description"() {
        expect:
        SqlUtils.isReadOnlyQuery(sql) == expected
        
        where:
        description    | sql                                  | expected
        "SELECT"       | "SELECT * FROM users"                | true
        "WITH CTE"     | "WITH cte AS (...) SELECT * FROM cte"| true
        "select lower" | "select * from users"                | true
        "INSERT"       | "INSERT INTO users VALUES (1)"       | false
        "UPDATE"       | "UPDATE users SET name = 'test'"     | false
        "DELETE"       | "DELETE FROM users WHERE id = 1"     | false
        "null"         | null                                 | false
        "empty"        | ""                                   | false
    }

    // ==================== Tests de getSqlType ====================
    
    @Unroll
    def "should identify SQL type: #description"() {
        expect:
        SqlUtils.getSqlType(sql) == expectedType
        
        where:
        description       | sql                                    | expectedType
        "SELECT"          | "SELECT * FROM users"                  | SqlType.SELECT
        "WITH"            | "WITH cte AS (...) SELECT * FROM cte"  | SqlType.SELECT
        "INSERT"          | "INSERT INTO users VALUES (1)"         | SqlType.INSERT
        "UPDATE"          | "UPDATE users SET name = 'test'"       | SqlType.UPDATE
        "DELETE"          | "DELETE FROM users WHERE id = 1"       | SqlType.DELETE
        "CREATE TABLE"    | "CREATE TABLE users (id INT)"          | SqlType.DDL
        "DROP TABLE"      | "DROP TABLE users"                     | SqlType.DDL
        "ALTER TABLE"     | "ALTER TABLE users ADD COLUMN age INT" | SqlType.DDL
        "CALL procedure"  | "CALL sp_update_users()"               | SqlType.PROCEDURE
        "EXEC procedure"  | "EXEC sp_update_users"                 | SqlType.PROCEDURE
        "lowercase"       | "select * from users"                  | SqlType.SELECT
        "unknown"         | "SOMETHING WEIRD"                      | SqlType.UNKNOWN
        "null"            | null                                   | SqlType.UNKNOWN
        "empty"           | ""                                     | SqlType.UNKNOWN
    }

    // ==================== Tests de requiresResultType ====================
    
    @Unroll
    def "should determine if SQL type requires resultType: #type"() {
        expect:
        SqlUtils.requiresResultType(type) == expected
        
        where:
        type              | expected
        SqlType.SELECT    | true
        SqlType.PROCEDURE | true
        SqlType.INSERT    | false
        SqlType.UPDATE    | false
        SqlType.DELETE    | false
        SqlType.DDL       | false
        SqlType.UNKNOWN   | false
    }

    // ==================== Tests de extractTableName ====================
    
    @Unroll
    def "should extract table name from SQL: #description"() {
        expect:
        SqlUtils.extractTableName(sql) == expectedTable
        
        where:
        description        | sql                                      | expectedTable
        "SELECT simple"    | "SELECT * FROM users"                    | "USERS"
        "SELECT with JOIN" | "SELECT * FROM users u JOIN orders o"    | "USERS"
        "INSERT"           | "INSERT INTO products VALUES (1)"        | "PRODUCTS"
        "UPDATE"           | "UPDATE customers SET name = 'test'"     | "CUSTOMERS"
        "DELETE"           | "DELETE FROM orders WHERE id = 1"        | "ORDERS"
        "schema.table"     | "SELECT * FROM myschema.users"           | "MYSCHEMA.USERS"
        "lowercase"        | "select * from users"                    | "USERS"
        "with WHERE"       | "SELECT * FROM users WHERE active = true"| "USERS"
        "complex"          | "SELECT u.* FROM users u"                | "USERS"
        "unknown"          | "SOMETHING WEIRD"                        | "unknown"
        "null"             | null                                     | "unknown"
        "empty"            | ""                                       | "unknown"
    }
    
    def "should extract table name with whitespace variations"() {
        expect:
        SqlUtils.extractTableName(sql).toUpperCase() == "USERS"
        
        where:
        sql << [
            "SELECT * FROM   users",
            "SELECT * FROM\tusers",
            "SELECT * FROM\n\nusers",
            "  SELECT * FROM users  "
        ]
    }

    // ==================== Tests de Edge Cases ====================
    
    def "should handle SQL with multiple statements"() {
        given:
        def sql = "SELECT * FROM users; DELETE FROM orders;"
        
        when:
        def sqlType = SqlUtils.getSqlType(sql)
        def tableName = SqlUtils.extractTableName(sql)
        
        then:
        sqlType == SqlType.SELECT // Primera sentencia
        tableName == "USERS" // Primera tabla
    }
    
    def "should handle SQL with comments mixed with code"() {
        given:
        def sql = """
            -- Get all users
            SELECT * FROM users /* with active flag */
            WHERE active = true -- only active
        """
        
        when:
        def cleaned = SqlUtils.cleanSql(sql)
        def tableName = SqlUtils.extractTableName(cleaned)
        
        then:
        tableName == "USERS"
        !cleaned.contains("--")
        !cleaned.contains("/*")
    }
    
    def "should handle SQL with nested subqueries"() {
        given:
        def sql = """
            SELECT * FROM (
                SELECT * FROM (
                    SELECT * FROM users WHERE id > 0
                ) inner_query
            ) outer_query
        """
        
        when:
        def whereInfo = SqlUtils.analyzeWhereClause(sql)
        
        then:
        !whereInfo.exists() // WHERE está dentro de paréntesis
    }
    
    def "should handle SQL with special characters in strings"() {
        given:
        def sql = "SELECT * FROM users WHERE name = 'O''Brien' AND email LIKE '%@test.com'"
        
        when:
        def params = SqlUtils.extractParameterNames(sql)
        
        then:
        params.isEmpty() // No hay parámetros nombrados
    }
    
    def "should handle SQL with CASE statements"() {
        given:
        def sql = """
            SELECT 
                CASE 
                    WHEN age > :maxAge THEN 'old'
                    WHEN age > :minAge THEN 'young'
                    ELSE 'unknown'
                END as age_category
            FROM users
        """
        
        when:
        def params = SqlUtils.extractParameterNames(sql)
        
        then:
        params.size() == 2
        params.containsAll(["maxAge", "minAge"])
    }
    
    def "should handle SQL with window functions"() {
        given:
        def sql = """
            SELECT 
                ROW_NUMBER() OVER (PARTITION BY department_id ORDER BY salary DESC) as rank
            FROM users
            WHERE department_id = :deptId
        """
        
        when:
        def params = SqlUtils.extractParameterNames(sql)
        
        then:
        params == ["deptId"] as Set
    }
    
    def "should handle extremely long SQL"() {
        given:
        def longSql = "SELECT * FROM users WHERE " + 
            (1..100).collect { "field$it = :param$it" }.join(" OR ")
        
        when:
        def params = SqlUtils.extractParameterNames(longSql)
        
        then:
        params.size() == 100
        params.contains("param1")
        params.contains("param100")
    }
}
