# Spring Dynamic Query Starter

[![CI](https://github.com/latam/spring-dynamic-query-starter/workflows/CI/badge.svg)](https://github.com/latam/spring-dynamic-query-starter/actions)
[![Maven Central](https://img.shields.io/maven-central/v/com.latam/spring-dynamic-query-starter.svg)](https://search.maven.org/artifact/com.latam/spring-dynamic-query-starter)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Coverage](https://img.shields.io/codecov/c/github/latam/spring-dynamic-query-starter)](https://codecov.io/gh/latam/spring-dynamic-query-starter)

A Spring Boot starter that provides MyBatis-like dynamic SQL queries using YAML configuration with full Spring Data JPA integration.

## Features

- 🚀 **Zero Configuration**: Auto-configures with Spring Boot
- 📝 **YAML Queries**: Define queries in YAML files similar to MyBatis XML
- ⚡ **Dynamic Filtering**: Conditional query building with FilterCriteria
- 🔧 **Full JPA Integration**: Works alongside standard Spring Data JPA repositories
- 🧪 **Testing Support**: Built-in testing utilities and mocks
- 📊 **Multiple Databases**: Supports PostgreSQL, MySQL, H2, HSQLDB, and more

## Quick Start

### 1. Add Dependency

```gradle
implementation 'com.latam:spring-dynamic-query-starter:1.0.0'
```

### 2. Configure Application

```yaml
app:
  dynamic-query:
    enabled: true
    scan-packages:
      - "sql/*.yml"
```

### 3. Create Query YAML

```yaml
# src/main/resources/sql/UserMapper.yml
namespace: com.company.mapper.UserMapper
queries:
  - id: findUsers
    sql: |
      SELECT * FROM users u
```

### 4. Use in Repository

```java
@Repository
public interface UserRepository extends DynamicRepository<User, Long> {
    
    default List<User> findUsersByName(String name) {
        Map<String, FilterCriteria> filters = Map.of(
            "name", FilterCriteria.whenNotEmpty("u.name LIKE :name", "%" + name + "%")
        );
        return executeNamedQuery("UserMapper.findUsers", filters);
    }
}
```

## Documentation

- [User Guide](docs/user-guide.md)
- [Configuration Reference](docs/configuration.md)
- [Migration from MyBatis](docs/mybatis-migration.md)
- [API Documentation](docs/api.md)

## Requirements

- Java 21+
- Spring Boot 3.5+
- Spring Data JPA 3.3+

## License

Apache License 2.0

# ==================== LICENSE ====================
Apache License
Version 2.0, January 2004
http://www.apache.org/licenses/

[... resto de la licencia Apache 2.0 ...]