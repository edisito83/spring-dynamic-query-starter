package com.latam.springdynamicquery.testmodel;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Modelo de prueba para Order.
 */
@Entity
@Table(name = "orders")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;
    
    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;
    
    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;
    
    @Column(name = "status", length = 20)
    private String status = "PENDING";
    
    @Column(name = "created_date")
    private LocalDateTime createdDate = LocalDateTime.now();
}
