package com.latam.springdynamicquery.testmodel;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Modelo de prueba para GuiaDespacho (tu caso específico).
 */
@Entity
@Table(name = "guias_despacho")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GuiaDespacho {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @Column(name = "numero_guia", nullable = false, length = 50)
    private String numeroGuia;
    
    @Column(name = "estado", length = 30)
    private String estado = "CREADA";
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;
    
    @Column(name = "cliente_id", insertable = false, updatable = false)
    private Long clienteId;
    
    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();
    
    @Column(name = "fecha_despacho")
    private LocalDateTime fechaDespacho;
    
    @Column(name = "active")
    private Boolean active = true;
    
    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
}