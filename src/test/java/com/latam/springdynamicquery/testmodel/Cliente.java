package com.latam.springdynamicquery.testmodel;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Modelo de prueba para Cliente.
 */
@Entity
@Table(name = "clientes")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Cliente {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;
    
    @Column(name = "rut", unique = true, length = 20)
    private String rut;
    
    @Column(name = "email", length = 255)
    private String email;
    
    @Column(name = "telefono", length = 50)
    private String telefono;
    
    @Column(name = "activo")
    private Boolean activo = true;
    
    @Column(name = "created_date")
    private LocalDateTime createdDate = LocalDateTime.now();
}