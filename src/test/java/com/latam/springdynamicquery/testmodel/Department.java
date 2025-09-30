package com.latam.springdynamicquery.testmodel;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de prueba para Department.
 */
@Entity
@Table(name = "departments")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Department {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;
    
    @Column(name = "active")
    private Boolean active = true;
    
    @Column(name = "created_date")
    private LocalDateTime createdDate = LocalDateTime.now();
    
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();
}
