package com.latam.springdynamicquery.testrepository;

import com.latam.springdynamicquery.core.criteria.FilterCriteria;
import com.latam.springdynamicquery.repository.DynamicRepository;
import com.latam.springdynamicquery.testmodel.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository de prueba para User.
 */
@Repository
public interface TestUserRepository extends DynamicRepository<User, Long> {
    
    // ==================== Query Methods ====================
    List<User> findByNameContaining(String name);
    Optional<User> findByEmail(String email);
    List<User> findByActiveTrue();
    List<User> findByStatus(String status);
    
    // ==================== @Query JPQL ====================
    @Query("SELECT u FROM User u WHERE u.name LIKE %:name% AND u.active = true")
    List<User> findActiveUsersByNameJpql(@Param("name") String name);
    
    // ==================== @Query Native ====================
    @Query(value = "SELECT * FROM users u WHERE u.status = :status", nativeQuery = true)
    List<User> findUsersByStatusNative(@Param("status") String status);
    
    // ==================== Métodos dinámicos usando YAML ====================
    default Optional<User> findUserByIdFromYaml(Long id) {
        if (id == null) return Optional.empty();
        
        Map<String, Object> params = Map.of("id", id);
        
        return executeSingleResultWithParams("UserMapper.findUserById", params);
    }
    
    default List<User> findAllActiveUsersFromYaml() {
        return executeNamedQuery("UserMapper.findActiveUsers", Map.of());
    }
    
    default List<User> findUsersWithComplexSearch(String name, String email, List<Long> departmentIds, Integer minAge) {
        Map<String, FilterCriteria> filters = Map.of(
            "name", FilterCriteria.whenNotEmpty("u.name LIKE :name", "%" + name + "%"),
            "email", FilterCriteria.whenNotEmpty("u.email = :email", email),
            "departmentIds", FilterCriteria.when("u.department_id IN :departmentIds", departmentIds),
            "minAge", FilterCriteria.whenNumericPositive("u.age >= :minAge", minAge)
        );
        
        return executeNamedQuery("UserMapper.findUsersWithComplexSearch", filters);
    }
    
    default Long countUsersByStatus(String status) {
        Map<String, FilterCriteria> filters = Map.of(
            "status", FilterCriteria.when("u.status = :status", status)
        );
        
        return executeSingleResult("UserMapper.countUsersByStatus", filters, Long.class);
    }
}