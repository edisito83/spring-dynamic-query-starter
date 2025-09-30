package com.latam.springdynamicquery.testmodel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "guias_despacho", indexes = {
    @Index(name = "idx_guias_numero", columnList = "numero_guia"),
    @Index(name = "idx_guias_estado", columnList = "estado"),
    @Index(name = "idx_guias_cliente", columnList = "cliente_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuiaDespacho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_guia", nullable = false, length = 50)
    private String numeroGuia;

    @Column(length = 30)
    private String estado = "CREADA";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_despacho")
    private LocalDateTime fechaDespacho;

    @Column
    private Boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    // ✅ Relación con NumeroAwb
    @OneToMany(mappedBy = "guia", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NumeroAwb> numerosAwb = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    // ✅ Helper method para agregar números AWB
    public void addNumeroAwb(NumeroAwb numeroAwb) {
        numerosAwb.add(numeroAwb);
        numeroAwb.setGuia(this);
    }

    public void removeNumeroAwb(NumeroAwb numeroAwb) {
        numerosAwb.remove(numeroAwb);
        numeroAwb.setGuia(null);
    }
}