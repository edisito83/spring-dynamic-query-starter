package com.latam.springdynamicquery.testmodel;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "numero_awb", indexes = {
    @Index(name = "idx_numero_awb_numero", columnList = "numero"),
    @Index(name = "idx_numero_awb_guia_id", columnList = "guia_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NumeroAwb {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guia_id", nullable = false, foreignKey = @ForeignKey(name = "fk_numero_awb_guia"))
    private GuiaDespacho guia;

    @Column(nullable = false, length = 50)
    private String numero;

    @Column(length = 20)
    private String tipo = "AWB";

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }
}