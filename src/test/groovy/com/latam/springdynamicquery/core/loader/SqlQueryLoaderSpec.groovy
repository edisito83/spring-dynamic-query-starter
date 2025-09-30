package com.latam.springdynamicquery.core.loader

import com.fasterxml.jackson.databind.ObjectMapper
import com.latam.springdynamicquery.TestApplication
import com.latam.springdynamicquery.autoconfigure.DynamicQueryProperties
import com.latam.springdynamicquery.exception.InvalidQueryException
import com.latam.springdynamicquery.exception.QueryNotFoundException

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import spock.lang.Specification

//@SpringBootTest(classes = [TestApplication])
class SqlQueryLoaderSpec extends Specification {

    def "should load queries from valid YAML file"() {
        given:
        def props = new DynamicQueryProperties()
        props.setPreloadEnabled(true)
        props.setScanPackages(["sql/loader/UserMapper.yml"])
        def loader = new SqlQueryLoader(props)

        when:
        loader.afterPropertiesSet()

        then:
        def sql = loader.getQuery("UserMapper.findUserById")
        sql.toUpperCase().startsWith("SELECT")
        loader.hasQuery("UserMapper.findActiveUsers")
        loader.getAvailableQueryKeys().contains("UserMapper.findUserById")
        loader.getAvailableQueriesByNamespace().containsKey("UserMapper")
        loader.getQueriesForNamespace("UserMapper").contains("findUserById")
    }

    def "should support short keys and full keys"() {
        given:
        def props = new DynamicQueryProperties()
        props.setPreloadEnabled(true)
        props.setScanPackages(["sql/loader/*.yml"])
        def loader = new SqlQueryLoader(props)
        loader.afterPropertiesSet()

        expect:
        loader.getQuery("UserMapper.findUserById") == loader.getQuery("findUserById")
    }

    def "should throw QueryNotFoundException for unknown key"() {
        given:
        def props = new DynamicQueryProperties()
        props.setPreloadEnabled(true)
        props.setScanPackages(["sql/loader/EmptyMapper.yml"])
        def loader = new SqlQueryLoader(props)
        loader.afterPropertiesSet()

        when:
        loader.getQuery("EmptyMapper.nonExistsQuery")

        then:
        def ex = thrown(QueryNotFoundException)
        ex.message.contains("EmptyMapper.nonExistsQuery")
    }

    def "should validate SQL syntax and parameters"() {
        given:
        def props = new DynamicQueryProperties()
        props.setPreloadEnabled(true)
        props.getValidation().setValidateAtStartup(true)
        props.getValidation().setValidateSqlSyntax(true)
        props.getValidation().setValidateRequiredParameters(true)
        props.getValidation().setStrictMode(false) // no lanzar excepción global
        props.setScanPackages(["sql/loader/InvalidMapper.yml"]) // archivo en test/resources
        def loader = new SqlQueryLoader(props)

        when:
        loader.afterPropertiesSet()

        then:
        noExceptionThrown() // se loguea como inválida pero no truena en modo no estricto
    }

    def "should throw in strict mode when query is invalid"() {
        given:
        def props = new DynamicQueryProperties()
        props.setPreloadEnabled(true)
        props.getValidation().setValidateAtStartup(true)
        props.getValidation().setValidateSqlSyntax(true)
        props.getValidation().setStrictMode(true) // modo estricto
        props.setScanPackages(["sql/loader/InvalidMapper.yml"])
        def loader = new SqlQueryLoader(props)

        when:
        loader.afterPropertiesSet()

        then:
        thrown(InvalidQueryException)
    }

    def "should load query dynamically when preload disabled"() {
        given:
        def props = new DynamicQueryProperties()
        props.setPreloadEnabled(false)
        props.setBasePath("sql/loader")
        props.setYamlExtension("yml")
        def loader = new SqlQueryLoader(props)

        when:
        def query = loader.getQuery("UserMapper.findUserById")

        then:
        query.contains("SELECT")
    }

    def "should return LoaderStats with correct counts"() {
        given:
        def props = new DynamicQueryProperties()
        props.setPreloadEnabled(true)
        props.setScanPackages(["sql/loader/*.yml"])
        def loader = new SqlQueryLoader(props)
        loader.afterPropertiesSet()

        when:
        def stats = loader.getStats()

        then:
        stats.totalQueries > 0
        stats.totalNamespaces >= 1
        stats.toString().contains("LoaderStats")
    }

    def "should handle duplicate query keys"() {
        given:
        def props = new DynamicQueryProperties()
        props.setPreloadEnabled(true)
        props.setScanPackages(["sql/loader/DuplicateMapper.yml"])
        def loader = new SqlQueryLoader(props)

        when:
        loader.afterPropertiesSet()

        then:
        loader.getQuery("DupMapper.findUserById") != null
        loader.hasQuery("findUserById")
    }
}
